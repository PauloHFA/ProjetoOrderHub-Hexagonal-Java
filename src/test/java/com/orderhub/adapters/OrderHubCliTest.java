package com.orderhub.adapters;

import com.orderhub.adapters.in.cli.OrderHubCli;
import com.orderhub.adapters.out.inmemory.*;
import com.orderhub.application.usecase.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Adaptador Primário CLI (ARQ-45)")
class OrderHubCliTest {

    private OrderHubCli cli;

    @BeforeEach
    void setUp() {
        var repository = new PedidoRepositoryInMemory();
        var uow = new UnitOfWorkInMemory();
        var relogio = new RelogioSistema();
        var geradorDeId = new GeradorDeIdUuid();
        var publicador = new PublicadorDeEventosInMemory();

        var criarPedido = new CriarPedido(repository, uow, publicador, geradorDeId, relogio);
        var consultarPedido = new ConsultarPedido(repository);
        var cancelarPedido = new CancelarPedido(repository, uow, publicador, relogio);
        var listarPedidos = new ListarPedidosDoCliente(repository);

        cli = new OrderHubCli(criarPedido, consultarPedido, cancelarPedido, listarPedidos);
    }

    @Test
    void deveExecutarComandoCriarEConsultarPelaCli() {
        var outCriar = cli.executarComando(new String[]{"criar-pedido", "cli-cli", "SKU-CLI:2:50.00", "cli@test.com"});
        assertTrue(outCriar.success());
        assertTrue(outCriar.message().contains("Pedido criado com sucesso!"));

        String pedidoId = outCriar.message().split("ID: ")[1].trim();

        var outConsultar = cli.executarComando(new String[]{"consultar-pedido", pedidoId});
        assertTrue(outConsultar.success());
        assertTrue(outConsultar.message().contains("Pedido encontrado"));

        var outCancelar = cli.executarComando(new String[]{"cancelar-pedido", pedidoId, "Cliente desistiu"});
        assertTrue(outCancelar.success());
        assertTrue(outCancelar.message().contains("Pedido cancelado com sucesso!"));
    }

    @Test
    void deveExibirMensagemErroQuandoComandoInvalido() {
        var out = cli.executarComando(new String[]{"desconhecido"});
        assertFalse(out.success());
        assertTrue(out.message().contains("Comando desconhecido"));
    }
}

