package com.campusmarket.demo;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pruebas de CA-01-01 y CA-01-02 (endpoint de E07, S1-T09).
 * Requiere en application-test.properties: campusmarket.correo.dominio=uni.edu
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional // cada prueba hace rollback: no quedan usuarios entre tests
class AuthRegistroContratoTest {

    @Autowired MockMvc mvc;

    @MockitoBean JavaMailSender mailSender; // no se envían correos reales

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
        mvc.perform(post("/auth/registro").contentType(MediaType.APPLICATION_JSON).content(JSON))
                .andExpect(status().isAccepted());
        mvc.perform(post("/auth/registro").contentType(MediaType.APPLICATION_JSON).content(JSON))
                .andExpect(status().isConflict());
    }
}