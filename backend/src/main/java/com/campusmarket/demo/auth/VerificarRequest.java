package com.campusmarket.demo.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerificarRequest(
        @NotBlank @Email String correo,
        @NotBlank @Pattern(regexp = "\\d{6}") String token
) {}
