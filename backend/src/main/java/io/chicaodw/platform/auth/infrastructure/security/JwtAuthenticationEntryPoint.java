package io.chicaodw.platform.auth.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        var problem = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
        // ActiveAccountFilter calls this entry point directly (it runs before Spring
        // Security's ExceptionTranslationFilter, so it cannot rely on that machinery)
        // with a DisabledException — keep the title/detail identical to
        // GlobalExceptionHandler's DisabledException mapping so API consumers see the
        // same shape regardless of which code path produced the 401.
        if (authException instanceof DisabledException) {
            problem.setTitle("Account Disabled");
            problem.setDetail("Your account is inactive");
        } else {
            problem.setTitle("Unauthorized");
            problem.setDetail("Authentication is required to access this resource");
        }

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(problem));
    }
}
