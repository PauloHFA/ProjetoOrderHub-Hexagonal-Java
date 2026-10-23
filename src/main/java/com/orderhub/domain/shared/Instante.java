package com.orderhub.domain.shared;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * Value Object Instante (ARQ-12).
 * Domínio puro: instantes entram por parâmetro ou porta de relógio.
 */
public final class Instante {
    private final Instant valor;

    private Instante(Instant valor) {
        if (valor == null) {
            throw new DomainError.InvalidArgumentException("Instante temporal não pode ser nulo");
        }
        this.valor = valor;
    }

    public static Instante de(Instant valor) {
        return new Instante(valor);
    }

    public static Instante de(String isoString) {
        if (isoString == null || isoString.isBlank()) {
            throw new DomainError.InvalidArgumentException("Data ISO inválida: nula ou vazia");
        }
        return new Instante(Instant.parse(isoString));
    }

    public static Instante deEpochMilli(long epochMilli) {
        return new Instante(Instant.ofEpochMilli(epochMilli));
    }

    public Instant getValor() {
        return valor;
    }

    public Instante adicionarDias(long dias) {
        return new Instante(this.valor.plus(dias, ChronoUnit.DAYS));
    }

    public Instante adicionarHoras(long horas) {
        return new Instante(this.valor.plus(horas, ChronoUnit.HOURS));
    }

    public boolean ehAntesDe(Instante outro) {
        return this.valor.isBefore(outro.valor);
    }

    public boolean ehDepoisDe(Instante outro) {
        return this.valor.isAfter(outro.valor);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Instante that)) return false;
        return Objects.equals(valor, that.valor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(valor);
    }

    @Override
    public String toString() {
        return valor.toString();
    }
}

