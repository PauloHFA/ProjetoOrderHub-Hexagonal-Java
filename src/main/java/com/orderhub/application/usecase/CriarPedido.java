package com.orderhub.application.usecase;

import com.orderhub.application.port.in.CriarPedidoUseCase;
import com.orderhub.application.port.out.GeradorDeId;
import com.orderhub.application.port.out.PedidoRepository;
import com.orderhub.application.port.out.PublicadorDeEventos;
import com.orderhub.application.port.out.Relogio;
import com.orderhub.application.port.out.UnitOfWork;
import com.orderhub.application.shared.Result;
import com.orderhub.domain.pedido.ItemDoPedido;
import com.orderhub.domain.pedido.Pedido;
import com.orderhub.domain.shared.DomainError;
import com.orderhub.domain.shared.Identificadores.PedidoId;
import com.orderhub.domain.shared.Instante;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Caso de Uso: Criar Pedido (ARQ-20, ARQ-21).
 * Puro: sem dependências de frameworks, injeção estritamente por construtor (ARQ-60).
 */
public class CriarPedido implements CriarPedidoUseCase {

    private final PedidoRepository pedidoRepository;
    private final UnitOfWork unitOfWork;
    private final PublicadorDeEventos publicadorDeEventos;
    private final GeradorDeId geradorDeId;
    private final Relogio relogio;

    public CriarPedido(
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
    public Result<CriarPedidoResult, Exception> executar(CriarPedidoCommand cmd) {
        // Validação de consistência da requisição (Camada de Aplicação - ARQ-41)
        if (cmd.itens() == null || cmd.itens().isEmpty()) {
            return Result.err(new DomainError.BusinessRuleViolationException("Pedido deve conter pelo menos um item"));
        }

        Set<String> skus = new HashSet<>();
        for (var itemCmd : cmd.itens()) {
            if (!skus.add(itemCmd.sku().getValor())) {
                return Result.err(new DomainError.BusinessRuleViolationException("SKU duplicado no pedido: " + itemCmd.sku().getValor()));
            }
        }

        // Idempotência
        if (cmd.idempotencyKey() != null && !cmd.idempotencyKey().isBlank()) {
            if (pedidoRepository.existeChaveIdempotencia(cmd.idempotencyKey())) {
                return Result.err(new DomainError.IdempotencyConflictException("Chave de idempotência já utilizada: " + cmd.idempotencyKey()));
            }
        }

        try {
            return unitOfWork.executar(() -> {
                PedidoId pedidoId = geradorDeId.gerarPedidoId();

                if (pedidoRepository.existePorId(pedidoId)) {
                    throw new DomainError.BusinessRuleViolationException("ID de pedido já existe: " + pedidoId.getValor());
                }

                Instante agora = relogio.agora();

                List<ItemDoPedido> itens = new ArrayList<>();
                for (var itemCmd : cmd.itens()) {
                    itens.add(ItemDoPedido.criar(itemCmd.sku(), itemCmd.quantidade(), itemCmd.precoUnitario()));
                }

                Pedido pedido = Pedido.criar(pedidoId, cmd.clienteId(), cmd.clienteEmail(), itens, agora);

                pedidoRepository.salvar(pedido);

                if (cmd.idempotencyKey() != null && !cmd.idempotencyKey().isBlank()) {
                    pedidoRepository.registrarChaveIdempotencia(cmd.idempotencyKey(), pedido.getId(), 86400L);
                }

                publicadorDeEventos.publicar(pedido.getEventos());
                pedido.limparEventos();

                return Result.ok(new CriarPedidoResult(
                    pedido.getId(),
                    pedido.getStatus(),
                    pedido.total(),
                    pedido.getCriadoEm()
                ));
            });
        } catch (DomainError e) {
            return Result.err(e);
        } catch (Exception e) {
            return Result.err(e);
        }
    }
}

