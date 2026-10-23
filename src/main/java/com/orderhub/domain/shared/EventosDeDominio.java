package com.orderhub.domain.shared;

import com.orderhub.domain.shared.Identificadores.PedidoId;
import com.orderhub.domain.shared.Identificadores.ReservaId;
import com.orderhub.domain.shared.Identificadores.PagamentoId;
import com.orderhub.domain.shared.Identificadores.Sku;

import java.util.List;

/**
 * Eventos de Domínio do Order Hub (ARQ-4.4, ARQ-50).
 */
public final class EventosDeDominio {

    private EventosDeDominio() {}

    public interface EventoDeDominio {
        String getTipo();
        PedidoId getPedidoId();
        Instante getOcorridoEm();
    }

    public record ItemEvento(Sku sku, Quantidade quantidade) {}

    public record PedidoCriado(
        PedidoId pedidoId,
        Identificadores.ClienteId clienteId,
        Email clienteEmail,
        Dinheiro total,
        List<ItemEvento> itens,
        Instante ocorridoEm
    ) implements EventoDeDominio {
        @Override public String getTipo() { return "PedidoCriado"; }
        @Override public PedidoId getPedidoId() { return pedidoId; }
        @Override public Instante getOcorridoEm() { return ocorridoEm; }
    }

    public record EstoqueReservado(
        PedidoId pedidoId,
        ReservaId reservaId,
        List<ItemEvento> itens,
        Instante ocorridoEm
    ) implements EventoDeDominio {
        @Override public String getTipo() { return "EstoqueReservado"; }
        @Override public PedidoId getPedidoId() { return pedidoId; }
        @Override public Instante getOcorridoEm() { return ocorridoEm; }
    }

    public record PagamentoConfirmado(
        PedidoId pedidoId,
        PagamentoId pagamentoId,
        Dinheiro valor,
        Instante ocorridoEm
    ) implements EventoDeDominio {
        @Override public String getTipo() { return "PagamentoConfirmado"; }
        @Override public PedidoId getPedidoId() { return pedidoId; }
        @Override public Instante getOcorridoEm() { return ocorridoEm; }
    }

    public record PedidoCancelado(
        PedidoId pedidoId,
        String motivo,
        boolean estornado,
        Instante ocorridoEm
    ) implements EventoDeDominio {
        @Override public String getTipo() { return "PedidoCancelado"; }
        @Override public PedidoId getPedidoId() { return pedidoId; }
        @Override public Instante getOcorridoEm() { return ocorridoEm; }
    }
}

