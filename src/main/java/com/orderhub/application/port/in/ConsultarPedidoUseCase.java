package com.orderhub.application.port.in;

import com.orderhub.application.shared.Result;
import com.orderhub.domain.pedido.Pedido;
import com.orderhub.domain.shared.Identificadores.PedidoId;

/**
 * Porta de entrada para consulta de pedido (CQS - ARQ-23).
 */
public interface ConsultarPedidoUseCase {

    record ConsultarPedidoCommand(PedidoId pedidoId) {}

    record ConsultarPedidoResult(Pedido pedido) {}

    Result<ConsultarPedidoResult, Exception> executar(ConsultarPedidoCommand cmd);
}

