package io.chicaodw.platform.estimate.pdf;

import io.chicaodw.platform.estimate.domain.EstimateUnit;

import java.util.Map;

/** Presentation-only mapping — never used for domain logic. Backend has no shared code with the frontend, so this mirrors (independently) the label map used there. */
final class EstimateUnitLabels {

    private static final Map<EstimateUnit, String> LABELS = Map.of(
            EstimateUnit.UNIT, "Unidade",
            EstimateUnit.HOUR, "Hora",
            EstimateUnit.DAY, "Dia",
            EstimateUnit.M2, "m²",
            EstimateUnit.M3, "m³",
            EstimateUnit.LINEAR_METER, "Metro linear",
            EstimateUnit.FIXED, "Fixo"
    );

    private EstimateUnitLabels() {}

    static String label(EstimateUnit unit) {
        return LABELS.getOrDefault(unit, unit.name());
    }
}
