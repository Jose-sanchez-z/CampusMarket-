package com.campusmarket.demo.sesion;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

/** El "middleware": lee la cookie, valida la sesión y deja al usuario actual disponible. */
@Component
public class SesionInterceptor implements HandlerInterceptor {

    public static final String COOKIE = "CM_SESION";
    public static final String ATTR_USUARIO = "usuarioActual";
    public static final String ATTR_TOKEN = "sesionToken";

    private final SesionService sesionService;
    private final UsuarioRepository usuarios;

    public SesionInterceptor(SesionService sesionService, UsuarioRepository usuarios) {
        this.sesionService = sesionService;
        this.usuarios = usuarios;
    }

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) throws Exception {
        String token = leerCookie(req);
        Optional<Usuario> usuario = Optional.empty();
        if (token != null) {
            usuario = sesionService.validar(token)
                    .flatMap(s -> usuarios.findById(s.getUsuarioId()))
                    .filter(u -> "ACTIVA".equals(u.getEstado()));
        }
        usuario.ifPresent(u -> {
            req.setAttribute(ATTR_USUARIO, u);
            req.setAttribute(ATTR_TOKEN, token);
        });

        if (!(handler instanceof HandlerMethod hm)) return true;
        RequiereSesion req_ = AnnotatedElementUtils.findMergedAnnotation(hm.getMethod(), RequiereSesion.class);
        if (req_ == null) req_ = AnnotatedElementUtils.findMergedAnnotation(hm.getBeanType(), RequiereSesion.class);
        if (req_ == null) return true; // endpoint público

        if (usuario.isEmpty()) {
            error(res, 401, "No autenticado");
            return false;
        }
        if (!req_.rol().isBlank() && !req_.rol().equals(usuario.get().getRol())) {
            error(res, 403, "No tienes permisos para esta acción");
            return false;
        }
        return true;
    }

    private static String leerCookie(HttpServletRequest req) {
        if (req.getCookies() == null) return null;
        for (Cookie c : req.getCookies()) {
            if (COOKIE.equals(c.getName())) return c.getValue();
        }
        return null;
    }

    private static void error(HttpServletResponse res, int status, String mensaje) throws java.io.IOException {
        res.setStatus(status);
        res.setContentType("application/json");
        res.setCharacterEncoding(StandardCharsets.UTF_8.name());
        res.getWriter().write("{\"mensaje\":\"" + mensaje + "\"}");
    }
}
