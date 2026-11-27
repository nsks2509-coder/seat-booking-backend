package dev.sriram.booking.service;

import org.springframework.http.HttpStatus;

public final class ConflictException extends ApiException {
    public ConflictException(String message) { super(HttpStatus.CONFLICT, message); }
}
