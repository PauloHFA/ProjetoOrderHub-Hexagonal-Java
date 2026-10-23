package com.orderhub.domain.pedido;

import com.orderhub.domain.shared.Dinheiro;
import com.orderhub.domain.shared.DomainError;
import com.orderhub.domain.shared.Email;
import com.orderhub.domain.shared.EventosDeDominio;
import com.orderhub.domain.shared.EventosDeDominio.EventoDeDominio;
import com.orderhub.domain.shared.EventosDeDominio.ItemEvento;
import com.orderhub.domain.shared.Identificadores.ClienteId;
import com.orderhub.domain.shared.Identificadores.PagamentoId;
import com.orderhub.domain.shared.Identificadores.PedidoId;
import com.orderhub.domain.shared.Identificadores.ReservaId;
import com.orderhub.domain.shared.Instante;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Agregado Raiz Pedido (ARQ-10, ARQ-13, ARQ-53).
 * Protege seus invariantes: sem setters públicos, mudança de estado por métodos de negócio.
 */
public final class Pedido {
    private final PedidoId id;
    private final ClienteId clienteId;
    private final Email clienteEmail;
    private StatusDoPedido status;
    private final List<ItemDoPedido> itens;
    private int versao;
    private final Instante criadoEm;
    private Instante atualizadoEm;
    private final List<EventoDeDominio> eventos = new ArrayList<>();

    private Pedido(
        PedidoId id,
        ClienteId clienteId,
        Email clienteEmail,
        StatusDoPedido status,
        List<ItemDoPedido> itens,
        int versao,
        Instante criadoEm,
        Instante atualizadoEm
    ) {
        if (id == null) throw new DomainError.InvalidArgumentException("PedidoId é obrigatório");
        if (clienteId == null) throw new DomainError.InvalidArgumentException("ClienteId é obrigatório");
        if (clienteEmail == null) throw new DomainError.InvalidArgumentException("Email do cliente é obrigatório");
        if (itens == null || itens.isEmpty()) {
            throw new DomainError.BusinessRuleViolationException("Pedido deve conter pelo menos um item");
        }

        // Validar unicidade de SKU
        Set<String> skus = new HashSet<>();
        for (ItemDoPedido item : itens) {
            if (!skus.add(item.getSku().getValor())) {
                throw new DomainError.BusinessRuleViolationException("SKU duplicado no pedido: " + item.getSku().getValor());
            }
        }

        this.id = id;
        this.clienteId = clienteId;
        this.clienteEmail = clienteEmail;
        this.status = status != null ? status : StatusDoPedido.CRIADO;
        this.itens = new ArrayList<>(itens);
        this.versao = versao;
        this.criadoEm = criadoEm;
        this.atualizadoEm = atualizadoEm;
    }

    /**
     * Factory method para criação de um novo pedido
     */
    public static Pedido criar(PedidoId id, ClienteId clienteId, Email clienteEmail, List<ItemDoPedido> itens, Instante agora) {
        Pedido pedido = new Pedido(id, clienteId, clienteEmail, StatusDoPedido.CRIADO, itens, 0, agora, agora);
        
        List<ItemEvento> itensEvento = itens.stream()
            .map(i -> new ItemEvento(i.getSku(), i.getQuantidade()))
            .toList();

        pedido.eventos.add(new EventosDeDominio.PedidoCriado(
            id,
            clienteId,
            clienteEmail,
            pedido.total(),
            itensEvento,
            agora
        ));
        
        return pedido;
    }

    /**
     * Reconstituição a partir de persistência
     */
    public static Pedido reconstituir(
        PedidoId id,
        ClienteId clienteId,
        Email clienteEmail,
        StatusDoPedido status,
        List<ItemDoPedido> itens,
        int versao,
        Instante criadoEm,
        Instante atualizadoEm
    ) {
        return new Pedido(id, clienteId, clienteEmail, status, itens, versao, criadoEm, atualizadoEm);
    }

    public Dinheiro total() {
        return itens.stream()
            .map(ItemDoPedido::subtotal)
            .reduce(Dinheiro.zero(), Dinheiro::somar);
    }

    public void reservarEstoque(ReservaId reservaId, Instante agora) {
        transicionarPara(StatusDoPedido.ESTOQUE_RESERVADO, agora);
        
        List<ItemEvento> itensEvento = itens.stream()
            .map(i -> new ItemEvento(i.getSku(), i.getQuantidade()))
            .toList();

        this.eventos.add(new EventosDeDominio.EstoqueReservado(this.id, reservaId, itensEvento, agora));
    }

    public void confirmarPagamento(PagamentoId pagamentoId, Dinheiro valorPago, Instante agora) {
        if (!this.total().igual(valorPago)) {
            throw new DomainError.BusinessRuleViolationException(
                "Valor do pagamento difere do total do pedido. Total: " + this.total() + ", Pago: " + valorPago
            );
        }
        transicionarPara(StatusDoPedido.PAGO, agora);
        this.eventos.add(new EventosDeDominio.PagamentoConfirmado(this.id, pagamentoId, valorPago, agora));
    }

    public void cancelar(String motivo, boolean estornado, Instante agora) {
        transicionarPara(StatusDoPedido.CANCELADO, agora);
        this.eventos.add(new EventosDeDominio.PedidoCancelado(this.id, motivo, estornado, agora));
    }

    public void marcarComoEnviado(Instante agora) {
        transicionarPara(StatusDoPedido.ENVIADO, agora);
    }

    public void marcarComoEntregue(Instante agora) {
        transicionarPara(StatusDoPedido.ENTREGUE, agora);
    }

    private void transicionarPara(StatusDoPedido novoStatus, Instante agora) {
        if (!this.status.podeTransicionarPara(novoStatus)) {
            throw new DomainError.BusinessRuleViolationException(
                "Transição inválida de estado: de " + this.status + " para " + novoStatus
            );
        }
        this.status = novoStatus;
        this.atualizadoEm = agora;
    }

    public List<EventoDeDominio> getEventos() {
        return Collections.unmodifiableList(eventos);
    }

    public void limparEventos() {
        this.eventos.clear();
    }

    public void incrementarVersao() {
        this.versao++;
    }

    // Getters somente leitura (sem setters públicos)
    public PedidoId getId() { return id; }
    public ClienteId getClienteId() { return clienteId; }
    public Email getClienteEmail() { return clienteEmail; }
    public StatusDoPedido getStatus() { return status; }
    public List<ItemDoPedido> getItens() { return Collections.unmodifiableList(itens); }
    public int getVersao() { return versao; }
    public Instante getCriadoEm() { return criadoEm; }
    public Instante getAtualizadoEm() { return atualizadoEm; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Pedido pedido)) return false;
        return Objects.equals(id, pedido.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

