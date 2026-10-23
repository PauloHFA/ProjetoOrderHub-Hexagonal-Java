package com.orderhub.adapters.out.inmemory;

import com.orderhub.application.port.out.PublicadorDeEventos;
import com.orderhub.domain.shared.EventosDeDominio.EventoDeDominio;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PublicadorDeEventosInMemory implements PublicadorDeEventos {

    private final List<EventoDeDominio> eventosPublicados = new ArrayList<>();

    @Override
    public void publicar(List<EventoDeDominio> eventos) {
        if (eventos != null) {
            this.eventosPublicados.addAll(eventos);
        }
    }

    public List<EventoDeDominio> getEventosPublicados() {
        return Collections.unmodifiableList(eventosPublicados);
    }

    public void limpar() {
        this.eventosPublicados.clear();
    }
}

