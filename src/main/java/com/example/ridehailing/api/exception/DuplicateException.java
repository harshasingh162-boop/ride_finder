package com.example.ridehailing.api.exception;

public class DuplicateException extends RuntimeException {

    public DuplicateException(String message) {
        super(message);
    }
}
