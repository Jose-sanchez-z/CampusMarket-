package com.campusmarket.demo.auth;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;

public interface TokenVerificacionRepository extends JpaRepository<TokenVerificacion, Long> {

    // Con bloqueo para que dos verificaciones simultáneas no usen el mismo token
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<TokenVerificacion> findFirstByUsuarioIdAndTipoOrderByIdDesc(Long usuarioId, String tipo);

    List<TokenVerificacion> findByUsuarioIdAndTipoAndUsadoFalse(Long usuarioId, String tipo);

    // Se conservan para no romper las pruebas existentes
    Optional<TokenVerificacion> findFirstByUsuarioIdOrderByIdDesc(Long usuarioId);

    List<TokenVerificacion> findByUsuarioIdAndUsadoFalse(Long usuarioId);
}
