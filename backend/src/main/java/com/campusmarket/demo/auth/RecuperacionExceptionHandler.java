package com.campusmarket.demo.auth;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/** Limitado a RecuperacionController: no interfiere con los manejadores de registro ni de E08. */
@RestControllerAdvice(assignableTypes = RecuperacionController.class)
public class RecuperacionExceptionHandler {

    @ExceptionHandler(RecuperacionException.class)
    public ResponseEntity<Map<String, String>> recuperacion(RecuperacionException e) {
        return ResponseEntity.status(e.getStatus()).body(Map.of("mensaje", e.getMessage()));
    }

    // 400: campos vacíos o correo con formato inválido
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> invalido(MethodArgumentNotValidException e) {
        return ResponseEntity.badRequest().body(Map.of("mensaje", "Datos inválidos"));
    }
}
