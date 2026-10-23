# ADR 002: Aggregate Design - Pedido como Aggregate Root

## Status
Aceito

## Contexto
O domínio de pedidos precisa garantir consistência transacional e regras de negócio complexas:
- Transições de estado controladas (CRIADO → ESTOQUE_RESERVADO → PAGO → ENVIADO → ENTREGUE)
- Validações de negócio (SKU único, total calculado, valor do pagamento)
- Emissão de eventos de domínio para integração
- Controle de concorrência otimista

## Decisão
Modelar **Pedido** como **Aggregate Root** (Raiz de Agregado) seguindo DDD:

### Estrutura do Agregado
```
Pedido (Aggregate Root)
├── Identidade: PedidoId (UUID)
├── ClienteId, ClienteEmail
├── Itens: ItemDoPedido[] (Value Objects)
├── Status: StatusDoPedido (Value Object)
├── Versão: number (concorrência otimista)
├── Eventos: QualquerEventoDeDominio[] (Domain Events)
└── Timestamps: criadoEm, atualizadoEm (Instante)
```

### Regras do Agregado
1. **Consistência transacional**: Todas as mudanças de estado acontecem dentro do agregado
2. **Encapsulamento**: Itens não são expostos diretamente, apenas via métodos do agregado
3. **Validação na criação**: Pelo menos 1 item, SKUs únicos
4. **Máquina de estados**: Transições validadas via `podeTransicionar()`
5. **Eventos de domínio**: Emitidos automaticamente nas transições de estado
6. **Concorrência otimista**: Versão incrementada a cada mudança

### Value Objects Internos
- **ItemDoPedido**: SKU, Quantidade, PreçoUnitário, Subtotal
- **Dinheiro**: Valor + Moeda (operações aritméticas seguras)
- **Quantidade**: Inteiro positivo
- **Email**: Normalizado, validado, mascarável para logs
- **Identificadores**: Tipados (PedidoId, ClienteId, Sku, etc.)

## Consequências

### Positivas
- Garante invariantes de negócio no nível do código
- Transições de estado explícitas e auditáveis
- Eventos de domínio para integração assíncrona
- Testável isoladamente (sem banco, sem framework)
- Versionamento previne lost updates

### Negativas
- Requer reconstrução cuidadosa do estado (método `reconstruir`)
- Eventos devem ser limpos após persistência (`limparEventos()`)
- Operações em lote requerem cuidado com versão

## Implementação
- `Pedido.criar()` - Factory method para novo agregado
- `Pedido.reconstruir()` - Para hidratação do banco
- Métodos de transição: `reservarEstoque()`, `confirmarPagamento()`, `cancelar()`, `marcarComoEnviado()`, `marcarComoEntregue()`
- `transicionarPara()` - Privado, valida via `podeTransicionar()`
- `verificarVersao()` - Para concorrência otimista

## Referências
- [DDD Aggregates - Martin Fowler](https://martinfowler.com/bliki/DDD_Aggregate.html)
- [Effective Aggregate Design - Vaughn Vernon](https://dddcommunity.org/library/vernon_2011/)