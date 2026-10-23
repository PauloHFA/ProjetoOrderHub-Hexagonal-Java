package com.orderhub.application.usecase;

import com.orderhub.application.port.in.ListarPedidosDoClienteUseCase;
import com.orderhub.application.port.out.PedidoRepository;
import com.orderhub.application.shared.Result;
import com.orderhub.domain.pedido.Pedido;

import java.util.List;

public class ListarPedidosDoCliente implements ListarPedidosDoClienteUseCase {

    private final PedidoRepository pedidoRepository;

    public ListarPedidosDoCliente(PedidoRepository pedidoRepository) {
        this.pedidoRepository = pedidoRepository;
    }

    @Override
    public Result<ListarPedidosDoClienteResult, Exception> executar(ListarPedidosDoClienteCommand cmd) {
        int pagina = cmd.pagina() > 0 ? cmd.pagina() : 1;
        int tamanho = cmd.tamanho() > 0 ? cmd.tamanho() : 20;

        List<Pedido> pedidos = pedidoRepository.buscarPorClienteId(cmd.clienteId(), pagina, tamanho);
        int total = pedidoRepository.contarPorClienteId(cmd.clienteId());

        return Result.ok(new ListarPedidosDoClienteResult(pedidos, total, pagina, tamanho));
    }
}

