package io.chicaodw.platform.estimate;

import io.chicaodw.platform.common.exception.ConflictException;
import io.chicaodw.platform.estimate.domain.EstimateStatus;
import io.chicaodw.platform.estimate.domain.EstimateStatusTransitionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EstimateStatusTransitionServiceTest {

    private final EstimateStatusTransitionService transitionService = new EstimateStatusTransitionService();

    @ParameterizedTest
    @CsvSource({
            "DRAFT, SENT",
            "DRAFT, CANCELLED",
            "SENT, APPROVED",
            "SENT, REJECTED",
            "SENT, EXPIRED",
            "SENT, CANCELLED",
            "APPROVED, COMPLETED",
            "APPROVED, CANCELLED",
    })
    void validTransitions_doNotThrow(EstimateStatus from, EstimateStatus to) {
        assertThatCode(() -> transitionService.validateTransition(from, to)).doesNotThrowAnyException();
        org.assertj.core.api.Assertions.assertThat(transitionService.isValidTransition(from, to)).isTrue();
    }

    @ParameterizedTest
    @CsvSource({
            "DRAFT, APPROVED",
            "DRAFT, REJECTED",
            "DRAFT, EXPIRED",
            "DRAFT, COMPLETED",
            "SENT, DRAFT",
            "SENT, COMPLETED",
            "APPROVED, SENT",
            "APPROVED, REJECTED",
            "APPROVED, DRAFT",
    })
    void invalidTransitions_throwConflictException(EstimateStatus from, EstimateStatus to) {
        assertThatThrownBy(() -> transitionService.validateTransition(from, to))
                .isInstanceOf(ConflictException.class);
        org.assertj.core.api.Assertions.assertThat(transitionService.isValidTransition(from, to)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = EstimateStatus.class, names = {"REJECTED", "EXPIRED", "CANCELLED", "COMPLETED"})
    void terminalStatuses_haveNoValidTransitions(EstimateStatus terminal) {
        for (var target : EstimateStatus.values()) {
            assertThatThrownBy(() -> transitionService.validateTransition(terminal, target))
                    .isInstanceOf(ConflictException.class);
        }
    }

    @Test
    void sameStatus_throwsConflictException() {
        assertThatThrownBy(() -> transitionService.validateTransition(EstimateStatus.DRAFT, EstimateStatus.DRAFT))
                .isInstanceOf(ConflictException.class);
    }
}
