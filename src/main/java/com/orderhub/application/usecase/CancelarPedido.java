package com.orderhub.application.usecase;

import com.orderhub.application.port.in.CancelarPedidoUseCase;
import com.orderhub.application.port.out.PedidoRepository;
import com.orderhub.application.port.out.PublicadorDeEventos;
import com.orderhub.application.port.out.Relogio;
import com.orderhub.application.port.out.UnitOfWork;
import com.orderhub.application.shared.Result;
import com.orderhub.domain.pedido.Pedido;
import com.orderhub.domain.shared.DomainError;

public class CancelarPedido implements CancelarPedidoUseCase {

    private final PedidoRepository pedidoRepository;
    private final UnitOfWork unitOfWork;
    private final PublicadorDeEventos publicadorDeEventos;
    private final Relogio relogio;

    public CancelarPedido(
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
    public Result<CancelarPedidoResult, Exception> executar(CancelarPedidoCommand cmd) {
        try {
            return unitOfWork.executar(() -> {
                Pedido pedido = pedidoRepository.buscarPorId(cmd.pedidoId())
                    .orElseThrow(() -> DomainError.NotFoundException.paraRecurso("Pedido", cmd.pedidoId().getValor()));

                pedido.cancelar(cmd.motivo(), cmd.estornado(), relogio.agora());

                pedidoRepository.salvar(pedido);

                publicadorDeEventos.publicar(pedido.getEventos());
                pedido.limparEventos();

                return Result.ok(new CancelarPedidoResult(
                    pedido.getId(),
                    pedido.getStatus()
                ));
            });
        } catch (DomainError e) {
            return Result.err(e);
        } catch (Exception e) {
            return Result.err(e);
        }
    }
}

