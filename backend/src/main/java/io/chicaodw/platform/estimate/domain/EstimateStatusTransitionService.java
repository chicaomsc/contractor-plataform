package io.chicaodw.platform.estimate.domain;

import io.chicaodw.platform.common.exception.ConflictException;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Pure domain service enforcing the {@link EstimateStatus} state machine. No Spring
 * dependency, no database access.
 *
 * <pre>
 * DRAFT    → SENT, CANCELLED
 * SENT     → APPROVED, REJECTED, EXPIRED, CANCELLED
 * APPROVED → COMPLETED, CANCELLED
 * REJECTED, EXPIRED, CANCELLED, COMPLETED are terminal — no further transitions.
 * </pre>
 */
public class EstimateStatusTransitionService {

    private static final Map<EstimateStatus, Set<EstimateStatus>> ALLOWED = new EnumMap<>(EstimateStatus.class);

    static {
        ALLOWED.put(EstimateStatus.DRAFT, EnumSet.of(EstimateStatus.SENT, EstimateStatus.CANCELLED));
        ALLOWED.put(EstimateStatus.SENT, EnumSet.of(
                EstimateStatus.APPROVED, EstimateStatus.REJECTED, EstimateStatus.EXPIRED, EstimateStatus.CANCELLED));
        ALLOWED.put(EstimateStatus.APPROVED, EnumSet.of(EstimateStatus.COMPLETED, EstimateStatus.CANCELLED));
        ALLOWED.put(EstimateStatus.REJECTED, EnumSet.noneOf(EstimateStatus.class));
        ALLOWED.put(EstimateStatus.EXPIRED, EnumSet.noneOf(EstimateStatus.class));
        ALLOWED.put(EstimateStatus.CANCELLED, EnumSet.noneOf(EstimateStatus.class));
        ALLOWED.put(EstimateStatus.COMPLETED, EnumSet.noneOf(EstimateStatus.class));
    }

    public boolean isValidTransition(EstimateStatus from, EstimateStatus to) {
        return ALLOWED.getOrDefault(from, Set.of()).contains(to);
    }

    /** @throws ConflictException when the transition is not allowed (maps to HTTP 409). */
    public void validateTransition(EstimateStatus from, EstimateStatus to) {
        if (from == to) {
            throw new ConflictException("Estimate is already in status " + to);
        }
        if (!isValidTransition(from, to)) {
            throw new ConflictException("Invalid status transition from " + from + " to " + to);
        }
    }
}
