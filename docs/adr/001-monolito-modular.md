# ADR 001: Monolito Modular em vez de Microsserviços

## Status
Aceito

## Contexto
O Order Hub é um serviço que gerencia o ciclo de vida completo de pedidos de e-commerce, integrando reserva de estoque, confirmação de pagamento e emissão de notificações. Em projetos de estudo ou estágios iniciais de produto, adotar microsserviços precocemente introduz alta complexidade operacional (deploy distribuído, latência de rede, particionamento de rede, consistência eventual complexa e rastreamento distribuído).

## Decisão
Adotar uma arquitetura de **Monolito Modular** com fronteiras explícitas guiadas pela **Arquitetura Hexagonal (Portas e Adaptadores)**:
- Todo o sistema reside no mesmo repositório e processo de execução.
- As fronteiras entre Domínio, Aplicação e Adaptadores são estritamente isoladas e validadas por testes arquiteturais automatizados.
- A comunicação entre módulos ocorre via portas e casos de uso ou eventos de domínio em memória.

## Alternativas Consideradas
1. **Microsserviços Distribuídos:** Rejeitado pelo alto custo de infraestrutura e sobrecarga desnecessária para o escopo do projeto.
2. **Monolito Tradicional em Camadas (Layered):** Rejeitado por permitir vazamento frequente de detalhes de banco/ORM para o domínio (anemia de domínio).

## Consequências
### Positivas
- Facilidade de deploy, depuração e execução local sem dependências externas obrigatórias (ARQ-62).
- Garantia de consistência transacional atômica onde necessário via `UnitOfWork`.
- Permite futura extração para microsserviços caso necessário, já que as portas e contratos são desacoplados.

### Negativas
- Necessidade de disciplina rigorosa de código e testes estáticos contínuos para evitar acoplamento acidental (garantido pelo `dependency-cruiser`).

