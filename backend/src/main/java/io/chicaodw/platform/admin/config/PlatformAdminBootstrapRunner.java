package io.chicaodw.platform.admin.config;

import io.chicaodw.platform.auth.domain.PasswordPolicy;
import io.chicaodw.platform.auth.domain.User;
import io.chicaodw.platform.auth.domain.UserRole;
import io.chicaodw.platform.auth.domain.UserRoleInvariant;
import io.chicaodw.platform.auth.domain.UserStatus;
import io.chicaodw.platform.auth.infrastructure.persistence.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Idempotent bootstrap of the first SUPER_ADMIN account, driven entirely by
 * PLATFORM_ADMIN_BOOTSTRAP_EMAIL / PLATFORM_ADMIN_BOOTSTRAP_PASSWORD — see
 * docs/design/DT-011A.7-platform-admin-multitenancy.md §8 for the full behavior
 * table this class implements exactly:
 *
 * <ul>
 *   <li>neither variable set → no-op (safe on every restart, every profile);</li>
 *   <li>exactly one set → misconfiguration, fails startup (never a half-bootstrap);</li>
 *   <li>both set, email already a SUPER_ADMIN → no-op, idempotent, password untouched;</li>
 *   <li>both set, email belongs to a non-SUPER_ADMIN account → fails startup (never
 *       silently promotes an OWNER, never resolved automatically);</li>
 *   <li>both set, password fails the same policy as RegisterRequest.password → fails
 *       startup, password itself is never logged;</li>
 *   <li>both set, everything valid → creates the SUPER_ADMIN (companyId null,
 *       ACTIVE, BCrypt-hashed password).</li>
 * </ul>
 *
 * No HTTP endpoint exists for this — the only way to create a SUPER_ADMIN is this
 * startup-time, operator-controlled mechanism.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PlatformAdminBootstrapRunner implements ApplicationRunner {

    private final PlatformAdminBootstrapProperties properties;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        String email = trimToNull(properties.getEmail());
        String password = trimToNull(properties.getPassword());

        if (email == null && password == null) {
            log.info("Platform admin bootstrap: PLATFORM_ADMIN_BOOTSTRAP_EMAIL/PASSWORD not configured, skipping.");
            return;
        }
        if (email == null || password == null) {
            throw new IllegalStateException(
                    "Platform admin bootstrap misconfigured: PLATFORM_ADMIN_BOOTSTRAP_EMAIL and "
                            + "PLATFORM_ADMIN_BOOTSTRAP_PASSWORD must both be set, or neither.");
        }

        Optional<User> existing = userRepository.findByEmail(email);
        if (existing.isPresent()) {
            if (existing.get().getRole() == UserRole.SUPER_ADMIN) {
                log.info("Platform admin bootstrap: SUPER_ADMIN already exists for the configured email, skipping.");
                return;
            }
            throw new IllegalStateException(
                    "Platform admin bootstrap misconfigured: PLATFORM_ADMIN_BOOTSTRAP_EMAIL already belongs to a "
                            + "non-SUPER_ADMIN account.");
        }

        if (password.length() < PasswordPolicy.MIN_LENGTH || password.length() > PasswordPolicy.MAX_LENGTH) {
            throw new IllegalStateException(
                    "Platform admin bootstrap misconfigured: PLATFORM_ADMIN_BOOTSTRAP_PASSWORD must be between "
                            + PasswordPolicy.MIN_LENGTH + " and " + PasswordPolicy.MAX_LENGTH + " characters.");
        }

        User admin = new User();
        admin.setCompanyId(null);
        admin.setEmail(email);
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setName("Platform Admin");
        admin.setRole(UserRole.SUPER_ADMIN);
        admin.setStatus(UserStatus.ACTIVE);
        UserRoleInvariant.validate(admin.getRole(), admin.getCompanyId());
        userRepository.save(admin);

        log.info("Platform admin bootstrap: created the initial SUPER_ADMIN account.");
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
