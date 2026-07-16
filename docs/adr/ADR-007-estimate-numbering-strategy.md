# ADR-007 — Estratégia de Numeração de Orçamentos

- **Status:** Aceito
- **Data:** 2026-07-16
- **Sprint:** 10A — Estimate Domain & API
- **Decisores:** Francisco Costa

---

## Contexto

Cada `Estimate` precisa de um número legível por humanos, estável e único por empresa (ex: `ORC-2026-0001`), distinto do UUID interno. Este número é exibido ao cliente final e não pode:

- colidir entre orçamentos da mesma empresa;
- ter buracos perceptíveis de forma inconsistente sob concorrência;
- depender de uma contagem simples (`COUNT(*) + 1`) — vulnerável a condições de corrida quando dois orçamentos são criados na mesma transação temporal.

O sistema é multi-tenant: a numeração é sequencial **por empresa**, não global.

---

## Decisão

Manter um contador por `(company_id, year)` na tabela `estimate_number_sequences`:

```sql
CREATE TABLE estimate_number_sequences (
    company_id UUID    NOT NULL,
    year       INTEGER NOT NULL,
    last_value INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (company_id, year),
    FOREIGN KEY (company_id) REFERENCES companies (id)
);
```

E avançar o contador com uma única instrução atômica:

```sql
INSERT INTO estimate_number_sequences (company_id, year, last_value)
VALUES (:companyId, :year, 1)
ON CONFLICT (company_id, year)
DO UPDATE SET last_value = estimate_number_sequences.last_value + 1
RETURNING last_value;
```

O PostgreSQL adquire o lock de linha como parte desta instrução `INSERT ... ON CONFLICT DO UPDATE`. Duas requisições concorrentes para a mesma empresa/ano serializam nessa linha em vez de competir num ciclo leitura-depois-escrita — sem buracos, sem duplicados, sem lock explícito na aplicação nem necessidade de isolamento `SERIALIZABLE`.

`EstimateNumberGenerator.generate()` roda com `Propagation.MANDATORY`: só pode ser chamado dentro da transação de criação do orçamento (`EstimateService.createEstimate`), nunca em `REQUIRES_NEW`. Isto significa que, se a criação do orçamento falhar e a transação for revertida, o incremento da sequência também é revertido — a próxima tentativa reutiliza o mesmo número, mantendo a sequência densa mesmo sob falhas de criação.

O prefixo (`ORC` por padrão) vem de `Branding.quotationPrefix`, já existente desde a Sprint 8B, com fallback para `ORC` quando não configurado. O ano usado é o ano corrente no momento da criação (`Year.now()`), não o `issueDate` (que, no MVP, é sempre a data de criação).

Formato final: `{prefix}-{year}-{sequence com 4 dígitos}`, ex: `ORC-2026-0001`.

---

## Alternativas Consideradas

### A1 — `SELECT COUNT(*) FROM estimates WHERE company_id = ? AND number LIKE 'ORC-2026-%'` + 1
**Rejeitado.** Condição de corrida clássica: duas transações concorrentes podem ler a mesma contagem antes de qualquer uma commitar, gerando números duplicados. Exigiria lock explícito de tabela ou isolamento `SERIALIZABLE` (com retries) para ser seguro — mais complexo que a alternativa escolhida.

### A2 — `SELECT ... FOR UPDATE` numa linha de sequência, depois `UPDATE`
**Rejeitado.** Funcionalmente equivalente ao `INSERT ... ON CONFLICT DO UPDATE ... RETURNING`, mas exige duas instruções (leitura com lock + escrita) em vez de uma. Mais superfície para erro (esquecer o `FOR UPDATE`, esquecer de criar a linha na primeira vez) sem nenhum benefício adicional.

### A3 — Sequência nativa do PostgreSQL (`CREATE SEQUENCE`) por empresa
**Rejeitado.** PostgreSQL não suporta sequências criadas dinamicamente por linha de forma prática (uma sequência por empresa exigiria DDL dinâmico a cada novo tenant). Sequências nativas também não resetam por ano automaticamente e não são transacionais (um `ROLLBACK` não devolve o valor), o que criaria buracos visíveis sempre que uma criação de orçamento falhasse — o oposto do comportamento desejado.

### A4 — UUID como número visível
**Rejeitado.** Não é legível nem profissional para um documento comercial enviado ao cliente final.

---

## Consequências

**Positivas:**
- Sem condições de corrida, sem locks explícitos na aplicação.
- Sequência densa mesmo sob falhas de criação (rollback devolve o número).
- Reaproveita `Branding.quotationPrefix`, já existente, sem nova configuração.
- Reinício natural por ano (`year` faz parte da chave primária).

**Negativas / Riscos:**
- Acoplamento a uma instrução nativa do PostgreSQL (`ON CONFLICT ... RETURNING`) — aceitável, pois o projeto já depende de PostgreSQL como banco definitivo (ADR-001).
- `Propagation.MANDATORY` significa que o gerador nunca pode ser chamado fora de uma transação existente; um uso incorreto falha ruidosamente (`IllegalTransactionStateException`) em vez de silenciosamente gerar um número fora de transação — comportamento desejado, mas exige atenção ao integrar o gerador em novos fluxos futuros (ex: duplicar orçamento).

---

## Referências

- [Modelo de Domínio](../architecture/domain-model.md)
- [Release v1.0.0 — Estimate Domain & API](../releases/v1.0.0-estimate-domain-api.md)
