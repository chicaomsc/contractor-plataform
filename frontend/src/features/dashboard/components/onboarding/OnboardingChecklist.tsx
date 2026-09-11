import { Check, Circle, ArrowRight } from "lucide-react";
import Link from "next/link";
import type { OnboardingStatus } from "../../types/onboarding";

type ChecklistItem = {
  key: keyof OnboardingStatus;
  label: string;
  description: string;
  href: string;
  optional?: boolean;
};

const requiredItems: ChecklistItem[] = [
  {
    key: "companyCompleted",
    label: "Dados da empresa",
    description: "Complete as informações do seu negócio",
    href: "/dashboard/company",
  },
  {
    key: "brandingCompleted",
    label: "Identidade visual",
    description: "Personalize logo, cores e apresentação",
    href: "/dashboard/branding",
  },
  {
    key: "servicesCompleted",
    label: "Serviços",
    description: "Cadastre pelo menos um serviço",
    href: "/dashboard/services",
  },
  {
    key: "customerCompleted",
    label: "Primeiro cliente",
    description: "Cadastre os dados do seu primeiro cliente",
    href: "/dashboard/estimates/new",
  },
  {
    key: "estimateCompleted",
    label: "Primeiro orçamento",
    description: "Crie seu primeiro orçamento",
    href: "/dashboard/estimates/new",
  },
];

const optionalItem: ChecklistItem = {
  key: "teamCompleted",
  label: "Convide sua equipe",
  description: "Adicione colaboradores ao seu negócio",
  href: "/dashboard/team",
  optional: true,
};

function ChecklistRow({
  item,
  completed,
}: {
  item: ChecklistItem;
  completed: boolean;
}) {
  return (
    <li className="flex min-w-0 items-start gap-3 border-t border-border py-3 first:border-t-0">
      <span
        className={`mt-0.5 flex h-6 w-6 shrink-0 items-center justify-center border ${
          completed
            ? "border-success bg-success text-white"
            : "border-border bg-background text-[var(--muted-foreground)]"
        }`}
        aria-hidden="true"
      >
        {completed ? <Check size={15} strokeWidth={3} /> : <Circle size={12} />}
      </span>
      <div className="min-w-0 flex-1">
        <p className="m-0 font-semibold leading-6">{item.label}</p>
        <p className="m-0 text-sm leading-6 text-[var(--muted-foreground)]">
          {item.description}
        </p>
      </div>
      {!completed ? (
        <Link
          href={item.href}
          aria-label={
            item.optional
              ? "Convidar colaboradores"
              : `Configurar ${item.label}`
          }
          className="inline-flex min-h-10 shrink-0 items-center gap-1 self-center border border-border px-3 py-2 text-sm font-semibold no-underline hover:border-primary hover:text-primary"
        >
          Configurar
          <ArrowRight size={15} aria-hidden="true" />
        </Link>
      ) : null}
    </li>
  );
}

export function OnboardingChecklist({ status }: { status: OnboardingStatus }) {
  const completedRequired = requiredItems.filter(
    (item) => status[item.key],
  ).length;
  const percentage = (completedRequired / requiredItems.length) * 100;
  const allRequiredCompleted = completedRequired === requiredItems.length;

  return (
    <section
      aria-labelledby="onboarding-checklist-title"
      className="border border-border bg-surface p-5 sm:p-6"
    >
      <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <p className="m-0 text-xs font-bold uppercase tracking-[0.18em] text-primary">
            Primeiros passos
          </p>
          <h2
            id="onboarding-checklist-title"
            className="m-0 mt-2 font-display text-2xl font-semibold"
          >
            Configure sua empresa
          </h2>
          <p className="m-0 mt-1 text-sm text-[var(--muted-foreground)]">
            Complete os passos essenciais para começar.
          </p>
        </div>
        <p className="m-0 shrink-0 text-sm font-bold">
          {completedRequired} de {requiredItems.length}
        </p>
      </div>

      <div className="mt-5">
        <div className="flex items-center justify-between gap-3 text-sm">
          <span className="font-semibold">Progresso essencial</span>
          <span className="font-bold">{percentage}%</span>
        </div>
        <div
          className="mt-2 h-2 w-full overflow-hidden bg-surface-muted"
          role="progressbar"
          aria-label="Progresso da configuração essencial"
          aria-valuemin={0}
          aria-valuemax={100}
          aria-valuenow={percentage}
        >
          <div
            className="h-full bg-primary transition-[width]"
            style={{ width: `${percentage}%` }}
          />
        </div>
        {allRequiredCompleted ? (
          <p className="m-0 mt-2 text-sm font-semibold text-success">
            Configuração essencial concluída.
          </p>
        ) : null}
      </div>

      <ul className="m-0 mt-5 list-none p-0">
        {requiredItems.map((item) => (
          <ChecklistRow
            key={item.key}
            item={item}
            completed={status[item.key]}
          />
        ))}
      </ul>

      <div className="mt-4 border-t border-border pt-3">
        <p className="m-0 mb-1 text-xs font-bold uppercase tracking-[0.16em] text-[var(--muted-foreground)]">
          Opcional
        </p>
        <ChecklistRow item={optionalItem} completed={status.teamCompleted} />
      </div>
    </section>
  );
}
