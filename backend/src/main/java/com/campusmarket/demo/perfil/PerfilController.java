package com.campusmarket.demo.perfil;

import com.campusmarket.demo.sesion.RequiereSesion;
import com.campusmarket.demo.sesion.SesionInterceptor;
import com.campusmarket.demo.sesion.Usuario;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Perfil y contactos del usuario en sesión (HU-05, S2-T10).
 * El usuario sale SIEMPRE de la sesión (cookie CM_SESION), nunca del cuerpo ni de la URL.
 */
@RestController
@RequestMapping("/usuarios/yo")
@RequiereSesion
public class PerfilController {

    public record ContactoResponse(Long id, String tipo, String valor, boolean esPublico) {}
    public record PerfilResponse(Long id, String nombre, String correo, List<ContactoResponse> contactos) {}
    /** Cualquier otro campo del cuerpo (usuarioId, id...) se ignora. */
    public record ContactoRequest(String tipo, String valor, Boolean esPublico) {}
    public record VisibilidadRequest(Boolean esPublico) {}

    private final PerfilService servicio;

    public PerfilController(PerfilService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public PerfilResponse obtener(HttpServletRequest req) {
        return perfil(usuarioActual(req));
    }

    @PatchMapping
    public PerfilResponse actualizar(@RequestBody Map<String, Object> body, HttpServletRequest req) {
        Usuario actualizado = servicio.actualizarPerfil(usuarioActual(req).getId(), body);
        return perfil(actualizado);
    }

    @PostMapping("/contactos")
    public ResponseEntity<ContactoResponse> agregar(@RequestBody ContactoRequest body, HttpServletRequest req) {
        Contacto creado = servicio.agregarContacto(usuarioActual(req).getId(), body.tipo(), body.valor(), body.esPublico());
        return ResponseEntity.status(201).body(aRespuesta(creado));
    }

    @DeleteMapping("/contactos/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id, HttpServletRequest req) {
        servicio.eliminarContacto(usuarioActual(req).getId(), id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/contactos/{id}")
    public ContactoResponse cambiarVisibilidad(@PathVariable Long id, @RequestBody VisibilidadRequest body,
                                               HttpServletRequest req) {
        return aRespuesta(servicio.cambiarVisibilidad(usuarioActual(req).getId(), id, body.esPublico()));
    }

    private PerfilResponse perfil(Usuario u) {
        List<ContactoResponse> lista = servicio.contactosDe(u.getId()).stream().map(PerfilController::aRespuesta).toList();
        return new PerfilResponse(u.getId(), u.getNombre(), u.getCorreo(), lista);
    }

    private static ContactoResponse aRespuesta(Contacto c) {
        return new ContactoResponse(c.getId(), c.getTipo(), c.getValor(), c.isEsPublico());
    }

    private static Usuario usuarioActual(HttpServletRequest req) {
        return (Usuario) req.getAttribute(SesionInterceptor.ATTR_USUARIO);
    }
}
