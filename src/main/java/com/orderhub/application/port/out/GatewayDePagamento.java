package com.orderhub.application.port.out;

import com.orderhub.application.shared.Result;
import com.orderhub.domain.shared.Dinheiro;
import com.orderhub.domain.shared.Identificadores.PagamentoId;
import com.orderhub.domain.shared.Instante;

/**
 * Porta de saída para o Gateway de Pagamento (ARQ-30, ARQ-33).
 * Definida estritamente na linguagem do domínio, sem vazar SDKs externos.
 */
public interface GatewayDePagamento {

    record AutorizarPagamentoCommand(
        PagamentoId pagamentoId,
        Dinheiro valor,
        String meioDePagamento,
        String chaveDeIdempotencia
    ) {}

    record AutorizarPagamentoResult(
        PagamentoId pagamentoId,
        boolean aprovada,
        String motivoRecusa,
        Instante processadoEm
    ) {}

    record CapturarPagamentoResult(
        PagamentoId pagamentoId,
        boolean capturado,
        Dinheiro valorCapturado
    ) {}

    record EstornarPagamentoResult(
        PagamentoId pagamentoId,
        boolean estornado,
        Dinheiro valorEstornado
    ) {}

    Result<AutorizarPagamentoResult, Exception> autorizar(AutorizarPagamentoCommand cmd);
    Result<CapturarPagamentoResult, Exception> capturar(PagamentoId pagamentoId, Dinheiro valor);
    Result<EstornarPagamentoResult, Exception> estornar(PagamentoId pagamentoId, Dinheiro valor, String motivo);
}

