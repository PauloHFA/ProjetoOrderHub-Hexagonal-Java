package com.orderhub.adapters.in.cli;

import com.orderhub.application.port.in.CancelarPedidoUseCase;
import com.orderhub.application.port.in.CancelarPedidoUseCase.CancelarPedidoCommand;
import com.orderhub.application.port.in.ConsultarPedidoUseCase;
import com.orderhub.application.port.in.ConsultarPedidoUseCase.ConsultarPedidoCommand;
import com.orderhub.application.port.in.CriarPedidoUseCase;
import com.orderhub.application.port.in.CriarPedidoUseCase.CriarPedidoCommand;
import com.orderhub.application.port.in.CriarPedidoUseCase.ItemCommand;
import com.orderhub.application.port.in.ListarPedidosDoClienteUseCase;
import com.orderhub.application.port.in.ListarPedidosDoClienteUseCase.ListarPedidosDoClienteCommand;
import com.orderhub.application.shared.Result;
import com.orderhub.domain.shared.Dinheiro;
import com.orderhub.domain.shared.Email;
import com.orderhub.domain.shared.Identificadores.ClienteId;
import com.orderhub.domain.shared.Identificadores.PedidoId;
import com.orderhub.domain.shared.Identificadores.Sku;
import com.orderhub.domain.shared.Quantidade;

import java.util.List;

/**
 * Adaptador de entrada CLI (Fase 7 - Prova da Troca / ARQ-45).
 * Demonstra a troca da interface de entrada sem tocar em domain ou application.
 */
public class OrderHubCli {

    public record CliOutput(boolean success, String message, Object data) {}

    private final CriarPedidoUseCase criarPedidoUseCase;
    private final ConsultarPedidoUseCase consultarPedidoUseCase;
    private final CancelarPedidoUseCase cancelarPedidoUseCase;
    private final ListarPedidosDoClienteUseCase listarPedidosUseCase;

    public OrderHubCli(
        CriarPedidoUseCase criarPedidoUseCase,
        ConsultarPedidoUseCase consultarPedidoUseCase,
        CancelarPedidoUseCase cancelarPedidoUseCase,
        ListarPedidosDoClienteUseCase listarPedidosUseCase
    ) {
        this.criarPedidoUseCase = criarPedidoUseCase;
        this.consultarPedidoUseCase = consultarPedidoUseCase;
        this.cancelarPedidoUseCase = cancelarPedidoUseCase;
        this.listarPedidosUseCase = listarPedidosUseCase;
    }

    public CliOutput executarComando(String[] args) {
        if (args == null || args.length == 0) {
            return new CliOutput(false, "Nenhum comando informado", null);
        }

        String comando = args[0];
        try {
            return switch (comando) {
                case "criar-pedido" -> executarCriarPedido(args);
                case "consultar-pedido" -> executarConsultarPedido(args);
                case "cancelar-pedido" -> executarCancelarPedido(args);
                case "listar-pedidos" -> executarListarPedidos(args);
                default -> new CliOutput(false, "Comando desconhecido: " + comando, null);
            };
        } catch (Exception e) {
            return new CliOutput(false, "Erro de formato: " + e.getMessage(), null);
        }
    }

    private CliOutput executarCriarPedido(String[] args) {
        // Uso: criar-pedido <clienteId> <sku:qtd:preco> [email] [idempotencyKey]
        if (args.length < 3) {
            return new CliOutput(false, "Uso: criar-pedido <clienteId> <sku:qtd:preco> [email] [idemKey]", null);
        }

        String clienteId = args[1];
        String[] partesItem = args[2].split(":");
        String email = args.length > 3 ? args[3] : (clienteId + "@exemplo.com");
        String idemKey = args.length > 4 ? args[4] : null;

        var item = new ItemCommand(
            Sku.criar(partesItem[0]),
            Quantidade.criar(Integer.parseInt(partesItem[1])),
            Dinheiro.criar(Double.parseDouble(partesItem[2]))
        );

        CriarPedidoCommand cmd = new CriarPedidoCommand(
            ClienteId.criar(clienteId),
            Email.criar(email),
            List.of(item),
            idemKey
        );

        var result = criarPedidoUseCase.executar(cmd);
        if (result.isOk()) {
            var val = result.getValue();
            return new CliOutput(true, "Pedido criado com sucesso! ID: " + val.pedidoId().getValor(), val);
        }
        return new CliOutput(false, "Falha ao criar pedido: " + result.getError().getMessage(), null);
    }

    private CliOutput executarConsultarPedido(String[] args) {
        if (args.length < 2) {
            return new CliOutput(false, "Uso: consultar-pedido <pedidoId>", null);
        }
        var cmd = new ConsultarPedidoCommand(PedidoId.criar(args[1]));
        var result = consultarPedidoUseCase.executar(cmd);
        if (result.isOk()) {
            return new CliOutput(true, "Pedido encontrado", result.getValue().pedido());
        }
        return new CliOutput(false, "Pedido não encontrado: " + args[1], null);
    }

    private CliOutput executarCancelarPedido(String[] args) {
        if (args.length < 3) {
            return new CliOutput(false, "Uso: cancelar-pedido <pedidoId> <motivo>", null);
        }
        var cmd = new CancelarPedidoCommand(PedidoId.criar(args[1]), args[2], false);
        var result = cancelarPedidoUseCase.executar(cmd);
        if (result.isOk()) {
            return new CliOutput(true, "Pedido cancelado com sucesso!", result.getValue());
        }
        return new CliOutput(false, "Falha ao cancelar: " + result.getError().getMessage(), null);
    }

    private CliOutput executarListarPedidos(String[] args) {
        if (args.length < 2) {
            return new CliOutput(false, "Uso: listar-pedidos <clienteId>", null);
        }
        var cmd = new ListarPedidosDoClienteCommand(ClienteId.criar(args[1]), 1, 20);
        var result = listarPedidosUseCase.executar(cmd);
        if (result.isOk()) {
            return new CliOutput(true, "Total de pedidos: " + result.getValue().total(), result.getValue());
        }
        return new CliOutput(false, "Erro ao listar pedidos: " + result.getError().getMessage(), null);
    }
}

