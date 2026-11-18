package com.campusmarket.demo.sesion;

import jakarta.persistence.*;

/** Si E07 ya creó esta entidad (S1-T09), usa la suya y borra esta. */
@Entity
@Table(name = "usuarios")
public class Usuario {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private String correo;
    @Column(nullable = false)
    private String nombre;
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;
    @Column(nullable = false)
    private String estado; // PENDIENTE | ACTIVA
    @Column(nullable = false)
    private String rol;    // ESTUDIANTE | ADMIN

    public Long getId() { return id; }
    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }
}
