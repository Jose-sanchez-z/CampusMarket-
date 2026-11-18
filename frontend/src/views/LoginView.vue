<script setup>
// =====================================================================
// Marcador de posición para la ruta /login.
// PENDIENTE S1-T06 (E05): formulario de acceso -> POST /auth/login,
// error diferenciado para cuenta PENDIENTE y botón de cierre de sesión.
// Se deja creado para que los enlaces "Iniciar sesión" del registro funcionen.
// =====================================================================
import { RouterLink } from 'vue-router'
</script>

<template>
  <main class="cm-pagina-acceso">
    <section class="cm-tarjeta" style="max-width: 520px; text-align: center" aria-labelledby="titulo-login">
      <h1 id="titulo-login">Iniciar sesión</h1>
      <p>Esta pantalla se construye en la tarea S1-T06.</p>
      <RouterLink :to="{ name: 'registro' }">Crear una cuenta</RouterLink>
    </section>
  </main>
</template>
