package com.orderhub.application.usecase;

import com.orderhub.application.port.in.ReservarEstoqueUseCase;
import com.orderhub.application.port.out.PedidoRepository;
import com.orderhub.application.port.out.PublicadorDeEventos;
import com.orderhub.application.port.out.Relogio;
import com.orderhub.application.port.out.UnitOfWork;
import com.orderhub.application.shared.Result;
import com.orderhub.domain.pedido.Pedido;
import com.orderhub.domain.shared.DomainError;

public class ReservarEstoque implements ReservarEstoqueUseCase {

    private final PedidoRepository pedidoRepository;
    private final UnitOfWork unitOfWork;
    private final PublicadorDeEventos publicadorDeEventos;
    private final Relogio relogio;

    public ReservarEstoque(
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
    public Result<ReservarEstoqueResult, Exception> executar(ReservarEstoqueCommand cmd) {
        try {
            return unitOfWork.executar(() -> {
                Pedido pedido = pedidoRepository.buscarPorId(cmd.pedidoId())
                    .orElseThrow(() -> DomainError.NotFoundException.paraRecurso("Pedido", cmd.pedidoId().getValor()));

                pedido.reservarEstoque(cmd.reservaId(), relogio.agora());

                pedidoRepository.salvar(pedido);

                publicadorDeEventos.publicar(pedido.getEventos());
                pedido.limparEventos();

                return Result.ok(new ReservarEstoqueResult(
                    pedido.getId(),
                    pedido.getStatus(),
                    cmd.reservaId()
                ));
            });
        } catch (DomainError e) {
            return Result.err(e);
        } catch (Exception e) {
            return Result.err(e);
        }
    }
}

