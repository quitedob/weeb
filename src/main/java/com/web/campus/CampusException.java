package com.web.campus;

/** Expected campus validation, permission, and optimistic concurrency failures. */
public final class CampusException extends RuntimeException {
    private final int status;
    public CampusException(int status, String message) { super(message); this.status = status; }
    public int getStatus() { return status; }
}
