package com.campusmarket.demo.auth;

import jakarta.persistence.*;
import java.time.Instant;

/** Ajusta nombres de tabla/columnas a la migración de E09 (S1-T13). */
@Entity
@Table(name = "tokens_verificacion")
public class TokenVerificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "token_hash", nullable = false)
    private String tokenHash; // nunca el token en claro

    @Column(name = "vence_en", nullable = false)
    private Instant venceEn;

    @Column(nullable = false)
    private boolean usado;

    protected TokenVerificacion() {}

    public TokenVerificacion(Long usuarioId, String tokenHash, Instant venceEn) {
        this.usuarioId = usuarioId;
        this.tokenHash = tokenHash;
        this.venceEn = venceEn;
        this.usado = false;
    }

    public Long getId() { return id; }
    public Long getUsuarioId() { return usuarioId; }
    public String getTokenHash() { return tokenHash; }
    public Instant getVenceEn() { return venceEn; }
    public boolean isUsado() { return usado; }
    public void setUsado(boolean usado) { this.usado = usado; }
}
