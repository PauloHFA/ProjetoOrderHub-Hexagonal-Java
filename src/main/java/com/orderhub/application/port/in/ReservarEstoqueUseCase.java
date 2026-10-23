package com.orderhub.application.port.in;

import com.orderhub.application.shared.Result;
import com.orderhub.domain.pedido.StatusDoPedido;
import com.orderhub.domain.shared.Identificadores.PedidoId;
import com.orderhub.domain.shared.Identificadores.ReservaId;

/**
 * Porta de entrada para reserva de estoque (ARQ-20, ARQ-22).
 */
public interface ReservarEstoqueUseCase {

    record ReservarEstoqueCommand(
        PedidoId pedidoId,
        ReservaId reservaId
    ) {}

    record ReservarEstoqueResult(
        PedidoId pedidoId,
        StatusDoPedido status,
        ReservaId reservaId
    ) {}

    Result<ReservarEstoqueResult, Exception> executar(ReservarEstoqueCommand cmd);
}

