package com.campusmarket.demo;

import com.campusmarket.demo.sesion.*;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.http.MediaType;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthSesionTest {

    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired PasswordEncoder encoder;

    @BeforeEach
    void datos() {
        usuarios.deleteAll();
        crear("activa@uni.edu", "ACTIVA");
        crear("pendiente@uni.edu", "PENDIENTE");
    }

    private void crear(String correo, String estado) {
        Usuario u = new Usuario();
        u.setCorreo(correo); u.setNombre("Test"); u.setEstado(estado); u.setRol("ESTUDIANTE");
        u.setPasswordHash(encoder.encode("Clave123!"));
        usuarios.save(u);
    }

    private MvcResult login(String correo, String pass) throws Exception {
        return mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\":\"" + correo + "\",\"password\":\"" + pass + "\"}")).andReturn();
    }

    @Test // CA-03-01
    void loginActivaCreaCookieHttpOnly() throws Exception {
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"activa@uni.edu\",\"password\":\"Clave123!\"}"))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("CM_SESION"))
                .andExpect(cookie().httpOnly("CM_SESION", true))
                .andExpect(jsonPath("$.rol").value("ESTUDIANTE"));
    }

    @Test
    void credencialesIncorrectasDan401() throws Exception {
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"activa@uni.edu\",\"password\":\"mala\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(cookie().doesNotExist("CM_SESION"));
    }

    @Test // CA-03-02
    void cuentaPendienteEsDenegada() throws Exception {
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"pendiente@uni.edu\",\"password\":\"Clave123!\"}"))
                .andExpect(status().isForbidden())
                .andExpect(cookie().doesNotExist("CM_SESION"));
    }

    @Test
    void yoSinSesionDa401() throws Exception {
        mvc.perform(get("/auth/yo")).andExpect(status().isUnauthorized());
    }

    @Test // CA-03-03
    void logoutRevocaLaSesion() throws Exception {
        Cookie c = login("activa@uni.edu", "Clave123!").getResponse().getCookie("CM_SESION");

        mvc.perform(get("/auth/yo").cookie(c)).andExpect(status().isOk())
                .andExpect(jsonPath("$.correo").value("activa@uni.edu"));

        mvc.perform(post("/auth/logout").cookie(c)).andExpect(status().isOk());

        // la misma cookie ya no sirve
        mvc.perform(get("/auth/yo").cookie(c)).andExpect(status().isUnauthorized());
    }
}
