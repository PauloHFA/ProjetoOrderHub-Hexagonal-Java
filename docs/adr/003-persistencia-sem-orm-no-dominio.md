# ADR 003: Estratégia de Persistência e Mapeamento (Sem ORM no Domínio)

## Status
Aceito

## Contexto
ORMs tradicionais (como TypeORM, Hibernate ou Prisma) frequentemente forçam o domínio a aceitar anotações (`@Entity`, `@Column`), campos mutáveis, getters/setters públicos e construtores padrão sem parâmetros, o que viola o princípio de invariantes protegidos (ARQ-10, ARQ-42). Além disso, relações lazy loading causam problemas graves de performance e acoplamento.

## Decisão
1. O domínio é **100% livre de anotações de persistência** ou referências a bibliotecas de banco.
2. O agregado `Pedido` e seus Value Objects são construídos via construtores privados ou factory methods puros com validação invariante.
3. Para o adaptador de persistência SQL, utiliza-se o **Knex.js** como query builder leve com mapeamento explícito bidirecional:
   - `PedidoMapper.toDomain(record, items)`: reconstrói o agregado puro a partir de tabelas relacionais.
   - `PedidoMapper.toPersistence(pedido)`: decompõe o agregado para os registros do banco.
4. Para testes rápidos e isolados (ARQ-62), implementa-se um `PedidoRepositoryInMemory` que respeita exatamente o mesmo contrato do repositório SQL.

## Alternativas Consideradas
1. **Prisma / TypeORM:** Rejeitados devido à poluição do modelo com esquemas gerados e anotações invasivas.
2. **Document Store Puro (MongoDB):** Considerado viável, mas descartado como padrão primário pela necessidade de exemplificar mapeamento relacional explícito e transações SQL ACID.

## Consequências
### Positivas
- Domínio totalmente puro e portável para qualquer tecnologia.
- SQL explícito e otimizado sem surpresas de lazy loading (N+1 queries).
- Suporte nativo a execução 100% em memória nos testes.

### Negativas
- Exige escrita e manutenção manual de classes e funções de mapeamento (`toDomain` e `toPersistence`).

