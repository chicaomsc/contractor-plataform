package io.chicaodw.platform.auth.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * {@code debugToken}/{@code debugResetLink} are omitted from the JSON body — never
 * emitted as {@code null} — unless a token was genuinely created by this call and the
 * "prod" profile is not active (DT-011A.10 §3/§13).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ForgotPasswordResponse(
        String message,
        String debugToken,
        String debugResetLink
) {}
