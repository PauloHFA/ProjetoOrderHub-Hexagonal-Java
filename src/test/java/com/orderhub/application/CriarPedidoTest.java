package com.orderhub.application;

import com.orderhub.adapters.out.inmemory.*;
import com.orderhub.application.port.in.CriarPedidoUseCase.*;
import com.orderhub.application.shared.Result;
import com.orderhub.application.usecase.CriarPedido;
import com.orderhub.domain.pedido.StatusDoPedido;
import com.orderhub.domain.shared.*;
import com.orderhub.domain.shared.Identificadores.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Caso de Uso - Criar Pedido (ARQ-20, ARQ-21)")
class CriarPedidoTest {

    private PedidoRepositoryInMemory repository;
    private UnitOfWorkInMemory uow;
    private PublicadorDeEventosInMemory publicador;
    private GeradorDeIdUuid geradorDeId;
    private RelogioSistema relogio;
    private CriarPedido useCase;

    @BeforeEach
    void setUp() {
        repository = new PedidoRepositoryInMemory();
        uow = new UnitOfWorkInMemory();
        publicador = new PublicadorDeEventosInMemory();
        geradorDeId = new GeradorDeIdUuid();
        relogio = new RelogioSistema();
        useCase = new CriarPedido(repository, uow, publicador, geradorDeId, relogio);
    }

    @Test
    void deveCriarPedidoComSucesso() {
        var comando = new CriarPedidoCommand(
                ClienteId.criar("cli-001"),
                Email.criar("cliente@orderhub.com"),
                List.of(
                        new ItemCommand(Sku.criar("SKU-MOUSE-1"), Quantidade.criar(1), Dinheiro.criar(new BigDecimal("100.00"))),
                        new ItemCommand(Sku.criar("SKU-TECLADO-1"), Quantidade.criar(1), Dinheiro.criar(new BigDecimal("200.00")))
                ),
                null
        );

        var resultado = useCase.executar(comando);

        assertTrue(resultado.isOk());
        var res = ((Result.Ok<CriarPedidoResult, Exception>) resultado).value();
        assertNotNull(res.pedidoId());
        assertEquals(StatusDoPedido.CRIADO, res.status());
        assertEquals(Dinheiro.criar(300.00), res.total());

        // Verifica persistência e eventos
        assertTrue(repository.buscarPorId(res.pedidoId()).isPresent());
        assertEquals(1, publicador.getEventosPublicados().size());
        assertEquals("PedidoCriado", publicador.getEventosPublicados().get(0).getTipo());
    }

    @Test
    void deveRejeitarPedidoSemItens() {
        var comando = new CriarPedidoCommand(
                ClienteId.criar("cli-001"),
                Email.criar("cliente@orderhub.com"),
                List.of(),
                null
        );

        var resultado = useCase.executar(comando);

        assertTrue(resultado.isErr());
        var err = ((Result.Err<CriarPedidoResult, Exception>) resultado).error();
        assertTrue(err instanceof DomainError.BusinessRuleViolationException);
    }

    @Test
    void deveRejeitarPedidoComSkusDuplicados() {
        var comando = new CriarPedidoCommand(
                ClienteId.criar("cli-001"),
                Email.criar("cliente@orderhub.com"),
                List.of(
                        new ItemCommand(Sku.criar("SKU-DUPLICADO"), Quantidade.criar(1), Dinheiro.criar(50.0)),
                        new ItemCommand(Sku.criar("SKU-DUPLICADO"), Quantidade.criar(2), Dinheiro.criar(50.0))
                ),
                null
        );

        var resultado = useCase.executar(comando);

        assertTrue(resultado.isErr());
        var err = ((Result.Err<CriarPedidoResult, Exception>) resultado).error();
        assertTrue(err.getMessage().contains("SKU duplicado"));
    }
}

