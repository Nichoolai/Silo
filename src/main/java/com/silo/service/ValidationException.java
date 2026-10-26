package com.silo.service;

/** Thrown for any user-correctable problem; the message is shown to the user as-is. */
public class ValidationException extends RuntimeException {
    public ValidationException(String message) { super(message); }
}
