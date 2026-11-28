package com.campusmarket.demo.producto;

import com.campusmarket.demo.producto.ProductoService.ImagenDto;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

/** Endpoints de catálogo, creación, edición e imágenes de productos (S2-T11 / S2-T12). */
@RestController
@RequestMapping("/productos")
public class ProductoController {

    public record ProductoRequest(
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

    public record CambiarEstadoRequest(
            @NotBlank(message = "El estado es obligatorio")
            String estado
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
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ProductoResponse> crear(@Valid @RequestBody ProductoRequest body,
                                                  HttpServletRequest req) {
        Usuario vendedor = (Usuario) req.getAttribute(SesionInterceptor.ATTR_USUARIO);
        ProductoResponse creado = servicio.crear(
                vendedor, body.titulo(), body.descripcion(), body.categoriaId(), body.precio());
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @RequiereSesion
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductoResponse> publicar(@Valid @RequestPart("producto") ProductoRequest body,
                                                     @RequestPart("archivo") MultipartFile archivo,
                                                     HttpServletRequest req) {
        Usuario vendedor = (Usuario) req.getAttribute(SesionInterceptor.ATTR_USUARIO);
        ProductoResponse creado = servicio.crearConImagen(
                vendedor, body.titulo(), body.descripcion(), body.categoriaId(), body.precio(), archivo);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    /** Edita los datos de un producto existente verificando que el usuario sea su dueño (S2-T12). */
    @RequiereSesion
    @PutMapping("/{id}")
    public ProductoResponse editar(@PathVariable Long id,
                                   @Valid @RequestBody ProductoRequest body,
                                   HttpServletRequest req) {
        Usuario vendedor = (Usuario) req.getAttribute(SesionInterceptor.ATTR_USUARIO);
        return servicio.editar(id, vendedor, body.titulo(), body.descripcion(), body.categoriaId(), body.precio());
    }

    /** Cambia el estado de la publicación: DISPONIBLE, INACTIVO (retirado) o VENDIDO (S2-T12). */
    @RequiereSesion
    @PatchMapping("/{id}/estado")
    public ProductoResponse cambiarEstado(@PathVariable Long id,
                                          @Valid @RequestBody CambiarEstadoRequest body,
                                          HttpServletRequest req) {
        Usuario vendedor = (Usuario) req.getAttribute(SesionInterceptor.ATTR_USUARIO);
        return servicio.cambiarEstado(id, vendedor, body.estado());
    }

    /** Sube una imagen validada (JPG/PNG/WEBP, máx. 5 MB) asociada al producto del dueño (S2-T12). */
    @RequiereSesion
    @PostMapping(value = "/{id}/imagenes", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImagenDto> subirImagen(@PathVariable Long id,
                                                 @RequestPart("archivo") MultipartFile archivo,
                                                 HttpServletRequest req) {
        Usuario vendedor = (Usuario) req.getAttribute(SesionInterceptor.ATTR_USUARIO);
        ImagenDto creada = servicio.agregarImagen(id, vendedor, archivo);
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    /** Elimina una imagen perteneciente al producto del dueño (S2-T12). */
    @RequiereSesion
    @DeleteMapping("/{id}/imagenes/{imagenId}")
    public ResponseEntity<Void> eliminarImagen(@PathVariable Long id,
                                               @PathVariable Long imagenId,
                                               HttpServletRequest req) {
        Usuario vendedor = (Usuario) req.getAttribute(SesionInterceptor.ATTR_USUARIO);
        servicio.eliminarImagen(id, imagenId, vendedor);
        return ResponseEntity.noContent().build();
    }
}
