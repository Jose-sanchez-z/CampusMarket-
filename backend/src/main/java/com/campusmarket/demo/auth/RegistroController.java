package com.campusmarket.demo.auth;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** Controlador aparte del AuthController de E08 para evitar conflictos de merge. */
@RestController
@RequestMapping("/auth")
public class RegistroController {

    private final RegistroService registroService;

    public RegistroController(RegistroService registroService) {
        this.registroService = registroService;
    }

    // POST /auth/registro -> 202 sin iniciar sesión (CA-01-01)
    @PostMapping("/registro")
    public ResponseEntity<Map<String, String>> registro(@Valid @RequestBody RegistroRequest req) {
        registroService.registrar(req);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of("mensaje", "Código enviado"));
    }
}
