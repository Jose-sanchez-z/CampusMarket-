package com.campusmarket.demo.config;

import com.campusmarket.demo.sesion.SesionInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final SesionInterceptor sesionInterceptor;
    private final String origenFrontend;
    private final String directorioImagenes;

    public WebConfig(SesionInterceptor sesionInterceptor,
                     @Value("${app.cors.origen:http://localhost:5173}") String origenFrontend,
                     @Value("${app.imagenes.directorio:uploads/imagenes}") String directorioImagenes) {
        this.sesionInterceptor = sesionInterceptor;
        this.origenFrontend = origenFrontend;
        this.directorioImagenes = directorioImagenes;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(sesionInterceptor).addPathPatterns("/**");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(origenFrontend)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowCredentials(true);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path rutaAbsoluta = Paths.get(directorioImagenes).toAbsolutePath().normalize();
        registry.addResourceHandler("/imagenes/**")
                .addResourceLocations(rutaAbsoluta.toUri().toString());
    }

    /** Compártelo con E07 (registro): un solo bean de PasswordEncoder en todo el proyecto. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
