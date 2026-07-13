import { MessageCircle, Paintbrush, Phone, Ruler } from "lucide-react";
import { Container } from "@/components/layout/Container";

type TrustStripProps = {
  hasWhatsApp: boolean;
  hasPhone: boolean;
};

const baseItems = [
  {
    label: "Trabalho residencial e comercial",
    Icon: Paintbrush,
  },
  {
    label: "Acabamento e organização",
    Icon: Ruler,
  },
];

export function TrustStrip({ hasWhatsApp, hasPhone }: TrustStripProps) {
  const items = [
    ...baseItems,
    hasWhatsApp
      ? { label: "Atendimento por WhatsApp", Icon: MessageCircle }
      : { label: "Contacto direto", Icon: Phone },
    hasPhone
      ? { label: "Telefone público disponível", Icon: Phone }
      : { label: "Pedido sem compromisso", Icon: MessageCircle },
  ];

  return (
    <section
      aria-labelledby="trust-title"
      className="bg-[var(--surface-dark)] py-7 text-[var(--surface-dark-fg)] md:py-8"
    >
      <Container>
        <h2 id="trust-title" className="sr-only">
          Informação de confiança
        </h2>
        <dl className="grid gap-px md:grid-cols-4">
          {items.map(({ label, Icon }) => (
            <div
              key={label}
              className="border-t border-[var(--surface-dark-fg)]/18 py-5 first:border-t-0 md:border-l md:border-t-0 md:first:border-l-0 md:pl-6"
            >
              <dt className="flex items-start gap-3 text-sm font-semibold leading-6">
                <Icon
                  size={20}
                  aria-hidden="true"
                  className="mt-0.5 shrink-0 text-primary"
                />
                <span>{label}</span>
              </dt>
            </div>
          ))}
        </dl>
      </Container>
    </section>
  );
}
