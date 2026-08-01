package io.chicaodw.platform.auth.application;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.chicaodw.platform.auth.api.dto.ForgotPasswordResponse;
import io.chicaodw.platform.auth.api.dto.ResetPasswordResponse;
import io.chicaodw.platform.auth.domain.PasswordResetToken;
import io.chicaodw.platform.auth.domain.User;
import io.chicaodw.platform.auth.domain.UserRole;
import io.chicaodw.platform.auth.domain.UserStatus;
import io.chicaodw.platform.auth.infrastructure.persistence.PasswordResetTokenRepository;
import io.chicaodw.platform.auth.infrastructure.persistence.RefreshTokenRepository;
import io.chicaodw.platform.auth.infrastructure.persistence.UserRepository;
import io.chicaodw.platform.auth.infrastructure.security.PasswordResetProperties;
import io.chicaodw.platform.common.exception.BusinessRuleException;
import io.chicaodw.platform.common.exception.ConflictException;
import io.chicaodw.platform.common.exception.ResourceNotFoundException;
import io.chicaodw.platform.common.security.SecureTokenGenerator;
import io.chicaodw.platform.common.security.TokenHasher;
import io.chicaodw.platform.company.domain.CompanyStatus;
import io.chicaodw.platform.company.infrastructure.config.TenantProperties;
import io.chicaodw.platform.company.infrastructure.persistence.CompanyRepository;
import lombok.RequiredArgsConstructor;

/**
 * Owns the full lifecycle of password-reset tokens (DT-011A.10 §1/§2/§4/§15): creation
 * with cooldown (self-service) or without it (admin-triggered), revoke-previous before
 * issuing a new one, atomic single-use consumption, and the two public flows
 * (forgot/reset) that build the response DTOs directly — same split as InviteService,
 * whose acceptInvite/createInvite/reissueInvite are the direct template for the
 * matching methods here.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class PasswordResetTokenService {

    private static final String PROD_PROFILE = "prod";

    private static final String FORGOT_PASSWORD_MESSAGE =
            "Se existir uma conta para este email, as instruções foram geradas.";
    private static final String RESET_SUCCESS_MESSAGE = "Senha atualizada. Acesse sua conta novamente.";
    private static final String INVALID_LINK_MESSAGE =
            "O link de recuperação é inválido ou não está mais disponível.";
    private static final String SAME_PASSWORD_MESSAGE = "A nova senha deve ser diferente da atual.";

    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetProperties passwordResetProperties;
    private final TenantProperties tenantProperties;
    private final Environment environment;

    public record IssuedToken(String rawToken, Instant expiresAt) {}

    // ── Self-service: POST /auth/password/forgot ───────────────────────────────

    /**
     * Response is always the same shape/message regardless of whether the account
     * exists, its state, or an in-progress cooldown — see DT §2/§9. debugToken/
     * debugResetLink are only populated when a token was genuinely created by this
     * call AND the "prod" profile is not active (DT §3/§13).
     */
    public ForgotPasswordResponse forgotPassword(String email) {
        Optional<IssuedToken> issued = requestReset(email);
        if (issued.isEmpty() || isProdActive()) {
            return new ForgotPasswordResponse(FORGOT_PASSWORD_MESSAGE, null, null);
        }
        String rawToken = issued.get().rawToken();
        return new ForgotPasswordResponse(FORGOT_PASSWORD_MESSAGE, rawToken, buildResetLink(rawToken));
    }

    private Optional<IssuedToken> requestReset(String email) {
        User user = userRepository.findByEmailIgnoreCase(email).orElse(null);
        if (user == null || !isEligibleForReset(user)) {
            return Optional.empty();
        }
        if (isWithinCooldown(user.getId())) {
            return Optional.empty();
        }
        return Optional.of(issueNewToken(user.getId(), null));
    }

    // ── Self-service: POST /auth/password/reset ─────────────────────────────────

    /**
     * Ordem de validação (DT §4): token utilizável (busca + consumo atômico) → conta/
     * empresa elegível → nova senha != atual → sucesso. Qualquer falha lança
     * BusinessRuleException (422 uniforme via GlobalExceptionHandler já existente),
     * revertendo toda a transação — incluindo o consumo atômico do token, que por isso
     * pode acontecer antes das checagens de elegibilidade sem violar "tudo ou nada".
     */
    public ResetPasswordResponse resetPassword(String rawToken, String newPassword) {
        String hash = TokenHasher.sha256Hex(rawToken);
        PasswordResetToken token = passwordResetTokenRepository.findByTokenHash(hash)
                .orElseThrow(this::invalidLink);

        Instant now = Instant.now();
        int updated = passwordResetTokenRepository.markUsedIfStillValid(token.getId(), now);
        if (updated == 0) {
            throw invalidLink();
        }

        User user = userRepository.findById(token.getUserId())
                .orElseThrow(this::invalidLink);
        if (!isEligibleForReset(user)) {
            throw invalidLink();
        }

        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new BusinessRuleException(SAME_PASSWORD_MESSAGE);
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setAuthVersion(user.getAuthVersion() + 1);
        userRepository.save(user);

        refreshTokenRepository.revokeAllForUser(user.getId());

        return new ResetPasswordResponse(RESET_SUCCESS_MESSAGE);
    }

    // ── Admin-triggered: POST /admin/companies/{companyId}/owners/{ownerId}/password-reset ──

    /**
     * No cooldown — a privileged SUPER_ADMIN action, not self-service (DT §15).
     * Company-membership (404) is the caller's responsibility (AdminCompanyService's
     * requireOwnerInCompany, mirroring reissueInvite/revokeInvite) — this method only
     * enforces the owner-must-be-ACTIVE rule (409), which is specific to this flow.
     */
    public IssuedToken issueForActiveOwner(UUID ownerId, UUID actingSuperAdminId) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", ownerId));
        if (owner.getStatus() != UserStatus.ACTIVE) {
            throw new ConflictException("Owner is not active — reactivate the account before generating a reset link");
        }
        return issueNewToken(owner.getId(), actingSuperAdminId);
    }

    public String buildResetLink(String rawToken) {
        return tenantProperties.getFrontendBaseUrl() + "/reset-password#token=" + rawToken;
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    /** ACTIVE user; SUPER_ADMIN has no company to check, OWNER additionally needs an ACTIVE company (DT §6). */
    private boolean isEligibleForReset(User user) {
        if (user.getStatus() != UserStatus.ACTIVE) {
            return false;
        }
        if (user.getRole() == UserRole.SUPER_ADMIN) {
            return true;
        }
        CompanyStatus companyStatus = companyRepository.findStatusById(user.getCompanyId()).orElse(null);
        return companyStatus == CompanyStatus.ACTIVE;
    }

    private boolean isWithinCooldown(UUID userId) {
        Instant cooldownStart = Instant.now().minusSeconds(passwordResetProperties.getRequestCooldown());
        return passwordResetTokenRepository.findTopByUserIdOrderByCreatedAtDesc(userId)
                .map(t -> t.getCreatedAt().isAfter(cooldownStart))
                .orElse(false);
    }

    private IssuedToken issueNewToken(UUID userId, UUID createdBy) {
        revokeValidTokensForUser(userId);

        String rawToken = SecureTokenGenerator.generate();
        PasswordResetToken token = new PasswordResetToken();
        token.setUserId(userId);
        token.setTokenHash(TokenHasher.sha256Hex(rawToken));
        token.setExpiresAt(Instant.now().plusSeconds(passwordResetProperties.getTokenTtl()));
        token.setCreatedBy(createdBy);
        passwordResetTokenRepository.save(token);

        return new IssuedToken(rawToken, token.getExpiresAt());
    }

    private void revokeValidTokensForUser(UUID userId) {
        Instant now = Instant.now();
        List<PasswordResetToken> valid = passwordResetTokenRepository
                .findByUserIdAndUsedAtIsNullAndRevokedAtIsNullAndExpiresAtAfter(userId, now);
        for (PasswordResetToken t : valid) {
            t.setRevokedAt(now);
            passwordResetTokenRepository.save(t);
        }
    }

    private boolean isProdActive() {
        return Arrays.asList(environment.getActiveProfiles()).contains(PROD_PROFILE);
    }

    private BusinessRuleException invalidLink() {
        return new BusinessRuleException(INVALID_LINK_MESSAGE);
    }
}
