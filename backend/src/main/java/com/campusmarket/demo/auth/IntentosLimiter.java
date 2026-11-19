package com.campusmarket.demo.auth;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limitador simple en memoria (ventana deslizante) por clave: correo o IP.
 * Suficiente para el MVP con una sola instancia del backend.
 */
@Component
public class IntentosLimiter {

    private final Map<String, Deque<Instant>> eventos = new ConcurrentHashMap<>();

    /** true si la clave ya alcanzó el máximo de eventos dentro de la ventana. */
    public boolean bloqueado(String clave, int max, Duration ventana) {
        Deque<Instant> d = eventos.get(clave);
        if (d == null) return false;
        synchronized (d) {
            Instant corte = Instant.now().minus(ventana);
            while (!d.isEmpty() && d.peekFirst().isBefore(corte)) d.pollFirst();
            return d.size() >= max;
        }
    }

    public void registrar(String clave) {
        Deque<Instant> d = eventos.computeIfAbsent(clave, k -> new ArrayDeque<>());
        synchronized (d) {
            d.addLast(Instant.now());
        }
    }

    public void limpiar(String clave) {
        eventos.remove(clave);
    }
}
