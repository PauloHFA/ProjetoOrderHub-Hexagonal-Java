package com.orderhub.adapters.in.rest;

import com.orderhub.application.port.in.CancelarPedidoUseCase;
import com.orderhub.application.port.in.CancelarPedidoUseCase.CancelarPedidoCommand;
import com.orderhub.application.port.in.ConfirmarPagamentoUseCase;
import com.orderhub.application.port.in.ConfirmarPagamentoUseCase.ConfirmarPagamentoCommand;
import com.orderhub.application.port.in.ConsultarPedidoUseCase;
import com.orderhub.application.port.in.ConsultarPedidoUseCase.ConsultarPedidoCommand;
import com.orderhub.application.port.in.CriarPedidoUseCase;
import com.orderhub.application.port.in.CriarPedidoUseCase.CriarPedidoCommand;
import com.orderhub.application.port.in.CriarPedidoUseCase.ItemCommand;
import com.orderhub.application.port.in.ListarPedidosDoClienteUseCase;
import com.orderhub.application.port.in.ListarPedidosDoClienteUseCase.ListarPedidosDoClienteCommand;
import com.orderhub.application.port.in.ReservarEstoqueUseCase;
import com.orderhub.application.port.in.ReservarEstoqueUseCase.ReservarEstoqueCommand;
import com.orderhub.domain.pedido.Pedido;
import com.orderhub.domain.shared.Dinheiro;
import com.orderhub.domain.shared.DomainError;
import com.orderhub.domain.shared.Email;
import com.orderhub.domain.shared.Identificadores.ClienteId;
import com.orderhub.domain.shared.Identificadores.PagamentoId;
import com.orderhub.domain.shared.Identificadores.PedidoId;
import com.orderhub.domain.shared.Identificadores.ReservaId;
import com.orderhub.domain.shared.Identificadores.Sku;
import com.orderhub.domain.shared.Instante;
import com.orderhub.domain.shared.Quantidade;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Adaptador de entrada REST para Pedidos (ARQ-40).
 * Fino: valida formato, converte para Command, chama use case e formata HTTP.
 */
@RestController
public class PedidoController {

    private final CriarPedidoUseCase criarPedidoUseCase;
    private final ReservarEstoqueUseCase reservarEstoqueUseCase;
    private final ConfirmarPagamentoUseCase confirmarPagamentoUseCase;
    private final CancelarPedidoUseCase cancelarPedidoUseCase;
    private final ConsultarPedidoUseCase consultarPedidoUseCase;
    private final ListarPedidosDoClienteUseCase listarPedidosDoClienteUseCase;

    public PedidoController(
        CriarPedidoUseCase criarPedidoUseCase,
        ReservarEstoqueUseCase reservarEstoqueUseCase,
        ConfirmarPagamentoUseCase confirmarPagamentoUseCase,
        CancelarPedidoUseCase cancelarPedidoUseCase,
        ConsultarPedidoUseCase consultarPedidoUseCase,
        ListarPedidosDoClienteUseCase listarPedidosDoClienteUseCase
    ) {
        this.criarPedidoUseCase = criarPedidoUseCase;
        this.reservarEstoqueUseCase = reservarEstoqueUseCase;
        this.confirmarPagamentoUseCase = confirmarPagamentoUseCase;
        this.cancelarPedidoUseCase = cancelarPedidoUseCase;
        this.consultarPedidoUseCase = consultarPedidoUseCase;
        this.listarPedidosDoClienteUseCase = listarPedidosDoClienteUseCase;
    }

    public record PrecoRequest(Double valor, String moeda) {}
    public record ItemRequest(String sku, Integer quantidade, PrecoRequest precoUnitario) {}
    public record CriarPedidoRequest(String clienteId, String clienteEmail, String idempotencyKey, List<ItemRequest> itens) {}
    public record ReservarEstoqueRequest(String reservaId) {}
    public record ConfirmarPagamentoRequest(String pagamentoId, PrecoRequest valor, String pagoEm) {}
    public record CancelarPedidoRequest(String motivo, Boolean estornado) {}

    @PostMapping("/pedidos")
    public ResponseEntity<?> criarPedido(@RequestBody CriarPedidoRequest req) {
        try {
            String emailStr = req.clienteEmail() != null ? req.clienteEmail() :
                (req.clienteId().contains("@") ? req.clienteId() : req.clienteId() + "@exemplo.com");

            List<ItemCommand> itens = new ArrayList<>();
            if (req.itens() != null) {
                for (var i : req.itens()) {
                    itens.add(new ItemCommand(
                        Sku.criar(i.sku()),
                        Quantidade.criar(i.quantidade()),
                        Dinheiro.criar(i.precoUnitario().valor(), i.precoUnitario().moeda() != null ? i.precoUnitario().moeda() : "BRL")
                    ));
                }
            }

            CriarPedidoCommand cmd = new CriarPedidoCommand(
                ClienteId.criar(req.clienteId()),
                Email.criar(emailStr),
                itens,
                req.idempotencyKey()
            );

            var res = criarPedidoUseCase.executar(cmd);
            if (res.isOk()) {
                var v = res.getValue();
                return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "success", true,
                    "data", Map.of(
                        "pedidoId", v.pedidoId().getValor(),
                        "status", v.status().name(),
                        "total", Map.of("valor", v.total().getValor(), "moeda", v.total().getMoeda()),
                        "criadoEm", v.criadoEm().toString()
                    )
                ));
            }

            return mapearErro(res.getError());
        } catch (DomainError e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", Map.of("code", e.getCode(), "message", e.getMessage())
            ));
        }
    }

    @GetMapping("/pedidos/{pedidoId}")
    public ResponseEntity<?> consultarPedido(@PathVariable String pedidoId) {
        try {
            var cmd = new ConsultarPedidoCommand(PedidoId.criar(pedidoId));
            var res = consultarPedidoUseCase.executar(cmd);

            if (res.isOk()) {
                Pedido p = res.getValue().pedido();
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", Map.of(
                        "pedidoId", p.getId().getValor(),
                        "clienteId", p.getClienteId().getValor(),
                        "status", p.getStatus().name(),
                        "total", Map.of("valor", p.total().getValor(), "moeda", p.total().getMoeda()),
                        "itens", p.getItens().stream().map(i -> Map.of(
                            "sku", i.getSku().getValor(),
                            "quantidade", i.getQuantidade().getValor(),
                            "precoUnitario", Map.of("valor", i.getPrecoUnitario().getValor(), "moeda", i.getPrecoUnitario().getMoeda())
                        )).toList(),
                        "versao", p.getVersao(),
                        "criadoEm", p.getCriadoEm().toString(),
                        "atualizadoEm", p.getAtualizadoEm().toString()
                    )
                ));
            }

            return mapearErro(res.getError());
        } catch (DomainError e) {
            return mapearErro(e);
        }
    }

    @PostMapping("/pedidos/{pedidoId}/reservar-estoque")
    public ResponseEntity<?> reservarEstoque(@PathVariable String pedidoId, @RequestBody ReservarEstoqueRequest req) {
        try {
            var cmd = new ReservarEstoqueCommand(PedidoId.criar(pedidoId), ReservaId.criar(req.reservaId()));
            var res = reservarEstoqueUseCase.executar(cmd);

            if (res.isOk()) {
                var v = res.getValue();
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", Map.of(
                        "pedidoId", v.pedidoId().getValor(),
                        "status", v.status().name(),
                        "reservaId", v.reservaId().getValor()
                    )
                ));
            }

            return mapearErro(res.getError());
        } catch (DomainError e) {
            return mapearErro(e);
        }
    }

    @PostMapping("/pedidos/{pedidoId}/confirmar-pagamento")
    public ResponseEntity<?> confirmarPagamento(@PathVariable String pedidoId, @RequestBody ConfirmarPagamentoRequest req) {
        try {
            var cmd = new ConfirmarPagamentoCommand(
                PedidoId.criar(pedidoId),
                PagamentoId.criar(req.pagamentoId()),
                Dinheiro.criar(req.valor().valor(), req.valor().moeda() != null ? req.valor().moeda() : "BRL"),
                req.pagoEm() != null ? Instante.de(req.pagoEm()) : null
            );
            var res = confirmarPagamentoUseCase.executar(cmd);

            if (res.isOk()) {
                var v = res.getValue();
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", Map.of(
                        "pedidoId", v.pedidoId().getValor(),
                        "status", v.status().name(),
                        "pagamentoId", v.pagamentoId().getValor()
                    )
                ));
            }

            return mapearErro(res.getError());
        } catch (DomainError e) {
            return mapearErro(e);
        }
    }

    @PostMapping("/pedidos/{pedidoId}/cancelar")
    public ResponseEntity<?> cancelarPedido(@PathVariable String pedidoId, @RequestBody CancelarPedidoRequest req) {
        try {
            var cmd = new CancelarPedidoCommand(
                PedidoId.criar(pedidoId),
                req.motivo(),
                req.estornado() != null && req.estornado()
            );
            var res = cancelarPedidoUseCase.executar(cmd);

            if (res.isOk()) {
                var v = res.getValue();
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", Map.of(
                        "pedidoId", v.pedidoId().getValor(),
                        "status", v.status().name()
                    )
                ));
            }

            return mapearErro(res.getError());
        } catch (DomainError e) {
            return mapearErro(e);
        }
    }

    @GetMapping("/clientes/{clienteId}/pedidos")
    public ResponseEntity<?> listarPedidos(
        @PathVariable String clienteId,
        @RequestParam(defaultValue = "1") int pagina,
        @RequestParam(defaultValue = "20") int tamanho
    ) {
        try {
            var cmd = new ListarPedidosDoClienteCommand(ClienteId.criar(clienteId), pagina, tamanho);
            var res = listarPedidosDoClienteUseCase.executar(cmd);

            if (res.isOk()) {
                var v = res.getValue();
                int totalPaginas = (int) Math.ceil((double) v.total() / v.tamanho());
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", Map.of(
                        "pedidos", v.pedidos().stream().map(p -> Map.of(
                            "pedidoId", p.getId().getValor(),
                            "status", p.getStatus().name(),
                            "total", Map.of("valor", p.total().getValor(), "moeda", p.total().getMoeda()),
                            "criadoEm", p.getCriadoEm().toString()
                        )).toList(),
                        "paginacao", Map.of(
                            "pagina", v.pagina(),
                            "tamanho", v.tamanho(),
                            "total", v.total(),
                            "totalPaginas", totalPaginas
                        )
                    )
                ));
            }

            return mapearErro(res.getError());
        } catch (DomainError e) {
            return mapearErro(e);
        }
    }

    private ResponseEntity<?> mapearErro(Exception e) {
        if (e instanceof DomainError.IdempotencyConflictException) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "success", false,
                "error", Map.of("code", "IDEMPOTENCY_CONFLICT", "message", e.getMessage())
            ));
        }
        if (e instanceof DomainError.NotFoundException) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "success", false,
                "error", Map.of("code", "NOT_FOUND", "message", e.getMessage())
            ));
        }
        if (e instanceof DomainError.BusinessRuleViolationException de) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "success", false,
                "error", Map.of("code", de.getCode(), "message", de.getMessage())
            ));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
            "success", false,
            "error", Map.of("code", "BAD_REQUEST", "message", e.getMessage())
        ));
    }
}

