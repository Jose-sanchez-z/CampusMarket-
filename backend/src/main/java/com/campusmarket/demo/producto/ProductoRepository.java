package com.campusmarket.demo.producto;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductoRepository extends JpaRepository<Producto, Long> {
    List<Producto> findByEstadoOrderByFechaCreacionDescIdDesc(String estado);
    List<Producto> findByVendedorIdOrderByFechaCreacionDescIdDesc(Long vendedorId);
}
