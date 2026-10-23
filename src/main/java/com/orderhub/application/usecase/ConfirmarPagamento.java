package com.orderhub.application.usecase;

import com.orderhub.application.port.in.ConfirmarPagamentoUseCase;
import com.orderhub.application.port.out.PedidoRepository;
import com.orderhub.application.port.out.PublicadorDeEventos;
import com.orderhub.application.port.out.Relogio;
import com.orderhub.application.port.out.UnitOfWork;
import com.orderhub.application.shared.Result;
import com.orderhub.domain.pedido.Pedido;
import com.orderhub.domain.shared.DomainError;

public class ConfirmarPagamento implements ConfirmarPagamentoUseCase {

    private final PedidoRepository pedidoRepository;
    private final UnitOfWork unitOfWork;
    private final PublicadorDeEventos publicadorDeEventos;
    private final Relogio relogio;

    public ConfirmarPagamento(
        PedidoRepository pedidoRepository,
        UnitOfWork unitOfWork,
        PublicadorDeEventos publicadorDeEventos,
        Relogio relogio
    ) {
        this.pedidoRepository = pedidoRepository;
        this.unitOfWork = unitOfWork;
        this.publicadorDeEventos = publicadorDeEventos;
        this.relogio = relogio;
    }

    @Override
    public Result<ConfirmarPagamentoResult, Exception> executar(ConfirmarPagamentoCommand cmd) {
        try {
            return unitOfWork.executar(() -> {
                Pedido pedido = pedidoRepository.buscarPorId(cmd.pedidoId())
                    .orElseThrow(() -> DomainError.NotFoundException.paraRecurso("Pedido", cmd.pedidoId().getValor()));

                pedido.confirmarPagamento(cmd.pagamentoId(), cmd.valor(), cmd.pagoEm() != null ? cmd.pagoEm() : relogio.agora());

                pedidoRepository.salvar(pedido);

                publicadorDeEventos.publicar(pedido.getEventos());
                pedido.limparEventos();

                return Result.ok(new ConfirmarPagamentoResult(
                    pedido.getId(),
                    pedido.getStatus(),
                    cmd.pagamentoId()
                ));
            });
        } catch (DomainError e) {
            return Result.err(e);
        } catch (Exception e) {
            return Result.err(e);
        }
    }
}

