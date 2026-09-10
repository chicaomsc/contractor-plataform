package io.chicaodw.platform.auth.application;

import io.chicaodw.platform.auth.api.dto.AuthResponse;
import io.chicaodw.platform.auth.api.dto.TeamInvitationResponse;
import io.chicaodw.platform.auth.api.mapper.AuthMapper;
import io.chicaodw.platform.auth.domain.RefreshToken;
import io.chicaodw.platform.auth.domain.TeamInvitation;
import io.chicaodw.platform.auth.domain.User;
import io.chicaodw.platform.auth.domain.UserRole;
import io.chicaodw.platform.auth.domain.UserRoleInvariant;
import io.chicaodw.platform.auth.domain.UserStatus;
import io.chicaodw.platform.auth.infrastructure.persistence.RefreshTokenRepository;
import io.chicaodw.platform.auth.infrastructure.persistence.TeamInvitationRepository;
import io.chicaodw.platform.auth.infrastructure.persistence.UserRepository;
import io.chicaodw.platform.auth.infrastructure.security.JwtProperties;
import io.chicaodw.platform.auth.infrastructure.security.TeamInvitationProperties;
import io.chicaodw.platform.common.email.EmailService;
import io.chicaodw.platform.common.exception.BusinessRuleException;
import io.chicaodw.platform.common.exception.ConflictException;
import io.chicaodw.platform.common.exception.ResourceNotFoundException;
import io.chicaodw.platform.common.security.SecureTokenGenerator;
import io.chicaodw.platform.common.security.TokenHasher;
import io.chicaodw.platform.company.domain.Company;
import io.chicaodw.platform.company.domain.CompanyStatus;
import io.chicaodw.platform.company.infrastructure.persistence.CompanyRepository;
import io.chicaodw.platform.company.infrastructure.config.TenantProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Owns the full lifecycle of team invitations (DT-017B): an OWNER inviting a MANAGER or
 * MEMBER into their own Company. Deliberately a separate service from {@link
 * InviteService} — see {@link TeamInvitation}'s Javadoc for why the two flows are not
 * unified — but every security pattern below (SecureTokenGenerator, SHA-256 hash-only
 * persistence, atomic single-use consumption, revoke-before-reissue, uniform invalid-
 * token error) mirrors {@code InviteService}/{@code PasswordResetTokenService} exactly,
 * on purpose.
 *
 * <p><b>Central rule (DT-017B §1): no {@code User} is ever created at invitation time.</b>
 * An unaccepted invitation is not a user — unlike the owner-invite flow (which creates a
 * PENDING {@code User} up front), {@code TeamInvitation} carries its own {@code email}/
 * {@code role} directly, and the {@code User} row is created exactly once, atomically,
 * inside {@link #acceptInvitation}.
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class TeamInvitationService {

    private static final String EMAIL_ALREADY_IN_USE_MESSAGE = "Este e-mail já está associado a uma conta.";
    private static final String DUPLICATE_INVITATION_MESSAGE =
            "Já existe um convite pendente para este e-mail nesta empresa.";
    private static final String INVALID_ROLE_MESSAGE = "Role must be MANAGER or MEMBER";
    private static final String INVALID_INVITATION_MESSAGE =
            "O convite é inválido ou não está mais disponível.";
    private static final String ALREADY_ACCEPTED_MESSAGE = "Invitation was already accepted";

    private final TeamInvitationRepository teamInvitationRepository;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final AuthMapper authMapper;
    private final TeamInvitationProperties teamInvitationProperties;
    private final TenantProperties tenantProperties;
    private final EmailService emailService;

    // ── OWNER-facing: POST /team/invitations ────────────────────────────────────

    /**
     * {@code companyId}/{@code invitedByUserId} must always come from the authenticated
     * principal (DT-017B §7/§27) — the caller (controller) never accepts either from the
     * request body. Duplicate-invitation check (DT-017B §5, decision A): a still-valid
     * pending invitation for the same Company+email blocks this call outright — the
     * client must use resend/revoke instead of silently getting a second live token for
     * the same person. This check is a plain SELECT-then-INSERT, not atomic against a
     * genuinely concurrent duplicate create (unlike acceptance, which DT-017B §16
     * explicitly calls out as safety-critical) — an OWNER racing themselves here is a
     * low-severity, self-inflicted edge case (worst case: two live invitations exist
     * briefly, fixable with one revoke), not a security boundary, so no partial unique
     * index was added for it — see docs/design/DT-017B-team-invitations-backend.md §5.
     */
    public TeamInvitationResponse createInvitation(UUID companyId, UUID invitedByUserId, String email, String role) {
        UserRole parsedRole = parseInvitableRole(role);
        String normalizedEmail = email.trim();

        // Never reveal which Company (or whether SUPER_ADMIN) an existing account
        // belongs to — same email-in-use collapsing already used at register() (DT-011B.2
        // §13) and the exact wording DT-017B §6/§24 asks for.
        if (userRepository.findByEmailIgnoreCase(normalizedEmail).isPresent()) {
            throw new ConflictException(EMAIL_ALREADY_IN_USE_MESSAGE);
        }

        Instant now = Instant.now();
        boolean pendingExists = teamInvitationRepository
                .findByCompanyIdAndEmailIgnoreCaseAndUsedAtIsNullAndRevokedAtIsNullAndExpiresAtAfter(
                        companyId, normalizedEmail, now)
                .isPresent();
        if (pendingExists) {
            throw new ConflictException(DUPLICATE_INVITATION_MESSAGE);
        }

        IssuedInvitation issued = issueNewInvitation(companyId, normalizedEmail, parsedRole, invitedByUserId);
        sendInvitationEmail(issued);
        return toResponse(issued.invitation());
    }

    @Transactional(readOnly = true)
    public List<TeamInvitationResponse> listInvitations(UUID companyId) {
        return teamInvitationRepository.findByCompanyIdOrderByCreatedAtDesc(companyId).stream()
                .map(this::toResponse)
                .toList();
    }

    /** Idempotent for an already-revoked invitation (silent no-op success, same
     * "harmless to repeat" posture as {@code AuthService.logout}); a genuine domain
     * error — not idempotent — once the invitation has already been accepted, since
     * "revoke" no longer means anything at that point (DT-017B §13). */
    public void revokeInvitation(UUID companyId, UUID invitationId) {
        TeamInvitation invitation = requireOwnedInvitation(companyId, invitationId);
        if (invitation.getUsedAt() != null) {
            throw new ConflictException(ALREADY_ACCEPTED_MESSAGE);
        }
        if (invitation.getRevokedAt() == null) {
            invitation.setRevokedAt(Instant.now());
            teamInvitationRepository.save(invitation);
        }
    }

    /**
     * Revokes the old invitation and issues a brand-new token — the old one stops
     * working immediately (DT-017B §14), same "reissue" shape as {@code
     * InviteService.reissueInvite}. {@code actingOwnerId} becomes the new invitation's
     * {@code invitedByUserId} (who is resending right now), mirroring how {@code
     * AdminCompanyService.reissueInvite} attributes a reissue to whichever SUPER_ADMIN
     * is acting at that moment, not the original inviter.
     */
    public TeamInvitationResponse resendInvitation(UUID companyId, UUID invitationId, UUID actingOwnerId) {
        TeamInvitation existing = requireOwnedInvitation(companyId, invitationId);
        if (existing.getUsedAt() != null) {
            throw new ConflictException(ALREADY_ACCEPTED_MESSAGE);
        }
        if (existing.getRevokedAt() == null) {
            existing.setRevokedAt(Instant.now());
            teamInvitationRepository.save(existing);
        }

        IssuedInvitation reissued = issueNewInvitation(companyId, existing.getEmail(), existing.getRole(), actingOwnerId);
        sendInvitationEmail(reissued);
        return toResponse(reissued.invitation());
    }

    // ── Public: POST /auth/team-invitations/accept ──────────────────────────────

    /**
     * Consumes the invitation atomically before doing anything else, then validates
     * everything else inside the same {@code @Transactional} method — same ordering
     * rationale as {@code PasswordResetTokenService.resetPassword}/{@code
     * InviteService.acceptInvite}: if any later check throws, the whole transaction
     * (including the atomic consumption above) rolls back too, so "tudo ou nada" holds
     * without needing every individual check folded into one giant UPDATE (DT-017B §17).
     *
     * <p>Every failure — unknown token, expired, used, revoked, inactive Company, or the
     * email having been claimed by someone else since the invitation was issued — throws
     * the exact same {@link BusinessRuleException} with the exact same message. This is
     * intentionally MORE uniform than {@code InviteService.acceptInvite} (which still
     * returns 404 for a wholly unknown token) — DT-017B §24 explicitly asks this public,
     * unauthenticated endpoint to never be usable as an oracle for any of these cases.
     */
    public AuthResponse acceptInvitation(String rawToken, String name, String password) {
        String hash = TokenHasher.sha256Hex(rawToken);
        TeamInvitation invitation = teamInvitationRepository.findByTokenHash(hash)
                .orElseThrow(this::invalidInvitation);

        Instant now = Instant.now();
        int updated = teamInvitationRepository.markUsedIfStillValid(invitation.getId(), now);
        if (updated == 0) {
            throw invalidInvitation();
        }

        Company company = companyRepository.findById(invitation.getCompanyId())
                .orElseThrow(this::invalidInvitation);
        if (company.getStatus() != CompanyStatus.ACTIVE) {
            throw invalidInvitation();
        }

        // Race window between invitation creation and acceptance: the email could have
        // been claimed since by any other flow (register, another invitation's accept,
        // admin owner creation). Re-checked here, inside the same atomic acceptance
        // (DT-017B §25) — and users.email's own DB-level uniqueness (V2) is the final,
        // non-bypassable backstop below regardless.
        if (userRepository.findByEmailIgnoreCase(invitation.getEmail()).isPresent()) {
            throw invalidInvitation();
        }

        User user = new User();
        user.setCompanyId(invitation.getCompanyId());
        user.setEmail(invitation.getEmail());
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setName(name);
        user.setRole(invitation.getRole());
        user.setStatus(UserStatus.ACTIVE);
        UserRoleInvariant.validate(user.getRole(), user.getCompanyId());

        try {
            user = userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            // Genuine race lost: the email was claimed between the check above and this
            // INSERT. users.uq_users_email is what actually enforces this — the check
            // above only makes the common case fail with the right generic message
            // instead of a raw constraint-violation error.
            throw invalidInvitation();
        }

        String accessToken = jwtService.generateAccessToken(user);
        // Accepting a team invitation is this user's first session — same as
        // AuthService.login()/register()/InviteService.acceptInvite, its absolute
        // lifetime clock (DT-014) starts now.
        IssuedRefreshToken refresh = issueRefreshToken(user.getId(), Instant.now());

        return new AuthResponse(
                accessToken,
                refresh.rawToken(),
                authMapper.toUserResponse(user),
                authMapper.toCompanyResponse(company)
        );
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private UserRole parseInvitableRole(String role) {
        UserRole parsed;
        try {
            parsed = UserRole.valueOf(role.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BusinessRuleException(INVALID_ROLE_MESSAGE);
        }
        // DT-017A roles: OWNER/SUPER_ADMIN can never be the target of a team invitation
        // — enforced here (domain/service layer), not only by request validation, and
        // backed by chk_team_invitations_role (V16) as the real, non-bypassable
        // guarantee. See DT-017B §3.
        if (parsed != UserRole.MANAGER && parsed != UserRole.MEMBER) {
            throw new BusinessRuleException(INVALID_ROLE_MESSAGE);
        }
        return parsed;
    }

    private TeamInvitation requireOwnedInvitation(UUID companyId, UUID invitationId) {
        TeamInvitation invitation = teamInvitationRepository.findById(invitationId)
                .orElseThrow(() -> new ResourceNotFoundException("TeamInvitation", invitationId));
        // Cross-tenant: a real id belonging to another Company must look identical to a
        // nonexistent one — same anti-IDOR posture as AdminCompanyService.requireOwnerInCompany
        // (DT-017B §22).
        if (!companyId.equals(invitation.getCompanyId())) {
            throw new ResourceNotFoundException("TeamInvitation", invitationId);
        }
        return invitation;
    }

    /** Pairs the persisted row with its raw token — the ONLY place the raw value exists
     * outside {@link #acceptInvitation}'s own lookup-by-hash; never persisted (only
     * {@code tokenHash} is, on the entity itself) and never returned from {@link
     * #createInvitation}/{@link #resendInvitation} (DT-017B §23) — it lives only long
     * enough, in memory, for {@link #sendInvitationEmail} to build the accept link. */
    private record IssuedInvitation(TeamInvitation invitation, String rawToken) {}

    private IssuedInvitation issueNewInvitation(UUID companyId, String email, UserRole role, UUID invitedByUserId) {
        String rawToken = SecureTokenGenerator.generate();

        TeamInvitation invitation = new TeamInvitation();
        invitation.setCompanyId(companyId);
        invitation.setEmail(email);
        invitation.setRole(role);
        invitation.setTokenHash(TokenHasher.sha256Hex(rawToken));
        invitation.setExpiresAt(Instant.now().plusSeconds(teamInvitationProperties.getTtlSeconds()));
        invitation.setInvitedByUserId(invitedByUserId);
        teamInvitationRepository.save(invitation);

        return new IssuedInvitation(invitation, rawToken);
    }

    /**
     * Defense in depth on top of {@link EmailService}'s own never-throw contract — same
     * pattern as {@code PasswordResetTokenService.sendResetEmail}: even a misbehaving
     * future implementation can never turn a delivery failure into a different HTTP
     * response or a propagated exception here. The invitation row is already persisted
     * by the time this runs (DT-017B §11) — a Resend outage never prevents the
     * invitation from existing, only from being emailed; {@code resendInvitation} is how
     * an OWNER recovers that case (a fresh token, a fresh delivery attempt).
     */
    private void sendInvitationEmail(IssuedInvitation issued) {
        try {
            TeamInvitation invitation = issued.invitation();
            Company company = companyRepository.findById(invitation.getCompanyId())
                    .orElseThrow(() -> new ResourceNotFoundException("Company", invitation.getCompanyId()));
            String acceptLink = buildAcceptLink(issued.rawToken());
            Duration validity = Duration.between(Instant.now(), invitation.getExpiresAt());
            emailService.sendTeamInvitationEmail(
                    invitation.getEmail(), company.getName(), friendlyRoleName(invitation.getRole()),
                    acceptLink, validity);
        } catch (RuntimeException e) {
            log.warn("EmailService threw despite its never-throw contract — ignored, invitation stays created", e);
        }
    }

    private String friendlyRoleName(UserRole role) {
        return switch (role) {
            case MANAGER -> "Administrador";
            case MEMBER -> "Colaborador";
            // OWNER/SUPER_ADMIN can never reach here — parseInvitableRole already
            // rejects them before an invitation is ever created.
            case OWNER, SUPER_ADMIN -> throw new IllegalStateException("Not an invitable role: " + role);
        };
    }

    /** DT-017B §9 — fragment, not query string, so the token never lands in server logs,
     * browser history, or a Referer header; same shape as {@code
     * PasswordResetTokenService.buildResetLink}, same platform-wide frontend origin
     * (never a tenant-specific host — see {@code TenantProperties}'s own Javadoc for
     * why). NEVER logged — it contains the raw token. */
    private String buildAcceptLink(String rawToken) {
        return tenantProperties.getFrontendBaseUrl() + "/invite/team#token=" + rawToken;
    }

    private BusinessRuleException invalidInvitation() {
        return new BusinessRuleException(INVALID_INVITATION_MESSAGE);
    }

    private TeamInvitationResponse toResponse(TeamInvitation invitation) {
        return new TeamInvitationResponse(
                invitation.getId(),
                invitation.getEmail(),
                invitation.getRole().name(),
                deriveStatus(invitation).name(),
                invitation.getExpiresAt(),
                invitation.getCreatedAt()
        );
    }

    /** Not persisted — always computed from usedAt/revokedAt/expiresAt (DT-017B §12). */
    private enum DerivedStatus { PENDING, EXPIRED, USED, REVOKED }

    private DerivedStatus deriveStatus(TeamInvitation invitation) {
        if (invitation.getUsedAt() != null) {
            return DerivedStatus.USED;
        }
        if (invitation.getRevokedAt() != null) {
            return DerivedStatus.REVOKED;
        }
        if (invitation.getExpiresAt().isBefore(Instant.now())) {
            return DerivedStatus.EXPIRED;
        }
        return DerivedStatus.PENDING;
    }

    /** Mirrors AuthService's/InviteService's own private record of the same name/shape
     * — hash-only persistence. */
    private record IssuedRefreshToken(String rawToken, Instant expiresAt) {}

    /** @param sessionStartedAt the absolute-lifetime clock's origin (DT-014) — see
     *                          AuthService.issueRefreshToken for the full rationale. */
    private IssuedRefreshToken issueRefreshToken(UUID userId, Instant sessionStartedAt) {
        String rawToken = UUID.randomUUID() + "-" + UUID.randomUUID();

        RefreshToken token = new RefreshToken();
        token.setUserId(userId);
        token.setTokenHash(TokenHasher.sha256Hex(rawToken));
        token.setExpiresAt(Instant.now().plusSeconds(jwtProperties.getRefreshTokenTtl()));
        token.setSessionStartedAt(sessionStartedAt);
        refreshTokenRepository.save(token);

        return new IssuedRefreshToken(rawToken, token.getExpiresAt());
    }
}
