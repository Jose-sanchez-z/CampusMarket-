package com.campusmarket.demo.perfil;

import org.springframework.http.HttpStatus;

/** Error de negocio del perfil y los contactos, con el código HTTP que debe devolverse. */
public class PerfilException extends RuntimeException {

    private final HttpStatus status;

    private PerfilException(HttpStatus status, String mensaje) {
        super(mensaje);
        this.status = status;
    }

    /** 422: el dato enviado no es válido. */
    public static PerfilException invalido(String mensaje) {
        return new PerfilException(HttpStatus.UNPROCESSABLE_CONTENT, mensaje);
    }

    /** 404: el contacto no existe o no es del usuario (no se distingue, para no revelar ids ajenos). */
    public static PerfilException contactoNoEncontrado() {
        return new PerfilException(HttpStatus.NOT_FOUND, "Contacto no encontrado");
    }

    public HttpStatus getStatus() { return status; }
}
