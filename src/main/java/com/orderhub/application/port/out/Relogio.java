package com.orderhub.application.port.out;

import com.orderhub.domain.shared.Instante;

/**
 * Porta de saída para fornecimento de data e hora (ARQ-34).
 */
public interface Relogio {
    Instante agora();
}

