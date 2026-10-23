# ADR 007: Ferramenta de Testes Arquiteturais (Fitness Functions)

## Status
Aceito

## Contexto
Em arquiteturas limpas e hexagonais, é comum a degradação arquitetural ao longo do tempo ("arquitetura hexagonal de fachada"), onde desenvolvedores acidentalmente importam classes de adaptadores concretos dentro do domínio ou da aplicação. Revisões manuais de código são propensas a falhas humanas.

## Decisão
Adotar o **dependency-cruiser** integrado ao Vitest como função de aptidão arquitetural (Fitness Function) automatizada no pipeline de CI (ARQ-01 a ARQ-05):
1. O arquivo `.dependency-cruiser.json` estabelece regras rígidas:
   - `domain` não pode importar nada de `application`, `adapters` ou `bootstrap`.
   - `application` só pode importar de `domain` e de si mesma (`application/port`).
   - `adapters` podem importar `application` e `domain`, mas adaptadores de saída não se cruzam (`persistence` não conhece `payment`) e adaptadores de entrada não importam adaptadores de saída.
   - Apenas o `bootstrap` conhece as classes concretas de todos os adaptadores.
2. A suíte de testes `tests/architecture/dependency.test.ts` executa a validação e quebra o build caso qualquer regra seja violada.

## Consequências
### Positivas
- Garantia matemática e estática de conformidade arquitetural em tempo de compilação/teste.
- Impossibilita acoplamento indevido ou ciclos acidentais entre camadas.

### Negativas
- Necessidade de configuração prévia e manutenção das regras de dependência conforme o projeto evolui.

