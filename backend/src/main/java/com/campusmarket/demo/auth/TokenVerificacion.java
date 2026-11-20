package com.campusmarket.demo.auth;

import jakarta.persistence.*;
import java.time.Instant;

/** Alineada con la tabla "tokens" de la migración de E09 (V1__usuarios_tokens_sesiones.sql). */
@Entity
@Table(name = "tokens")
public class TokenVerificacion {

    public static final String TIPO_VERIFICACION = "VERIFICACION";
    public static final String TIPO_RECUPERACION = "RECUPERACION"; // lo usa S2-T09

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "token_hash", nullable = false)
    private String tokenHash; // nunca el token en claro

    @Column(nullable = false, length = 12)
    private String tipo;

    @Column(name = "fecha_vencimiento", nullable = false)
    private Instant fechaVencimiento;

    @Column(nullable = false)
    private boolean usado;

    @Column(name = "fecha_uso")
    private Instant fechaUso;

    @Column(name = "intentos_fallidos", nullable = false)
    private int intentosFallidos;

    @Column(name = "fecha_creacion", nullable = false)
    private Instant fechaCreacion = Instant.now();

    protected TokenVerificacion() {}

    /** Token de verificación de cuenta (el caso de registro y reenvío). */
    public TokenVerificacion(Long usuarioId, String tokenHash, Instant fechaVencimiento) {
        this(usuarioId, tokenHash, fechaVencimiento, TIPO_VERIFICACION);
    }

    public TokenVerificacion(Long usuarioId, String tokenHash, Instant fechaVencimiento, String tipo) {
        this.usuarioId = usuarioId;
        this.tokenHash = tokenHash;
        this.fechaVencimiento = fechaVencimiento;
        this.tipo = tipo;
        this.usado = false;
        this.intentosFallidos = 0;
    }

    /** Marca el token como usado y registra cuándo. */
    public void marcarUsado() {
        this.usado = true;
        this.fechaUso = Instant.now();
    }

    public Long getId() { return id; }
    public Long getUsuarioId() { return usuarioId; }
    public String getTokenHash() { return tokenHash; }
    public String getTipo() { return tipo; }
    public Instant getFechaVencimiento() { return fechaVencimiento; }
    public boolean isUsado() { return usado; }
    public Instant getFechaUso() { return fechaUso; }
    public int getIntentosFallidos() { return intentosFallidos; }
    public Instant getFechaCreacion() { return fechaCreacion; }
}
