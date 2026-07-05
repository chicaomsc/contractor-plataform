package io.chicaodw.platform.auth.infrastructure.security;

import io.chicaodw.platform.auth.application.JwtService;
import io.chicaodw.platform.auth.domain.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        try {
            Claims claims = jwtService.parseClaims(header.substring(7));

            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                UUID userId    = UUID.fromString(claims.getSubject());
                UUID companyId = UUID.fromString(claims.get("companyId", String.class));
                String email   = claims.get("email", String.class);
                UserRole role  = UserRole.valueOf(claims.get("role", String.class));

                var principal  = new JwtPrincipal(userId, companyId, email, role);
                var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
                var auth = new UsernamePasswordAuthenticationToken(principal, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        } catch (JwtException | IllegalArgumentException ignored) {
            // Invalid or expired token — leave request unauthenticated;
            // Spring Security's entry point will return 401 for protected resources.
        }

        chain.doFilter(request, response);
    }
}
