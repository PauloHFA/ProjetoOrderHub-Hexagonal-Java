package com.orderhub.adapters.in.messaging;

import com.orderhub.application.port.in.ProcessarWebhookDePagamentoUseCase;
import com.orderhub.application.port.in.ProcessarWebhookDePagamentoUseCase.ProcessarWebhookDePagamentoCommand;
import com.orderhub.application.shared.Result;
import com.orderhub.domain.shared.Dinheiro;
import com.orderhub.domain.shared.Identificadores.PagamentoId;
import com.orderhub.domain.shared.Identificadores.PedidoId;
import com.orderhub.domain.shared.Instante;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Consumidor de Eventos e Mensagens com tolerância a duplicidade (ARQ-52).
 */
public class EventConsumer {

    public record MensagemFila(
        String id,
        String topico,
        String timestamp,
        Map<String, Object> conteudo
    ) {}

    private final ProcessarWebhookDePagamentoUseCase webhookUseCase;
    private final Set<String> mensagensProcessadas = ConcurrentHashMap.newKeySet();

    public EventConsumer(ProcessarWebhookDePagamentoUseCase webhookUseCase) {
        this.webhookUseCase = webhookUseCase;
    }

    public Result<Boolean, Exception> consumir(MensagemFila mensagem) {
        // Tolerância a duplicidade
        if (mensagensProcessadas.contains(mensagem.id())) {
            return Result.ok(false);
        }

        try {
            if ("pagamentos.atualizacao".equals(mensagem.topico())) {
                String pagamentoId = (String) mensagem.conteudo().get("pagamentoId");
                String pedidoId = (String) mensagem.conteudo().get("pedidoId");
                String status = (String) mensagem.conteudo().get("status");
                Double valorNum = ((Number) mensagem.conteudo().get("valor")).doubleValue();

                ProcessarWebhookDePagamentoCommand cmd = new ProcessarWebhookDePagamentoCommand(
                    PagamentoId.criar(pagamentoId),
                    PedidoId.criar(pedidoId),
                    status,
                    Dinheiro.criar(valorNum),
                    Instante.de(mensagem.timestamp()),
                    mensagem.conteudo()
                );

                var res = webhookUseCase.executar(cmd);
                if (res.isErr()) {
                    return Result.err(res.getError());
                }
            }

            mensagensProcessadas.add(mensagem.id());
            return Result.ok(true);
        } catch (Exception e) {
            return Result.err(e);
        }
    }

    public boolean jaProcessada(String id) {
        return mensagensProcessadas.contains(id);
    }
}

