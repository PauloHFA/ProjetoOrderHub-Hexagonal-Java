# ADR 005: Estratégia de Idempotência

## Status
Aceito

## Contexto
Em redes não confiáveis, clientes HTTP ou gateways externos podem reenviar comandos devido a timeouts ou falhas transitórias. Repetir comandos como `CriarPedido` ou `ConfirmarPagamento` pode gerar pedidos duplicados, cobranças indevidas ou violação de invariantes.

## Decisão
Implementar idempotência em dois níveis complementares (ARQ-51):
1. **Chaves de Idempotência (`Idempotency-Key`):**
   - Comandos de criação expõem um identificador de idempotência único fornecido pelo chamador.
   - O repositório verifica se a chave já foi registrada (`existeChaveIdempotencia`). Caso positivo, rejeita a duplicação retornando `IDEMPOTENCY_CONFLICT` (HTTP 409).
   - Ao persistir o pedido com sucesso, a chave é registrada com TTL atrelada ao recurso criado.
2. **Idempotência Natural Orientada a Estado:**
   - Comandos com efeito sobre agregados existentes validam a máquina de estados.
   - No caso de webhooks assíncronos (`ProcessarWebhookDePagamento`), caso o pedido já se encontre no estado esperado (`PAGO` ou `CANCELADO`), o comando é considerado concluído com sucesso e retorna sem emitir eventos repetidos.

## Consequências
### Positivas
- Eliminação de transações duplicadas e cobranças duplas.
- Tolerância total a repetições de requisições de rede.

### Negativas
- Exige armazenamento e política de expiração (TTL) de chaves de idempotência na camada de persistência.

