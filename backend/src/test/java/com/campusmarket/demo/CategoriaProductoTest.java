package com.campusmarket.demo;

import com.campusmarket.demo.catalogo.CatalogoException.AccesoDenegado;
import com.campusmarket.demo.catalogo.CatalogoException.RecursoNoEncontrado;
import com.campusmarket.demo.categoria.Categoria;
import com.campusmarket.demo.categoria.CategoriaRepository;
import com.campusmarket.demo.producto.Producto;
import com.campusmarket.demo.producto.ProductoRepository;
import com.campusmarket.demo.producto.ProductoService;
import com.campusmarket.demo.sesion.SesionRepository;
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

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pruebas de S2-T11: categorías (CA-06-01, CA-06-02), creación de productos (CA-07-01)
 * y función reutilizable de titularidad.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CategoriaProductoTest {

    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired SesionRepository sesiones;
    @Autowired CategoriaRepository categorias;
    @Autowired ProductoRepository productos;
    @Autowired ProductoService productoService;
    @Autowired PasswordEncoder encoder;

    private Usuario admin;
    private Usuario vendedor;
    private Usuario otroEstudiante;

    @BeforeEach
    void preparar() {
        productos.deleteAll();
        categorias.deleteAll();
        sesiones.deleteAll();
        usuarios.deleteAll();

        admin = crearUsuario("admin@uni.edu", "Admin", "ADMIN");
        vendedor = crearUsuario("vendedor@uni.edu", "Ana Vendedora", "ESTUDIANTE");
        otroEstudiante = crearUsuario("otro@uni.edu", "Luis Comprador", "ESTUDIANTE");
    }

    private Usuario crearUsuario(String correo, String nombre, String rol) {
        Usuario u = new Usuario();
        u.setCorreo(correo);
        u.setNombre(nombre);
        u.setEstado("ACTIVA");
        u.setRol(rol);
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

    @Test // CA-06-01: admin crea categoría única; duplicada se rechaza con 409
    void adminCreaCategoriaUnicaYRechazaDuplicada() throws Exception {
        Cookie cookieAdmin = login(admin.getCorreo());

        mvc.perform(post("/admin/categorias")
                        .cookie(cookieAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Electrónica\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Electrónica"))
                .andExpect(jsonPath("$.activa").value(true));

        mvc.perform(post("/admin/categorias")
                        .cookie(cookieAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"  electrónica  \"}"))
                .andExpect(status().isConflict());

        mvc.perform(get("/categorias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nombre").value("Electrónica"));
    }

    @Test // CA-06-02: estudiante recibe 403 al intentar crear o desactivar categorías
    void estudianteRecibe403EnAdminCategorias() throws Exception {
        Categoria libros = categorias.save(new Categoria("Libros"));
        Cookie cookieEst = login(vendedor.getCorreo());

        mvc.perform(post("/admin/categorias")
                        .cookie(cookieEst)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Ropa\"}"))
                .andExpect(status().isForbidden());

        mvc.perform(patch("/admin/categorias/" + libros.getId())
                        .cookie(cookieEst)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"activa\":false}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminDesactivaCategoriaYNoSaleEnPublico() throws Exception {
        Categoria libros = categorias.save(new Categoria("Libros"));
        Cookie cookieAdmin = login(admin.getCorreo());

        mvc.perform(patch("/admin/categorias/" + libros.getId())
                        .cookie(cookieAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"activa\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activa").value(false));

        mvc.perform(get("/categorias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mvc.perform(get("/admin/categorias").cookie(cookieAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test // CA-07-01: vendedor ACTIVO crea producto DISPONIBLE y aparece en catálogo
    void vendedorCreaProductoDisponibleYApareceEnCatalogo() throws Exception {
        Categoria libros = categorias.save(new Categoria("Libros"));
        Cookie cookieVendedor = login(vendedor.getCorreo());

        mvc.perform(post("/productos")
                        .cookie(cookieVendedor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "titulo": "Cálculo de Stewart",
                                  "descripcion": "Edición 8 en buen estado",
                                  "categoriaId": %d,
                                  "precio": 65000.00
                                }
                                """.formatted(libros.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.titulo").value("Cálculo de Stewart"))
                .andExpect(jsonPath("$.estado").value("DISPONIBLE"))
                .andExpect(jsonPath("$.vendedorId").value(vendedor.getId()))
                .andExpect(jsonPath("$.categoriaNombre").value("Libros"))
                .andExpect(jsonPath("$.precioFinal").value(65000.00));

        mvc.perform(get("/productos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].titulo").value("Cálculo de Stewart"));
    }

    @Test
    void crearProductoSinSesionOConCategoriaInactivaFalla() throws Exception {
        Categoria inactiva = new Categoria("Inactiva");
        inactiva.setActiva(false);
        inactiva = categorias.save(inactiva);

        mvc.perform(post("/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "titulo": "Objeto",
                                  "descripcion": "Desc",
                                  "categoriaId": %d,
                                  "precio": 1000
                                }
                                """.formatted(inactiva.getId())))
                .andExpect(status().isUnauthorized());

        Cookie cookieVendedor = login(vendedor.getCorreo());
        mvc.perform(post("/productos")
                        .cookie(cookieVendedor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "titulo": "Objeto",
                                  "descripcion": "Desc",
                                  "categoriaId": %d,
                                  "precio": 1000
                                }
                                """.formatted(inactiva.getId())))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void verificarTitularidadReutilizable() {
        Categoria libros = categorias.save(new Categoria("Libros"));
        Producto p = productos.save(new Producto(
                vendedor.getId(), libros.getId(), "Libro Java", "Nuevo", new BigDecimal("50000.00")));

        assertTrue(productoService.esPropietario(p.getId(), vendedor.getId()));
        assertFalse(productoService.esPropietario(p.getId(), otroEstudiante.getId()));
        assertFalse(productoService.esPropietario(99999L, vendedor.getId()));

        Producto verificado = productoService.verificarPropietario(p.getId(), vendedor);
        assertEquals(p.getId(), verificado.getId());

        assertThrows(AccesoDenegado.class,
                () -> productoService.verificarPropietario(p.getId(), otroEstudiante));
        assertThrows(RecursoNoEncontrado.class,
                () -> productoService.verificarPropietario(99999L, vendedor));
    }
}
