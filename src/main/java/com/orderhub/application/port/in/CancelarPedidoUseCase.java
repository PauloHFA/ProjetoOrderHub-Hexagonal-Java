package com.orderhub.application.port.in;

import com.orderhub.application.shared.Result;
import com.orderhub.domain.pedido.StatusDoPedido;
import com.orderhub.domain.shared.Identificadores.PedidoId;

/**
 * Porta de entrada para cancelamento de pedidos (ARQ-20, ARQ-22).
 */
public interface CancelarPedidoUseCase {

    record CancelarPedidoCommand(
        PedidoId pedidoId,
        String motivo,
        boolean estornado
    ) {}

    record CancelarPedidoResult(
        PedidoId pedidoId,
        StatusDoPedido status
    ) {}

    Result<CancelarPedidoResult, Exception> executar(CancelarPedidoCommand cmd);
}

