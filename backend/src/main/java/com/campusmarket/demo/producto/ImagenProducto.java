package com.campusmarket.demo.producto;

import jakarta.persistence.*;
import java.time.Instant;

/** Entidad alineada con la tabla "imagenes" de la migración V3 (S2-T13). */
@Entity
@Table(name = "imagenes")
public class ImagenProducto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "producto_id", nullable = false)
    private Long productoId;

    @Column(name = "nombre_archivo", nullable = false, unique = true, length = 255)
    private String nombreArchivo;

    @Column(name = "tipo_mime", nullable = false, length = 100)
    private String tipoMime;

    @Column(name = "tamano_bytes", nullable = false)
    private Long tamanoBytes;

    @Column(name = "fecha_creacion", nullable = false)
    private Instant fechaCreacion = Instant.now();

    protected ImagenProducto() {}

    public ImagenProducto(Long productoId, String nombreArchivo, String tipoMime, Long tamanoBytes) {
        this.productoId = productoId;
        this.nombreArchivo = nombreArchivo;
        this.tipoMime = tipoMime;
        this.tamanoBytes = tamanoBytes;
    }

    public Long getId() { return id; }
    public Long getProductoId() { return productoId; }
    public String getNombreArchivo() { return nombreArchivo; }
    public String getTipoMime() { return tipoMime; }
    public Long getTamanoBytes() { return tamanoBytes; }
    public Instant getFechaCreacion() { return fechaCreacion; }
}
