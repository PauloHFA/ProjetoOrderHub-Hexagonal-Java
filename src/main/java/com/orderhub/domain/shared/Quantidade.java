package com.orderhub.domain.shared;

import java.util.Objects;

/**
 * Value Object Quantidade (ARQ-11).
 * Inteiro estritamente positivo (> 0).
 */
public final class Quantidade {
    private final int valor;

    private Quantidade(int valor) {
        if (valor <= 0) {
            throw new DomainError.InvalidArgumentException("Quantidade deve ser um inteiro positivo maior que zero. Recebido: " + valor);
        }
        this.valor = valor;
    }

    public static Quantidade criar(int valor) {
        return new Quantidade(valor);
    }

    public int getValor() {
        return valor;
    }

    public Quantidade somar(Quantidade outra) {
        return new Quantidade(this.valor + outra.valor);
    }

    public Quantidade subtrair(Quantidade outra) {
        return new Quantidade(this.valor - outra.valor);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Quantidade that)) return false;
        return valor == that.valor;
    }

    @Override
    public int hashCode() {
        return Objects.hash(valor);
    }

    @Override
    public String toString() {
        return String.valueOf(valor);
    }
}

