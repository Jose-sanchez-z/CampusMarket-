<script setup>
// =====================================================================
// Pantalla de verificación — destino del registro exitoso (CA-01-01).
// Diseño: "boceto cel blanco.png", Grupo 3 "REGISTRO • ENLACE ENVIADO"
// (MODAL_CHECK_EMAIL). En escritorio se muestra la misma tarjeta centrada.
// PENDIENTE S1-T04 (E04): campo del token -> POST /auth/verificar y botón
// "Reenviar código" con bloqueo temporal -> POST /auth/reenviartoken.
// =====================================================================
import { computed } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import logo from '@/assets/img/logo-claro.png'

const TEXTOS = {
  titulo: '¡Registro exitoso!',
  tituloSinRegistro: 'Verifica tu cuenta',
  texto: 'Hemos enviado un código a tu correo. Debes verificarlo para acceder a Campus Market',
  boton: 'Ingresar código de verificación',
  pendiente: 'El formulario del código se construye en la tarea S1-T04.',
  volver: 'Volver a editar mis datos'
}

const route = useRoute()
const correo = computed(() => (typeof route.query.correo === 'string' ? route.query.correo : ''))
const vieneDeRegistro = computed(() => route.query.registro === 'ok')

// Hora de envío del código y nombre (vienen del registro en history.state).
const estadoHistorial = {
  enviadoEn: window.history.state?.enviadoEn ?? Date.now(),
  nombre: window.history.state?.nombre ?? ''
}
</script>

<template>
  <main class="cm-pagina-acceso verificacion-pagina">
    <section class="verificacion" aria-labelledby="titulo-verificacion">
      <img :src="logo" alt="CampusMarket" class="verificacion__logo" width="128" height="128" />

      <div class="verificacion__tarjeta">
        <div class="verificacion__icono" aria-hidden="true">
          <span class="material-symbols-outlined">mark_email_read</span>
        </div>

        <h1 id="titulo-verificacion" class="verificacion__titulo">
          {{ vieneDeRegistro ? TEXTOS.titulo : TEXTOS.tituloSinRegistro }}
        </h1>

        <p class="verificacion__texto" role="status">{{ TEXTOS.texto }}</p>

        <p v-if="correo" class="verificacion__correo">{{ correo }}</p>
      </div>

      <div class="verificacion__acciones">
        <!-- Lleva a la pantalla del código (S1-T04) conservando la hora de envío y el nombre. -->
        <RouterLink
          v-if="vieneDeRegistro"
          :to="{ name: 'verificar-codigo', query: { correo }, state: estadoHistorial }"
          class="cm-boton verificacion__boton"
        >
          {{ TEXTOS.boton }}
        </RouterLink>
        <p v-else class="verificacion__pendiente">{{ TEXTOS.pendiente }}</p>

        <RouterLink :to="{ name: 'registro' }" class="verificacion__volver">{{ TEXTOS.volver }}</RouterLink>
      </div>
    </section>
  </main>
</template>

<style scoped>
/* Medidas del mockup móvil (Tailwind): logo 128px, tarjeta slate-50 con
   borde slate-200 y radio 16px, icono morado 64px, botón 44px. */
.verificacion {
  width: 100%;
  max-width: 420px;
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: var(--cm-esp-lg);
  padding: 16px;
  border-radius: var(--cm-radio-lg);
  background: #ffffff;
  box-shadow: var(--cm-sombra);
  text-align: center;
}

.verificacion__logo {
  width: 128px;
  height: 128px;
  margin: 8px auto 0;
  object-fit: contain;
  filter: drop-shadow(0 1px 1px rgba(0, 0, 0, 0.05));
}

.verificacion__tarjeta {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 16px;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  background: #f8fafc;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

.verificacion__icono {
  width: 64px;
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 12px;
  border: 1px solid #e9d5ff;
  border-radius: 9999px;
  background: #faf5ff;
  color: #9333ea;
  box-shadow: inset 0 2px 4px rgba(0, 0, 0, 0.05);
}

.verificacion__icono .material-symbols-outlined {
  font-size: 36px;
}

.verificacion__titulo {
  margin: 0;
  font-size: 20px;
  line-height: 28px;
  font-weight: 700;
  color: #0f172a;
}

.verificacion__texto {
  margin: 8px 0 0;
  font-size: 12px;
  line-height: 1.625;
  color: #475569;
}

.verificacion__correo {
  margin: 12px 0 0;
  padding: 6px 12px;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  background: #ffffff;
  color: #2563eb;
  font-size: 12px;
  font-weight: 600;
}

.verificacion__acciones {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.verificacion__boton {
  height: 44px;
  padding: 0 16px;
  font-size: 12px;
  line-height: 16px;
  text-decoration: none;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -2px rgba(0, 0, 0, 0.1);
}

.verificacion__boton:hover {
  text-decoration: none;
}

.verificacion__pendiente {
  margin: 0;
  font-size: 12px;
  color: #64748b;
}

.verificacion__volver {
  font-size: 12px;
  font-weight: 500;
}

/* Celular: pantalla completa blanca, logo arriba, tarjeta al centro y botón abajo */
@media (max-width: 767px) {
  .verificacion-pagina {
    padding: 0;
    align-items: stretch;
    background: #ffffff;
  }

  .verificacion {
    max-width: none;
    min-height: 100vh;
    min-height: 100dvh;
    justify-content: space-between;
    padding: 16px;
    border-radius: 0;
    box-shadow: none;
  }

  .verificacion__acciones {
    padding-bottom: 8px;
  }
}
</style>
