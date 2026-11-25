package com.campusmarket.demo;

import com.campusmarket.demo.auth.RegistroService;
import com.campusmarket.demo.auth.TokenRecuperacion;
import com.campusmarket.demo.auth.TokenRecuperacionRepository;
import com.campusmarket.demo.sesion.Sesion;
import com.campusmarket.demo.sesion.SesionRepository;
import com.campusmarket.demo.sesion.Usuario;
import com.campusmarket.demo.sesion.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Pruebas de CA-04-01, CA-04-02 y CA-04-03 (S2-T09, E07).
 * Cada prueba usa su propio correo e IP: el limitador de intentos vive en memoria y se comparte.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthRecuperacionContratoTest {

    private static final String VIEJA = "ClaveVieja123!";
    private static final String NUEVA = "ClaveNueva456!";
    private static final String GENERICO =
            "{\"mensaje\":\"Si el correo está registrado, te enviamos un token de recuperación\"}";

    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired TokenRecuperacionRepository tokens;
    @Autowired SesionRepository sesiones;
    @Autowired PasswordEncoder encoder;
    @MockitoBean JavaMailSender mailSender; // no se envían correos reales

    // ---------- ayudas ----------

    private Usuario crearUsuario(String correo, String estado) {
        Usuario u = new Usuario();
        u.setCorreo(correo);
        u.setNombre("Test");
        u.setPasswordHash(encoder.encode(VIEJA));
        u.setEstado(estado);
        u.setRol("ESTUDIANTE");
        return usuarios.saveAndFlush(u);
    }

    private MockHttpServletResponse enviar(String ruta, String json, String ip) throws Exception {
        return mvc.perform(post(ruta).contentType(MediaType.APPLICATION_JSON).content(json)
                        .with(request -> { request.setRemoteAddr(ip); return request; }))
                .andReturn().getResponse();
    }

    private MockHttpServletResponse solicitar(String correo, String ip) throws Exception {
        return enviar("/auth/recuperacion/solicitar", "{\"correo\":\"" + correo + "\"}", ip);
    }

    private MockHttpServletResponse confirmar(String token, String clave, String ip) throws Exception {
        return enviar("/auth/recuperacion/confirmar",
                "{\"token\":\"" + token + "\",\"nuevaContrasena\":\"" + clave + "\"}", ip);
    }

    /** El correo se envía en otro hilo: se espera hasta 3 s y se extrae el token del texto. */
    private List<String> tokensEnviados(int cantidad) {
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, timeout(3000).times(cantidad)).send(captor.capture());
        Pattern p = Pattern.compile("es: (\\S+)");
        return captor.getAllValues().stream().map(m -> {
            Matcher mt = p.matcher(m.getText());
            assertTrue(mt.find(), "El correo debe contener el token");
            return mt.group(1);
        }).toList();
    }

    private void assertClaveSinCambios(String correo) {
        Usuario u = usuarios.findByCorreo(correo).orElseThrow();
        assertTrue(encoder.matches(VIEJA, u.getPasswordHash()), "La clave no debía cambiar");
    }

    // ---------- CA-04-01 ----------

    @Test
    void solicitarCorreoExistenteEnviaTokenYRespondeGenerico() throws Exception {
        crearUsuario("r1@uni.edu", "ACTIVA");
        MockHttpServletResponse r = solicitar("r1@uni.edu", "10.20.0.1");
        assertEquals(200, r.getStatus());
        assertEquals(GENERICO, r.getContentAsString(StandardCharsets.UTF_8));

        String token = tokensEnviados(1).get(0);
        assertTrue(token.length() >= 40, "El token debe ser largo y aleatorio, no de 6 dígitos");
        // se guardó el hash, no el token
        assertTrue(tokens.findByTokenHash(RegistroService.hash(token)).isPresent());
        assertTrue(tokens.findByTokenHash(token).isEmpty());
    }

    @Test
    void solicitarCorreoInexistenteDaLaMismaRespuestaYNoEnvia() throws Exception {
        MockHttpServletResponse r = solicitar("nadie@uni.edu", "10.20.0.2");
        assertEquals(200, r.getStatus());
        assertEquals(GENERICO, r.getContentAsString(StandardCharsets.UTF_8));
        verify(mailSender, after(500).never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void solicitarCuentaPendienteNoEnvia() throws Exception {
        crearUsuario("r3@uni.edu", "PENDIENTE");
        MockHttpServletResponse r = solicitar("r3@uni.edu", "10.20.0.3");
        assertEquals(200, r.getStatus());
        assertEquals(GENERICO, r.getContentAsString(StandardCharsets.UTF_8));
        verify(mailSender, after(500).never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void excesoDeSolicitudesSigueRespondiendoLoMismoPeroNoEnvia() throws Exception {
        crearUsuario("r4@uni.edu", "ACTIVA");
        for (int i = 0; i < 4; i++) {
            MockHttpServletResponse r = solicitar("r4@uni.edu", "10.20.0.4");
            assertEquals(200, r.getStatus());
            assertEquals(GENERICO, r.getContentAsString(StandardCharsets.UTF_8));
        }
        // límite: 3 por hora y por correo; la cuarta no envía nada
        verify(mailSender, after(1000).times(3)).send(any(SimpleMailMessage.class));
    }

    // ---------- CA-04-02 ----------

    @Test
    void tokenValidoCambiaClaveMarcaUsadoYRevocaSesiones() throws Exception {
        Usuario u = crearUsuario("r5@uni.edu", "ACTIVA");
        String idA = "a".repeat(64), idB = "b".repeat(64);
        sesiones.save(new Sesion(idA, u.getId(), Instant.now().plus(1, ChronoUnit.HOURS)));
        sesiones.save(new Sesion(idB, u.getId(), Instant.now().plus(1, ChronoUnit.HOURS)));

        solicitar("r5@uni.edu", "10.20.0.5");
        String token = tokensEnviados(1).get(0);

        assertEquals(200, confirmar(token, NUEVA, "10.20.0.5").getStatus());

        Usuario actualizado = usuarios.findByCorreo("r5@uni.edu").orElseThrow();
        assertTrue(encoder.matches(NUEVA, actualizado.getPasswordHash()), "La clave nueva debe funcionar");
        assertFalse(encoder.matches(VIEJA, actualizado.getPasswordHash()), "La clave anterior ya no sirve");
        assertTrue(tokens.findByTokenHash(RegistroService.hash(token)).orElseThrow().isUsado());
        assertTrue(sesiones.findById(idA).orElseThrow().isRevocada());
        assertTrue(sesiones.findById(idB).orElseThrow().isRevocada());
    }

    @Test
    void elTokenNoSirveDosVeces() throws Exception {
        crearUsuario("r6@uni.edu", "ACTIVA");
        solicitar("r6@uni.edu", "10.20.0.6");
        String token = tokensEnviados(1).get(0);
        assertEquals(200, confirmar(token, NUEVA, "10.20.0.6").getStatus());
        assertEquals(400, confirmar(token, "OtraClave789!", "10.20.0.6").getStatus());
    }

    @Test
    void nuevaSolicitudInvalidaElTokenAnterior() throws Exception {
        crearUsuario("r7@uni.edu", "ACTIVA");
        solicitar("r7@uni.edu", "10.20.0.7");
        solicitar("r7@uni.edu", "10.20.0.7");
        List<String> enviados = tokensEnviados(2);
        assertEquals(400, confirmar(enviados.get(0), NUEVA, "10.20.0.7").getStatus());
        assertClaveSinCambios("r7@uni.edu");
        assertEquals(200, confirmar(enviados.get(1), NUEVA, "10.20.0.7").getStatus());
    }

    @Test
    void contrasenaInvalidaDa422YNoConsumeElToken() throws Exception {
        crearUsuario("r8@uni.edu", "ACTIVA");
        solicitar("r8@uni.edu", "10.20.0.8");
        String token = tokensEnviados(1).get(0);

        assertEquals(422, confirmar(token, "corta", "10.20.0.8").getStatus());
        assertClaveSinCambios("r8@uni.edu");
        assertFalse(tokens.findByTokenHash(RegistroService.hash(token)).orElseThrow().isUsado());

        assertEquals(200, confirmar(token, NUEVA, "10.20.0.8").getStatus()); // aún sirve
    }

    // ---------- CA-04-03 ----------

    @Test
    void tokenExpiradoNoCambiaLaClave() throws Exception {
        Usuario u = crearUsuario("r9@uni.edu", "ACTIVA");
        Instant ahora = Instant.now();
        tokens.saveAndFlush(new TokenRecuperacion(u.getId(), RegistroService.hash("token-expirado-de-prueba"),
                ahora.minus(1, ChronoUnit.HOURS), ahora.minus(2, ChronoUnit.HOURS)));

        assertEquals(400, confirmar("token-expirado-de-prueba", NUEVA, "10.20.0.9").getStatus());
        assertClaveSinCambios("r9@uni.edu");
    }

    @Test
    void tokenYaUsadoNoCambiaLaClave() throws Exception {
        Usuario u = crearUsuario("r10@uni.edu", "ACTIVA");
        Instant ahora = Instant.now();
        TokenRecuperacion t = new TokenRecuperacion(u.getId(), RegistroService.hash("token-usado-de-prueba"),
                ahora.plus(1, ChronoUnit.HOURS), ahora.minus(1, ChronoUnit.MINUTES));
        t.setUsado(true);
        tokens.saveAndFlush(t);

        assertEquals(400, confirmar("token-usado-de-prueba", NUEVA, "10.20.0.10").getStatus());
        assertClaveSinCambios("r10@uni.edu");
    }

    @Test
    void tokenAlteradoNoCambiaLaClave() throws Exception {
        crearUsuario("r11@uni.edu", "ACTIVA");
        solicitar("r11@uni.edu", "10.20.0.11");
        String token = tokensEnviados(1).get(0);

        assertEquals(400, confirmar(token + "x", NUEVA, "10.20.0.11").getStatus());
        assertClaveSinCambios("r11@uni.edu");
    }

    @Test
    void excesoDeConfirmacionesFallidasDa429() throws Exception {
        for (int i = 0; i < 10; i++) {
            assertEquals(400, confirmar("token-falso-" + i, NUEVA, "10.20.0.12").getStatus());
        }
        assertEquals(429, confirmar("token-falso-final", NUEVA, "10.20.0.12").getStatus());
    }
}
