# DT-018B — Dashboard Onboarding Checklist

## Objetivo

Exibir no dashboard do OWNER um checklist compacto de configuração inicial,
sem wizard, redirect, persistência ou dismiss. O dashboard continua sendo a
experiência principal.

## Contrato e fonte da verdade

O frontend chama `GET /onboarding/status` somente para OWNER e valida a
resposta com Zod. Os seis booleanos são derivados pelo backend; o frontend não
calcula nem persiste o estado das etapas.

## Etapas

As cinco etapas obrigatórias são Dados da empresa, Identidade visual, Serviços,
Primeiro cliente e Primeiro orçamento. Cliente e orçamento apontam
intencionalmente para `/dashboard/estimates/new`.

Convide sua equipe é uma etapa opcional e aponta para `/dashboard/team`.
Ela aparece no checklist, mas nunca participa do percentual obrigatório.

## Progresso

O percentual é `completedRequired / 5 * 100`, usando apenas as cinco flags
obrigatórias. Ao concluir todas, o checklist permanece visível e mostra
“Configuração essencial concluída.”.

## Escopo e segurança

O checklist é OWNER-only. MANAGER e MEMBER não fazem a chamada nem renderizam
o componente; SUPER_ADMIN continua fora do dashboard tenant. O request usa o
client autenticado existente, preservando o fluxo DT-014 de 401/403.

Não há rota `/onboarding`, redirect, modal obrigatório, autosave, dismissal ou
novo armazenamento client-side. A DT-018C tratará dismissal, se aprovada.
