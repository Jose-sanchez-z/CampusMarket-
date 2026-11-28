package com.campusmarket.demo.producto;

import com.campusmarket.demo.catalogo.CatalogoException.ReglaNegocio;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Almacenamiento local seguro de imágenes con validación de firma MIME (S2-T12). */
@Service
public class ImagenStorageService {

    public static final long MAX_BYTES = 5L * 1024 * 1024; // 5 MB
    private static final Set<String> EXTENSIONES_PERMITIDAS = Set.of("jpg", "jpeg", "png", "webp");
    private static final Map<String, String> EXT_POR_MIME = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp"
    );

    public record ArchivoGuardado(String nombreArchivo, String tipoMime, long tamanoBytes) {}

    private final Path raiz;

    public ImagenStorageService(@Value("${app.imagenes.directorio:uploads/imagenes}") String directorio) {
        this.raiz = Paths.get(directorio).toAbsolutePath().normalize();
    }

    public ArchivoGuardado guardar(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new IllegalArgumentException("Debes adjuntar un archivo de imagen");
        }
        if (archivo.getSize() > MAX_BYTES) {
            throw new ReglaNegocio("La imagen no puede superar los 5 MB");
        }

        String original = archivo.getOriginalFilename();
        String extOriginal = extraerExtension(original);
        if (!EXTENSIONES_PERMITIDAS.contains(extOriginal)) {
            throw new ReglaNegocio("Formato no permitido. Usa JPG, PNG o WEBP");
        }

        String mimeReal = detectarMimeReal(archivo);
        String extSegura = EXT_POR_MIME.get(mimeReal);
        if (extSegura == null) {
            throw new ReglaNegocio("El contenido del archivo no corresponde a una imagen válida (JPG, PNG o WEBP)");
        }

        String nombreSeguro = UUID.randomUUID() + "." + extSegura;

        try {
            Files.createDirectories(raiz);
            Path destino = raiz.resolve(nombreSeguro).normalize();
            if (!destino.startsWith(raiz)) {
                throw new ReglaNegocio("Ruta de archivo inválida");
            }
            try (InputStream in = archivo.getInputStream()) {
                Files.copy(in, destino, StandardCopyOption.REPLACE_EXISTING);
            }
            return new ArchivoGuardado(nombreSeguro, mimeReal, archivo.getSize());
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo guardar la imagen en el servidor", e);
        }
    }

    public void eliminarSiExiste(String nombreArchivo) {
        if (nombreArchivo == null || nombreArchivo.isBlank()) {
            return;
        }
        try {
            Path destino = raiz.resolve(nombreArchivo).normalize();
            if (destino.startsWith(raiz)) {
                Files.deleteIfExists(destino);
            }
        } catch (IOException ignored) {
            // No bloquear la operación si el archivo físico ya no estaba en disco
        }
    }

    private String extraerExtension(String nombre) {
        if (nombre == null) return "";
        int punto = nombre.lastIndexOf('.');
        if (punto < 0 || punto == nombre.length() - 1) return "";
        return nombre.substring(punto + 1).trim().toLowerCase();
    }

    private String detectarMimeReal(MultipartFile archivo) {
        byte[] header = new byte[12];
        int leidos;
        try (InputStream in = archivo.getInputStream()) {
            leidos = in.read(header);
        } catch (IOException e) {
            throw new IllegalArgumentException("No se pudo leer el archivo enviado");
        }

        if (leidos >= 3
                && (header[0] & 0xFF) == 0xFF
                && (header[1] & 0xFF) == 0xD8
                && (header[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }

        if (leidos >= 8
                && (header[0] & 0xFF) == 0x89
                && header[1] == 0x50
                && header[2] == 0x4E
                && header[3] == 0x47
                && header[4] == 0x0D
                && header[5] == 0x0A
                && header[6] == 0x1A
                && header[7] == 0x0A) {
            return "image/png";
        }

        if (leidos >= 12
                && header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F'
                && header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P') {
            return "image/webp";
        }

        return "";
    }
}
