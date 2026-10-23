package com.orderhub.domain.shared;

import java.util.Objects;
import java.util.UUID;

/**
 * Value Objects Identificadores fortemente tipados (ARQ-11, ARQ-14).
 * Referências entre agregados são feitas por ID, nunca por referência a objeto.
 */
public final class Identificadores {

    private Identificadores() {}

    public abstract static class Identificador {
        private final String valor;

        protected Identificador(String valor) {
            if (valor == null || valor.trim().isEmpty()) {
                throw new DomainError.InvalidArgumentException(getClass().getSimpleName() + " é obrigatório");
            }
            this.valor = valor.trim();
        }

        public String getValor() {
            return valor;
        }

        public boolean igual(Identificador outro) {
            if (outro == null) return false;
            return this.valor.equals(outro.valor);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Identificador that = (Identificador) o;
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

    public static final class PedidoId extends Identificador {
        private PedidoId(String valor) {
            super(valor);
        }

        public static PedidoId criar(String valor) {
            return new PedidoId(valor);
        }

        public static PedidoId gerar() {
            return new PedidoId(UUID.randomUUID().toString());
        }
    }

    public static final class ClienteId extends Identificador {
        private ClienteId(String valor) {
            super(valor);
        }

        public static ClienteId criar(String valor) {
            return new ClienteId(valor);
        }

        public static ClienteId gerar() {
            return new ClienteId(UUID.randomUUID().toString());
        }
    }

    public static final class Sku extends Identificador {
        private Sku(String valor) {
            super(valor.trim().toUpperCase());
            if (getValor().length() < 3 || getValor().length() > 50) {
                throw new DomainError.InvalidArgumentException("SKU deve ter entre 3 e 50 caracteres: " + valor);
            }
        }

        public static Sku criar(String valor) {
            return new Sku(valor);
        }
    }

    public static final class ReservaId extends Identificador {
        private ReservaId(String valor) {
            super(valor);
        }

        public static ReservaId criar(String valor) {
            return new ReservaId(valor);
        }

        public static ReservaId gerar() {
            return new ReservaId(UUID.randomUUID().toString());
        }
    }

    public static final class PagamentoId extends Identificador {
        private PagamentoId(String valor) {
            super(valor);
        }

        public static PagamentoId criar(String valor) {
            return new PagamentoId(valor);
        }

        public static PagamentoId gerar() {
            return new PagamentoId(UUID.randomUUID().toString());
        }
    }

    public static final class IdempotenciaChave extends Identificador {
        private IdempotenciaChave(String valor) {
            super(valor);
            if (getValor().length() < 5 || getValor().length() > 100) {
                throw new DomainError.InvalidArgumentException("Chave de idempotência deve ter entre 5 e 100 caracteres");
            }
        }

        public static IdempotenciaChave criar(String valor) {
            return new IdempotenciaChave(valor);
        }
    }
}

