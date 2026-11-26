package com.campusmarket.demo.categoria;

import com.campusmarket.demo.catalogo.CatalogoException.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Lógica de negocio para consulta y administración de categorías (HU-06). */
@Service
public class CategoriaService {

    private final CategoriaRepository categorias;

    public CategoriaService(CategoriaRepository categorias) {
        this.categorias = categorias;
    }

    @Transactional(readOnly = true)
    public List<Categoria> listarActivas() {
        return categorias.findByActivaTrueOrderByNombreAsc();
    }

    @Transactional(readOnly = true)
    public List<Categoria> listarTodas() {
        return categorias.findAllByOrderByNombreAsc();
    }

    @Transactional(readOnly = true)
    public Categoria obtenerActiva(Long id) {
        Categoria cat = categorias.findById(id)
                .orElseThrow(() -> new RecursoNoEncontrado("La categoría no existe"));
        if (!cat.isActiva()) {
            throw new ReglaNegocio("La categoría seleccionada está inactiva");
        }
        return cat;
    }

    @Transactional
    public Categoria crear(String nombre) {
        String limpio = normalizarNombre(nombre);
        if (categorias.existsByNombreIgnoreCase(limpio)) {
            throw new Conflicto("Ya existe una categoría con ese nombre");
        }
        return categorias.save(new Categoria(limpio));
    }

    @Transactional
    public Categoria actualizar(Long id, Boolean activa, String nombre) {
        if (activa == null && (nombre == null || nombre.isBlank())) {
            throw new IllegalArgumentException("Debes indicar al menos el estado 'activa' o el nuevo 'nombre'");
        }
        Categoria cat = categorias.findById(id)
                .orElseThrow(() -> new RecursoNoEncontrado("Categoría no encontrada"));

        if (nombre != null && !nombre.isBlank()) {
            String limpio = normalizarNombre(nombre);
            if (categorias.existsByNombreIgnoreCaseAndIdNot(limpio, id)) {
                throw new Conflicto("Ya existe otra categoría con ese nombre");
            }
            cat.setNombre(limpio);
        }
        if (activa != null) {
            cat.setActiva(activa);
        }
        return categorias.save(cat);
    }

    private static String normalizarNombre(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre de la categoría es obligatorio");
        }
        String limpio = nombre.trim().replaceAll("\\s+", " ");
        if (limpio.length() > 80) {
            throw new IllegalArgumentException("El nombre de la categoría no puede superar 80 caracteres");
        }
        return limpio;
    }
}
