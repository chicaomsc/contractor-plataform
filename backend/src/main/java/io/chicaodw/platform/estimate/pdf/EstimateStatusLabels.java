package io.chicaodw.platform.estimate.pdf;

import io.chicaodw.platform.estimate.domain.EstimateStatus;

import java.util.Map;

/** Presentation-only mapping — never used for domain logic. */
final class EstimateStatusLabels {

    private static final Map<EstimateStatus, String> LABELS = Map.of(
            EstimateStatus.DRAFT, "Rascunho",
            EstimateStatus.SENT, "Enviado",
            EstimateStatus.APPROVED, "Aprovado",
            EstimateStatus.REJECTED, "Rejeitado",
            EstimateStatus.EXPIRED, "Expirado",
            EstimateStatus.CANCELLED, "Cancelado",
            EstimateStatus.COMPLETED, "Concluído"
    );

    private EstimateStatusLabels() {}

    static String label(EstimateStatus status) {
        return LABELS.getOrDefault(status, status.name());
    }
}
