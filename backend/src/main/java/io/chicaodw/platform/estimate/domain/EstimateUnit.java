package io.chicaodw.platform.estimate.domain;

/**
 * Controlled unit vocabulary shared by {@link EstimateItem} and {@link Material}.
 * Using an enum (rather than a free-text string) keeps values consistent for future
 * PDF rendering and reporting, at the cost of a migration if new units are needed —
 * an acceptable trade-off for the fixed set of units a contractor estimate uses.
 */
public enum EstimateUnit {
    UNIT,
    HOUR,
    DAY,
    M2,
    M3,
    LINEAR_METER,
    FIXED
}
