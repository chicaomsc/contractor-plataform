# ADR-008 — Estratégia de Geração de PDF

- **Status:** Aceito
- **Data:** 2026-07-18
- **Sprint:** 10C — Professional Estimate PDF
- **Decisores:** Francisco Costa

---

## Contexto

O Estimate Builder (Sprint 10B) permite criar e editar orçamentos no dashboard, mas o resultado só existe como dados estruturados na API. Esta sprint adiciona `GET /estimates/{id}/pdf`: um documento comercial pronto para envio ao cliente final, gerado em memória, autenticado e isolado por tenant.

Requisitos que restringem a escolha da biblioteca:
- **Multi-página com paginação correta:** nenhuma linha de tabela pode ser cortada entre páginas; o cabeçalho da tabela de itens/materiais deve repetir em cada nova página.
- **Sem HTML-to-PDF** nesta sprint (fora de escopo — descarta Flying Saucer, que era o candidato inicial mencionado no roadmap para uma fase futura).
- **Geração 100% em memória**, sem arquivo temporário em disco.
- **Sem fonte remota** — o build já foi endurecido para funcionar offline (Sprint 9); qualquer fonte usada tem de estar disponível localmente ou ser uma das 14 fontes padrão do PDF.
- **Licença compatível com uso comercial fechado**, sem exigir abertura do código do produto.

## Decisão

Usar **OpenPDF** (`com.github.librepdf:openpdf`, versão `1.4.2`, pacote `com.lowagie.text.*`).

- **Licença:** dual LGPL-2.1+ / MPL-2.0 — SPDX `MPL-2.0 OR LGPL-2.1+`. Ambas permitem uso comercial fechado: a aplicação apenas *usa* a biblioteca via dependência Maven (não a modifica nem a embute como código-fonte), o que satisfaz tanto o requisito de "linking dinâmico" da LGPL quanto o regime de arquivo da MPL. Nenhuma obrigação de disponibilizar o código-fonte do Contractor Platform.
- **Fontes:** apenas as 14 fontes PDF padrão (Helvetica/Helvetica-Bold via `FontFactory`), referenciadas por nome — nunca embutidas nem baixadas. Todo leitor de PDF já as possui. Zero arquivos de fonte adicionados ao artefacto.
- **Motivo central da escolha:** a API de alto nível `PdfPTable`/`PdfPCell` resolve, de forma nativa e testada, exatamente os dois requisitos mais difíceis desta sprint:
  - `table.setHeaderRows(1)` repete a linha de cabeçalho da tabela em cada nova página automaticamente.
  - `table.setSplitRows(false)` impede que uma linha seja cortada no meio do texto entre páginas — a linha inteira migra para a página seguinte.

  Isto elimina a necessidade de implementar paginação manual (medir altura de texto, decidir quebras de página, redesenhar cabeçalhos) — a superfície de bugs de layout fica drasticamente menor do que com uma biblioteca de baixo nível.

## Alternativas Analisadas

### A1 — Apache PDFBox
**Rejeitado para esta sprint.** Licença Apache 2.0 (a mais permissiva das opções), API `PDPageContentStream` é puramente baixo-nível: desenha texto/linhas em coordenadas absolutas, sem tabela nem paginação automática. Implementar "não cortar linha entre páginas" e "repetir cabeçalho de tabela" exigiria código de layout manual e testado à parte — mais superfície de bugs para o mesmo resultado. Reavaliar apenas se o formato de documento evoluir para algo que PDFBox faça melhor (ex.: manipulação de PDF existente, assinatura digital — fora do escopo do MVP).

### A2 — iText 5 / iText 7 (Community ou Commercial)
**Rejeitado.** iText 5+ é AGPL ou requer licença comercial paga para uso em produto fechado. Risco comercial direto para um SaaS de código fechado — exatamente o problema que o fork OpenPDF resolve.

### A3 — Flying Saucer (HTML/CSS → PDF)
**Rejeitado nesta sprint por definição de escopo** ("Nenhum HTML-to-PDF nesta sprint"). Continua sendo um candidato razoável para uma fase futura de templates customizáveis pelo tenant, mas introduziria uma camada de renderização CSS totalmente nova sem necessidade para o MVP — o documento atual usa um layout único, fixo e já satisfatório com a API de tabelas do OpenPDF.

### A4 — OpenPDF 3.x (pacote `org.openpdf`)
**Rejeitado por maturidade.** A major 3.x renomeou o pacote de `com.lowagie.text` para `org.openpdf.text`, uma mudança recente e ainda pouco documentada externamente. Optou-se pela linha `1.4.x`, que mantém o pacote clássico `com.lowagie.text` (idêntico à API do iText 2/4, extensamente documentada há mais de uma década) — reduz o risco de erros de API mal compreendida numa sprint com muito código de layout novo.

## Consequências

**Positivas:**
- `PdfPTable` elimina a necessidade de paginação manual — menos código, menos bugs.
- Licença sem risco comercial, sem custo, sem obrigação de abertura de código.
- API bem documentada (herdada do iText clássico) reduz o risco de uso incorreto.
- Fontes padrão evitam qualquer dependência de rede ou arquivo de fonte no artefacto.

**Negativas / Riscos:**
- Acoplamento à API `com.lowagie.text.*` — uma futura migração para OpenPDF 3.x (`org.openpdf.text`) exigirá reescrever os imports e validar a API, embora o modelo de domínio (`EstimatePdfDocument`) permaneça inalterado por estar isolado do renderer.
- OpenPDF é um fork comunitário (não uma empresa com suporte comercial) — risco de manutenção de longo prazo a monitorar, mitigado pela atividade real do projeto (releases frequentes, mantido pela LibrePDF org).
- A escolha de `PdfPTable` para paginação automática tem um efeito colateral encontrado durante a inspeção visual desta sprint: **`setSplitRows` tem semântica contraintuitiva** — `true` permite cortar uma linha entre páginas, e foi necessário `setSplitRows(false)` (combinado com `setSplitLate(true)`) para obter o comportamento correto ("nunca cortar uma linha"). Documentado como comentário no código-fonte (`EstimatePdfRenderer`) e coberto por um teste de regressão (`render_neverSplitsARowAcrossPages`).

## Geração em Memória

`EstimatePdfRenderer.render(EstimatePdfDocument)` escreve para um `ByteArrayOutputStream` e retorna `byte[]` diretamente — nenhum arquivo é criado em disco, nem sequer temporariamente. O PDF nunca é persistido: cada download é uma renderização nova a partir dos dados já persistidos (Estimate + snapshot do Customer + dados atuais de Company/Branding).

## Limitações do MVP

- Nenhuma fonte customizada por tenant (branding usa apenas cor primária + logo, não tipografia).
- Nenhuma internacionalização — todos os textos fixos do documento são em português.
- Nenhum template alternativo — layout único, aplicável a qualquer prestador de serviço.
- Sem cache do PDF gerado — cada download reprocessa o documento do zero (aceitável para o volume esperado do MVP; ver seção de Performance na release notes).

## Referências

- [Release v1.0.2 — Estimate PDF](../releases/v1.0.2-estimate-pdf.md)
- [Modelo de Domínio](../architecture/domain-model.md)
- [ADR-007 — Estratégia de Numeração de Orçamentos](ADR-007-estimate-numbering-strategy.md)
