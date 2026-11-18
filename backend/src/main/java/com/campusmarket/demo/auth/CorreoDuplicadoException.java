package com.campusmarket.demo.auth;

public class CorreoDuplicadoException extends RuntimeException {
    public CorreoDuplicadoException() {
        super("El correo ya está registrado");
    }
}
