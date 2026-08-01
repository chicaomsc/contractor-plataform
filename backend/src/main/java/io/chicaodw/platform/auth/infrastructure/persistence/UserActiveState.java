package io.chicaodw.platform.auth.infrastructure.persistence;

import io.chicaodw.platform.auth.domain.UserStatus;

/**
 * Scalar projection used by ActiveAccountFilter — status plus auth_version, no full
 * entity hydration (DT-011A.10 §5).
 */
public record UserActiveState(UserStatus status, long authVersion) {}
