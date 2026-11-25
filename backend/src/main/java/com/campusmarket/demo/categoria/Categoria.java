package com.campusmarket.demo.categoria;

import jakarta.persistence.*;
import java.time.Instant;

/** Entidad alineada con la tabla "categorias" de la migración V3 (S2-T13). */
@Entity
@Table(name = "categorias")
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String nombre;

    @Column(nullable = false)
    private boolean activa = true;

    @Column(name = "fecha_creacion", nullable = false)
    private Instant fechaCreacion = Instant.now();

    protected Categoria() {}

    public Categoria(String nombre) {
        this.nombre = nombre;
        this.activa = true;
    }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public boolean isActiva() { return activa; }
    public void setActiva(boolean activa) { this.activa = activa; }
    public Instant getFechaCreacion() { return fechaCreacion; }
}
