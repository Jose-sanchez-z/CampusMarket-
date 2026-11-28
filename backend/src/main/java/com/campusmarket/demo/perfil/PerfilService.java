package com.campusmarket.demo.perfil;

import com.campusmarket.demo.sesion.Usuario;
import com.campusmarket.demo.sesion.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class PerfilService {

    /** Lo único que el usuario puede cambiar de su perfil. Rol, estado y correo nunca. */
    private static final Set<String> CAMPOS_PERFIL = Set.of("nombre");
    private static final int NOMBRE_MAX = 120;   // usuarios.nombre VARCHAR(120)
    private static final int VALOR_MAX = 150;    // contactos.valor VARCHAR(150)

    /** Mismos valores que ck_contactos_tipo de la migración V3. */
    private static final Set<String> TIPOS =
            Set.of("WHATSAPP", "TELEFONO", "CORREO", "INSTAGRAM", "TELEGRAM", "OTRO");

    /** Formato del valor según el tipo. OTRO solo exige que no esté vacío. */
    private static final Map<String, Pattern> FORMATOS = Map.of(
            "WHATSAPP", Pattern.compile("^\\d{10,15}$"),               // solo dígitos, con indicativo
            "TELEFONO", Pattern.compile("^\\+?\\d{7,15}$"),
            "CORREO", Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"),
            "INSTAGRAM", Pattern.compile("^@?[A-Za-z0-9._]{1,30}$"),
            "TELEGRAM", Pattern.compile("^@?[A-Za-z0-9_]{3,32}$"));

    private static final Map<String, String> AYUDA_FORMATO = Map.of(
            "WHATSAPP", "WHATSAPP debe tener solo dígitos, con indicativo de país (10 a 15 dígitos)",
            "TELEFONO", "TELEFONO debe tener de 7 a 15 dígitos, con + opcional al inicio",
            "CORREO", "CORREO debe ser una dirección de correo válida",
            "INSTAGRAM", "INSTAGRAM debe ser un usuario válido (letras, números, punto o guion bajo)",
            "TELEGRAM", "TELEGRAM debe ser un usuario válido (3 a 32 letras, números o guion bajo)");

    /** Contacto visible para otros usuarios. Sin id de dueño ni marca de visibilidad: solo lo necesario. */
    public record ContactoPublico(String tipo, String valor) {}

    private final UsuarioRepository usuarios;
    private final ContactoRepository contactos;

    public PerfilService(UsuarioRepository usuarios, ContactoRepository contactos) {
        this.usuarios = usuarios;
        this.contactos = contactos;
    }

    // ------------------------------------------------------------------ perfil

    /** Solo se acepta `nombre`. Cualquier otro campo (rol, estado, correo...) se rechaza con 422. */
    @Transactional
    public Usuario actualizarPerfil(Long usuarioId, Map<String, Object> campos) {
        if (campos == null || campos.isEmpty()) {
            throw PerfilException.invalido("No hay campos para actualizar");
        }
        for (String campo : campos.keySet()) {
            if (!CAMPOS_PERFIL.contains(campo)) {
                throw PerfilException.invalido("El campo '" + campo + "' no se puede modificar");
            }
        }
        if (!(campos.get("nombre") instanceof String nombre) || nombre.isBlank()) {
            throw PerfilException.invalido("El nombre es obligatorio");
        }
        nombre = nombre.trim();
        if (nombre.length() > NOMBRE_MAX) {
            throw PerfilException.invalido("El nombre no puede superar " + NOMBRE_MAX + " caracteres");
        }
        Usuario usuario = usuarios.findById(usuarioId).orElseThrow(PerfilException::contactoNoEncontrado);
        usuario.setNombre(nombre);
        return usuarios.save(usuario);
    }

    // --------------------------------------------------------------- contactos

    /** Todos los contactos del usuario, privados incluidos. Solo para el propio dueño (GET /usuarios/yo). */
    @Transactional(readOnly = true)
    public List<Contacto> contactosDe(Long usuarioId) {
        return contactos.findByUsuarioIdOrderByIdAsc(usuarioId);
    }

    /**
     * Función reutilizable para el Sprint 3 (detalle de producto, HU-10): devuelve SOLO los contactos
     * con es_publico = true. El filtro lo hace la consulta, así que un privado nunca llega a memoria.
     */
    @Transactional(readOnly = true)
    public List<ContactoPublico> contactosPublicos(Long usuarioId) {
        return contactos.findByUsuarioIdAndEsPublicoTrueOrderByIdAsc(usuarioId).stream()
                .map(c -> new ContactoPublico(c.getTipo(), c.getValor()))
                .toList();
    }

    /** El dueño es siempre el usuario de la sesión. Privado por defecto si no se indica esPublico. */
    @Transactional
    public Contacto agregarContacto(Long usuarioId, String tipo, String valor, Boolean esPublico) {
        if (tipo == null || tipo.isBlank()) {
            throw PerfilException.invalido("El tipo es obligatorio");
        }
        String tipoNormalizado = tipo.trim().toUpperCase(Locale.ROOT);
        if (!TIPOS.contains(tipoNormalizado)) {
            throw PerfilException.invalido("Tipo de contacto no permitido. Permitidos: " + String.join(", ", TIPOS));
        }
        if (valor == null || valor.isBlank()) {
            throw PerfilException.invalido("El valor del contacto es obligatorio");
        }
        String valorNormalizado = valor.trim();
        if (valorNormalizado.length() > VALOR_MAX) {
            throw PerfilException.invalido("El valor no puede superar " + VALOR_MAX + " caracteres");
        }
        Pattern formato = FORMATOS.get(tipoNormalizado);
        if (formato != null && !formato.matcher(valorNormalizado).matches()) {
            throw PerfilException.invalido(AYUDA_FORMATO.get(tipoNormalizado));
        }
        if ("CORREO".equals(tipoNormalizado)) {
            valorNormalizado = valorNormalizado.toLowerCase(Locale.ROOT);
        }
        return contactos.save(new Contacto(usuarioId, tipoNormalizado, valorNormalizado, Boolean.TRUE.equals(esPublico)));
    }

    /** 404 si el contacto no existe o es de otro usuario. */
    @Transactional
    public void eliminarContacto(Long usuarioId, Long contactoId) {
        Contacto contacto = contactos.findByIdAndUsuarioId(contactoId, usuarioId)
                .orElseThrow(PerfilException::contactoNoEncontrado);
        contactos.delete(contacto);
    }

    /** 404 si el contacto no existe o es de otro usuario. */
    @Transactional
    public Contacto cambiarVisibilidad(Long usuarioId, Long contactoId, Boolean esPublico) {
        if (esPublico == null) {
            throw PerfilException.invalido("esPublico es obligatorio (true o false)");
        }
        Contacto contacto = contactos.findByIdAndUsuarioId(contactoId, usuarioId)
                .orElseThrow(PerfilException::contactoNoEncontrado);
        contacto.setEsPublico(esPublico);
        return contactos.save(contacto);
    }
}
