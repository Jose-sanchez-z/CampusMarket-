package com.campusmarket.demo.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class VerificacionController {

    private final VerificacionService verificacionService;

    public VerificacionController(VerificacionService verificacionService) {
        this.verificacionService = verificacionService;
    }

    // POST /auth/verificar -> 200 | 422 token inválido/vencido/usado | 429 demasiados intentos
    @PostMapping("/verificar")
    public ResponseEntity<Map<String, String>> verificar(@Valid @RequestBody VerificarRequest body,
                                                         HttpServletRequest req) {
        verificacionService.verificar(body.correo(), body.token(), req.getRemoteAddr());
        return ResponseEntity.ok(Map.of("mensaje", "Cuenta activada"));
    }

    // POST /auth/reenviartoken -> 200 | 429 exceso de reenvíos
    @PostMapping("/reenviartoken")
    public ResponseEntity<Map<String, String>> reenviar(@Valid @RequestBody ReenviarRequest body,
                                                        HttpServletRequest req) {
        verificacionService.reenviar(body.correo(), req.getRemoteAddr());
        return ResponseEntity.ok(Map.of("mensaje", "Nuevo código enviado"));
    }
}
