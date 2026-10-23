# Regras Arquiteturais do Order Hub (ARQ-01 a ARQ-73)

Tabela de rastreabilidade completa das regras arquiteturais obrigatórias definidas no documento de especificação do Order Hub.

---

## 1. Dependências e Limites Arquiteturais

| ID | Regra | Verificação | Status |
| :--- | :--- | :--- | :--- |
| **ARQ-01** | `domain` não importa nada fora de si mesmo (nem framework, nem ORM, nem biblioteca de JSON) | Teste arquitetural (`dependency-cruiser`) | ✅ Implementado |
| **ARQ-02** | `application` importa apenas `domain` e suas próprias portas | Teste arquitetural (`dependency-cruiser`) | ✅ Implementado |
| **ARQ-03** | `adapters` importam `application` e `domain`; nunca o contrário, e nunca um adaptador importa outro | Teste arquitetural (`dependency-cruiser`) | ✅ Implementado |
| **ARQ-04** | Somente o `bootstrap` (composition root) conhece as classes concretas de todos os adaptadores | Teste arquitetural (`dependency-cruiser`) | ✅ Implementado |
| **ARQ-05** | Não há ciclos entre pacotes/camadas | Análise estática (`dependency-cruiser`) | ✅ Implementado |

---

## 2. Domínio e Agregados

| ID | Regra | Verificação | Status |
| :--- | :--- | :--- | :--- |
| **ARQ-10** | Entidades protegem seus invariantes: sem setters públicos; mudança de estado por métodos com nome de negócio (`confirmarPagamento()`, `reservarEstoque()`) | Revisão + Testes de domínio (`tests/domain/`) | ✅ Implementado |
| **ARQ-11** | Nenhum tipo primitivo "solto" para conceitos de negócio (`Dinheiro`, `Quantidade`, `Sku`, `Email`, etc.) | Revisão de código + Testes de VOs | ✅ Implementado |
| **ARQ-12** | Domínio puro: sem I/O, sem `Date.now()`, sem aleatoriedade direta; tempo e IDs entram por parâmetro ou porta | Teste arquitetural + Testes unitários | ✅ Implementado |
| **ARQ-13** | Cada agregado é persistido e carregado inteiro, e uma transação altera um agregado | Testes de integração e casos de uso | ✅ Implementado |
| **ARQ-14** | Referências entre agregados são feitas por identidade (`PedidoId`, `ClienteId`), nunca por referência a objeto em memória | Revisão de modelo de domínio | ✅ Implementado |
| **ARQ-15** | Regras que não cabem em uma entidade vão para serviços de domínio sem estado | Revisão de domínio | ✅ Implementado |

---

## 3. Aplicação e Casos de Uso

| ID | Regra | Verificação | Status |
| :--- | :--- | :--- | :--- |
| **ARQ-20** | Um caso de uso = uma classe = uma operação do sistema, exposta por uma porta de entrada | Revisão da camada `application/usecase` | ✅ Implementado |
| **ARQ-21** | O caso de uso orquestra: carrega, chama o domínio, persiste, publica eventos. Nenhuma regra de negócio reside nele | Revisão + Cobertura de domínio | ✅ Implementado |
| **ARQ-22** | Entrada e saída dos casos de uso usam DTOs próprios (`Command` / `Result`), nunca entidades expostas diretamente | Teste de integração / tipagem TS | ✅ Implementado |
| **ARQ-23** | Comandos e consultas são separados (CQS): consultas não alteram estado e não emitem eventos | Revisão de use cases | ✅ Implementado |
| **ARQ-24** | O caso de uso define a fronteira transacional por meio de uma porta `UnitOfWork` | Testes de caso de uso | ✅ Implementado |
| **ARQ-25** | Falhas esperadas retornam resultado tipado (`Result<Ok, ErroDeNegocio>`); exceções ficam restritas a falhas inesperadas | Revisão + Testes unitários | ✅ Implementado |

---

## 4. Portas (Contratos)

| ID | Regra | Verificação | Status |
| :--- | :--- | :--- | :--- |
| **ARQ-30** | Portas de saída são definidas pelo núcleo, na linguagem do domínio (`GatewayDePagamento.autorizar()`, e não `StripeClient.createCharge()`) | Revisão de interfaces em `port/out` | ✅ Implementado |
| **ARQ-31** | Portas pequenas e sob medida para o consumidor (ISP); sem repositório genérico inchado com CRUD indiscriminado | Revisão de portas | ✅ Implementado |
| **ARQ-32** | Todo contrato de porta tem suite de testes de contrato executada contra todas as implementações (inclusive fakes em memória) para garantir LSP | Testes de contrato (`tests/contracts/`) | ✅ Implementado |
| **ARQ-33** | Portas não vazam tecnologia: sem `SQLException`, sem `HttpResponse`, sem tipos de SDK de terceiros | Teste arquitetural / verificação de tipos | ✅ Implementado |
| **ARQ-34** | Relógio, gerador de ID e publicador de eventos são portas formais (`Relogio`, `GeradorDeId`, `PublicadorDeEventos`) | Revisão da camada `application/port/out` | ✅ Implementado |

---

## 5. Adaptadores

| ID | Regra | Verificação | Status |
| :--- | :--- | :--- | :--- |
| **ARQ-40** | Adaptadores de entrada só traduzem: validação de formato, conversão para `Command`, chamada da porta, conversão do resultado para HTTP/CLI | Revisão de controllers e CLI | ✅ Implementado |
| **ARQ-41** | Validação em três níveis: formato (adaptador), consistência da requisição (aplicação), regra de negócio (domínio) | Testes E2E e unitários | ✅ Implementado |
| **ARQ-42** | Mapeamento explícito entre modelo de persistência e domínio (nenhuma anotação de ORM no domínio) | Mappers dedicados (`PedidoMapper`) | ✅ Implementado |
| **ARQ-43** | Adaptadores de saída tratam falhas técnicas (timeout, erros de rede) e as traduzem para erros da porta | Testes de integração | ✅ Implementado |
| **ARQ-44** | Anti-corruption layer (ACL): o modelo do gateway externo não entra no domínio; o adaptador converte tudo | `GatewayDePagamentoACL` | ✅ Implementado |
| **ARQ-45** | Um adaptador por tecnologia por porta; trocar a tecnologia não altera `domain` nem `application` | Fase 7: CLI Adapter testado sem tocar no núcleo | ✅ Implementado |

---

## 6. Consistência, Mensageria e Idempotência

| ID | Regra | Verificação | Status |
| :--- | :--- | :--- | :--- |
| **ARQ-50** | Eventos de domínio são publicados via Transactional Outbox: gravados na mesma transação do agregado e enviados depois | Tabela `outbox_events` + `PublicadorDeEventosKnex` | ✅ Implementado |
| **ARQ-51** | Todo comando exposto externamente é idempotente (chave de idempotência ou estado que impeça repetição) | Testes de repetição E2E (`tests/e2e/api-e2e.test.ts`) | ✅ Implementado |
| **ARQ-52** | Consumidores de evento toleram entrega duplicada e fora de ordem | Lógica idempotente nos use cases | ✅ Implementado |
| **ARQ-53** | Concorrência tratada com controle otimista (`versao` do agregado) | Coluna `versao` + verificação condicional | ✅ Implementado |
| **ARQ-54** | Operações entre agregados usam consistência eventual com compensação (ex.: cancelar e estornar caso pagamento falhe) | Casos de uso de compensação | ✅ Implementado |

---

## 7. Composição e Configuração

| ID | Regra | Verificação | Status |
| :--- | :--- | :--- | :--- |
| **ARQ-60** | Injeção por construtor; sem service locator nem singleton global no domínio ou adaptadores | Revisão de construtores | ✅ Implementado |
| **ARQ-61** | Configuração lida só no bootstrap e entregue como objetos tipados; o núcleo não lê `process.env` | Configuração isolada no bootstrap | ✅ Implementado |
| **ARQ-62** | A aplicação deve subir com todos os adaptadores em memória (perfil de teste), sem requerer infraestrutura | `obterContainerTeste()` executa 100% dos testes sem banco | ✅ Implementado |
| **ARQ-63** | Contêiner de DI é opcional e restrito ao bootstrap | Adaptadores e use cases desacoplados do container | ✅ Implementado |

---

## 8. Observabilidade e Operação

| ID | Regra | Verificação | Status |
| :--- | :--- | :--- | :--- |
| **ARQ-70** | Logs estruturados com `correlationId` propagado da entrada até os adaptadores de saída | Logger estruturado Pino | ✅ Implementado |
| **ARQ-71** | Cada caso de uso emite métricas via decorator, sem poluir a lógica do caso de uso | Decorator de métricas | ✅ Implementado |
| **ARQ-72** | Health check de liveness (`/health`) e readiness (`/ready`) | Endpoints REST no Fastify | ✅ Implementado |
| **ARQ-73** | Logs não contêm dados sensíveis (número de cartão, senhas, documentos ou emails completos) | Mascaramento de dados no logging | ✅ Implementado |

