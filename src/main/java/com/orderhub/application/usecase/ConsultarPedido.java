package com.orderhub.application.usecase;

import com.orderhub.application.port.in.ConsultarPedidoUseCase;
import com.orderhub.application.port.out.PedidoRepository;
import com.orderhub.application.shared.Result;
import com.orderhub.domain.pedido.Pedido;
import com.orderhub.domain.shared.DomainError;

public class ConsultarPedido implements ConsultarPedidoUseCase {

    private final PedidoRepository pedidoRepository;

    public ConsultarPedido(PedidoRepository pedidoRepository) {
        this.pedidoRepository = pedidoRepository;
    }

    @Override
    public Result<ConsultarPedidoResult, Exception> executar(ConsultarPedidoCommand cmd) {
        return pedidoRepository.buscarPorId(cmd.pedidoId())
            .map(pedido -> Result.<ConsultarPedidoResult, Exception>ok(new ConsultarPedidoResult(pedido)))
            .orElseGet(() -> Result.err(DomainError.NotFoundException.paraRecurso("Pedido", cmd.pedidoId().getValor())));
    }
}

