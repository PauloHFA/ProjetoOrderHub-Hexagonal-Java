package com.orderhub.application.usecase;

import com.orderhub.application.port.in.ProcessarWebhookDePagamentoUseCase;
import com.orderhub.application.port.out.GeradorDeId;
import com.orderhub.application.port.out.PedidoRepository;
import com.orderhub.application.port.out.PublicadorDeEventos;
import com.orderhub.application.port.out.Relogio;
import com.orderhub.application.port.out.UnitOfWork;
import com.orderhub.application.shared.Result;
import com.orderhub.domain.pedido.Pedido;
import com.orderhub.domain.pedido.StatusDoPedido;
import com.orderhub.domain.shared.DomainError;
import com.orderhub.domain.shared.Identificadores.ReservaId;
import com.orderhub.domain.shared.Instante;

public class ProcessarWebhookDePagamento implements ProcessarWebhookDePagamentoUseCase {

    private final PedidoRepository pedidoRepository;
    private final UnitOfWork unitOfWork;
    private final PublicadorDeEventos publicadorDeEventos;
    private final GeradorDeId geradorDeId;
    private final Relogio relogio;

    public ProcessarWebhookDePagamento(
        PedidoRepository pedidoRepository,
        UnitOfWork unitOfWork,
        PublicadorDeEventos publicadorDeEventos,
        GeradorDeId geradorDeId,
        Relogio relogio
    ) {
        this.pedidoRepository = pedidoRepository;
        this.unitOfWork = unitOfWork;
        this.publicadorDeEventos = publicadorDeEventos;
        this.geradorDeId = geradorDeId;
        this.relogio = relogio;
    }

    @Override
    public Result<ProcessarWebhookDePagamentoResult, Exception> executar(ProcessarWebhookDePagamentoCommand cmd) {
        try {
            return unitOfWork.executar(() -> {
                Pedido pedido = pedidoRepository.buscarPorId(cmd.pedidoId())
                    .orElseThrow(() -> DomainError.NotFoundException.paraRecurso("Pedido", cmd.pedidoId().getValor()));

                boolean pagamentoProcessado = false;
                Instante agora = cmd.processadoEm() != null ? cmd.processadoEm() : relogio.agora();

                switch (cmd.status().toUpperCase()) {
                    case "APROVADO" -> {
                        // Idempotência: se já está pago ou entregue, não faz nada
                        if (pedido.getStatus() == StatusDoPedido.PAGO ||
                            pedido.getStatus() == StatusDoPedido.ENVIADO ||
                            pedido.getStatus() == StatusDoPedido.ENTREGUE) {
                            pagamentoProcessado = true;
                            break;
                        }

                        // Se ainda estiver em CRIADO, primeiro reserva estoque
                        if (pedido.getStatus() == StatusDoPedido.CRIADO) {
                            ReservaId reservaId = geradorDeId.gerarReservaId();
                            pedido.reservarEstoque(reservaId, agora);
                        }

                        pedido.confirmarPagamento(cmd.pagamentoId(), cmd.valor(), agora);
                        pagamentoProcessado = true;
                    }
                    case "RECUSADO" -> {
                        if (pedido.getStatus() == StatusDoPedido.CRIADO || pedido.getStatus() == StatusDoPedido.ESTOQUE_RESERVADO) {
                            pedido.cancelar("Pagamento recusado via webhook", false, agora);
                            pagamentoProcessado = true;
                        }
                    }
                    case "PENDENTE" -> {
                        pagamentoProcessado = true; // Apenas registra o recebimento sem alterar estado
                    }
                    default -> throw new DomainError.InvalidArgumentException("Status de webhook desconhecido: " + cmd.status());
                }

                if (pagamentoProcessado && !pedido.getEventos().isEmpty()) {
                    pedidoRepository.salvar(pedido);
                    publicadorDeEventos.publicar(pedido.getEventos());
                    pedido.limparEventos();
                }

                return Result.ok(new ProcessarWebhookDePagamentoResult(
                    pedido.getId(),
                    pedido.getStatus(),
                    pagamentoProcessado
                ));
            });
        } catch (DomainError e) {
            return Result.err(e);
        } catch (Exception e) {
            return Result.err(e);
        }
    }
}

