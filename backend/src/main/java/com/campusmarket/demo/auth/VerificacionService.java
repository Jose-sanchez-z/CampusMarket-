package com.campusmarket.demo.auth;

import com.campusmarket.demo.sesion.Usuario;
import com.campusmarket.demo.sesion.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class VerificacionService {

    private static final Duration VENTANA = Duration.ofMinutes(15);
    private static final int MAX_FALLOS_CORREO = 5;
    private static final int MAX_FALLOS_IP = 20;
    private static final int MAX_REENVIOS_CORREO = 3;
    private static final int MAX_REENVIOS_IP = 10;

    private final UsuarioRepository usuarios;
    private final TokenVerificacionRepository tokens;
    private final EmailService email;
    private final IntentosLimiter limiter;
    private final long minutosVigencia;

    public VerificacionService(UsuarioRepository usuarios,
                               TokenVerificacionRepository tokens,
                               EmailService email,
                               IntentosLimiter limiter,
                               @Value("${campusmarket.token.minutos-vigencia:15}") long minutosVigencia) {
        this.usuarios = usuarios;
        this.tokens = tokens;
        this.email = email;
        this.limiter = limiter;
        this.minutosVigencia = minutosVigencia;
    }

    /** CA-02-01: token correcto activa la cuenta y queda usado. CA-02-02: vencido/usado no activa. */
    @Transactional
    public void verificar(String correoRaw, String token, String ip) {
        String correo = correoRaw.trim().toLowerCase();
        String claveCorreo = "v:" + correo;
        String claveIp = "vip:" + ip;

        if (limiter.bloqueado(claveCorreo, MAX_FALLOS_CORREO, VENTANA)
                || limiter.bloqueado(claveIp, MAX_FALLOS_IP, VENTANA)) {
            throw new DemasiadosIntentosException();
        }

        Usuario u = usuarios.findByCorreo(correo).orElse(null);
        TokenVerificacion t = (u == null) ? null
                : tokens.findFirstByUsuarioIdOrderByIdDesc(u.getId()).orElse(null);

        boolean ok = t != null
                && !t.isUsado()
                && t.getVenceEn().isAfter(Instant.now())
                && MessageDigest.isEqual(
                        RegistroService.hash(token).getBytes(StandardCharsets.UTF_8),
                        t.getTokenHash().getBytes(StandardCharsets.UTF_8));

        if (!ok) {
            limiter.registrar(claveCorreo);
            limiter.registrar(claveIp);
            // Mismo error para correo inexistente, token incorrecto, vencido o usado
            throw new TokenInvalidoException();
        }

        t.setUsado(true);
        u.setEstado("ACTIVA");
        tokens.save(t);
        usuarios.save(u);
        limiter.limpiar(claveCorreo);
    }

    /** Invalida el token anterior, genera uno nuevo y lo envía. Responde igual exista o no el correo. */
    @Transactional
    public void reenviar(String correoRaw, String ip) {
        String correo = correoRaw.trim().toLowerCase();
        String claveCorreo = "r:" + correo;
        String claveIp = "rip:" + ip;

        if (limiter.bloqueado(claveCorreo, MAX_REENVIOS_CORREO, VENTANA)
                || limiter.bloqueado(claveIp, MAX_REENVIOS_IP, VENTANA)) {
            throw new DemasiadosIntentosException();
        }
        limiter.registrar(claveCorreo);
        limiter.registrar(claveIp);

        Usuario u = usuarios.findByCorreo(correo).orElse(null);
        if (u == null || !"PENDIENTE".equals(u.getEstado())) {
            return; // no se revela si el correo existe o ya está activo
        }

        for (TokenVerificacion previo : tokens.findByUsuarioIdAndUsadoFalse(u.getId())) {
            previo.setUsado(true);
            tokens.save(previo);
        }

        String token = RegistroService.generarToken();
        tokens.save(new TokenVerificacion(
                u.getId(),
                RegistroService.hash(token),
                Instant.now().plus(minutosVigencia, ChronoUnit.MINUTES)));

        email.enviarToken(correo, token, minutosVigencia);
    }
}
