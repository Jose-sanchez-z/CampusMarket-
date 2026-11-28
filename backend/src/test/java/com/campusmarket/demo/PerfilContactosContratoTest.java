package com.campusmarket.demo;

import com.campusmarket.demo.perfil.Contacto;
import com.campusmarket.demo.perfil.ContactoRepository;
import com.campusmarket.demo.perfil.PerfilService;
import com.campusmarket.demo.sesion.Usuario;
import com.campusmarket.demo.sesion.UsuarioRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** S2-T10 · HU-05: perfil y privacidad de contactos (CA-05-01, CA-05-02). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PerfilContactosContratoTest {

    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired ContactoRepository contactos;
    @Autowired PerfilService perfil;
    @Autowired PasswordEncoder encoder;

    Long idAna;
    Long idLuis;
    Cookie cookieAna;
    Cookie cookieLuis;

    @BeforeEach
    void datos() throws Exception {
        contactos.deleteAll();
        usuarios.deleteAll();
        idAna = crear("ana@uni.edu", "Ana");
        idLuis = crear("luis@uni.edu", "Luis");
        cookieAna = login("ana@uni.edu");
        cookieLuis = login("luis@uni.edu");
    }

    private Long crear(String correo, String nombre) {
        Usuario u = new Usuario();
        u.setCorreo(correo);
        u.setNombre(nombre);
        u.setEstado("ACTIVA");
        u.setRol("ESTUDIANTE");
        u.setPasswordHash(encoder.encode("Clave123!"));
        return usuarios.save(u).getId();
    }

    private Cookie login(String correo) throws Exception {
        return mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"" + correo + "\",\"password\":\"Clave123!\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getCookie("CM_SESION");
    }

    private Long contacto(Long usuarioId, String tipo, String valor, boolean publico) {
        return contactos.save(new Contacto(usuarioId, tipo, valor, publico)).getId();
    }

    // ------------------------------------------------------------ sesión

    @Test
    void sinSesionTodosLosEndpointsDan401() throws Exception {
        mvc.perform(get("/usuarios/yo")).andExpect(status().isUnauthorized());
        mvc.perform(patch("/usuarios/yo").contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"X\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/usuarios/yo/contactos").contentType(MediaType.APPLICATION_JSON)
                .content("{\"tipo\":\"OTRO\",\"valor\":\"x\"}")).andExpect(status().isUnauthorized());
        mvc.perform(delete("/usuarios/yo/contactos/1")).andExpect(status().isUnauthorized());
        mvc.perform(patch("/usuarios/yo/contactos/1").contentType(MediaType.APPLICATION_JSON)
                .content("{\"esPublico\":true}")).andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------ GET /usuarios/yo

    @Test // CA-05-01
    void getDevuelveTodosLosContactosDelDuenoIncluidosPrivados() throws Exception {
        contacto(idAna, "WHATSAPP", "573001234567", true);
        contacto(idAna, "CORREO", "privado@uni.edu", false);

        mvc.perform(get("/usuarios/yo").cookie(cookieAna))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(idAna))
                .andExpect(jsonPath("$.nombre").value("Ana"))
                .andExpect(jsonPath("$.correo").value("ana@uni.edu"))
                .andExpect(jsonPath("$.contactos.length()").value(2))
                .andExpect(jsonPath("$.contactos[0].tipo").value("WHATSAPP"))
                .andExpect(jsonPath("$.contactos[0].esPublico").value(true))
                .andExpect(jsonPath("$.contactos[1].valor").value("privado@uni.edu"))
                .andExpect(jsonPath("$.contactos[1].esPublico").value(false));
    }

    // ------------------------------------------------------------ PATCH /usuarios/yo

    @Test // CA-05-01
    void patchCambiaElNombreYSePersiste() throws Exception {
        mvc.perform(patch("/usuarios/yo").cookie(cookieAna).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"  Ana María  \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Ana María"));

        assertEquals("Ana María", usuarios.findById(idAna).orElseThrow().getNombre());
        mvc.perform(get("/usuarios/yo").cookie(cookieAna)).andExpect(jsonPath("$.nombre").value("Ana María"));
    }

    @Test
    void patchRechazaRolEstadoCorreoYOtrosCampos() throws Exception {
        List<String> cuerpos = List.of(
                "{\"rol\":\"ADMIN\"}",
                "{\"estado\":\"PENDIENTE\"}",
                "{\"correo\":\"otro@uni.edu\"}",
                "{\"id\":999}",
                "{\"nombre\":\"Hack\",\"rol\":\"ADMIN\"}");
        for (String cuerpo : cuerpos) {
            mvc.perform(patch("/usuarios/yo").cookie(cookieAna).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                    .andExpect(status().isUnprocessableEntity());
        }
        Usuario ana = usuarios.findById(idAna).orElseThrow();
        assertEquals("Ana", ana.getNombre());          // ni siquiera el nombre válido del último cuerpo se aplicó
        assertEquals("ESTUDIANTE", ana.getRol());
        assertEquals("ACTIVA", ana.getEstado());
        assertEquals("ana@uni.edu", ana.getCorreo());
    }

    @Test
    void patchConNombreInvalidoDa422() throws Exception {
        for (String cuerpo : List.of("{\"nombre\":\"   \"}", "{\"nombre\":5}", "{}", "{\"nombre\":\"" + "a".repeat(121) + "\"}")) {
            mvc.perform(patch("/usuarios/yo").cookie(cookieAna).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                    .andExpect(status().isUnprocessableEntity());
        }
        assertEquals("Ana", usuarios.findById(idAna).orElseThrow().getNombre());
    }

    // ------------------------------------------------------------ POST /usuarios/yo/contactos

    @Test // CA-05-01
    void postCreaContactoPrivadoPorDefectoYApareceEnElPerfil() throws Exception {
        mvc.perform(post("/usuarios/yo/contactos").cookie(cookieAna).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"WHATSAPP\",\"valor\":\"573001234567\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("WHATSAPP"))
                .andExpect(jsonPath("$.valor").value("573001234567"))
                .andExpect(jsonPath("$.esPublico").value(false));

        mvc.perform(get("/usuarios/yo").cookie(cookieAna))
                .andExpect(jsonPath("$.contactos.length()").value(1))
                .andExpect(jsonPath("$.contactos[0].valor").value("573001234567"));
    }

    @Test
    void postPuedeCrearContactoPublico() throws Exception {
        mvc.perform(post("/usuarios/yo/contactos").cookie(cookieAna).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"instagram\",\"valor\":\"@ana.uni\",\"esPublico\":true}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("INSTAGRAM"))
                .andExpect(jsonPath("$.esPublico").value(true));
    }

    @Test
    void postAsociaElContactoAlUsuarioDeLaSesionYNuncaAlDelCuerpo() throws Exception {
        mvc.perform(post("/usuarios/yo/contactos").cookie(cookieAna).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuarioId\":" + idLuis + ",\"tipo\":\"TELEFONO\",\"valor\":\"3001234567\"}"))
                .andExpect(status().isCreated());

        assertEquals(1, contactos.findByUsuarioIdOrderByIdAsc(idAna).size());
        assertEquals(0, contactos.findByUsuarioIdOrderByIdAsc(idLuis).size());
    }

    @Test
    void postConFormatoOTipoInvalidoDa422YNoGuardaNada() throws Exception {
        List<String> cuerpos = List.of(
                "{\"tipo\":\"WHATSAPP\",\"valor\":\"abc\"}",
                "{\"tipo\":\"WHATSAPP\",\"valor\":\"+573001234567\"}",   // WhatsApp: solo dígitos
                "{\"tipo\":\"WHATSAPP\",\"valor\":\"12345\"}",            // sin indicativo / muy corto
                "{\"tipo\":\"TELEFONO\",\"valor\":\"12-34\"}",
                "{\"tipo\":\"CORREO\",\"valor\":\"no-es-correo\"}",
                "{\"tipo\":\"FAX\",\"valor\":\"573001234567\"}",
                "{\"tipo\":\"OTRO\",\"valor\":\"   \"}",
                "{\"valor\":\"573001234567\"}",
                "{\"tipo\":\"WHATSAPP\"}");
        for (String cuerpo : cuerpos) {
            mvc.perform(post("/usuarios/yo/contactos").cookie(cookieAna).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                    .andExpect(status().isUnprocessableEntity());
        }
        assertEquals(0, contactos.count());
    }

    // ------------------------------------------------------------ DELETE /usuarios/yo/contactos/{id}

    @Test
    void deleteEliminaUnContactoPropio() throws Exception {
        Long id = contacto(idAna, "OTRO", "mi sitio", false);

        mvc.perform(delete("/usuarios/yo/contactos/" + id).cookie(cookieAna)).andExpect(status().isNoContent());

        assertTrue(contactos.findById(id).isEmpty());
    }

    @Test
    void deleteDeUnContactoAjenoDa404YNoLoBorra() throws Exception {
        Long idDeAna = contacto(idAna, "OTRO", "de ana", true);

        mvc.perform(delete("/usuarios/yo/contactos/" + idDeAna).cookie(cookieLuis)).andExpect(status().isNotFound());
        mvc.perform(delete("/usuarios/yo/contactos/999999").cookie(cookieLuis)).andExpect(status().isNotFound());

        assertTrue(contactos.findById(idDeAna).isPresent());
    }

    // ------------------------------------------------------------ PATCH /usuarios/yo/contactos/{id}

    @Test
    void patchVisibilidadCambiaSoloEnContactosPropios() throws Exception {
        Long id = contacto(idAna, "WHATSAPP", "573001234567", false);

        mvc.perform(patch("/usuarios/yo/contactos/" + id).cookie(cookieAna).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"esPublico\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.esPublico").value(true));
        assertTrue(contactos.findById(id).orElseThrow().isEsPublico());

        // Luis intenta volver privado el contacto de Ana: 404 y no cambia
        mvc.perform(patch("/usuarios/yo/contactos/" + id).cookie(cookieLuis).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"esPublico\":false}"))
                .andExpect(status().isNotFound());
        assertTrue(contactos.findById(id).orElseThrow().isEsPublico());
    }

    @Test
    void patchVisibilidadSinEsPublicoDa422() throws Exception {
        Long id = contacto(idAna, "OTRO", "x", false);
        mvc.perform(patch("/usuarios/yo/contactos/" + id).cookie(cookieAna).contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnprocessableEntity());
    }

    // ------------------------------------------------------------ privacidad (CA-05-02)

    @Test // CA-05-02
    void otroUsuarioNoVeElContactoPrivado() throws Exception {
        contacto(idAna, "WHATSAPP", "573001234567", true);
        contacto(idAna, "CORREO", "secreto@uni.edu", false);

        // Consulta como OTRO usuario: su perfil no trae ningún contacto de Ana
        mvc.perform(get("/usuarios/yo").cookie(cookieLuis))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(idLuis))
                .andExpect(jsonPath("$.contactos.length()").value(0))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("secreto@uni.edu"))));

        // La función que usará el Sprint 3 (detalle de producto): solo contactos públicos
        List<PerfilService.ContactoPublico> publicos = perfil.contactosPublicos(idAna);
        assertEquals(1, publicos.size());
        assertEquals("573001234567", publicos.get(0).valor());
        assertTrue(publicos.stream().noneMatch(c -> c.valor().equals("secreto@uni.edu")));
    }

    @Test // CA-05-02
    void alVolverPrivadoUnContactoDejaDeSerPublico() throws Exception {
        Long id = contacto(idAna, "WHATSAPP", "573001234567", true);
        assertEquals(1, perfil.contactosPublicos(idAna).size());

        mvc.perform(patch("/usuarios/yo/contactos/" + id).cookie(cookieAna).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"esPublico\":false}"))
                .andExpect(status().isOk());

        assertEquals(0, perfil.contactosPublicos(idAna).size());
    }
}
