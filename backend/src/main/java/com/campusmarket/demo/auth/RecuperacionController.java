package com.campusmarket.demo.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints públicos (sin @RequiereSesion). */
@RestController
@RequestMapping("/auth/recuperacion")
public class RecuperacionController {

    public record SolicitarRequest(@NotBlank @Email String correo) {}
    public record ConfirmarRequest(@NotBlank String token, @NotBlank String nuevaContrasena) {}
    public record Mensaje(String mensaje) {}

    static final String MENSAJE_GENERICO = "Si el correo está registrado, te enviamos un token de recuperación";

    private final RecuperacionService servicio;

    public RecuperacionController(RecuperacionService servicio) {
        this.servicio = servicio;
    }

    // POST /auth/recuperacion/solicitar -> siempre 200 con el mismo cuerpo (CA-04-01)
    @PostMapping("/solicitar")
    public ResponseEntity<Mensaje> solicitar(@Valid @RequestBody SolicitarRequest body, HttpServletRequest req) {
        servicio.solicitar(body.correo(), req.getRemoteAddr());
        return ResponseEntity.ok(new Mensaje(MENSAJE_GENERICO));
    }

    // POST /auth/recuperacion/confirmar -> 200 | 400 token inválido/vencido/usado | 422 contraseña | 429
    @PostMapping("/confirmar")
    public ResponseEntity<Mensaje> confirmar(@Valid @RequestBody ConfirmarRequest body, HttpServletRequest req) {
        servicio.confirmar(body.token(), body.nuevaContrasena(), req.getRemoteAddr());
        return ResponseEntity.ok(new Mensaje("Contraseña actualizada. Inicia sesión de nuevo"));
    }
}
