package com.orderhub.application.port.in;

import com.orderhub.application.shared.Result;
import com.orderhub.domain.pedido.StatusDoPedido;
import com.orderhub.domain.shared.Dinheiro;
import com.orderhub.domain.shared.Identificadores.PagamentoId;
import com.orderhub.domain.shared.Identificadores.PedidoId;
import com.orderhub.domain.shared.Instante;

/**
 * Porta de entrada para confirmação de pagamento (ARQ-20, ARQ-22).
 */
public interface ConfirmarPagamentoUseCase {

    record ConfirmarPagamentoCommand(
        PedidoId pedidoId,
        PagamentoId pagamentoId,
        Dinheiro valor,
        Instante pagoEm
    ) {}

    record ConfirmarPagamentoResult(
        PedidoId pedidoId,
        StatusDoPedido status,
        PagamentoId pagamentoId
    ) {}

    Result<ConfirmarPagamentoResult, Exception> executar(ConfirmarPagamentoCommand cmd);
}

