# Roadmap do Produto

**Versão:** 1.0 — Sprint 1  
**Data:** 2026-07-04  
**Horizonte:** MVP + Pós-MVP imediato

---

## Princípios de Priorização

1. **Valor para o utilizador beta primeiro:** cada entrega deve resolver um problema concreto do prestador de serviço.
2. **Infraestrutura antes de funcionalidade:** segurança, isolamento de dados e deploy devem estar prontos antes de qualquer módulo de negócio.
3. **Sem features "por precaução":** nada entra no roadmap sem um caso de uso identificado.
4. **Entregas verticais:** cada sprint entrega uma fatia funcional completa (backend + frontend + testes básicos), não uma camada horizontal.

---

## Estado Atual

| Item | Status |
|---|---|
| Estrutura de monorepo (`/backend`, `/frontend`, `/docs`, `/docker`) | Concluído |
| Documentação arquitetural (ADRs, domínio, módulos) | Em andamento (Sprint 1) |
| Código de produção | Não iniciado |

---

## Sprint 1 — Fundação Arquitetural

**Objetivo:** Zero código de produção. Toda a equipe alinhada na arquitetura antes de qualquer implementação.

**Artefatos:**
- [x] ADR-000: Visão e Escopo
- [x] ADR-001: Estilo de Arquitetura
- [x] Modelo de Domínio
- [x] Diagrama de Módulos
- [x] Roadmap (este documento)

**Critério de saída:** Documentos revisados e aprovados. Estrutura de pastas do monorepo criada.

---

## Sprint 2 — Infraestrutura e Autenticação

**Objetivo:** Sistema rodando end-to-end com autenticação funcional. Nenhuma feature de negócio ainda.

**Backend:**
- [ ] Projeto Spring Boot inicializado com Java 25
- [ ] Configuração de datasource PostgreSQL
- [ ] Flyway com migration inicial (`V1__init_schema.sql`)
- [ ] Módulo `shared`: exceções base, `ApiResponse<T>`, handler global
- [ ] Módulo `auth`: login, geração de JWT, `JwtAuthenticationFilter`
- [ ] Endpoint `POST /api/v1/auth/login`
- [ ] Seed de empresa e utilizador admin para desenvolvimento local

**Frontend:**
- [ ] Projeto React + Vite + TypeScript inicializado
- [ ] Configuração de rotas (React Router)
- [ ] Estrutura de pastas `landing/` e `admin/`
- [ ] Tela de login funcional consumindo `/api/v1/auth/login`
- [ ] Guard de rota para área administrativa
- [ ] Armazenamento seguro do JWT (httpOnly cookie ou memory + refresh)

**Infraestrutura:**
- [ ] `Dockerfile` para o backend
- [ ] `docker-compose.yml` para desenvolvimento local (app + banco)
- [ ] Variáveis de ambiente documentadas (`.env.example`)

**Critério de saída:** Login funcionando em ambiente local. JWT gerado e validado. Rota protegida acessível apenas com token válido.

---

## Sprint 3 — Módulo Company (Configuração da Empresa)

**Objetivo:** O prestador consegue configurar a sua empresa, branding e settings no painel administrativo.

**Backend:**
- [ ] Módulo `company`: CRUD de `Company`, `Branding`, `Settings`
- [ ] Migration para tabelas `companies`, `brandings`, `settings`
- [ ] Integração com Supabase Storage para upload de logo
- [ ] Endpoints: `GET/PUT /api/v1/companies/me`, `GET/PUT /api/v1/companies/me/branding`, `GET/PUT /api/v1/companies/me/settings`

**Frontend:**
- [ ] Página "Configurações da Empresa" no painel admin
- [ ] Upload e preview de logo
- [ ] Formulário de branding (cores, slogan, sobre)
- [ ] Formulário de settings (moeda, impostos, validade de orçamentos)

**Critério de saída:** Admin consegue configurar a empresa completa e ver as alterações persistidas.

---

## Sprint 4 — Catálogo (Serviços e Galeria)

**Objetivo:** O prestador gere os serviços oferecidos e as imagens da galeria.

**Backend:**
- [ ] Módulo `catalog`: CRUD de `Service` e `GalleryItem`
- [ ] Migration para tabelas `services`, `gallery_items`
- [ ] Upload de imagens para Supabase Storage
- [ ] Reordenação de itens (drag-and-drop order)

**Frontend:**
- [ ] Página de gestão de serviços (listar, criar, editar, desativar)
- [ ] Página de gestão de galeria (listar, fazer upload, remover)
- [ ] Preview das imagens carregadas

**Critério de saída:** Admin consegue gerir catálogo completo. Imagens carregadas no Supabase acessíveis via URL pública.

---

## Sprint 5 — Landing Page Pública

**Objetivo:** Qualquer pessoa consegue visitar a landing page da empresa via slug.

**Backend:**
- [ ] Módulo `public`: endpoints sem autenticação
- [ ] `GET /public/{slug}` — perfil e branding
- [ ] `GET /public/{slug}/services` — serviços ativos
- [ ] `GET /public/{slug}/gallery` — galeria ativa

**Frontend:**
- [ ] Landing page dinâmica consumindo API pública
- [ ] Secções: Hero, Sobre, Serviços, Galeria, Contacto
- [ ] Responsividade (mobile-first)
- [ ] SEO básico (meta tags dinâmicas via slug)

**Critério de saída:** Landing page acessível via `/{slug}` com dados reais do cliente beta.

---

## Sprint 6 — Clientes e Orçamentos (Parte 1)

**Objetivo:** O prestador gere a carteira de clientes e cria orçamentos.

**Backend:**
- [ ] Módulo `customer`: CRUD de `Customer`
- [ ] Migration para tabela `customers`
- [ ] Módulo `estimate`: criação de `Estimate` com `EstimateItem`
- [ ] Migration para tabelas `estimates`, `estimate_items`
- [ ] Geração de `referenceNumber` sequencial por empresa
- [ ] Cálculo de totais

**Frontend:**
- [ ] Página de gestão de clientes
- [ ] Formulário de criação/edição de orçamento
- [ ] Adição/remoção de itens no orçamento
- [ ] Visualização de totais em tempo real

**Critério de saída:** Admin consegue criar um orçamento completo com múltiplos itens para um cliente.

---

## Sprint 7 — Materiais e PDF

**Objetivo:** Orçamentos incluem materiais e podem ser exportados em PDF.

**Backend:**
- [ ] `Material` associado a `EstimateItem`
- [ ] Migration para tabela `materials`
- [ ] Geração de PDF (`POST /api/v1/estimates/{id}/pdf`)
- [ ] Template de PDF com branding da empresa (logo, cores, rodapé)

**Frontend:**
- [ ] Gestão de materiais por item de orçamento
- [ ] Botão de download do PDF no orçamento
- [ ] Preview do orçamento antes de exportar

**Critério de saída:** PDF gerado com branding correto, todos os itens, materiais e totais. Download funcional no browser.

---

## Sprint 8 — Hardening e Deploy

**Objetivo:** Sistema estável, seguro e deployado nas plataformas de produção.

**Backend:**
- [ ] Validação de inputs em todos os endpoints
- [ ] Rate limiting no login
- [ ] Logs estruturados (JSON)
- [ ] Health check endpoint (`/actuator/health`)
- [ ] Deploy no Railway com variáveis de ambiente de produção

**Frontend:**
- [ ] Deploy no Vercel com variáveis de produção
- [ ] Tratamento de erros globais (página 404, error boundary)
- [ ] Loading states e feedback visual consistente

**Infra:**
- [ ] PostgreSQL gerenciado no Railway
- [ ] Backup automático do banco configurado
- [ ] CORS configurado para domínio de produção

**Critério de saída:** Sistema funcionando em produção com o cliente beta. Todos os fluxos do MVP testados manualmente.

---

## Pós-MVP — Backlog Prioritário

Itens confirmados para após o lançamento, sem sprint definida ainda:

| Funcionalidade | Justificativa |
|---|---|
| Transições de status de orçamento com notificação por email | Comunicação com o cliente final |
| Depoimentos na landing page | Prova social para conversão |
| Dashboard com métricas básicas (orçamentos por status, receita prevista) | Visibilidade para o prestador |
| Auto-cadastro (multi-tenant self-service) | Escalar além do cliente beta |
| CRM leve (histórico de interações por cliente) | Retenção e relacionamento |
| Integração de pagamento (Stripe) | Monetização |
| Agenda/calendário de execução | Gestão operacional |
| WhatsApp API (envio de orçamento via WhatsApp) | Canal preferencial em Portugal/Brasil |
| App mobile (React Native ou PWA) | Mobilidade para o prestador em campo |

---

## Dependências Críticas (Riscos)

| Dependência | Risco | Mitigação |
|---|---|---|
| Supabase Storage | Serviço externo fora do controlo | Interface de storage abstraída — troca por S3/R2 não quebra domínio |
| Railway (backend) | Custo pode aumentar com uso | Dockerfile portável — migração para outro provider é viável |
| Vercel (frontend) | Build limits no plano gratuito | App estática é leve; fallback para Netlify ou self-hosted |
| Java 25 (LTS) | Versão recente, suporte de ecossistema | Usar apenas features estáveis; avaliar downgrade para Java 21 LTS se necessário |
| Geração de PDF | Bibliotecas Java (iText, OpenPDF, Flying Saucer) requerem avaliação de licença | Decidir biblioteca na Sprint 7; Flying Saucer (LGPL) é candidato principal |

---

## Referências

- [ADR-000 — Visão e Escopo](adr/ADR-000-vision-and-scope.md)
- [ADR-001 — Estilo de Arquitetura](adr/ADR-001-architecture-style.md)
- [Modelo de Domínio](architecture/domain-model.md)
- [Diagrama de Módulos](architecture/module-diagram.md)
