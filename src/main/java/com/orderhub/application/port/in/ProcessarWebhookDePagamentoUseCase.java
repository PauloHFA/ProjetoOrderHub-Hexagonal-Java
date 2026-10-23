package com.orderhub.application.port.in;

import com.orderhub.application.shared.Result;
import com.orderhub.domain.pedido.StatusDoPedido;
import com.orderhub.domain.shared.Dinheiro;
import com.orderhub.domain.shared.Identificadores.PagamentoId;
import com.orderhub.domain.shared.Identificadores.PedidoId;
import com.orderhub.domain.shared.Instante;

import java.util.Map;

/**
 * Porta de entrada para processamento de webhook de pagamento assíncrono (ARQ-20, ARQ-51).
 */
public interface ProcessarWebhookDePagamentoUseCase {

    record ProcessarWebhookDePagamentoCommand(
        PagamentoId pagamentoId,
        PedidoId pedidoId,
        String status,
        Dinheiro valor,
        Instante processadoEm,
        Map<String, Object> dadosExternos
    ) {}

    record ProcessarWebhookDePagamentoResult(
        PedidoId pedidoId,
        StatusDoPedido status,
        boolean pagamentoProcessado
    ) {}

    Result<ProcessarWebhookDePagamentoResult, Exception> executar(ProcessarWebhookDePagamentoCommand cmd);
}

