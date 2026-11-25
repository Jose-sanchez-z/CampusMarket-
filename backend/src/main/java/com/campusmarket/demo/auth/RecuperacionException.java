package com.campusmarket.demo.auth;

import org.springframework.http.HttpStatus;

public class RecuperacionException extends RuntimeException {

    private final HttpStatus status;

    public RecuperacionException(HttpStatus status, String mensaje) {
        super(mensaje);
        this.status = status;
    }

    public HttpStatus getStatus() { return status; }
}
