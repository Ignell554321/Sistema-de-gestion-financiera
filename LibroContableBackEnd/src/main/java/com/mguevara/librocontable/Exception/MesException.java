package com.mguevara.librocontable.Exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class MesException extends RuntimeException {

    private final HttpStatus status;

    public MesException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

}