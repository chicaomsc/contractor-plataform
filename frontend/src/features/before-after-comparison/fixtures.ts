import type { BeforeAfterComparisonProps } from "./types";

export const beforeAfterComparisonFixture: BeforeAfterComparisonProps = {
  title: "Fixture técnica de comparação",
  description:
    "Dados locais para avaliar comportamento, acessibilidade e composição sem conteúdo de tenant.",
  beforeImageUrl: "/lab/before-after-before.svg",
  afterImageUrl: "/lab/before-after-after.svg",
  beforeAlt: "Parede de teste antes da intervenção, com superfície irregular",
  afterAlt:
    "A mesma parede de teste depois da intervenção, com acabamento uniforme",
  initialPosition: 50,
};

export const incompleteBeforeAfterComparisonFixture: BeforeAfterComparisonProps =
  {
    title: "Fixture técnica incompleta",
    description: "Estado usado para validar fallback sem imagem depois.",
    beforeImageUrl: "/lab/before-after-before.svg",
    afterImageUrl: null,
    beforeAlt: "Parede de teste antes da intervenção",
    afterAlt: null,
  };
