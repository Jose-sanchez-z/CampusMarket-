package com.campusmarket.demo.catalogo;

import org.springframework.http.HttpStatus;

/** Excepciones de dominio para categorías, productos e imágenes con su código HTTP asociado. */
public abstract class CatalogoException extends RuntimeException {
    private final HttpStatus status;

    protected CatalogoException(HttpStatus status, String mensaje) {
        super(mensaje);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public static class RecursoNoEncontrado extends CatalogoException {
        public RecursoNoEncontrado(String mensaje) {
            super(HttpStatus.NOT_FOUND, mensaje);
        }
    }

    public static class AccesoDenegado extends CatalogoException {
        public AccesoDenegado(String mensaje) {
            super(HttpStatus.FORBIDDEN, mensaje);
        }
    }

    public static class Conflicto extends CatalogoException {
        public Conflicto(String mensaje) {
            super(HttpStatus.CONFLICT, mensaje);
        }
    }

    public static class ReglaNegocio extends CatalogoException {
        public ReglaNegocio(String mensaje) {
            super(HttpStatus.UNPROCESSABLE_CONTENT, mensaje);
        }
    }
}
