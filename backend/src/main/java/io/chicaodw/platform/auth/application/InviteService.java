package io.chicaodw.platform.auth.application;

import io.chicaodw.platform.auth.api.dto.AuthResponse;
import io.chicaodw.platform.auth.api.mapper.AuthMapper;
import io.chicaodw.platform.auth.domain.OwnerInvite;
import io.chicaodw.platform.auth.domain.RefreshToken;
import io.chicaodw.platform.auth.domain.User;
import io.chicaodw.platform.auth.domain.UserStatus;
import io.chicaodw.platform.auth.infrastructure.persistence.OwnerInviteRepository;
import io.chicaodw.platform.auth.infrastructure.persistence.RefreshTokenRepository;
import io.chicaodw.platform.auth.infrastructure.persistence.UserRepository;
import io.chicaodw.platform.auth.infrastructure.security.JwtProperties;
import io.chicaodw.platform.common.exception.BusinessRuleException;
import io.chicaodw.platform.common.exception.ConflictException;
import io.chicaodw.platform.common.exception.ResourceNotFoundException;
import io.chicaodw.platform.common.security.SecureTokenGenerator;
import io.chicaodw.platform.common.security.TokenHasher;
import io.chicaodw.platform.company.domain.Company;
import io.chicaodw.platform.company.infrastructure.persistence.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Owns the full lifecycle of owner-onboarding invites (DT-011A.7 §9/§11/§12):
 * creation, reissue (revoke previous + create new), revocation (no reissue), and the
 * unauthenticated token-accept flow that sets the owner's password and activates the
 * account. Only {@link OwnerInvite#getTokenHash()} (SHA-256) is ever persisted — same
 * "shown once" pattern as EstimateShareService.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class InviteService {

    private static final int EXPIRES_IN_DAYS = 7;

    private final OwnerInviteRepository ownerInviteRepository;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final AuthMapper authMapper;

    public record IssuedInvite(String rawToken, Instant expiresAt) {}

    /** Used right after a new PENDING owner is created — no prior invite can exist yet. */
    public IssuedInvite createInvite(UUID ownerId, UUID createdByUserId) {
        return issueNewInvite(ownerId, createdByUserId);
    }

    /** Revokes any still-valid invite for this owner and issues a brand new one. */
    public IssuedInvite reissueInvite(UUID ownerId, UUID actingSuperAdminId) {
        User owner = requirePendingOwner(ownerId);
        revokeValidInvitesForUser(owner.getId(), null);
        return issueNewInvite(ownerId, actingSuperAdminId);
    }

    /** Revokes the owner's current valid invite without issuing a replacement. */
    public void revokeInvite(UUID ownerId) {
        User owner = requirePendingOwner(ownerId);
        int revoked = revokeValidInvitesForUser(owner.getId(), null);
        if (revoked == 0) {
            throw new ConflictException("No active invite to revoke for this owner");
        }
    }

    /**
     * Consumes the invite atomically before doing anything else — same ordering
     * rationale as {@code PasswordResetTokenService.resetPassword}: the whole method
     * is transactional, so if a later check (owner not PENDING) throws, the atomic
     * consumption rolls back too, and "tudo ou nada" still holds (SEC-AUTH-06,
     * Sprint 11B.6D).
     */
    public AuthResponse acceptInvite(String rawToken, String newPassword) {
        String hash = TokenHasher.sha256Hex(rawToken);
        OwnerInvite invite = ownerInviteRepository.findByTokenHash(hash)
                .orElseThrow(() -> new ResourceNotFoundException("Invite", "token"));

        Instant now = Instant.now();
        int updated = ownerInviteRepository.markUsedIfStillValid(invite.getId(), now);
        if (updated == 0) {
            throw new BusinessRuleException("Invite token is expired, revoked or already used");
        }

        User owner = userRepository.findById(invite.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", invite.getUserId()));
        if (owner.getStatus() != UserStatus.PENDING) {
            throw new BusinessRuleException("Invite token is expired, revoked or already used");
        }

        owner.setPasswordHash(passwordEncoder.encode(newPassword));
        owner.setStatus(UserStatus.ACTIVE);
        userRepository.save(owner);

        // Defensive: normally at most one valid invite exists per owner, but never
        // leave a second still-valid invite usable after one has been accepted.
        revokeValidInvitesForUser(owner.getId(), invite.getId());

        Company company = companyRepository.findById(owner.getCompanyId())
                .orElseThrow(() -> new ResourceNotFoundException("Company", owner.getCompanyId()));

        String accessToken = jwtService.generateAccessToken(owner);
        IssuedRefreshToken refresh = issueRefreshToken(owner.getId());

        return new AuthResponse(
                accessToken,
                refresh.rawToken(),
                authMapper.toUserResponse(owner),
                authMapper.toCompanyResponse(company)
        );
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private User requirePendingOwner(UUID ownerId) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", ownerId));
        if (owner.getStatus() != UserStatus.PENDING) {
            throw new ConflictException("Owner is not pending — nothing to reissue/revoke");
        }
        return owner;
    }

    private int revokeValidInvitesForUser(UUID userId, UUID exceptInviteId) {
        Instant now = Instant.now();
        List<OwnerInvite> valid = ownerInviteRepository
                .findByUserIdAndUsedAtIsNullAndRevokedAtIsNullAndExpiresAtAfter(userId, now);
        int count = 0;
        for (OwnerInvite invite : valid) {
            if (exceptInviteId != null && exceptInviteId.equals(invite.getId())) {
                continue;
            }
            invite.setRevokedAt(now);
            ownerInviteRepository.save(invite);
            count++;
        }
        return count;
    }

    private IssuedInvite issueNewInvite(UUID ownerId, UUID createdByUserId) {
        String rawToken = SecureTokenGenerator.generate();

        OwnerInvite invite = new OwnerInvite();
        invite.setUserId(ownerId);
        invite.setTokenHash(TokenHasher.sha256Hex(rawToken));
        invite.setExpiresAt(Instant.now().plus(EXPIRES_IN_DAYS, ChronoUnit.DAYS));
        invite.setCreatedBy(createdByUserId);
        ownerInviteRepository.save(invite);

        return new IssuedInvite(rawToken, invite.getExpiresAt());
    }

    /** Mirrors AuthService's own private record of the same name/shape — hash-only persistence. */
    private record IssuedRefreshToken(String rawToken, Instant expiresAt) {}

    private IssuedRefreshToken issueRefreshToken(UUID userId) {
        String rawToken = UUID.randomUUID() + "-" + UUID.randomUUID();

        RefreshToken token = new RefreshToken();
        token.setUserId(userId);
        token.setTokenHash(TokenHasher.sha256Hex(rawToken));
        token.setExpiresAt(Instant.now().plusSeconds(jwtProperties.getRefreshTokenTtl()));
        refreshTokenRepository.save(token);

        return new IssuedRefreshToken(rawToken, token.getExpiresAt());
    }
}
