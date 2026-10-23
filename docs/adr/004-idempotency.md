# ADR 004: Idempotência em APIs e Processamento de Webhooks

## Status
Aceito

## Contexto
Operações de rede podem falhar ou ser reenviadas:
- Cliente reenvia POST /pedidos por timeout
- Gateway de pagamento reenvia webhook (Stripe, MercadoPago, etc.)
- Retry automático de message broker

Sem idempotência, isso causa:
- Pedidos duplicados
- Pagamentos duplicados
- Estoque reservado incorretamente

## Decisão
Implementar idempotência em **duas camadas**:

### 1. Nível API (HTTP) - Chave de Idempotência
- Cliente envia header `Idempotency-Key` ou campo `idempotencyKey` no body
- Servidor armazena chave + resultado na tabela `chaves_idempotencia`
- Requisições subsequentes com mesma chave retornam resultado armazenado

```typescript
// Exemplo de uso
POST /pedidos
{
  "clienteId": "uuid",
  "idempotencyKey": "cliente-123-pedido-456",  // Chave única por operação de negócio
  "itens": [...]
}
```

### 2. Nível Domínio - Webhooks de Pagamento
- Webhook contém `gateway_event_id` único por evento do gateway
- Tabela `webhooks_pagamento` com unique constraint `(gateway, gateway_event_id)`
- Processamento verifica se já foi processado antes de aplicar mudanças

```sql
CREATE TABLE webhooks_pagamento (
  id UUID PRIMARY KEY,
  gateway VARCHAR(50) NOT NULL,
  evento_tipo VARCHAR(100) NOT NULL,
  gateway_event_id VARCHAR(100) NOT NULL,
  payload JSONB NOT NULL,
  status VARCHAR(30) NOT NULL,
  erro_processamento TEXT NULL,
  recebido_em TIMESTAMPTZ NOT NULL,
  processado_em TIMESTAMPTZ NULL,
  UNIQUE (gateway, gateway_event_id)  -- Chave de idempotência
);
```

### 3. Nível Domínio - Processamento Idempotente no Agregado
O caso de uso `ProcessarWebhookDePagamento` verifica estado atual antes de processar:
- Se pedido já está PAGO/ENVIADO/ENTREGUE → ignora (idempotente)
- Se webhook RECUSADO para pedido já cancelado → ignora

## Consequências

### Positivas
- **Segurança**: Reenvio não causa efeitos colaterais
- **Confiabilidade**: Gateways de pagamento exigem idempotência
- **Simplicidade**: Cliente não precisa implementar lógica complexa de retry

### Negativas
- **Storage**: Tabelas de chaves crescem (TTL para limpeza)
- **Memória**: In-memory implementation não persiste entre restarts
- **Complexidade**: Requer coordenação entre camadas

## Implementação

### Tabela chaves_idempotencia
```sql
CREATE TABLE chaves_idempotencia (
  chave VARCHAR(100) PRIMARY KEY,
  pedido_id UUID NULL REFERENCES pedidos(id),
  criado_em TIMESTAMPTZ NOT NULL,
  expira_em TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_chaves_idempotencia_expira_em ON chaves_idempotencia(expira_em);
```

### TTL padrão: 24 horas (86400 segundos)
- Job de limpeza remove chaves expiradas periodicamente

### Fluxo no Use Case CriarPedido
```typescript
async executar(cmd: CriarPedidoCommand) {
  // 1. Verificar idempotência ANTES de iniciar UoW
  const existe = await this.pedidos.existeChaveIdempotencia(cmd.idempotencyKey);
  if (existe) throw new Error('IDEMPOTENCY_CONFLICT');
  
  // 2. Dentro da transação
  await this.uow.executar(async () => {
    // ... criar pedido ...
    
    // 3. Registrar chave com pedidoId para referência
    await this.pedidos.registrarChaveIdempotencia(cmd.idempotencyKey, pedido.id);
  });
}
```

## Referências
- [Idempotency Keys - Stripe](https://stripe.com/docs/api/idempotent_requests)
- [Building Idempotent APIs - AWS](https://aws.amazon.com/builders-library/building-idempotent-apis/)