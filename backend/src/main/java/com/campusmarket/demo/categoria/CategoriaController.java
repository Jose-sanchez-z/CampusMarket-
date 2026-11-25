package com.campusmarket.demo.categoria;

import com.campusmarket.demo.sesion.RequiereSesion;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Endpoints de categorías: consulta pública (GET) y administración solo ADMIN (POST/PATCH). */
@RestController
public class CategoriaController {

    public record CategoriaResponse(Long id, String nombre, boolean activa) {
        static CategoriaResponse desde(Categoria c) {
            return new CategoriaResponse(c.getId(), c.getNombre(), c.isActiva());
        }
    }

    public record CrearCategoriaRequest(
            @NotBlank(message = "El nombre es obligatorio")
            @Size(max = 80, message = "Máximo 80 caracteres")
            String nombre
    ) {}

    public record ActualizarCategoriaRequest(Boolean activa, String nombre) {}

    private final CategoriaService servicio;

    public CategoriaController(CategoriaService servicio) {
        this.servicio = servicio;
    }

    /** Lista las categorías activas disponibles para publicar y filtrar. */
    @GetMapping("/categorias")
    public List<CategoriaResponse> listarPublicas() {
        return servicio.listarActivas().stream().map(CategoriaResponse::desde).toList();
    }

    /** Lista todas las categorías (activas e inactivas) para el panel de administración. */
    @RequiereSesion(rol = "ADMIN")
    @GetMapping("/admin/categorias")
    public List<CategoriaResponse> listarAdmin() {
        return servicio.listarTodas().stream().map(CategoriaResponse::desde).toList();
    }

    /** Crea una categoría única (CA-06-01; estudiantes reciben 403 por CA-06-02). */
    @RequiereSesion(rol = "ADMIN")
    @PostMapping("/admin/categorias")
    public ResponseEntity<CategoriaResponse> crear(@Valid @RequestBody CrearCategoriaRequest body) {
        Categoria creada = servicio.crear(body.nombre());
        return ResponseEntity.status(HttpStatus.CREATED).body(CategoriaResponse.desde(creada));
    }

    /** Activa, desactiva o renombra una categoría existente (solo ADMIN). */
    @RequiereSesion(rol = "ADMIN")
    @PatchMapping("/admin/categorias/{id}")
    public CategoriaResponse actualizar(@PathVariable Long id, @RequestBody ActualizarCategoriaRequest body) {
        Categoria actualizada = servicio.actualizar(id, body.activa(), body.nombre());
        return CategoriaResponse.desde(actualizada);
    }
}
