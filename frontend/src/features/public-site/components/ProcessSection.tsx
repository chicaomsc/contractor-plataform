import { Container } from "@/components/layout/Container";
import { Section } from "@/components/layout/Section";
import { SectionLabel } from "./SectionLabel";

const processSteps = [
  {
    title: "Contacto",
    description:
      "Explique o que precisa por WhatsApp ou telefone, com a informação essencial da obra.",
  },
  {
    title: "Avaliação",
    description:
      "A empresa avalia o trabalho e confirma se há dados suficientes para avançar.",
  },
  {
    title: "Orçamento",
    description:
      "Recebe uma estimativa clara, alinhada com o trabalho descrito e os prazos combinados.",
  },
  {
    title: "Execução",
    description:
      "O trabalho é realizado com foco no acabamento, organização e entrega combinada.",
  },
];

export function ProcessSection() {
  return (
    <Section
      id="como-trabalhamos"
      labelledBy="process-title"
      className="bg-[var(--surface-dark)] text-[var(--surface-dark-fg)]"
    >
      <Container className="space-y-10">
        <header className="max-w-3xl space-y-4">
          <SectionLabel>Processo</SectionLabel>
          <h2
            id="process-title"
            className="m-0 font-display text-3xl font-bold md:text-4xl"
          >
            Como trabalhamos
          </h2>
        </header>

        <ol className="grid gap-8 md:grid-cols-2 lg:grid-cols-4">
          {processSteps.map((step, index) => (
            <li
              key={step.title}
              className="space-y-4 border-t border-[var(--surface-dark-fg)]/25 pt-6"
            >
              <span
                aria-hidden="true"
                className="font-display text-5xl font-bold text-primary"
              >
                {index + 1}
              </span>
              <h3 className="m-0 font-display text-2xl font-semibold">
                {step.title}
              </h3>
              <p className="m-0 text-sm leading-6 text-[var(--surface-dark-fg)]/75">
                {step.description}
              </p>
            </li>
          ))}
        </ol>
      </Container>
    </Section>
  );
}
