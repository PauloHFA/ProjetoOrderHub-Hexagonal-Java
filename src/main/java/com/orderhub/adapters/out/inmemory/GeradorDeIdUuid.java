package com.orderhub.adapters.out.inmemory;

import com.orderhub.application.port.out.GeradorDeId;
import com.orderhub.domain.shared.Identificadores.PagamentoId;
import com.orderhub.domain.shared.Identificadores.PedidoId;
import com.orderhub.domain.shared.Identificadores.ReservaId;

public class GeradorDeIdUuid implements GeradorDeId {

    @Override
    public PedidoId gerarPedidoId() {
        return PedidoId.gerar();
    }

    @Override
    public ReservaId gerarReservaId() {
        return ReservaId.gerar();
    }

    @Override
    public PagamentoId gerarPagamentoId() {
        return PagamentoId.gerar();
    }
}

