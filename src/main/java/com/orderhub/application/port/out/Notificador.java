package com.orderhub.application.port.out;

import com.orderhub.domain.shared.Email;
import com.orderhub.domain.shared.Identificadores.ClienteId;
import com.orderhub.domain.shared.Identificadores.PedidoId;

/**
 * Porta de saída para notificações externas (ARQ-30).
 */
public interface Notificador {

    record Notificacao(
        String tipo,
        PedidoId pedidoId,
        ClienteId clienteId,
        Email clienteEmail,
        String mensagem
    ) {}

    void notificar(Notificacao notificacao);
}

