package com.orderhub.application;

import com.orderhub.adapters.out.inmemory.*;
import com.orderhub.application.port.in.CancelarPedidoUseCase.*;
import com.orderhub.application.port.in.ConfirmarPagamentoUseCase.*;
import com.orderhub.application.port.in.ConsultarPedidoUseCase.*;
import com.orderhub.application.port.in.CriarPedidoUseCase.*;
import com.orderhub.application.port.in.ProcessarWebhookDePagamentoUseCase.*;
import com.orderhub.application.port.in.ReservarEstoqueUseCase.*;
import com.orderhub.application.shared.Result;
import com.orderhub.application.usecase.*;
import com.orderhub.domain.pedido.StatusDoPedido;
import com.orderhub.domain.shared.*;
import com.orderhub.domain.shared.Identificadores.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Ciclo de Vida dos Casos de Uso (ARQ-20, ARQ-22)")
class UseCasesLifecycleTest {

    private PedidoRepositoryInMemory repository;
    private UnitOfWorkInMemory uow;
    private PublicadorDeEventosInMemory publicador;
    private GeradorDeIdUuid geradorDeId;
    private RelogioSistema relogio;

    private CriarPedido criarPedido;
    private ReservarEstoque reservarEstoque;
    private ConfirmarPagamento confirmarPagamento;
    private CancelarPedido cancelarPedido;
    private ConsultarPedido consultarPedido;
    private ProcessarWebhookDePagamento webhookPagamento;

    @BeforeEach
    void setUp() {
        repository = new PedidoRepositoryInMemory();
        uow = new UnitOfWorkInMemory();
        publicador = new PublicadorDeEventosInMemory();
        geradorDeId = new GeradorDeIdUuid();
        relogio = new RelogioSistema();

        criarPedido = new CriarPedido(repository, uow, publicador, geradorDeId, relogio);
        reservarEstoque = new ReservarEstoque(repository, uow, publicador, relogio);
        confirmarPagamento = new ConfirmarPagamento(repository, uow, publicador, relogio);
        cancelarPedido = new CancelarPedido(repository, uow, publicador, relogio);
        consultarPedido = new ConsultarPedido(repository);
        webhookPagamento = new ProcessarWebhookDePagamento(repository, uow, publicador, geradorDeId, relogio);
    }

    private PedidoId criarPedidoExemplo() {
        var res = criarPedido.executar(new CriarPedidoCommand(
                ClienteId.criar("cli-001"),
                Email.criar("cliente@orderhub.com"),
                List.of(new ItemCommand(Sku.criar("SKU-HEADSET-1"), Quantidade.criar(1), Dinheiro.criar(new BigDecimal("150.00")))),
                null
        ));
        assertTrue(res.isOk());
        return ((Result.Ok<CriarPedidoResult, Exception>) res).value().pedidoId();
    }

    @Test
    void fluxoCompletoDeSucesso_CriadoParaEstoqueReservadoParaPago() {
        PedidoId id = criarPedidoExemplo();

        // 1. Reservar Estoque
        var resEstoque = reservarEstoque.executar(new ReservarEstoqueCommand(id, ReservaId.criar("RES-001")));
        assertTrue(resEstoque.isOk());
        var dtoEstoque = ((Result.Ok<ReservarEstoqueResult, Exception>) resEstoque).value();
        assertEquals(StatusDoPedido.ESTOQUE_RESERVADO, dtoEstoque.status());

        // 2. Confirmar Pagamento
        var resPagamento = confirmarPagamento.executar(new ConfirmarPagamentoCommand(
                id, PagamentoId.criar("PAG-001"), Dinheiro.criar(150.00), null
        ));
        assertTrue(resPagamento.isOk());
        var dtoPago = ((Result.Ok<ConfirmarPagamentoResult, Exception>) resPagamento).value();
        assertEquals(StatusDoPedido.PAGO, dtoPago.status());

        // 3. Consultar Pedido
        var resConsulta = consultarPedido.executar(new ConsultarPedidoCommand(id));
        assertTrue(resConsulta.isOk());
        var dtoConsulta = ((Result.Ok<ConsultarPedidoResult, Exception>) resConsulta).value();
        assertEquals(StatusDoPedido.PAGO, dtoConsulta.pedido().getStatus());
        assertEquals(id, dtoConsulta.pedido().getId());
    }

    @Test
    void deveCancelarPedidoComSucesso() {
        PedidoId id = criarPedidoExemplo();

        var res = cancelarPedido.executar(new CancelarPedidoCommand(id, "Cliente desistiu da compra", false));
        assertTrue(res.isOk());
        var dto = ((Result.Ok<CancelarPedidoResult, Exception>) res).value();
        assertEquals(StatusDoPedido.CANCELADO, dto.status());
    }

    @Test
    void deveProcessarWebhookDePagamentoAprovado() {
        PedidoId id = criarPedidoExemplo();
        reservarEstoque.executar(new ReservarEstoqueCommand(id, ReservaId.criar("RES-001")));

        var res = webhookPagamento.executar(new ProcessarWebhookDePagamentoCommand(
                PagamentoId.criar("PAG-WH-999"), id, "APROVADO", Dinheiro.criar(150.00), null, Map.of()
        ));

        assertTrue(res.isOk());
        var resp = ((Result.Ok<ProcessarWebhookDePagamentoResult, Exception>) res).value();
        assertTrue(resp.pagamentoProcessado());
        assertEquals(StatusDoPedido.PAGO, resp.status());
    }

    @Test
    void deveProcessarWebhookDePagamentoRecusadoECancelar() {
        PedidoId id = criarPedidoExemplo();
        reservarEstoque.executar(new ReservarEstoqueCommand(id, ReservaId.criar("RES-001")));

        var res = webhookPagamento.executar(new ProcessarWebhookDePagamentoCommand(
                PagamentoId.criar("PAG-WH-888"), id, "RECUSADO", Dinheiro.criar(150.00), null, Map.of()
        ));

        assertTrue(res.isOk());
        var resp = ((Result.Ok<ProcessarWebhookDePagamentoResult, Exception>) res).value();
        assertEquals(StatusDoPedido.CANCELADO, resp.status());
    }
}
