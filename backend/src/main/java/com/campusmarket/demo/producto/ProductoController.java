package com.campusmarket.demo.producto;

import com.campusmarket.demo.producto.ProductoService.ProductoResponse;
import com.campusmarket.demo.sesion.RequiereSesion;
import com.campusmarket.demo.sesion.SesionInterceptor;
import com.campusmarket.demo.sesion.Usuario;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/** Endpoints de catálogo simple y creación de productos (S2-T11). */
@RestController
@RequestMapping("/productos")
public class ProductoController {

    public record CrearProductoRequest(
            @NotBlank(message = "El título es obligatorio")
            @Size(max = 150, message = "El título no puede superar 150 caracteres")
            String titulo,
            @NotBlank(message = "La descripción es obligatoria")
            String descripcion,
            @NotNull(message = "La categoría es obligatoria")
            Long categoriaId,
            @NotNull(message = "El precio es obligatorio")
            @DecimalMin(value = "0.01", message = "El precio debe ser mayor que cero")
            BigDecimal precio
    ) {}

    private final ProductoService servicio;

    public ProductoController(ProductoService servicio) {
        this.servicio = servicio;
    }

    /** Catálogo simple (Sprint 2): lista solo productos DISPONIBLE ordenados de más reciente a más antiguo. */
    @GetMapping
    public List<ProductoResponse> listarCatalogo() {
        return servicio.listarDisponibles();
    }

    /** Publicaciones del vendedor autenticado (incluye DISPONIBLE, INACTIVO y VENDIDO). */
    @RequiereSesion
    @GetMapping("/mios")
    public List<ProductoResponse> listarMios(HttpServletRequest req) {
        Usuario vendedor = (Usuario) req.getAttribute(SesionInterceptor.ATTR_USUARIO);
        return servicio.listarMios(vendedor);
    }

    /** Consulta básica de un producto por id. */
    @GetMapping("/{id}")
    public ProductoResponse detalle(@PathVariable Long id, HttpServletRequest req) {
        Usuario usuarioActual = (Usuario) req.getAttribute(SesionInterceptor.ATTR_USUARIO);
        return servicio.obtenerPorId(id, usuarioActual);
    }

    /** Crea un producto en estado DISPONIBLE asociado al usuario de la sesión (CA-07-01). */
    @RequiereSesion
    @PostMapping
    public ResponseEntity<ProductoResponse> crear(@Valid @RequestBody CrearProductoRequest body,
                                                  HttpServletRequest req) {
        Usuario vendedor = (Usuario) req.getAttribute(SesionInterceptor.ATTR_USUARIO);
        ProductoResponse creado = servicio.crear(
                vendedor, body.titulo(), body.descripcion(), body.categoriaId(), body.precio());
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }
}
