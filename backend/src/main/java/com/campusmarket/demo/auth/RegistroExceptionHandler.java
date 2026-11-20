package com.campusmarket.demo.auth;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/** Limitado a los controladores de registro y verificación para no interferir con los errores de E08. */
@RestControllerAdvice(assignableTypes = {RegistroController.class, VerificacionController.class})
public class RegistroExceptionHandler {

    // 409: correo duplicado (CA-01-02)
    @ExceptionHandler(CorreoDuplicadoException.class)
    public ResponseEntity<Map<String, String>> duplicado(CorreoDuplicadoException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("mensaje", e.getMessage()));
    }

    // 422: token inválido, vencido o usado (CA-02-02)
    @ExceptionHandler(TokenInvalidoException.class)
    public ResponseEntity<Map<String, String>> tokenInvalido(TokenInvalidoException e) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).body(Map.of("mensaje", e.getMessage()));
    }

    // 429: exceso de intentos o reenvíos
    @ExceptionHandler(DemasiadosIntentosException.class)
    public ResponseEntity<Map<String, String>> demasiados(DemasiadosIntentosException e) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of("mensaje", e.getMessage()));
    }

    // 400: campos inválidos
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> invalido(MethodArgumentNotValidException e) {
        return ResponseEntity.badRequest().body(Map.of("mensaje", "Datos inválidos"));
    }

    // 400: correo fuera del dominio permitido
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> reglaNegocio(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("mensaje", e.getMessage()));
    }
}
