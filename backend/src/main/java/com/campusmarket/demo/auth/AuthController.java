package com.campusmarket.demo.auth;

import com.campusmarket.demo.sesion.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/auth")
public class AuthController {

    public record LoginRequest(@NotBlank String correo, @NotBlank String password) {}
    public record LoginResponse(Long id, String nombre, String rol) {}
    public record YoResponse(Long id, String nombre, String correo, String rol) {}
    public record Mensaje(String mensaje) {}

    // Hash válido de relleno: gasta el mismo tiempo cuando el correo no existe (evita enumerar usuarios)
    private final String hashFalso;

    private final UsuarioRepository usuarios;
    private final SesionService sesiones;
    private final PasswordEncoder encoder;
    private final boolean cookieSecure;
    private final String cookieSameSite;

    public AuthController(UsuarioRepository usuarios, SesionService sesiones, PasswordEncoder encoder,
                          @Value("${app.cookie.secure:false}") boolean cookieSecure,
                          @Value("${app.cookie.samesite:Lax}") String cookieSameSite) {
        this.usuarios = usuarios;
        this.sesiones = sesiones;
        this.encoder = encoder;
        this.hashFalso = encoder.encode("relleno-anti-enumeracion");
        this.cookieSecure = cookieSecure;
        this.cookieSameSite = cookieSameSite;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest body) {
        var usuario = usuarios.findByCorreo(body.correo().trim().toLowerCase()).orElse(null);

        boolean passwordOk = encoder.matches(body.password(),
                usuario != null ? usuario.getPasswordHash() : hashFalso);

        if (usuario == null || !passwordOk) {
            return ResponseEntity.status(401).body(new Mensaje("Credenciales incorrectas"));   // 401 Inválido
        }
        if (!"ACTIVA".equals(usuario.getEstado())) {
            return ResponseEntity.status(403).body(new Mensaje("Cuenta pendiente de verificación")); // CA-03-02
        }

        String token = sesiones.crear(usuario);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie(token, sesiones.getDuracion()).toString()) // CA-03-01
                .body(new LoginResponse(usuario.getId(), usuario.getNombre(), usuario.getRol()));
    }

    @RequiereSesion
    @PostMapping("/logout")
    public ResponseEntity<Mensaje> logout(HttpServletRequest req) {
        String token = (String) req.getAttribute(SesionInterceptor.ATTR_TOKEN);
        sesiones.revocar(token);                                                              // CA-03-03
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO).toString())
                .body(new Mensaje("Sesión cerrada"));
    }

    @RequiereSesion
    @GetMapping("/yo")
    public YoResponse yo(HttpServletRequest req) {
        Usuario u = (Usuario) req.getAttribute(SesionInterceptor.ATTR_USUARIO);
        return new YoResponse(u.getId(), u.getNombre(), u.getCorreo(), u.getRol());
    }

    private ResponseCookie cookie(String valor, Duration maxAge) {
        return ResponseCookie.from(SesionInterceptor.COOKIE, valor)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path("/")
                .maxAge(maxAge)
                .build();
    }
}
