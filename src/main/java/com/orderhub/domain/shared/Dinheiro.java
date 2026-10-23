package com.orderhub.domain.shared;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Value Object Dinheiro (ARQ-11).
 * Imutável, sem float, com validação de precisão e arredondamento monetário.
 */
public final class Dinheiro {
    private final BigDecimal valor;
    private final String moeda;

    public static final String MOEDA_PADRAO = "BRL";

    private Dinheiro(BigDecimal valor, String moeda) {
        if (valor == null) {
            throw new DomainError.InvalidArgumentException("Valor monetário não pode ser nulo");
        }
        if (moeda == null || !moeda.matches("^[A-Z]{3}$")) {
            throw new DomainError.InvalidArgumentException("Moeda inválida: deve ter 3 letras maiúsculas (ex: BRL)");
        }
        this.valor = valor.setScale(2, RoundingMode.HALF_EVEN);
        this.moeda = moeda;
    }

    public static Dinheiro criar(BigDecimal valor, String moeda) {
        return new Dinheiro(valor, moeda);
    }

    public static Dinheiro criar(BigDecimal valor) {
        return new Dinheiro(valor, MOEDA_PADRAO);
    }

    public static Dinheiro criar(double valor, String moeda) {
        return new Dinheiro(BigDecimal.valueOf(valor), moeda);
    }

    public static Dinheiro criar(double valor) {
        return new Dinheiro(BigDecimal.valueOf(valor), MOEDA_PADRAO);
    }

    public static Dinheiro criar(long valor) {
        return new Dinheiro(BigDecimal.valueOf(valor), MOEDA_PADRAO);
    }

    public static Dinheiro zero(String moeda) {
        return new Dinheiro(BigDecimal.ZERO, moeda);
    }

    public static Dinheiro zero() {
        return new Dinheiro(BigDecimal.ZERO, MOEDA_PADRAO);
    }

    public Dinheiro somar(Dinheiro outro) {
        validarMesmaMoeda(outro);
        return new Dinheiro(this.valor.add(outro.valor), this.moeda);
    }

    public Dinheiro subtrair(Dinheiro outro) {
        validarMesmaMoeda(outro);
        return new Dinheiro(this.valor.subtract(outro.valor), this.moeda);
    }

    public Dinheiro multiplicar(int quantidade) {
        if (quantidade < 0) {
            throw new DomainError.InvalidArgumentException("Multiplicador não pode ser negativo");
        }
        return new Dinheiro(this.valor.multiply(BigDecimal.valueOf(quantidade)), this.moeda);
    }

    public boolean ehMaiorQue(Dinheiro outro) {
        validarMesmaMoeda(outro);
        return this.valor.compareTo(outro.valor) > 0;
    }

    public boolean ehMenorQue(Dinheiro outro) {
        validarMesmaMoeda(outro);
        return this.valor.compareTo(outro.valor) < 0;
    }

    public boolean igual(Dinheiro outro) {
        if (outro == null) return false;
        return this.moeda.equals(outro.moeda) && this.valor.compareTo(outro.valor) == 0;
    }

    private void validarMesmaMoeda(Dinheiro outro) {
        if (!this.moeda.equals(outro.moeda)) {
            throw new DomainError.BusinessRuleViolationException(
                "Não é permitido operar moedas distintas: " + this.moeda + " e " + outro.moeda
            );
        }
    }

    public BigDecimal getValor() {
        return valor;
    }

    public String getMoeda() {
        return moeda;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Dinheiro that)) return false;
        return this.igual(that);
    }

    @Override
    public int hashCode() {
        return Objects.hash(valor, moeda);
    }

    @Override
    public String toString() {
        return moeda + " " + valor.toPlainString();
    }
}

