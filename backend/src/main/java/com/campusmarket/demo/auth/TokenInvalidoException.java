package com.campusmarket.demo.auth;

public class TokenInvalidoException extends RuntimeException {
    public TokenInvalidoException() {
        super("Código inválido o vencido");
    }
}
