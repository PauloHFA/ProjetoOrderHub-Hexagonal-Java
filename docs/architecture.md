# Arquitetura do Order Hub

## 1. Visão Geral
O **Order Hub** é um sistema para orquestração do ciclo de vida de pedidos em um e-commerce. O projeto foi projetado e implementado aplicando rigorosamente a **Arquitetura Hexagonal (Portas e Adaptadores)** combinada aos princípios **SOLID** e práticas avançadas de **Domain-Driven Design (DDD)**.

---

## 2. Diagrama Hexagonal

```
               [ ADAPTADORES DE ENTRADA (Driving / Primary) ]
       REST Controller (Fastify)   |   CLI Adapter   |   Webhook Gateway
                                     │
                                     ▼ (usam)
                    ┌───────────────────────────────────┐
                    │     PORTAS DE ENTRADA (In Ports)  │
                    │   - CriarPedidoUseCase            │
                    │   - ReservarEstoqueUseCase        │
                    │   - ConfirmarPagamentoUseCase     │
                    │   - CancelarPedidoUseCase         │
                    │   - ConsultarPedidoUseCase        │
                    │   - ListarPedidosDoClienteUseCase │
                    ├───────────────────────────────────┤
                    │         CAMADA DE APLICAÇÃO       │
                    │   - Orquestra fluxos              │
                    │   - Gerencia transações (UoW)     │
                    │   - Sem regras de negócio         │
                    ├───────────────────────────────────┤
                    │           NÚCLEO DE DOMÍNIO       │
                    │   - Agregado Raiz Pedido          │
                    │   - Value Objects imutáveis       │
                    │   - Máquina de estados            │
                    │   - Eventos de Domínio            │
                    ├───────────────────────────────────┤
                    │     PORTAS DE SAÍDA (Out Ports)   │
                    │   - PedidoRepository              │
                    │   - UnitOfWork                    │
                    │   - GatewayDePagamento            │
                    │   - PublicadorDeEventos           │
                    │   - Notificador                   │
                    │   - Relogio & GeradorDeId         │
                    └───────────────────────────────────┘
                                     ▲ (implementam)
                                     │
               [ ADAPTADORES DE SAÍDA (Driven / Secondary) ]
     PostgreSQL/Knex | Outbox Relay | In-Memory Fakes | Stripe Gateway (ACL)
```

---

## 3. Máquina de Estados do Pedido

A transição de status do pedido é estritamente protegida no agregado raiz `Pedido`:

```
CRIADO ──────────► ESTOQUE_RESERVADO ──────────► PAGO ──────────► ENVIADO ──────────► ENTREGUE
  │                       │                       │
  └───────────────────────┴───────────────────────┴────────► CANCELADO
```

Qualquer transição inválida (ex.: `ENTREGUE -> CRIADO` ou `PAGO -> CRIADO`) é rejeitada pelo domínio com `BusinessRuleViolationError`.

---

## 4. Camadas e Responsabilidades

### 4.1 Domínio (`src/domain/`)
- **Puro e Isolado (ARQ-01, ARQ-12):** Nenhuma dependência externa, framework, banco ou biblioteca de JSON.
- **Value Objects:** `PedidoId`, `ClienteId`, `Sku`, `Quantidade`, `Dinheiro`, `Email`, `Instante`. Todos imutáveis e auto-validados na criação (ARQ-11).
- **Invariantes Protegidos (ARQ-10):** Métodos de negócio expressivos (`reservarEstoque()`, `confirmarPagamento()`, `cancelar()`). Sem setters públicos.
- **Eventos de Domínio (ARQ-4.4):** `PedidoCriado`, `EstoqueReservado`, `PagamentoConfirmado`, `PedidoCancelado`.

### 4.2 Aplicação (`src/application/`)
- **1 Caso de Uso = 1 Classe = 1 Operação (ARQ-20):** `CriarPedido`, `ReservarEstoque`, etc.
- **Orquestração Pura (ARQ-21):** Carrega o agregado, executa a regra no domínio, persiste e despacha eventos.
- **Separação CQS (ARQ-23):** Comandos alteram estado; consultas apenas lêem e não emitem eventos.
- **Fronteira Transacional (ARQ-24):** Gerenciada via porta `UnitOfWork`.
- **Tratamento de Erros Tipados (ARQ-25):** Retorno de `Result<Ok, Err>`.

### 4.3 Adaptadores (`src/adapters/`)
- **Adaptadores de Entrada (`src/adapters/in/`):**
  - `rest/`: Controladores Fastify finos (validação de formato, conversão para Command, retorno tipado).
  - `cli/`: Adaptador de linha de comando (`OrderHubCli`) como prova de troca de interface primária sem alterar o núcleo (Fase 7).
- **Adaptadores de Saída (`src/adapters/out/`):**
  - `persistence/`: Repositório Knex/SQL com mapeamento explícito e tabela Transactional Outbox.
  - `payment/`: Anti-Corruption Layer (`GatewayDePagamentoACL`) isolando provedores externos (Stripe / Mock).
  - `notification/`: `EmailNotificador` e composição de canais.
  - `inmemory/`: Fakes em memória para execução completa sem infraestrutura (ARQ-62).

### 4.4 Bootstrap (`src/bootstrap/`)
- Composition Root único (ARQ-04).
- Montagem do grafo de dependências via injeção por construtor (ARQ-60, ARQ-63).
- Configuração de perfil de produção (com banco e serviços) e perfil de testes (100% em memória).

---

## 5. Princípios SOLID Aplicados

| Princípio | Aplicação no Order Hub |
| :--- | :--- |
| **S (Single Responsibility)** | 1 caso de uso por classe; controladores que apenas traduzem formato; agregados focados no ciclo de vida de pedidos. |
| **O (Open/Closed)** | Novos meios de pagamento, canais de notificação ou interfaces de entrada (CLI/gRPC) são adicionados via novos adaptadores sem alterar o domínio. |
| **L (Liskov Substitution)** | Testes de contrato unificados garantem que adaptadores reais (SQL/Knex) e adaptadores em memória (InMemory) são 100% intercambiáveis. |
| **I (Interface Segregation)** | Portas pequenas e sob medida para o consumidor (`PedidoRepository`, `PublicadorDeEventos`, `GatewayDePagamento`), sem repositórios genéricos gigantes com CRUD inflado. |
| **D (Dependency Inversion)** | O domínio e a aplicação definem as portas (interfaces); a infraestrutura externa depende e implementa essas portas; o bootstrap conecta tudo. |

