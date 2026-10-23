package com.orderhub.application.port.out;

import com.orderhub.domain.shared.EventosDeDominio.EventoDeDominio;
import java.util.List;

/**
 * Porta de saída para publicação de eventos de domínio (ARQ-34).
 */
public interface PublicadorDeEventos {
    void publicar(List<EventoDeDominio> eventos);
}

