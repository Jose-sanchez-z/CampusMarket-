package com.campusmarket.demo.producto;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

/** Entidad alineada con la tabla "productos" de la migración V3 (S2-T13). */
@Entity
@Table(name = "productos")
public class Producto {

    public static final String ESTADO_DISPONIBLE = "DISPONIBLE";
    public static final String ESTADO_INACTIVO = "INACTIVO";
    public static final String ESTADO_VENDIDO = "VENDIDO";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "vendedor_id", nullable = false)
    private Long vendedorId;

    @Column(name = "categoria_id", nullable = false)
    private Long categoriaId;

    @Column(nullable = false, length = 150)
    private String titulo;

    @Column(nullable = false, columnDefinition = "text")
    private String descripcion;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precio;

    @Column(nullable = false, length = 15)
    private String estado = ESTADO_DISPONIBLE;

    @Column(name = "fecha_creacion", nullable = false)
    private Instant fechaCreacion = Instant.now();

    protected Producto() {}

    public Producto(Long vendedorId, Long categoriaId, String titulo, String descripcion, BigDecimal precio) {
        this.vendedorId = vendedorId;
        this.categoriaId = categoriaId;
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.precio = precio;
        this.estado = ESTADO_DISPONIBLE;
    }

    public Long getId() { return id; }
    public Long getVendedorId() { return vendedorId; }
    public Long getCategoriaId() { return categoriaId; }
    public void setCategoriaId(Long categoriaId) { this.categoriaId = categoriaId; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public BigDecimal getPrecio() { return precio; }
    public void setPrecio(BigDecimal precio) { this.precio = precio; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public Instant getFechaCreacion() { return fechaCreacion; }
}
