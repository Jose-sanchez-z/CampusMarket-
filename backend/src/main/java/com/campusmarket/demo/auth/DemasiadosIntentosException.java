package com.campusmarket.demo.auth;

public class DemasiadosIntentosException extends RuntimeException {
    public DemasiadosIntentosException() {
        super("Demasiados intentos. Intenta de nuevo más tarde");
    }
}
