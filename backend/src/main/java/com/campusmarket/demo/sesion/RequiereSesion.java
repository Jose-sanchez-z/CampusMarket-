package com.campusmarket.demo.sesion;

import java.lang.annotation.*;

/**
 * Pon esta anotación en un controlador o método para exigir sesión.
 *   @RequiereSesion                      -> cualquier usuario autenticado (401 si no hay)
 *   @RequiereSesion(rol = "ADMIN")       -> solo administradores (403 si no lo es)
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequiereSesion {
    String rol() default "";
}
