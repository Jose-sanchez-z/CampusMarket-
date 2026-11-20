package com.campusmarket.demo;

import com.campusmarket.demo.auth.RegistroService;
import com.campusmarket.demo.auth.TokenVerificacion;
import com.campusmarket.demo.auth.TokenVerificacionRepository;
import com.campusmarket.demo.sesion.Usuario;
import com.campusmarket.demo.sesion.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** Pruebas de CA-02-01, CA-02-02 y de los límites de intentos/reenvío (S1-T10, E07). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthVerificacionContratoTest {

    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired TokenVerificacionRepository tokens;
    @MockitoBean JavaMailSender mailSender; // no se envían correos reales

    private Usuario crearPendiente(String correo, String codigo, Instant vence, boolean usado) {
        Usuario u = new Usuario();
        u.setCorreo(correo);
        u.setNombre("Test");
        u.setPasswordHash("hash-de-prueba");
        u.setEstado("PENDIENTE");
        u.setRol("ESTUDIANTE");
        u = usuarios.saveAndFlush(u);
        TokenVerificacion t = new TokenVerificacion(u.getId(), RegistroService.hash(codigo), vence);
        if (usado) t.marcarUsado();
        tokens.saveAndFlush(t);
        return u;
    }

    private static Instant enUnaHora() { return Instant.now().plus(1, ChronoUnit.HOURS); }
    private static Instant haceUnaHora() { return Instant.now().minus(1, ChronoUnit.HOURS); }

    private int verificar(String correo, String token) throws Exception {
        return mvc.perform(post("/auth/verificar").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"" + correo + "\",\"token\":\"" + token + "\"}"))
                .andReturn().getResponse().getStatus();
    }

    private int reenviar(String correo) throws Exception {
        return mvc.perform(post("/auth/reenviartoken").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"" + correo + "\"}"))
                .andReturn().getResponse().getStatus();
    }

    @Test // CA-02-01
    void tokenCorrectoActivaCuentaYQuedaUsado() throws Exception {
        Usuario u = crearPendiente("a1@uni.edu", "123456", enUnaHora(), false);
        assertEquals(200, verificar("a1@uni.edu", "123456"));
        assertEquals("ACTIVA", usuarios.findByCorreo("a1@uni.edu").orElseThrow().getEstado());
        assertTrue(tokens.findFirstByUsuarioIdOrderByIdDesc(u.getId()).orElseThrow().isUsado());
    }

    @Test // CA-02-02
    void tokenVencidoNoActiva() throws Exception {
        crearPendiente("a2@uni.edu", "123456", haceUnaHora(), false);
        assertEquals(422, verificar("a2@uni.edu", "123456"));
        assertEquals("PENDIENTE", usuarios.findByCorreo("a2@uni.edu").orElseThrow().getEstado());
    }

    @Test // CA-02-02
    void tokenUsadoNoActiva() throws Exception {
        crearPendiente("a3@uni.edu", "123456", enUnaHora(), true);
        assertEquals(422, verificar("a3@uni.edu", "123456"));
        assertEquals("PENDIENTE", usuarios.findByCorreo("a3@uni.edu").orElseThrow().getEstado());
    }

    @Test
    void tokenIncorrectoNoActiva() throws Exception {
        crearPendiente("a4@uni.edu", "123456", enUnaHora(), false);
        assertEquals(422, verificar("a4@uni.edu", "654321"));
        assertEquals("PENDIENTE", usuarios.findByCorreo("a4@uni.edu").orElseThrow().getEstado());
    }

    @Test // CA-02-01: el token queda inutilizado
    void elMismoTokenNoSirveDosVeces() throws Exception {
        crearPendiente("a5@uni.edu", "123456", enUnaHora(), false);
        assertEquals(200, verificar("a5@uni.edu", "123456"));
        assertEquals(422, verificar("a5@uni.edu", "123456"));
    }

    @Test
    void reenvioInvalidaTokenAnteriorYGeneraUnoNuevo() throws Exception {
        Usuario u = crearPendiente("a6@uni.edu", "111111", enUnaHora(), false);
        assertEquals(200, reenviar("a6@uni.edu"));
        // el código viejo ya no sirve
        assertEquals(422, verificar("a6@uni.edu", "111111"));
        // hay un token nuevo sin usar
        assertEquals(1, tokens.findByUsuarioIdAndUsadoFalse(u.getId()).size());
    }

    @Test
    void reenvioDeCorreoInexistenteResponde200() throws Exception {
        assertEquals(200, reenviar("noexiste@uni.edu"));
    }

    @Test
    void excesoDeReenviosDa429() throws Exception {
        crearPendiente("a7@uni.edu", "123456", enUnaHora(), false);
        assertEquals(200, reenviar("a7@uni.edu"));
        assertEquals(200, reenviar("a7@uni.edu"));
        assertEquals(200, reenviar("a7@uni.edu"));
        assertEquals(429, reenviar("a7@uni.edu"));
    }

    @Test
    void excesoDeIntentosFallidosDa429() throws Exception {
        crearPendiente("a8@uni.edu", "123456", enUnaHora(), false);
        for (int i = 0; i < 5; i++) {
            assertEquals(422, verificar("a8@uni.edu", "000000"));
        }
        // bloqueado: incluso con el código correcto
        assertEquals(429, verificar("a8@uni.edu", "123456"));
    }
}
