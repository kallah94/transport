package com.gayale.transport.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class MissingRateException extends RuntimeException {
    public MissingRateException(String message) {
        super(message);
    }
}
