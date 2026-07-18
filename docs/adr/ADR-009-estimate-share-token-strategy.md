# ADR-009 — Estratégia de Token de Partilha Pública de Orçamentos

- **Status:** Aceito
- **Data:** 2026-07-18
- **Sprint:** 10D — Estimate Sharing
- **Decisores:** Francisco Costa

---

## Contexto

A Sprint 10D introduz um link público, não autenticado, para um cliente final visualizar e baixar o PDF de um orçamento (`GET /public/share/{token}`, `GET /public/share/{token}/pdf`). Ao contrário de todo o resto da API, estas rotas não têm `JwtPrincipal` nem `companyId` de contexto — a única credencial é o próprio token na URL.

Isto muda o perfil de risco em relação ao precedente já existente no projeto (`RefreshToken`, Sprint 2): um refresh token é armazenado em texto plano, mas nunca aparece numa URL, num histórico de navegador, num log de proxy, ou num link reencaminhado por WhatsApp/email — é enviado apenas num corpo de requisição HTTPS. Um token de partilha, por definição, viaja exatamente por esses canais de maior exposição.

Requisitos explícitos:
- Nunca usar um identificador previsível (`/estimate/{id}`) ou um UUID simples enumerável.
- Mínimo 128 bits de entropia, gerado por `SecureRandom`.
- Armazenamento seguro, preferencialmente hash.
- Nunca sequencial.

---

## Decisão

### Geração

`SecureTokenGenerator.generate()` (`common/security`): 20 bytes de `java.security.SecureRandom`, codificados em Base64 URL-safe sem padding (`Base64.getUrlEncoder().withoutPadding()`). 160 bits de entropia — acima do mínimo de 128 exigido. Formato final: string opaca de 27 caracteres (ex.: `xh83JSkLm82A...`), sem qualquer relação com o `id` do `Estimate` ou da `Company`.

### Armazenamento — hash, não texto plano

Desvio deliberado do precedente do `RefreshToken`: `EstimateShare.tokenHash` guarda **apenas o SHA-256 hexadecimal** do token bruto (`TokenHasher.sha256Hex`, `common/security`). O token bruto nunca é persistido.

**Justificativa da divergência:** um refresh token só é lido de volta pelo próprio browser que o armazenou (via `localStorage`, nunca exposto numa URL). Um token de partilha é, por desenho, colado numa mensagem de WhatsApp, share de link ou email — a superfície de exposição (histórico de navegador do destinatário, logs de proxies intermediários, capturas de tela) é ordens de magnitude maior. Guardar apenas o hash garante que uma leitura da tabela `estimate_shares` (dump de backup, acesso indevido ao banco) nunca revela um link funcional.

### Consequência de UX aceite: o token não pode ser reexibido

Como só o hash é persistido, o backend **não consegue** devolver o link de partilha numa consulta posterior — apenas na resposta do `POST /estimates/{id}/share` que o criou. Isto é o mesmo modelo já familiar de chaves de API/tokens de acesso pessoal ("mostrado uma única vez"). `GET /estimates/{id}/share` devolve o estado do link (ativo/expirado/revogado, `createdAt`, `expiresAt`, `accessCount`, `lastAccessAt`) mas nunca o token em si.

Para recuperar um link "perdido" (ex.: o utilizador fechou a página sem copiar), a única ação possível é gerar um novo link — o que revoga automaticamente o anterior (ver `EstimateShareService.createShare`, no máximo um link ativo por orçamento). Este é o comportamento intencional do botão "Gerar novo link" no frontend.

### Validação — mesma resposta para inexistente, expirado e revogado

`EstimateShareService.resolveValidShareReadOnly` faz o hash do token recebido, procura por `tokenHash` e lança `ResourceNotFoundException` (404) tanto para um token que nunca existiu quanto para um expirado ou revogado. Nunca há uma resposta diferenciada (ex.: 410 Gone para expirado) — um atacante tentando enumerar/adivinhar tokens não recebe nenhum sinal de que "quase acertou".

---

## Alternativas Consideradas

### A1 — Token em texto plano, único, como o `RefreshToken`
**Rejeitado.** Manteria o link recuperável a qualquer momento (boa UX), mas um dump da tabela `estimate_shares` exporia diretamente links funcionais a orçamentos de qualquer cliente da plataforma — inaceitável dado o canal de exposição muito mais amplo de um link público partilhado.

### A2 — UUID v4 como token
**Rejeitado explicitamente pelo requisito da sprint.** Embora um UUID v4 aleatório tenha ~122 bits de entropia (perto do mínimo exigido), o requisito é explícito: nunca "baseado em UUID simples previsível por enumeração" — e um `UUID.randomUUID()` não usa `SecureRandom` por contrato da JDK (normalmente usa `ThreadLocalRandom`/generators não criptográficos em algumas implementações), então não há garantia forte de imprevisibilidade.

### A3 — Hash reversível/criptografado (permitir reexibir o token)
**Rejeitado.** Cifrar o token em vez de aplicar hash permitiria reexibi-lo, mas introduz gestão de chave de criptografia (rotação, armazenamento seguro da chave) — complexidade desproporcional para o MVP. O hash irreversível (SHA-256) é mais simples e estritamente mais seguro; o custo (token "mostrado uma vez") é aceitável e já é um padrão bem conhecido de outros produtos.

---

## Consequências

**Positivas:**
- Um dump/leitura indevida da tabela `estimate_shares` nunca expõe um link funcional.
- Nenhum novo mecanismo de criptografia/gestão de chaves — reaproveita `MessageDigest`/`SecureRandom` da JDK.
- Resposta uniforme (404) para token inexistente/expirado/revogado elimina um vetor de enumeração.

**Negativas / Riscos aceites:**
- O token não pode ser reexibido — UX exige gerar um novo link (revogando o anterior) se o utilizador perder o link já copiado. Documentado explicitamente no frontend (`ShareEstimatePanel`).
- Divergência de padrão em relação ao `RefreshToken` existente — mitigado documentando esta ADR como a exceção deliberada, não um novo padrão universal para todo token do sistema.

---

## Referências

- [Modelo de Domínio](../architecture/domain-model.md)
- [Release v1.0.3 — Estimate Sharing](../releases/v1.0.3-estimate-sharing.md)
- [Security — Authentication Review](../security/authentication-review.md)
