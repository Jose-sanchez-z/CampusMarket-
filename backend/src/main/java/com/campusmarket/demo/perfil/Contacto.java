package com.campusmarket.demo.perfil;

import jakarta.persistence.*;

import java.time.Instant;

/** Medio de contacto de un usuario. Tabla `contactos` (V3, S2-T13). Privado por defecto. */
@Entity
@Table(name = "contactos")
public class Contacto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false, updatable = false)
    private Long usuarioId;

    @Column(nullable = false, length = 20)
    private String tipo;

    @Column(nullable = false, length = 150)
    private String valor;

    @Column(name = "es_publico", nullable = false)
    private boolean esPublico;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private Instant fechaCreacion = Instant.now();

    protected Contacto() {}

    public Contacto(Long usuarioId, String tipo, String valor, boolean esPublico) {
        this.usuarioId = usuarioId;
        this.tipo = tipo;
        this.valor = valor;
        this.esPublico = esPublico;
    }

    public Long getId() { return id; }
    public Long getUsuarioId() { return usuarioId; }
    public String getTipo() { return tipo; }
    public String getValor() { return valor; }
    public boolean isEsPublico() { return esPublico; }
    public void setEsPublico(boolean esPublico) { this.esPublico = esPublico; }
}
