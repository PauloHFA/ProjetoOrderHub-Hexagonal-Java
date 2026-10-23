package com.orderhub.adapters.out.payment;

import com.orderhub.application.port.out.GatewayDePagamento;
import com.orderhub.application.shared.Result;
import com.orderhub.domain.shared.Dinheiro;
import com.orderhub.domain.shared.Identificadores.PagamentoId;
import com.orderhub.domain.shared.Instante;

import java.time.Instant;

/**
 * Anti-Corruption Layer (ACL) para o Gateway de Pagamento (ARQ-44).
 * Isola o domínio das particularidades de provedores externos (ex: Stripe, MercadoPago).
 */
public class GatewayDePagamentoACL implements GatewayDePagamento {

    public interface ProvedorPagamentoExterno {
        TransacaoExternaResponse autorizar(TransacaoExternaRequest request);
        TransacaoExternaResponse capturar(String id, long valorCentavos);
        TransacaoExternaResponse estornar(String id, long valorCentavos, String motivo);
    }

    public record TransacaoExternaRequest(
        String id,
        long valorCentavos,
        String moeda,
        String metodo,
        String idempotencyKey
    ) {}

    public record TransacaoExternaResponse(
        String id,
        String status, // "APPROVED", "DECLINED", "ERROR"
        String motivoRecusa,
        long valorCentavos,
        String processadoEmIso
    ) {}

    private final ProvedorPagamentoExterno provedor;

    public GatewayDePagamentoACL(ProvedorPagamentoExterno provedor) {
        this.provedor = provedor;
    }

    @Override
    public Result<AutorizarPagamentoResult, Exception> autorizar(AutorizarPagamentoCommand cmd) {
        try {
            long centavos = cmd.valor().getValor().multiply(java.math.BigDecimal.valueOf(100)).longValue();
            TransacaoExternaRequest request = new TransacaoExternaRequest(
                cmd.pagamentoId().getValor(),
                centavos,
                cmd.valor().getMoeda(),
                cmd.meioDePagamento(),
                cmd.chaveDeIdempotencia()
            );

            TransacaoExternaResponse response = provedor.autorizar(request);
            boolean aprovada = "APPROVED".equalsIgnoreCase(response.status());

            return Result.ok(new AutorizarPagamentoResult(
                PagamentoId.criar(response.id()),
                aprovada,
                response.motivoRecusa(),
                response.processadoEmIso() != null ? Instante.de(response.processadoEmIso()) : Instante.de(Instant.now())
            ));
        } catch (Exception e) {
            return Result.err(e);
        }
    }

    @Override
    public Result<CapturarPagamentoResult, Exception> capturar(PagamentoId pagamentoId, Dinheiro valor) {
        try {
            long centavos = valor.getValor().multiply(java.math.BigDecimal.valueOf(100)).longValue();
            TransacaoExternaResponse response = provedor.capturar(pagamentoId.getValor(), centavos);
            return Result.ok(new CapturarPagamentoResult(
                pagamentoId,
                "APPROVED".equalsIgnoreCase(response.status()),
                valor
            ));
        } catch (Exception e) {
            return Result.err(e);
        }
    }

    @Override
    public Result<EstornarPagamentoResult, Exception> estornar(PagamentoId pagamentoId, Dinheiro valor, String motivo) {
        try {
            long centavos = valor.getValor().multiply(java.math.BigDecimal.valueOf(100)).longValue();
            TransacaoExternaResponse response = provedor.estornar(pagamentoId.getValor(), centavos, motivo);
            return Result.ok(new EstornarPagamentoResult(
                pagamentoId,
                "APPROVED".equalsIgnoreCase(response.status()),
                valor
            ));
        } catch (Exception e) {
            return Result.err(e);
        }
    }
}

