package com.campusmarket.demo.auth;

import com.campusmarket.demo.sesion.Sesion;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * Repositorio propio de E07 sobre la entidad Sesion (de E08). Se separa del
 * SesionRepository de E08 para no tocar su archivo y evitar conflictos de merge.
 * V2 quitó la restricción de coherencia: basta con revocada = true.
 */
public interface SesionRevocacionRepository extends Repository<Sesion, String> {

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Sesion s set s.revocada = true where s.usuarioId = :usuarioId and s.revocada = false")
    int revocarTodas(@Param("usuarioId") Long usuarioId);
}
