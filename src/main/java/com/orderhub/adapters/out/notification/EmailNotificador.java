package com.orderhub.adapters.out.notification;

import com.orderhub.application.port.out.Notificador;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EmailNotificador implements Notificador {

    private final List<Notificacao> notificacoesEnviadas = new ArrayList<>();

    @Override
    public void notificar(Notificacao notificacao) {
        notificacoesEnviadas.add(notificacao);
    }

    public List<Notificacao> getNotificacoesEnviadas() {
        return Collections.unmodifiableList(notificacoesEnviadas);
    }

    public void limpar() {
        notificacoesEnviadas.clear();
    }
}

