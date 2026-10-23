package com.orderhub.domain.pedido;

/**
 * Máquina de estados do Pedido (ARQ-4.3).
 * Transições válidas:
 * CRIADO -> ESTOQUE_RESERVADO -> PAGO -> ENVIADO -> ENTREGUE
 *   │               │              │
 *   └───► CANCELADO ◄──────────────┘
 */
public enum StatusDoPedido {
    CRIADO,
    ESTOQUE_RESERVADO,
    PAGO,
    ENVIADO,
    ENTREGUE,
    CANCELADO;

    public boolean podeTransicionarPara(StatusDoPedido destino) {
        if (destino == null) return false;
        return switch (this) {
            case CRIADO -> destino == ESTOQUE_RESERVADO || destino == CANCELADO;
            case ESTOQUE_RESERVADO -> destino == PAGO || destino == CANCELADO;
            case PAGO -> destino == ENVIADO || destino == CANCELADO;
            case ENVIADO -> destino == ENTREGUE;
            case ENTREGUE, CANCELADO -> false; // Estados terminais
        };
    }
}

