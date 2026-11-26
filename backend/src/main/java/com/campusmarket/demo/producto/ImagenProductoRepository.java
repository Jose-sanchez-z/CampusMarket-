package com.campusmarket.demo.producto;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ImagenProductoRepository extends JpaRepository<ImagenProducto, Long> {
    List<ImagenProducto> findByProductoIdOrderByIdAsc(Long productoId);
    List<ImagenProducto> findByProductoIdInOrderByIdAsc(Collection<Long> productoIds);
    Optional<ImagenProducto> findByNombreArchivo(String nombreArchivo);
}
