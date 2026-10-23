package com.orderhub.application.port.in;

import com.orderhub.application.shared.Result;
import com.orderhub.domain.pedido.StatusDoPedido;
import com.orderhub.domain.shared.Dinheiro;
import com.orderhub.domain.shared.Email;
import com.orderhub.domain.shared.Identificadores.ClienteId;
import com.orderhub.domain.shared.Identificadores.PedidoId;
import com.orderhub.domain.shared.Identificadores.Sku;
import com.orderhub.domain.shared.Instante;
import com.orderhub.domain.shared.Quantidade;

import java.util.List;

/**
 * Porta de entrada para criação de pedidos (ARQ-20, ARQ-22).
 */
public interface CriarPedidoUseCase {

    record ItemCommand(Sku sku, Quantidade quantidade, Dinheiro precoUnitario) {}

    record CriarPedidoCommand(
        ClienteId clienteId,
        Email clienteEmail,
        List<ItemCommand> itens,
        String idempotencyKey
    ) {}

    record CriarPedidoResult(
        PedidoId pedidoId,
        StatusDoPedido status,
        Dinheiro total,
        Instante criadoEm
    ) {}

    Result<CriarPedidoResult, Exception> executar(CriarPedidoCommand cmd);
}

