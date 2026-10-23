package com.orderhub.application.port.out;

import com.orderhub.domain.shared.Identificadores.PedidoId;
import com.orderhub.domain.shared.Identificadores.ReservaId;
import com.orderhub.domain.shared.Identificadores.PagamentoId;

/**
 * Porta de saída para geração de identificadores (ARQ-34).
 */
public interface GeradorDeId {
    PedidoId gerarPedidoId();
    ReservaId gerarReservaId();
    PagamentoId gerarPagamentoId();
}

