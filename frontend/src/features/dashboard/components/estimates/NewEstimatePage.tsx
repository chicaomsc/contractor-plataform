import { PageHeader } from "../PageHeader";
import { EstimateWizard } from "./EstimateWizard";

export function NewEstimatePage() {
  return (
    <div className="space-y-8">
      <PageHeader
        eyebrow="Orçamentos"
        title="Novo orçamento"
        description="Selecione o cliente, preencha as informações gerais e adicione itens e materiais."
      />
      <EstimateWizard />
    </div>
  );
}
