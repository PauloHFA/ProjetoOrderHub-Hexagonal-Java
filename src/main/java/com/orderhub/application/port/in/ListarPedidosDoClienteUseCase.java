package com.orderhub.application.port.in;

import com.orderhub.application.shared.Result;
import com.orderhub.domain.pedido.Pedido;
import com.orderhub.domain.shared.Identificadores.ClienteId;

import java.util.List;

/**
 * Porta de entrada para listagem paginada de pedidos do cliente (CQS - ARQ-23).
 */
public interface ListarPedidosDoClienteUseCase {

    record ListarPedidosDoClienteCommand(
        ClienteId clienteId,
        int pagina,
        int tamanho
    ) {}

    record ListarPedidosDoClienteResult(
        List<Pedido> pedidos,
        int total,
        int pagina,
        int tamanho
    ) {}

    Result<ListarPedidosDoClienteResult, Exception> executar(ListarPedidosDoClienteCommand cmd);
}

