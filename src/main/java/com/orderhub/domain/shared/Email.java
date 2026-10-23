package com.orderhub.domain.shared;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Value Object Email (ARQ-11).
 */
public final class Email {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private final String valor;

    private Email(String valor) {
        if (valor == null || valor.trim().isEmpty()) {
            throw new DomainError.InvalidArgumentException("Email é obrigatório");
        }
        String normalizado = valor.trim().toLowerCase();
        if (!EMAIL_PATTERN.matcher(normalizado).matches()) {
            throw new DomainError.InvalidArgumentException("Formato de email inválido: " + valor);
        }
        this.valor = normalizado;
    }

    public static Email criar(String valor) {
        return new Email(valor);
    }

    public String getValor() {
        return valor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Email that)) return false;
        return Objects.equals(valor, that.valor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(valor);
    }

    @Override
    public String toString() {
        return valor;
    }
}

