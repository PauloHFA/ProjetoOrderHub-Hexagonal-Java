# ADR 002: Estilo de Tratamento de Erros (Result vs. Exceções)

## Status
Aceito

## Contexto
O Order Hub possui falhas previsíveis de negócio (ex.: pedido já pago, valor incorreto, estoque indisponível, SKU duplicado, recurso não encontrado) e falhas inesperadas de infraestrutura (ex.: queda de banco de dados, falha de rede fatal). O uso indiscriminado de exceções (`try/catch`) para fluxo de controle prejudica a previsibilidade do código, mascara falhas e impede que o compilador auxilie na exaustividade do tratamento.

## Decisão
Adotar o padrão **Result Monad (`Result<T, E>`)** para falhas esperadas de negócio e reservar exceções (`throw`) exclusivamente para falhas técnicas catastróficas ou bugs inesperados (ARQ-25):
- Todos os casos de uso expõem métodos que retornam `Promise<Result<T, Error>>`.
- O tipo `Result` possui variantes explícitas `Ok<T>` e `Err<E>`.
- O domínio define erros tipados ricos (`DomainError`, `InvalidArgumentError`, `BusinessRuleViolationError`, `NotFoundError`, `IdempotencyConflictError`, `ConcurrencyError`).
- Os adaptadores de entrada inspecionam o `Result` retornado e realizam o mapeamento HTTP correspondente (200, 201, 400, 404, 409).

## Alternativas Consideradas
1. **Lançamento de Exceções (`throw new BusinessException(...)`):** Rejeitado por obscurecer as assinaturas das portas de entrada e depender de handlers globais opacos.
2. **Códigos de Retorno Numéricos (estilo C):** Rejeitado por falta de expressividade e tipos fracos.

## Consequências
### Positivas
- Tipagem estrita: a assinatura do caso de uso documenta explicitamente que ele pode falhar.
- Os adaptadores são forçados a tratar os dois cenários (sucesso e falha esperada).
- Excelente ergonomia para testes unitários com checagens funcionais simples (`isOk`, `isErr`).

### Negativas
- Leve verbosidade comparada ao estilo puramente imperativo.

