package io.chicaodw.platform.auth.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Rate limits only the five authentication endpoints named in DT-011B.5 §9 HARD-01
 * (SEC-AUTH-03) — never a global limiter (see {@link #resolveRule}, an explicit
 * allowlist of exactly those five routes). Runs before {@link JwtAuthenticationFilter}
 * so a request can be rejected before any JWT parsing/DB work — the key is the caller's
 * remote address plus the request path, so the admin-password-reset endpoint (which
 * does require authentication) is still throttled the same way regardless of whether
 * the caller is ultimately authorized.
 */
@Component
@RequiredArgsConstructor
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();
    private static final String ADMIN_PASSWORD_RESET_PATTERN = "/admin/companies/*/owners/*/password-reset";

    private final RateLimitProperties properties;
    private final InMemoryRateLimiter rateLimiter;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain chain) throws ServletException, IOException {
        RateLimitProperties.Rule rule = properties.isEnabled() ? resolveRule(request) : null;

        if (rule != null) {
            String key = request.getRemoteAddr() + "|" + request.getRequestURI();
            InMemoryRateLimiter.Decision decision = rateLimiter.tryAcquire(key, rule.getCapacity(), rule.getWindowSeconds());
            if (!decision.allowed()) {
                writeTooManyRequests(response, decision.retryAfterSeconds());
                return;
            }
        }

        chain.doFilter(request, response);
    }

    private RateLimitProperties.Rule resolveRule(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return null;
        }

        String path = request.getRequestURI();
        return switch (path) {
            case "/auth/login" -> properties.getLogin();
            case "/auth/password/forgot" -> properties.getForgotPassword();
            case "/auth/password/reset" -> properties.getResetPassword();
            case "/auth/invites/accept" -> properties.getInviteAccept();
            default -> PATH_MATCHER.match(ADMIN_PASSWORD_RESET_PATTERN, path) ? properties.getAdminPasswordReset() : null;
        };
    }

    private void writeTooManyRequests(HttpServletResponse response, long retryAfterSeconds) throws IOException {
        var problem = ProblemDetail.forStatus(HttpStatus.TOO_MANY_REQUESTS);
        problem.setTitle("Too Many Requests");
        problem.setDetail("Too many requests. Try again later.");

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds));
        response.getWriter().write(objectMapper.writeValueAsString(problem));
    }
}
