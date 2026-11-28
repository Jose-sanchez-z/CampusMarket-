package com.campusmarket.demo.perfil;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/** Limitado al controlador de perfil para no interferir con los errores de auth. Formato: { "mensaje": "..." }. */
@RestControllerAdvice(assignableTypes = PerfilController.class)
public class PerfilExceptionHandler {

    @ExceptionHandler(PerfilException.class)
    public ResponseEntity<Map<String, String>> perfil(PerfilException e) {
        return ResponseEntity.status(e.getStatus()).body(Map.of("mensaje", e.getMessage()));
    }

    // 400: cuerpo ausente o que no es un JSON válido
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> cuerpoInvalido(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest().body(Map.of("mensaje", "Cuerpo de la petición inválido"));
    }
}
