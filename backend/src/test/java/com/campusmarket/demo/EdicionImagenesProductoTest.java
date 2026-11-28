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

import com.jayway.jsonpath.JsonPath;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Stream;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
    void publicaProductoConFotoYRevierteSiElArchivoEsInvalido() throws Exception {
        Cookie cookie = login(vendedor.getCorreo());
        MockMultipartFile datos = new MockMultipartFile("producto", "", "application/json", """
                {"titulo":"Libro de cálculo","descripcion":"Como nuevo","categoriaId":%d,"precio":45000}
                """.formatted(categoria.getId()).getBytes(StandardCharsets.UTF_8));
        MockMultipartFile archivoInvalido = new MockMultipartFile("archivo", "foto.png", "image/png", new byte[]{1});

        mvc.perform(multipart("/productos").file(datos).file(archivoInvalido).cookie(cookie))
                .andExpect(status().isUnprocessableContent());

        assertEquals(1, productos.count());
        assertEquals(0, imagenes.count());

        String respuesta = mvc.perform(multipart("/productos").file(datos).file(crearPngValido("foto.png")).cookie(cookie))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("DISPONIBLE"))
                .andExpect(jsonPath("$.imagenes.length()").value(1))
                .andReturn().getResponse().getContentAsString();
        String url = JsonPath.read(respuesta, "$.imagenPrincipal");

        mvc.perform(get(url))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG));
    }

    @Test
    void dosSubidasEnParaleloSoloPuedenOcuparUnaPlaza() throws Exception {
        Cookie cookie = login(vendedor.getCorreo());
        String url = "/productos/" + producto.getId() + "/imagenes";
        for (int i = 0; i < 4; i++) {
            mvc.perform(multipart(url).file(crearPngValido("inicial.png")).cookie(cookie))
                    .andExpect(status().isCreated());
        }

        CountDownLatch preparadas = new CountDownLatch(2);
        CountDownLatch comenzar = new CountDownLatch(1);
        var ejecutor = Executors.newFixedThreadPool(2);
        try {
            Callable<Integer> subir = () -> {
                preparadas.countDown();
                assertTrue(comenzar.await(10, TimeUnit.SECONDS));
                return mvc.perform(multipart(url).file(crearPngValido("paralela.png")).cookie(cookie))
                        .andReturn().getResponse().getStatus();
            };
            var primera = ejecutor.submit(subir);
            var segunda = ejecutor.submit(subir);
            assertTrue(preparadas.await(10, TimeUnit.SECONDS));
            comenzar.countDown();
            var estados = Stream.of(primera.get(10, TimeUnit.SECONDS),
                    segunda.get(10, TimeUnit.SECONDS)).sorted().toList();
            assertEquals(List.of(201, 422), estados);
            assertEquals(5, imagenes.findByProductoIdOrderByIdAsc(producto.getId()).size());
        } finally {
            ejecutor.shutdownNow();
        }
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
