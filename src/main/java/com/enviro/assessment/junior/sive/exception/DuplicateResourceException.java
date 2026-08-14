package com.enviro.assessment.junior.sive.exception;

/** Thrown when creating a resource would violate a uniqueness constraint (e.g. an investor email that already exists). Mapped to HTTP 409. */
public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) {
        super(message);
    }
}
