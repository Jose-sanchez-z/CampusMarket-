<script setup>
// =====================================================================
// Pantalla de verificación — destino del registro exitoso (CA-01-01).
// En S1-T03 solo confirma que el código fue enviado.
// PENDIENTE S1-T04 (E04): campo del token -> POST /auth/verificar y botón
// "Reenviar código" con bloqueo temporal -> POST /auth/reenviartoken.
// =====================================================================
import { computed } from 'vue'
import { RouterLink, useRoute } from 'vue-router'

const route = useRoute()
const correo = computed(() => (typeof route.query.correo === 'string' ? route.query.correo : ''))
const vieneDeRegistro = computed(() => route.query.registro === 'ok')
</script>

<template>
  <main class="cm-pagina-acceso">
    <section class="cm-tarjeta verificacion" aria-labelledby="titulo-verificacion">
      <div class="verificacion__icono" aria-hidden="true">✉</div>

      <h1 id="titulo-verificacion" class="verificacion__titulo">
        {{ vieneDeRegistro ? '¡Registro exitoso!' : 'Verifica tu cuenta' }}
      </h1>

      <p class="verificacion__texto" role="status">
        Hemos enviado un código a tu correo institucional. Debes verificarlo para acceder a tu
        catálogo universitario.
      </p>

      <p v-if="correo" class="verificacion__correo">{{ correo }}</p>

      <!-- Aquí va el formulario del token (S1-T04). -->

      <p class="verificacion__pie">
        <RouterLink :to="{ name: 'registro' }">Volver a editar mis datos</RouterLink>
      </p>
    </section>
  </main>
</template>

<style scoped>
.verificacion {
  max-width: 520px;
  text-align: center;
}

.verificacion__icono {
  width: 56px;
  height: 56px;
  margin: 0 auto var(--cm-esp-md);
  display: grid;
  place-items: center;
  border-radius: var(--cm-radio-lg);
  background: var(--cm-primario-suave);
  color: var(--cm-primario);
  font-size: 1.75rem;
}

.verificacion__titulo {
  margin: 0 0 var(--cm-esp-sm);
  font-size: 1.5rem;
}

.verificacion__texto {
  margin: 0 0 var(--cm-esp-md);
  color: var(--cm-texto-suave);
}

.verificacion__correo {
  display: inline-block;
  margin: 0 0 var(--cm-esp-lg);
  padding: var(--cm-esp-xs) var(--cm-esp-md);
  border: 1px solid var(--cm-borde);
  border-radius: var(--cm-radio);
  background: var(--cm-campo);
  font-weight: 600;
  color: var(--cm-primario);
}

.verificacion__pie {
  margin: 0;
}
</style>
