package com.campusmarket.demo.auth;

import com.campusmarket.demo.sesion.Usuario;
import com.campusmarket.demo.sesion.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;

@Service
public class RegistroService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UsuarioRepository usuarios;
    private final TokenVerificacionRepository tokens;
    private final PasswordEncoder encoder;
    private final EmailService email;
    private final String dominio;
    private final long minutosVigencia;

    public RegistroService(UsuarioRepository usuarios,
                           TokenVerificacionRepository tokens,
                           PasswordEncoder encoder,
                           EmailService email,
                           @Value("${campusmarket.correo.dominio}") String dominio,
                           @Value("${campusmarket.token.minutos-vigencia:15}") long minutosVigencia) {
        this.usuarios = usuarios;
        this.tokens = tokens;
        this.encoder = encoder;
        this.email = email;
        this.dominio = dominio.toLowerCase();
        this.minutosVigencia = minutosVigencia;
    }

    /** CA-01-01: crea cuenta PENDIENTE y envía token. CA-01-02: rechaza duplicados. */
    @Transactional
    public void registrar(RegistroRequest req) {
        String correo = req.correo().trim().toLowerCase();

        if (!correo.endsWith("@" + dominio)) {
            throw new IllegalArgumentException("Debes usar un correo válido de " + dominio);
        }
        if (usuarios.existsByCorreo(correo)) {
            throw new CorreoDuplicadoException();
        }

        Usuario u = new Usuario();
        u.setCorreo(correo);
        u.setNombre(req.nombre().trim());
        u.setPasswordHash(encoder.encode(req.password())); // BCrypt, nunca texto plano
        u.setEstado("PENDIENTE");
        u.setRol("ESTUDIANTE");

        try {
            usuarios.saveAndFlush(u); // si hay carrera, salta la restricción UNIQUE
        } catch (DataIntegrityViolationException e) {
            throw new CorreoDuplicadoException();
        }

        String token = generarToken();
        TokenVerificacion t = new TokenVerificacion(
                u.getId(),
                hash(token),
                Instant.now().plus(minutosVigencia, ChronoUnit.MINUTES));
        tokens.save(t);

        email.enviarToken(correo, token, minutosVigencia);
    }

    /** Código numérico de 6 dígitos (como en el contrato: "123456"). */
     static String generarToken() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }

    /** SHA-256 en hex. Reutilizable en S1-T10 (verificar). */
    public static String hash(String token) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
