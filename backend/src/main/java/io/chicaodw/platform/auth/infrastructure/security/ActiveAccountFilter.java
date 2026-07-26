package io.chicaodw.platform.auth.infrastructure.security;

import io.chicaodw.platform.auth.domain.UserRole;
import io.chicaodw.platform.auth.domain.UserStatus;
import io.chicaodw.platform.auth.infrastructure.persistence.UserRepository;
import io.chicaodw.platform.company.domain.CompanyStatus;
import io.chicaodw.platform.company.infrastructure.persistence.CompanyRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Runs immediately after JwtAuthenticationFilter for every request that carries a
 * JwtPrincipal. A syntactically valid, unexpired access token is not sufficient to
 * authorize a request — the account (and, for an OWNER, its company) must still be
 * ACTIVE right now, not just at the moment the token was issued (DT-011A.7 §13/§14).
 *
 * Two plain single-entity scalar queries (User.status, then Company.status only when
 * the principal is not a SUPER_ADMIN) — deliberately not a single cross-entity ad-hoc
 * join: that shape was tried first and dropped in favor of this simpler, unambiguous
 * pair of queries after it proved unreliable for a NULL company_id (SUPER_ADMIN) in a
 * real Testcontainers run. No Redis, no cache, no distributed session (DT §14).
 *
 * This filter runs before Spring Security's ExceptionTranslationFilter in the chain
 * (registered right after JwtAuthenticationFilter, itself before
 * UsernamePasswordAuthenticationFilter), so it cannot rely on that filter to translate
 * a thrown AuthenticationException into a response — it must write the 401 itself,
 * via the same JwtAuthenticationEntryPoint used elsewhere, and return without calling
 * the rest of the chain.
 */
@Component
@RequiredArgsConstructor
public class ActiveAccountFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final JwtAuthenticationEntryPoint authenticationEntryPoint;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain chain) throws ServletException, IOException {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.getPrincipal() instanceof JwtPrincipal principal) {
            if (!isActive(principal)) {
                SecurityContextHolder.clearContext();
                authenticationEntryPoint.commence(request, response, new DisabledException("Account is inactive"));
                return;
            }
        }

        chain.doFilter(request, response);
    }

    private boolean isActive(JwtPrincipal principal) {
        UserStatus userStatus = userRepository.findStatusById(principal.userId()).orElse(null);
        if (userStatus != UserStatus.ACTIVE) {
            return false;
        }

        if (principal.role() == UserRole.SUPER_ADMIN) {
            return true;
        }

        if (principal.companyId() == null) {
            return false;
        }

        CompanyStatus companyStatus = companyRepository.findStatusById(principal.companyId()).orElse(null);
        return companyStatus == CompanyStatus.ACTIVE;
    }
}
