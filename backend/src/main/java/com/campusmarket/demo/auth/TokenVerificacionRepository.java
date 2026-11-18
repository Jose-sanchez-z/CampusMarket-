package com.campusmarket.demo.auth;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TokenVerificacionRepository extends JpaRepository<TokenVerificacion, Long> {
}
