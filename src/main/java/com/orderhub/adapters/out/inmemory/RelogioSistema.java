package com.orderhub.adapters.out.inmemory;

import com.orderhub.application.port.out.Relogio;
import com.orderhub.domain.shared.Instante;

import java.time.Instant;

public class RelogioSistema implements Relogio {

    private Instant fixado;

    @Override
    public Instante agora() {
        return fixado != null ? Instante.de(fixado) : Instante.de(Instant.now());
    }

    public void fixar(Instant instante) {
        this.fixado = instante;
    }

    public void desfixar() {
        this.fixado = null;
    }
}

