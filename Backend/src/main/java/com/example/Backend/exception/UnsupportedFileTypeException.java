package com.example.Backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
public class UnsupportedFileTypeException extends RuntimeException {

    public UnsupportedFileTypeException(String message) {
        super(message);
    }
}