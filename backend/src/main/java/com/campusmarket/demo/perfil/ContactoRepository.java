package com.campusmarket.demo.perfil;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ContactoRepository extends JpaRepository<Contacto, Long> {

    /** Todos los contactos del dueño (incluye privados). Solo para el propio usuario. */
    List<Contacto> findByUsuarioIdOrderByIdAsc(Long usuarioId);

    /** Solo los contactos marcados como públicos (CA-05-02). */
    List<Contacto> findByUsuarioIdAndEsPublicoTrueOrderByIdAsc(Long usuarioId);

    /** La pertenencia se comprueba en la propia consulta: un id ajeno simplemente no aparece. */
    Optional<Contacto> findByIdAndUsuarioId(Long id, Long usuarioId);
}
