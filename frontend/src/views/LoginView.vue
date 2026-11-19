<script setup>
// =====================================================================
// Marcador de posición para la ruta /login.
// PENDIENTE S1-T06 (E05): formulario de acceso -> POST /auth/login,
// error diferenciado para cuenta PENDIENTE y botón de cierre de sesión.
// Usar los componentes de '@/components/ui' (ver su README.md).
// =====================================================================
import { RouterLink } from 'vue-router'
import { BaseCard } from '@/components/ui'
</script>

<template>
  <main class="cm-pagina-acceso">
    <BaseCard size="sm" class="login" aria-labelledby="titulo-login">
      <h1 id="titulo-login">Iniciar sesión</h1>
      <p>Esta pantalla se construye en la tarea S1-T06.</p>
      <RouterLink :to="{ name: 'registro' }">Crear una cuenta</RouterLink>
    </BaseCard>
  </main>
</template>

<style scoped>
.login { text-align: center; }
</style>
