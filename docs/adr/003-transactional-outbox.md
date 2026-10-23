# ADR 003: Transactional Outbox Pattern para Eventos de Domínio

## Status
Aceito

## Contexto
O domínio emite eventos (PedidoCriado, EstoqueReservado, PagamentoConfirmado, PedidoCancelado) que precisam ser publicados para:
- Notificações (email, push)
- Integração com outros sistemas (ERP, CRM)
- Auditoria e analytics

O desafio: **garantir atomicidade** entre persistência do agregado e publicação dos eventos. Não podemos:
- Publicar antes de persistir (risco de evento órfão se persistência falhar)
- Persistir sem publicar (risco de evento perdido se publicação falhar)
- Usar transação distribuída (complexidade, acoplamento)

## Decisão
Implementar **Transactional Outbox Pattern**:

### Como funciona
1. **Mesma transação**: Salvar agregado + eventos na mesma transação do banco
2. **Tabela outbox**: `eventos_dominio` armazena eventos não publicados
3. **Publisher assíncrono**: Processo separado lê eventos não publicados e publica
4. **Idempotência**: Consumidores devem ser idempotentes (chave: aggregate_id + aggregate_version)

### Schema da tabela outbox
```sql
CREATE TABLE eventos_dominio (
  id UUID PRIMARY KEY,
  tipo VARCHAR(100) NOT NULL,
  payload JSONB NOT NULL,
  aggregate_id UUID NOT NULL,
  aggregate_version INTEGER NOT NULL,
  ocorrido_em TIMESTAMPTZ NOT NULL,
  publicado_em TIMESTAMPTZ NULL,
  tentativas INTEGER DEFAULT 0,
  erro_ultima_tentativa TEXT NULL
);
```

### Fluxo
```
Use Case
    │
    ▼
UnitOfWork.iniciar()
    │
    ├── PedidoRepository.salvar(pedido)  ──► INSERT/UPDATE pedidos, pedido_itens
    │
    ├── PedidoRepository.salvarEventos(pedido.eventos)  ──► INSERT eventos_dominio
    │
    ▼
UnitOfWork.commit()  ◄── Transação única, atômica
    │
    ▼
Publisher (background)
    │
    ├── SELECT * FROM eventos_dominio WHERE publicado_em IS NULL AND tentativas < 5
    │
    ├── Publicar no message broker (Kafka, RabbitMQ, etc.)
    │
    ├── UPDATE eventos_dominio SET publicado_em = NOW() WHERE id IN (...)
    │
    └── Se falhar: UPDATE tentativas = tentativas + 1, erro_ultima_tentativa = ...
```

## Consequências

### Positivas
- **Atomicidade garantida**: Evento só existe se agregado persistido
- **Resiliência**: Retry automático com backoff via tentativas
- **Ordem preservada**: Eventos publicados na ordem de ocorrência
- **Auditoria**: Histórico completo de eventos no banco
- **Replay**: Possível reprocessar eventos (útil para novos consumidores)

### Negativas
- **Latência**: Evento não é publicado instantaneamente
- **Complexidade**: Requer publisher background + monitoramento
- **Duplicação**: Consumidores devem ser idempotentes
- **Storage**: Tabela cresce se publisher parar

## Implementação
- `PedidoRepository.salvarEventos()` - Insere na tabela outbox
- `PedidoRepository.buscarEventosNaoPublicados()` - Para publisher
- `PedidoRepository.marcarEventosComoPublicados()` - Marca como publicados
- `PedidoRepository.registrarFalhaPublicacaoEvento()` - Incrementa tentativas
- Publisher roda como job separado (cron, worker, ou sidecar)

## Alternativas Consideradas
1. **Dual Write** - Rejeitado: risco de inconsistência
2. **CDC (Change Data Capture)** - Considerado para futuro (Debezium)
3. **Event Sourcing** - Rejeitado: overkill para este domínio

## Referências
- [Transactional Outbox - Microsoft](https://learn.microsoft.com/en-us/azure/architecture/reference-architectures/saga/saga)
- [Outbox Pattern - Chris Richardson](https://microservices.io/patterns/data/transactional-outbox.html)