# ADR 001: Arquitetura Hexagonal (Ports and Adapters)

## Status
Aceito

## Contexto
O projeto Order Hub precisa de uma arquitetura que:
- Isolamento do domínio de frameworks e infraestrutura
- Facilite testes unitários e de integração
- Permita trocar adaptadores (banco, gateway de pagamento, notificações) sem afetar o domínio
- Suporte evolução independente das camadas

## Decisão
Adotar **Arquitetura Hexagonal (Ports and Adapters)** com as seguintes camadas:

```
src/
├── domain/           # Núcleo - regras de negócio puras, sem dependências externas
├── application/      # Casos de uso - orquestra o domínio, define portas (interfaces)
├── adapters/         # Adaptadores - implementações concretas das portas
│   ├── in/           # Adaptadores de entrada (REST, CLI, Messaging)
│   └── out/          # Adaptadores de saída (DB, Payment Gateway, Notifications)
└── bootstrap/        # Composition root - wiring de dependências
```

### Regras de Dependência (enforced by dependency-cruiser)
1. **domain** não importa nada fora de si mesmo
2. **application** importa apenas domain e application/port
3. **adapters** importam application e domain; nunca o contrário
4. **bootstrap** é o único que conhece classes concretas de todos os adaptadores
5. Não há ciclos entre camadas

## Consequências

### Positivas
- Domínio puro, testável sem mocks de infraestrutura
- Troca de PostgreSQL para MongoDB afeta apenas adapters/out/persistence
- Troca de Stripe para MercadoPago afeta apenas adapters/out/payment
- Testes rápidos com adaptadores em memória
- Separação clara de responsabilidades

### Negativas
- Mais boilerplate inicial (interfaces, mappers)
- Curva de aprendizado para time não familiarizado
- Overhead de mapeamento domain ↔ persistence

## Implementação
- Validação arquitetural via `npm run test:arch` (dependency-cruiser)
- Container de DI com tsyringe (teste vs produção)
- Contratos de porta definidos em `application/port/out/`
- Testes de contrato para garantir implementações corretas

## Referências
- [Ports and Adapters - Alistair Cockburn](https://alistair.cockburn.us/hexagonal-architecture/)
- [Dependency Cruiser](https://github.com/sverweij/dependency-cruiser)