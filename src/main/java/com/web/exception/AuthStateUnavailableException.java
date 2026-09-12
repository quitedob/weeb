package com.web.exception;

/** Authentication must stop during a datastore outage without declaring the caller's token invalid. */
public class AuthStateUnavailableException extends RuntimeException {
    public AuthStateUnavailableException(Throwable cause) {
        super("Authentication storage is temporarily unavailable", cause);
    }
}
