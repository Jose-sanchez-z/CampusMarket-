package com.campusmarket.demo;

import com.campusmarket.demo.categoria.Categoria;
import com.campusmarket.demo.categoria.CategoriaRepository;
import com.campusmarket.demo.producto.ImagenProductoRepository;
import com.campusmarket.demo.producto.Producto;
import com.campusmarket.demo.producto.ProductoRepository;
import com.campusmarket.demo.sesion.SesionRepository;
import com.campusmarket.demo.sesion.Usuario;
import com.campusmarket.demo.sesion.UsuarioRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Pruebas de S2-T12: edición de productos, cambio de estado y subida segura de imágenes. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EdicionImagenesProductoTest {

    @TempDir
    static Path tempUploads;

    @DynamicPropertySource
    static void configurarDirectorioTemporal(DynamicPropertyRegistry registry) {
        registry.add("app.imagenes.directorio", () -> tempUploads.toString());
    }

    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired SesionRepository sesiones;
    @Autowired CategoriaRepository categorias;
    @Autowired ProductoRepository productos;
    @Autowired ImagenProductoRepository imagenes;
    @Autowired PasswordEncoder encoder;

    private Usuario vendedor;
    private Usuario otroUsuario;
    private Categoria categoria;
    private Producto producto;

    @BeforeEach
    void preparar() {
        imagenes.deleteAll();
        productos.deleteAll();
        categorias.deleteAll();
        sesiones.deleteAll();
        usuarios.deleteAll();

        vendedor = crearUsuario("dueno@uni.edu", "Carlos Vendedor");
        otroUsuario = crearUsuario("intruso@uni.edu", "Pedro Intruso");
        categoria = categorias.save(new Categoria("Tecnología"));
        producto = productos.save(new Producto(
                vendedor.getId(),
                categoria.getId(),
                "Portátil Lenovo",
                "8GB RAM, buen estado",
                new BigDecimal("1200000.00")
        ));
    }

    private Usuario crearUsuario(String correo, String nombre) {
        Usuario u = new Usuario();
        u.setCorreo(correo);
        u.setNombre(nombre);
        u.setEstado("ACTIVA");
        u.setRol("ESTUDIANTE");
        u.setPasswordHash(encoder.encode("Clave123!"));
        return usuarios.save(u);
    }

    private Cookie login(String correo) throws Exception {
        return mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"" + correo + "\",\"password\":\"Clave123!\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getCookie("CM_SESION");
    }

    private MockMultipartFile crearPngValido(String nombreArchivo) {
        byte[] firmaPng = new byte[]{
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
                0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52
        };
        return new MockMultipartFile("archivo", nombreArchivo, "image/png", firmaPng);
    }

    @Test
    void duenoEditaProductoYRechazaTercerosCon403() throws Exception {
        Cookie cookieDueno = login(vendedor.getCorreo());
        Cookie cookieIntruso = login(otroUsuario.getCorreo());

        String bodyEdicion = """
                {
                  "titulo": "Portátil Lenovo ThinkPad",
                  "descripcion": "16GB RAM y SSD 512GB",
                  "categoriaId": %d,
                  "precio": 1350000.00
                }
                """.formatted(categoria.getId());

        mvc.perform(put("/productos/" + producto.getId())
                        .cookie(cookieIntruso)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyEdicion))
                .andExpect(status().isForbidden());

        mvc.perform(put("/productos/" + producto.getId())
                        .cookie(cookieDueno)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyEdicion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Portátil Lenovo ThinkPad"))
                .andExpect(jsonPath("$.precio").value(1350000.00));
    }

    @Test
    void duenoRetiraProductoYNoSaleEnCatalogoPublico() throws Exception {
        Cookie cookieDueno = login(vendedor.getCorreo());

        mvc.perform(patch("/productos/" + producto.getId() + "/estado")
                        .cookie(cookieDueno)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"INACTIVO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("INACTIVO"));

        mvc.perform(get("/productos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mvc.perform(get("/productos/" + producto.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void subeImagenValidaYRechazaArchivoFalso() throws Exception {
        Cookie cookieDueno = login(vendedor.getCorreo());

        MockMultipartFile falso = new MockMultipartFile(
                "archivo", "virus.jpg", "image/jpeg", "texto plano falso".getBytes());

        mvc.perform(multipart("/productos/" + producto.getId() + "/imagenes")
                        .file(falso)
                        .cookie(cookieDueno))
                .andExpect(status().isUnprocessableContent());

        mvc.perform(multipart("/productos/" + producto.getId() + "/imagenes")
                        .file(crearPngValido("../../hack.png"))
                        .cookie(cookieDueno))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipoMime").value("image/png"))
                .andExpect(jsonPath("$.url").isNotEmpty());

        assertEquals(1, imagenes.findByProductoIdOrderByIdAsc(producto.getId()).size());
    }

    @Test
    void respetaLimiteDe5ImagenesYPermiteEliminar() throws Exception {
        Cookie cookieDueno = login(vendedor.getCorreo());

        for (int i = 1; i <= 5; i++) {
            mvc.perform(multipart("/productos/" + producto.getId() + "/imagenes")
                            .file(crearPngValido("foto" + i + ".png"))
                            .cookie(cookieDueno))
                    .andExpect(status().isCreated());
        }

        mvc.perform(multipart("/productos/" + producto.getId() + "/imagenes")
                        .file(crearPngValido("foto6.png"))
                        .cookie(cookieDueno))
                .andExpect(status().isUnprocessableContent());

        Long primeraId = imagenes.findByProductoIdOrderByIdAsc(producto.getId()).get(0).getId();

        mvc.perform(delete("/productos/" + producto.getId() + "/imagenes/" + primeraId)
                        .cookie(cookieDueno))
                .andExpect(status().isNoContent());

        assertEquals(4, imagenes.findByProductoIdOrderByIdAsc(producto.getId()).size());
    }
}
