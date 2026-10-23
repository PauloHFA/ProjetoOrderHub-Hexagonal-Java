# Order Hub – Arquitetura Hexagonal (Java 21 / Spring Boot 3)

Implementação de referência em **Java 21**, **Spring Boot 3.3.4** e **ArchUnit** do case **Order Hub**, baseada nos princípios de Arquitetura Hexagonal (Ports & Adapters) de Alistair Cockburn e Clean Architecture.

Esta versão é a contraparte idêntica e fidedigna da implementação em TypeScript (`ProjetoOrderHub-Hexagonal`), respeitando todas as regras de negócio (DOM), casos de uso (APP), regras arquiteturais (ARQ), adaptadores (DRV / DRV-01 / DRV-02 / SEC) e testes automatizados.

---

## 🏛️ Princípios Arquiteturais e Restrições Rígidas

1. **Domínio Puro (0 Dependências Externas):**
   - Pacote `com.orderhub.domain` não possui nenhuma anotação de frameworks (`@Component`, `@Entity`, etc.).
   - Tipos de domínio ricos: `Dinheiro`, `Quantidade`, `Email`, `Instante`, `PedidoId`, `ClienteId`, `ProdutoId`.
   - Máquina de estados finita explícita (`CRIADO` -> `AGUARDANDO_PAGAMENTO` -> `PAGO` / `CANCELADO`).

2. **Aplicação Pura (Use Cases & Ports):**
   - Casos de uso implementados como classes POJO puras em `com.orderhub.application.usecase`.
   - Portas de entrada (`port.in`) e saída (`port.out`) desacopladas.
   - Retornos funcionais tipados com padrão `Result<T, DomainError>` usando **Java 21 Sealed Interfaces**.

3. **Inversão de Dependência & Composition Root:**
   - O Spring Boot é mantido exclusivamente no adaptador primário REST e na raiz de composição (`com.orderhub.bootstrap.DependencyInjectionConfig`).
   - Os use cases são instanciados como Beans explícitos, garantindo que o núcleo permaneça agnóstico a frameworks.

4. **Fitness Functions com ArchUnit:**
   - 5 regras arquiteturais executadas a cada `mvn test`:
     - **ARQ-01:** Domínio não depende de ninguém além de si mesmo e da JRE.
     - **ARQ-02:** Aplicação depende exclusivamente de Domínio e de si mesma (nunca de Adaptadores ou Bootstrap).
     - **ARQ-03:** Adaptadores secundários implementam Portas de Saída (`port.out`).
     - **ARQ-04:** Adaptadores primários invocam Portas de Entrada (`port.in`).
     - **ARQ-05:** Injeção de dependência e Spring isolados no Bootstrap e camada de Adapters.

---

## 🚀 Como Executar

### Pré-requisitos
- **Java 21** (JDK 21+)
- **Maven 3.9+** (ou `./mvnw`)
- **Docker / Docker Compose** (opcional, para PostgreSQL)

### Comandos Rápidos (Makefile ou Maven)
```bash
# Rodar todos os testes (Unitários, ArchUnit, E2E MockMvc)
mvn test
# ou:
make test

# Rodar apenas as Fitness Functions do ArchUnit
mvn test -Dtest=ArchitectureFitnessTest
# ou:
make arch

# Executar a aplicação Spring Boot
mvn spring-boot:run
# ou:
make dev

# Executar a CLI interativa
mvn spring-boot:run -Dspring-boot.run.arguments="consultar,ped-001"
```

---

## 📖 Documentação da API (Swagger / OpenAPI)
Quando a aplicação estiver em execução:
- **Swagger UI:** [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
- **OpenAPI JSON:** [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

---

## 🧪 Estrutura de Testes
- `ArchitectureFitnessTest`: Validação automática de acoplamento com ArchUnit.
- `PedidoTest` & `ValueObjectsTest`: Testes unitários puros de domínio (regras DOM-01 a DOM-13).
- `CriarPedidoTest` & `UseCasesLifecycleTest`: Testes de use cases e ciclo de vida completo com adaptadores in-memory.
- `OrderHubCliTest`: Teste de aceitação do driver CLI.
- `PedidoApiE2ETest`: Teste de integração E2E com `MockMvc`.

