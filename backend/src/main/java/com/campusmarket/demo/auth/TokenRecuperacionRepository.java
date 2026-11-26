package com.campusmarket.demo.auth;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TokenRecuperacionRepository extends JpaRepository<TokenRecuperacion, Long> {

    // Con bloqueo: dos confirmaciones simultáneas no pueden usar el mismo token
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<TokenRecuperacion> findByTokenHash(String tokenHash);

    /** Al pedir un token nuevo, los anteriores del usuario dejan de servir. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update TokenRecuperacion t set t.usado = true where t.usuarioId = :usuarioId and t.usado = false")
    int invalidarPendientes(@Param("usuarioId") Long usuarioId);
}
