package com.campusmarket.demo.auth;

import jakarta.persistence.*;
import java.time.Instant;

/** Tabla tokens_recuperacion de la migración V3 (S2-T13). Aparte de "tokens" (verificación). */
@Entity
@Table(name = "tokens_recuperacion")
public class TokenRecuperacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "token_hash", nullable = false, unique = true, length = 255)
    private String tokenHash; // nunca el token en claro

    @Column(nullable = false)
    private Instant vencimiento;

    @Column(nullable = false)
    private boolean usado;

    @Column(name = "fecha_creacion", nullable = false)
    private Instant fechaCreacion;

    protected TokenRecuperacion() {}

    /** La BD exige vencimiento > fecha_creacion (CHECK). */
    public TokenRecuperacion(Long usuarioId, String tokenHash, Instant vencimiento, Instant fechaCreacion) {
        this.usuarioId = usuarioId;
        this.tokenHash = tokenHash;
        this.vencimiento = vencimiento;
        this.fechaCreacion = fechaCreacion;
        this.usado = false;
    }

    public Long getId() { return id; }
    public Long getUsuarioId() { return usuarioId; }
    public String getTokenHash() { return tokenHash; }
    public Instant getVencimiento() { return vencimiento; }
    public boolean isUsado() { return usado; }
    public void setUsado(boolean usado) { this.usado = usado; }
    public Instant getFechaCreacion() { return fechaCreacion; }
}
