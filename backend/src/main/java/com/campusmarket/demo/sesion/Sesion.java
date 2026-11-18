package com.campusmarket.demo.sesion;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Se guarda el HASH del token (no el token). Si la cookie se filtra de la BD, no sirve.
 * Ajusta nombres de columnas a la migración de E09 (S1-T13).
 */
@Entity
@Table(name = "sesiones")
public class Sesion {
    @Id
    @Column(length = 64)
    private String id; // SHA-256 (hex) del token de la cookie
    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;
    @Column(name = "fecha_vencimiento", nullable = false)
    private Instant vencimiento;
    @Column(nullable = false)
    private boolean revocada;

    protected Sesion() {}
    public Sesion(String id, Long usuarioId, Instant vencimiento) {
        this.id = id; this.usuarioId = usuarioId; this.vencimiento = vencimiento;
    }
    public String getId() { return id; }
    public Long getUsuarioId() { return usuarioId; }
    public Instant getVencimiento() { return vencimiento; }
    public boolean isRevocada() { return revocada; }
    public void setRevocada(boolean revocada) { this.revocada = revocada; }
}
