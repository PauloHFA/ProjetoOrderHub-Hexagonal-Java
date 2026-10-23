# ADR 006: Controle de Concorrência Otimista (Optimistic Locking)

## Status
Aceito

## Contexto
Múltiplas requisições simultâneas podem tentar alterar o mesmo agregado `Pedido` (por exemplo, um cancelamento concorrente com uma confirmação de pagamento ou alteração de reserva de estoque). O bloqueio pessimista (`SELECT FOR UPDATE`) degrada a performance do banco e pode ocasionar deadlocks.

## Decisão
Adotar **Controle de Concorrência Otimista (ARQ-53)** baseado em número de versão (`versao: number`):
1. A entidade `Pedido` mantém um campo interno `_versao`.
2. Ao persistir alterações em um pedido existente, a query SQL executa um update condicional:
   ```sql
   UPDATE pedidos SET status = ?, versao = versao + 1, atualizado_em = ?
   WHERE id = ? AND versao = ?
   ```
3. Se nenhuma linha for atualizada, identifica-se que o registro foi modificado concomitantemente por outra transação, disparando um erro de concorrência (`ConcurrencyError`).
4. A camada de aplicação captura o erro e responde com conflito (HTTP 409) ou aplica política de retry quando aplicável.

## Consequências
### Positivas
- Ausência de locks de banco longos, aumentando o throughput global.
- Detecção infalível de leituras e escritas concorrentes obsoletas.

### Negativas
- Em cenários com altíssima taxa de conflito simultâneo, algumas requisições falham e demandam reexecução do cliente.

