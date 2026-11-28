package com.campusmarket.demo.catalogo;

import com.campusmarket.demo.categoria.CategoriaController;
import com.campusmarket.demo.producto.ProductoController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.Map;

/** Manejo uniforme de errores para los controladores de categorías y productos (E08). */
@RestControllerAdvice(assignableTypes = {CategoriaController.class, ProductoController.class})
public class CatalogoExceptionHandler {

    @ExceptionHandler(CatalogoException.class)
    public ResponseEntity<Map<String, String>> catalogo(CatalogoException e) {
        return ResponseEntity.status(e.getStatus()).body(Map.of("mensaje", e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> validacion(MethodArgumentNotValidException e) {
        String mensaje = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(err -> err.getDefaultMessage() != null ? err.getDefaultMessage() : "Datos inválidos")
                .orElse("Datos inválidos");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("mensaje", mensaje));
    }

    @ExceptionHandler({IllegalArgumentException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<Map<String, String>> badRequest(Exception e) {
        String mensaje = (e instanceof IllegalArgumentException && e.getMessage() != null)
                ? e.getMessage()
                : "Solicitud con formato inválido";
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("mensaje", mensaje));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> archivoMuyGrande(MaxUploadSizeExceededException e) {
        return ResponseEntity.status(HttpStatus.valueOf(413))
                .body(Map.of("mensaje", "La imagen supera el tamaño máximo permitido (5 MB)"));
    }
}
