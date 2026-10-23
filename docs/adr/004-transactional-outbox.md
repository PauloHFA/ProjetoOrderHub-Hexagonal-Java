# ADR 004: Outbox Transacional para Publicação de Eventos de Domínio

## Status
Aceito

## Contexto
Quando um agregado é persistido e um evento de domínio deve ser emitido (ex.: `PedidoCriado`, `PagamentoConfirmado`), publicar diretamente em uma fila de mensageria (RabbitMQ, Kafka) após o commit pode resultar em perda de mensagens caso a aplicação caia antes da publicação (dual-write problem). Da mesma forma, publicar antes do commit pode gerar mensagens "fantasma" caso a transação no banco sofra rollback.

## Decisão
Adotar o padrão **Transactional Outbox (ARQ-50)**:
1. Durante a transação atômica do caso de uso (orquestrada pelo `UnitOfWork`), o agregado é salvo e seus eventos de domínio pendentes são gravados na tabela `outbox_events` na mesma transação de banco de dados.
2. Um relay assíncrono (ou rotina programada) lê os eventos pendentes da tabela outbox e os despacha para os destinatários/filas.
3. Após a publicação confirmada, o status do evento na tabela outbox é atualizado para `PUBLICADO` com timestamp.
4. Para testes em memória, o `PublicadorDeEventosInMemory` armazena e expõe eventos imediatamente sem requerer infraestrutura.

## Alternativas Consideradas
1. **Publicação síncrona pós-commit:** Rejeitada pelo risco de perda de eventos em caso de crash do processo.
2. **Two-Phase Commit (2PC):** Rejeitada pela complexidade, lentidão e suporte precário em sistemas modernos.

## Consequências
### Positivas
- Garantia de entrega *at-least-once* para todos os eventos de domínio.
- Nenhuma dependência síncrona com brokers externos dentro do ciclo de vida da requisição HTTP primária.

### Negativas
- Consumidores precisam ser idempotentes para tolerar eventuais entregas duplicadas (ARQ-52).
- Necessidade de processo em background (worker ou relay) para drenar a tabela outbox.

