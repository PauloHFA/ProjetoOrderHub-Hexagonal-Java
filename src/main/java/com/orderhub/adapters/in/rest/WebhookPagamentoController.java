package com.orderhub.adapters.in.rest;

import com.orderhub.application.port.in.ProcessarWebhookDePagamentoUseCase;
import com.orderhub.application.port.in.ProcessarWebhookDePagamentoUseCase.ProcessarWebhookDePagamentoCommand;
import com.orderhub.domain.shared.Dinheiro;
import com.orderhub.domain.shared.DomainError;
import com.orderhub.domain.shared.Identificadores.PagamentoId;
import com.orderhub.domain.shared.Identificadores.PedidoId;
import com.orderhub.domain.shared.Instante;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class WebhookPagamentoController {

    private final ProcessarWebhookDePagamentoUseCase webhookUseCase;

    public WebhookPagamentoController(ProcessarWebhookDePagamentoUseCase webhookUseCase) {
        this.webhookUseCase = webhookUseCase;
    }

    public record PrecoWebhookRequest(Double valor, String moeda) {}
    public record WebhookRequest(
        String pagamentoId,
        String pedidoId,
        String status,
        PrecoWebhookRequest valor,
        String processadoEm,
        Map<String, Object> dadosExternos
    ) {}

    @PostMapping("/webhooks/pagamento")
    public ResponseEntity<?> processarWebhook(@RequestBody WebhookRequest req) {
        try {
            var cmd = new ProcessarWebhookDePagamentoCommand(
                PagamentoId.criar(req.pagamentoId()),
                PedidoId.criar(req.pedidoId()),
                req.status(),
                Dinheiro.criar(req.valor().valor(), req.valor().moeda() != null ? req.valor().moeda() : "BRL"),
                req.processadoEm() != null ? Instante.de(req.processadoEm()) : null,
                req.dadosExternos() != null ? req.dadosExternos() : Map.of()
            );

            var res = webhookUseCase.executar(cmd);
            if (res.isOk()) {
                var v = res.getValue();
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", Map.of(
                        "pedidoId", v.pedidoId().getValor(),
                        "status", v.status().name(),
                        "pagamentoProcessado", v.pagamentoProcessado()
                    )
                ));
            }

            Exception e = res.getError();
            if (e instanceof DomainError.NotFoundException) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "success", false,
                    "error", Map.of("code", "NOT_FOUND", "message", e.getMessage())
                ));
            }

            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", Map.of("code", "BUSINESS_RULE_VIOLATION", "message", e.getMessage())
            ));
        } catch (DomainError e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", Map.of("code", e.getCode(), "message", e.getMessage())
            ));
        }
    }
}

