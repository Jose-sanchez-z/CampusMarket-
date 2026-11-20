package com.campusmarket.demo.sesion;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class SesionService {

    private final SesionRepository sesiones;
    private final Duration duracion;
    private final SecureRandom random = new SecureRandom();

    public SesionService(SesionRepository sesiones,
                         @Value("${app.sesion.horas:24}") long horas) {
        this.sesiones = sesiones;
        this.duracion = Duration.ofHours(horas);
    }

    public Duration getDuracion() { return duracion; }

    /** Crea la sesión en BD y devuelve el token que va en la cookie. */
    public String crear(Usuario usuario) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        sesiones.save(new Sesion(hash(token), usuario.getId(), Instant.now().plus(duracion)));
        return token;
    }

    /** Existe, no está revocada y no ha vencido. */
    public Optional<Sesion> validar(String token) {
        return sesiones.findById(hash(token))
                .filter(s -> !s.isRevocada())
                .filter(s -> s.getVencimiento().isAfter(Instant.now()));
    }

    public void revocar(String token) {
        sesiones.findById(hash(token)).ifPresent(s -> {
            s.setRevocada(true);
            sesiones.save(s);
        });
    }

    private static String hash(String token) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
