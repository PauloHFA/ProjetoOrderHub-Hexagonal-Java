package com.orderhub.adapters.out.messaging;

import com.orderhub.application.shared.Result;
import com.orderhub.domain.shared.EventosDeDominio.EventoDeDominio;

import java.util.List;

/**
 * Transactional Outbox Relay (ARQ-50).
 */
public class OutboxRelay {

    public interface OutboxStorage {
        List<EventoDeDominio> buscarEventosPendentes(int limite);
        void marcarComoPublicado(EventoDeDominio evento);
    }

    public interface MessageBroker {
        void enviar(String topico, Object payload);
    }

    private final OutboxStorage storage;
    private final MessageBroker broker;

    public OutboxRelay(OutboxStorage storage, MessageBroker broker) {
        this.storage = storage;
        this.broker = broker;
    }

    public Result<Integer, Exception> processarLote(int limite) {
        try {
            List<EventoDeDominio> pendentes = storage.buscarEventosPendentes(limite);
            int publicados = 0;

            for (EventoDeDominio evento : pendentes) {
                String topico = "pedidos." + evento.getTipo();
                broker.enviar(topico, evento);
                storage.marcarComoPublicado(evento);
                publicados++;
            }

            return Result.ok(publicados);
        } catch (Exception e) {
            return Result.err(e);
        }
    }
}

