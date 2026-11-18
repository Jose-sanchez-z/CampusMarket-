package com.campusmarket.demo;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pruebas de CA-01-01 y CA-01-02 (endpoint de E07, S1-T09).
 * Quita @Disabled cuando el PR de E07 esté en develop. Si su servicio de correo
 * envía mails reales, añade un @MockBean del sender. Ajusta el dominio institucional.
 */
@Disabled("Activar cuando S1-T09 (E07) esté integrada")
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthRegistroContratoTest {

    @Autowired MockMvc mvc;

    private static final String JSON =
            "{\"nombre\":\"Ana\",\"correo\":\"ana@uni.edu\",\"password\":\"Clave123!\"}";

    @Test // CA-01-01
    void registroNuevoDa202() throws Exception {
        mvc.perform(post("/auth/registro").contentType(MediaType.APPLICATION_JSON).content(JSON))
                .andExpect(status().isAccepted())
                .andExpect(cookie().doesNotExist("CM_SESION")); // no inicia sesión
    }

    @Test // CA-01-02
    void registroDuplicadoDa409() throws Exception {
        mvc.perform(post("/auth/registro").contentType(MediaType.APPLICATION_JSON).content(JSON));
        mvc.perform(post("/auth/registro").contentType(MediaType.APPLICATION_JSON).content(JSON))
                .andExpect(status().isConflict());
    }
}
