package com.orderhub.domain;

import com.orderhub.domain.pedido.ItemDoPedido;
import com.orderhub.domain.pedido.Pedido;
import com.orderhub.domain.pedido.StatusDoPedido;
import com.orderhub.domain.shared.Dinheiro;
import com.orderhub.domain.shared.DomainError;
import com.orderhub.domain.shared.Email;
import com.orderhub.domain.shared.Identificadores.*;
import com.orderhub.domain.shared.Instante;
import com.orderhub.domain.shared.Quantidade;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PedidoTest {

    private final Instante agora = Instante.de(Instant.now());

    private Pedido criarPedidoPadrao() {
        return Pedido.criar(
            PedidoId.criar("ped-1"),
            ClienteId.criar("cli-1"),
            Email.criar("cliente@teste.com"),
            List.of(
                ItemDoPedido.criar(Sku.criar("SKU-001"), Quantidade.criar(2), Dinheiro.criar(50.0)),
                ItemDoPedido.criar(Sku.criar("SKU-002"), Quantidade.criar(1), Dinheiro.criar(100.0))
            ),
            agora
        );
    }

    @Test
    @DisplayName("Deve criar pedido com estado CRIADO e calcular total correto")
    void deveCriarPedidoCorretamente() {
        Pedido pedido = criarPedidoPadrao();

        assertThat(pedido.getStatus()).isEqualTo(StatusDoPedido.CRIADO);
        assertThat(pedido.total().getValor()).isEqualByComparingTo("200.00");
        assertThat(pedido.getEventos()).hasSize(1);
        assertThat(pedido.getEventos().get(0).getTipo()).isEqualTo("PedidoCriado");
    }

    @Test
    @DisplayName("Não deve permitir pedido sem itens")
    void naoDevePermitirPedidoSemItens() {
        assertThatThrownBy(() -> Pedido.criar(
            PedidoId.criar("ped-1"),
            ClienteId.criar("cli-1"),
            Email.criar("cli@teste.com"),
            List.of(),
            agora
        )).isInstanceOf(DomainError.BusinessRuleViolationException.class)
          .hasMessageContaining("pelo menos um item");
    }

    @Test
    @DisplayName("Não deve permitir SKUs duplicados no mesmo pedido")
    void naoDevePermitirSkusDuplicados() {
        assertThatThrownBy(() -> Pedido.criar(
            PedidoId.criar("ped-1"),
            ClienteId.criar("cli-1"),
            Email.criar("cli@teste.com"),
            List.of(
                ItemDoPedido.criar(Sku.criar("SKU-001"), Quantidade.criar(1), Dinheiro.criar(50.0)),
                ItemDoPedido.criar(Sku.criar("SKU-001"), Quantidade.criar(2), Dinheiro.criar(50.0))
            ),
            agora
        )).isInstanceOf(DomainError.BusinessRuleViolationException.class)
          .hasMessageContaining("SKU duplicado");
    }

    @Test
    @DisplayName("Deve reservar estoque com sucesso a partir de CRIADO")
    void deveReservarEstoque() {
        Pedido pedido = criarPedidoPadrao();
        pedido.limparEventos();

        pedido.reservarEstoque(ReservaId.criar("res-1"), agora);

        assertThat(pedido.getStatus()).isEqualTo(StatusDoPedido.ESTOQUE_RESERVADO);
        assertThat(pedido.getEventos()).hasSize(1);
        assertThat(pedido.getEventos().get(0).getTipo()).isEqualTo("EstoqueReservado");
    }

    @Test
    @DisplayName("Deve confirmar pagamento com sucesso quando valor confere e estoque está reservado")
    void deveConfirmarPagamento() {
        Pedido pedido = criarPedidoPadrao();
        pedido.reservarEstoque(ReservaId.criar("res-1"), agora);
        pedido.limparEventos();

        pedido.confirmarPagamento(PagamentoId.criar("pag-1"), Dinheiro.criar(200.0), agora);

        assertThat(pedido.getStatus()).isEqualTo(StatusDoPedido.PAGO);
        assertThat(pedido.getEventos()).hasSize(1);
        assertThat(pedido.getEventos().get(0).getTipo()).isEqualTo("PagamentoConfirmado");
    }

    @Test
    @DisplayName("Não deve confirmar pagamento com valor divergente do total")
    void naoDeveConfirmarPagamentoValorDivergente() {
        Pedido pedido = criarPedidoPadrao();
        pedido.reservarEstoque(ReservaId.criar("res-1"), agora);

        assertThatThrownBy(() -> pedido.confirmarPagamento(PagamentoId.criar("pag-1"), Dinheiro.criar(150.0), agora))
            .isInstanceOf(DomainError.BusinessRuleViolationException.class)
            .hasMessageContaining("Valor do pagamento difere do total");
    }

    @Test
    @DisplayName("Não deve permitir transições inválidas (ex: CRIADO direto para PAGO sem reserva)")
    void naoDevePermitirTransicaoInvalida() {
        Pedido pedido = criarPedidoPadrao();

        assertThatThrownBy(() -> pedido.confirmarPagamento(PagamentoId.criar("pag-1"), Dinheiro.criar(200.0), agora))
            .isInstanceOf(DomainError.BusinessRuleViolationException.class)
            .hasMessageContaining("Transição inválida");
    }

    @Test
    @DisplayName("Deve permitir cancelar pedido em CRIADO ou ESTOQUE_RESERVADO ou PAGO")
    void deveCancelarPedido() {
        Pedido pedido = criarPedidoPadrao();
        pedido.cancelar("Desistência", false, agora);
        assertThat(pedido.getStatus()).isEqualTo(StatusDoPedido.CANCELADO);
    }
}

