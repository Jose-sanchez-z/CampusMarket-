package com.campusmarket.demo.auth;

import com.campusmarket.demo.sesion.Usuario;
import com.campusmarket.demo.sesion.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;

@Service
public class RecuperacionService {

    private static final Logger log = LoggerFactory.getLogger(RecuperacionService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private static final Duration VENTANA_SOLICITUD = Duration.ofHours(1);
    private static final int MAX_SOLICITUDES_CORREO = 3;   // por hora
    private static final int MAX_SOLICITUDES_IP = 10;      // por hora

    private static final Duration VENTANA_CONFIRMACION = Duration.ofMinutes(15);
    private static final int MAX_FALLOS_CONFIRMACION_IP = 10;

    // Misma política que el registro (RegistroRequest): 8 a 72 caracteres (72 = límite de BCrypt)
    private static final int PASSWORD_MIN = 8;
    private static final int PASSWORD_MAX = 72;

    private static final String MSG_TOKEN = "Token inválido o vencido";

    private final UsuarioRepository usuarios;
    private final TokenRecuperacionRepository tokens;
    private final SesionRevocacionRepository sesiones;
    private final PasswordEncoder encoder;
    private final RecuperacionMailer mailer;
    private final IntentosLimiter limiter; // de S1-T10
    private final long minutosVigencia;

    public RecuperacionService(UsuarioRepository usuarios,
                               TokenRecuperacionRepository tokens,
                               SesionRevocacionRepository sesiones,
                               PasswordEncoder encoder,
                               RecuperacionMailer mailer,
                               IntentosLimiter limiter,
                               @Value("${campusmarket.recuperacion.minutos-vigencia:30}") long minutosVigencia) {
        this.usuarios = usuarios;
        this.tokens = tokens;
        this.sesiones = sesiones;
        this.encoder = encoder;
        this.mailer = mailer;
        this.limiter = limiter;
        this.minutosVigencia = minutosVigencia;
    }

    /**
     * CA-04-01. No devuelve nada ni lanza errores según el correo: el controlador responde siempre igual.
     * Si hay exceso de solicitudes tampoco cambia la respuesta: simplemente no se envía nada.
     */
    @Transactional
    public void solicitar(String correoRaw, String ip) {
        String correo = correoRaw.trim().toLowerCase();
        String claveCorreo = "rs:" + correo;
        String claveIp = "rsip:" + ip;

        boolean limitado = limiter.bloqueado(claveCorreo, MAX_SOLICITUDES_CORREO, VENTANA_SOLICITUD)
                || limiter.bloqueado(claveIp, MAX_SOLICITUDES_IP, VENTANA_SOLICITUD);
        limiter.registrar(claveCorreo); // se cuenta exista o no el correo
        limiter.registrar(claveIp);
        if (limitado) return;

        Usuario u = usuarios.findByCorreo(correo).orElse(null);
        if (u == null || !"ACTIVA".equals(u.getEstado())) return;

        tokens.invalidarPendientes(u.getId());

        String token = generarToken();
        Instant ahora = Instant.now();
        tokens.save(new TokenRecuperacion(
                u.getId(),
                RegistroService.hash(token),            // solo se guarda el hash
                ahora.plus(minutosVigencia, ChronoUnit.MINUTES),
                ahora));

        // El envío (lento) va fuera del hilo de la petición: así el tiempo de respuesta
        // es parecido exista o no el correo. Nunca se registra el token en logs.
        CompletableFuture.runAsync(() -> {
            try {
                mailer.enviar(correo, token, minutosVigencia);
            } catch (RuntimeException e) {
                log.warn("No se pudo enviar el correo de recuperación ({})", e.getClass().getSimpleName());
            }
        });
    }

    /** CA-04-02 y CA-04-03. Todo o nada: clave nueva + token usado + sesiones revocadas en una transacción. */
    @Transactional(noRollbackFor = RecuperacionException.class)
    public void confirmar(String tokenPlano, String nuevaContrasena, String ip) {
        String claveIp = "rc:" + ip;
        if (limiter.bloqueado(claveIp, MAX_FALLOS_CONFIRMACION_IP, VENTANA_CONFIRMACION)) {
            throw new RecuperacionException(HttpStatus.TOO_MANY_REQUESTS,
                    "Demasiados intentos. Intenta de nuevo más tarde");
        }

        TokenRecuperacion t = tokens.findByTokenHash(RegistroService.hash(tokenPlano.trim())).orElse(null);
        Usuario u = (t == null) ? null : usuarios.findById(t.getUsuarioId()).orElse(null);

        if (t == null || u == null || t.isUsado() || !t.getVencimiento().isAfter(Instant.now())) {
            limiter.registrar(claveIp);
            throw new RecuperacionException(HttpStatus.BAD_REQUEST, MSG_TOKEN); // sin tocar la clave
        }

        if (nuevaContrasena.length() < PASSWORD_MIN || nuevaContrasena.length() > PASSWORD_MAX) {
            // el token NO se consume: el usuario puede reintentar con una clave válida
            throw new RecuperacionException(HttpStatus.UNPROCESSABLE_CONTENT,
                    "La contraseña debe tener entre " + PASSWORD_MIN + " y " + PASSWORD_MAX + " caracteres");
        }

        u.setPasswordHash(encoder.encode(nuevaContrasena)); // BCrypt, nunca texto plano
        t.setUsado(true);
        usuarios.save(u);
        tokens.save(t);
        sesiones.revocarTodas(u.getId());
    }

    /** Token largo y aleatorio (256 bits, base64 url-safe). No puede ser de 6 dígitos: token_hash es UNIQUE. */
    private static String generarToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
