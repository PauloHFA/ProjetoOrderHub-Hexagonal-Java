package com.orderhub.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = com.orderhub.bootstrap.Application.class)
@AutoConfigureMockMvc
@DisplayName("E2E - Pedido API REST (DRV-01, ARQ-40)")
class PedidoApiE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void fluxoCompletoDePedidoViaRest() throws Exception {
        // 1. Criar Pedido (POST /pedidos)
        String requestBody = """
                {
                    "clienteId": "cli-rest-1",
                    "clienteEmail": "rest@orderhub.com",
                    "itens": [
                        {
                            "sku": "SKU-MONITOR",
                            "quantidade": 1,
                            "precoUnitario": { "valor": 1200.00, "moeda": "BRL" }
                        }
                    ]
                }
                """;

        var resultCriar = mockMvc.perform(post("/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.pedidoId").isNotEmpty())
                .andExpect(jsonPath("$.data.status").value("CRIADO"))
                .andExpect(jsonPath("$.data.total.valor").value(1200.00))
                .andReturn();

        JsonNode jsonCriar = objectMapper.readTree(resultCriar.getResponse().getContentAsString());
        String pedidoId = jsonCriar.get("data").get("pedidoId").asText();

        // 2. Reservar Estoque (POST /pedidos/{id}/reservar-estoque)
        mockMvc.perform(post("/pedidos/{id}/reservar-estoque", pedidoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "reservaId": "RES-E2E-1" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("ESTOQUE_RESERVADO"));

        // 3. Confirmar Pagamento (POST /pedidos/{id}/confirmar-pagamento)
        mockMvc.perform(post("/pedidos/{id}/confirmar-pagamento", pedidoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "pagamentoId": "PAG-E2E-1",
                                    "valor": { "valor": 1200.00, "moeda": "BRL" }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PAGO"));

        // 4. Consultar Pedido (GET /pedidos/{id})
        mockMvc.perform(get("/pedidos/{id}", pedidoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.pedidoId").value(pedidoId))
                .andExpect(jsonPath("$.data.status").value("PAGO"));

        // 5. Listar Pedidos do Cliente (GET /clientes/{clienteId}/pedidos)
        mockMvc.perform(get("/clientes/{clienteId}/pedidos", "cli-rest-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.pedidos").isArray())
                .andExpect(jsonPath("$.data.pedidos[0].pedidoId").value(pedidoId));
    }

    @Test
    void deveRetornar400AoCriarPedidoComDadosInvalidos() throws Exception {
        String requestInvalido = """
                {
                    "clienteId": "cli-1",
                    "clienteEmail": "email-invalido",
                    "itens": []
                }
                """;

        mockMvc.perform(post("/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestInvalido))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").isNotEmpty());
    }

    @Test
    void deveRetornar404AoBuscarPedidoInexistente() throws Exception {
        mockMvc.perform(get("/pedidos/{id}", "pedido-inexistente"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    void deveProcessarWebhookDePagamentoComSucesso() throws Exception {
        // Criar pedido
        String requestBody = """
                {
                    "clienteId": "cli-wh",
                    "clienteEmail": "wh@orderhub.com",
                    "itens": [
                        {
                            "sku": "SKU-WEBHOOK",
                            "quantidade": 1,
                            "precoUnitario": { "valor": 50.00, "moeda": "BRL" }
                        }
                    ]
                }
                """;
        var res = mockMvc.perform(post("/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andReturn();
        String pedidoId = objectMapper.readTree(res.getResponse().getContentAsString()).get("data").get("pedidoId").asText();

        // Reservar estoque
        mockMvc.perform(post("/pedidos/{id}/reservar-estoque", pedidoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "reservaId": "RES-WH-1" }
                                """))
                .andExpect(status().isOk());

        // Webhook pagamento aprovado
        String webhookBody = """
                {
                    "pagamentoId": "PAG-WH-999",
                    "pedidoId": "%s",
                    "status": "APROVADO",
                    "valor": { "valor": 50.00, "moeda": "BRL" }
                }
                """.formatted(pedidoId);

        mockMvc.perform(post("/webhooks/pagamento")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(webhookBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PAGO"));
    }
}
