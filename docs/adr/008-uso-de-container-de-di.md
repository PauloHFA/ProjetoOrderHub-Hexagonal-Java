# ADR 008: Uso de Contêiner de Injeção de Dependências (DI)

## Status
Aceito

## Contexto
A inversão de dependência (Princípio D do SOLID) exige que módulos de alto nível (casos de uso) dependam de abstrações (portas), e não de detalhes de implementação. No entanto, o uso indiscriminado de Service Locators ou injeções globais em qualquer camada espalha dependências de infraestrutura por todo o sistema.

## Decisão
1. **Injeção estritamente por Construtor (ARQ-60):** Todas as classes de domínio e casos de uso recebem suas dependências explicitamente via construtor.
2. **Contêiner de DI Restrito ao Bootstrap (ARQ-63):**
   - O contêiner de DI (`tsyringe`) é **opcional** e restrito ao diretório `src/bootstrap/`.
   - Nenhuma classe em `src/domain/`, `src/application/` ou adaptadores individuais chama o container diretamente para resolver dependências em tempo de execução.
   - O `src/bootstrap/container.ts` atua como Composition Root único, conectando adaptadores e casos de uso para dois perfis:
     - Perfil de Produção: conecta implementações reais (Knex, Stripe, etc.).
     - Perfil de Teste (ARQ-62): conecta exclusivamente adaptadores em memória, permitindo execução integral sem bancos de dados ou serviços externos ativos.

## Consequências
### Positivas
- Código altamente testável e desacoplado de bibliotecas de terceiros.
- Zero contaminação de Service Locator dentro dos adaptadores ou casos de uso.
- Capacidade de subir o sistema inteiro em memória em milissegundos.

### Negativas
- Configuração explícita de fiação (wiring) centralizada no bootstrap.

