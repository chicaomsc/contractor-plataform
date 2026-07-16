package io.chicaodw.platform.common.exception;

/** Signals a 409 Conflict: invalid state transition or a uniqueness conflict the client can act on. */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
