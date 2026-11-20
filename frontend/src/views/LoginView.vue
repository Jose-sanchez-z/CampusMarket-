<script setup>
// S1-T06 · Pantalla de login (HU-03). POST /auth/login según el contrato.
import { ref, computed } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { useAuth } from '@/stores/auth'

const router = useRouter()
const { iniciarSesion, marcarPendiente } = useAuth()
const logo = import.meta.env.BASE_URL + 'logo.png'

const correo = ref('')
const password = ref('')
const verPassword = ref(false)
const enviando = ref(false)
const intentado = ref(false)
const error = ref('')
const cuentaPendiente = ref(false)

const errorCorreo = computed(() => {
  if (!intentado.value) return ''
  if (!correo.value.trim()) return 'Ingresa tu correo electrónico.'
  if (!/^\S+@\S+\.\S+$/.test(correo.value.trim())) return 'Escribe un correo válido.'
  return ''
})
const errorPassword = computed(() =>
  intentado.value && !password.value ? 'Ingresa tu contraseña.' : ''
)

async function enviar() {
  intentado.value = true
  error.value = ''
  cuentaPendiente.value = false
  if (errorCorreo.value || errorPassword.value) return

  enviando.value = true
  try {
    await iniciarSesion({ correo: correo.value.trim(), password: password.value })
    router.push({ name: 'inicio' })
  } catch (e) {
    mostrarError(e)
  } finally {
    enviando.value = false
  }
}

// Mensajes según el código que devuelve el backend
function mostrarError(e) {
  switch (e.status) {
    case 400:
      error.value = 'Ingresa tu correo y tu contraseña.'
      break
    case 401:
      error.value = 'Correo o contraseña incorrectos.'
      break
    case 403:
      error.value = 'Tu cuenta aún no está verificada.'
      cuentaPendiente.value = true
      marcarPendiente(correo.value.trim())
      break
    case 0:
      error.value = 'No fue posible conectar con el servidor. Intenta de nuevo.'
      break
    default:
      error.value = e.message || 'Ocurrió un error inesperado.'
  }
}
</script>

<template>
  <main class="cm-pagina-acceso">
    <section class="cm-tarjeta tarjeta-login" aria-labelledby="titulo-login">
      <RouterLink :to="{ name: 'registro' }" class="volver" aria-label="Volver">←</RouterLink>
      <img class="logo" :src="logo" alt="CampusMarket" width="64" height="64" />
      <h1 id="titulo-login">Iniciar sesión</h1>
      <p class="subtitulo">Ingresa tus credenciales del campus universitario</p>

      <form novalidate @submit.prevent="enviar">
        <div v-if="error" class="alerta" role="alert">
          {{ error }}
          <RouterLink v-if="cuentaPendiente" :to="{ name: 'verificacion' }">Verificar mi cuenta</RouterLink>
        </div>

        <label for="login-correo">Correo electrónico</label>
        <input
          id="login-correo"
          v-model="correo"
          type="email"
          autocomplete="username"
          placeholder="estudiante@gmail.com"
          :aria-invalid="!!errorCorreo"
        />
        <p v-if="errorCorreo" class="error-campo">{{ errorCorreo }}</p>

        <div class="fila-etiqueta">
          <label for="login-password">Contraseña</label>
          <a href="#" class="enlace" @click.prevent>¿Olvidaste tu contraseña?</a>
        </div>
        <div class="campo-password">
          <input
            id="login-password"
            v-model="password"
            :type="verPassword ? 'text' : 'password'"
            autocomplete="current-password"
            :aria-invalid="!!errorPassword"
          />
          <button type="button" class="ver" :aria-pressed="verPassword" @click="verPassword = !verPassword">
            {{ verPassword ? 'Ocultar' : 'Mostrar' }}
          </button>
        </div>
        <p v-if="errorPassword" class="error-campo">{{ errorPassword }}</p>

        <button type="submit" class="boton-ingresar" :disabled="enviando">
          {{ enviando ? 'Ingresando…' : 'Iniciar sesión' }}
        </button>
      </form>

      <p class="pie-tarjeta">
        ¿No tienes cuenta? <RouterLink :to="{ name: 'registro' }">Regístrate aquí</RouterLink>
      </p>
    </section>
  </main>
</template>

<style scoped>
.tarjeta-login {
  position: relative; width: 100%; max-width: 540px; margin: 0 auto; padding: 2rem;
  background: #ffffff; border: 1px solid #dcdcf0; border-radius: 16px;
  box-shadow: 0 8px 30px rgba(27, 27, 58, 0.12); color: #1b1b3a; text-align: left;
}
.logo { display: block; margin: 0 auto 0.75rem; }
h1 { text-align: center; margin: 0 0 0.25rem; font-size: 1.75rem; }
.subtitulo { text-align: center; margin: 0 0 1.5rem; color: #5b5b7a; font-size: 0.9rem; }
label { display: block; margin: 1rem 0 0.35rem; font-size: 0.85rem; font-weight: 600; }
input {
  width: 100%; min-height: 44px; padding: 0.6rem 0.8rem; border: 1px solid #dcdcf0;
  border-radius: 8px; background: #f3f3fc; color: inherit; font: inherit;
}
input[aria-invalid='true'] { border-color: #c62828; }
.fila-etiqueta { display: flex; justify-content: space-between; align-items: baseline; }
.enlace, .pie-tarjeta a, .alerta a { color: #0050cf; font-size: 0.85rem; }
.campo-password { position: relative; }
.campo-password input { padding-right: 5.5rem; }
.ver {
  position: absolute; right: 0.4rem; top: 50%; transform: translateY(-50%);
  border: 0; background: transparent; color: #0050cf; font: inherit; font-size: 0.85rem; cursor: pointer;
}
.error-campo { margin: 0.35rem 0 0; color: #c62828; font-size: 0.8rem; }
.alerta {
  padding: 0.75rem 1rem; border-radius: 8px; background: #fdecea; color: #8e1b1b;
  font-size: 0.9rem; display: flex; flex-direction: column; gap: 0.35rem;
}
.boton-ingresar {
  width: 100%; min-height: 48px; margin-top: 1.5rem; border: 0; border-radius: 8px;
  background: #0050cf; color: #fff; font: inherit; font-weight: 600; cursor: pointer;
}
.boton-ingresar:disabled { opacity: 0.7; cursor: wait; }
.pie-tarjeta { text-align: center; margin: 1.25rem 0 0; font-size: 0.85rem; color: #5b5b7a; }
.volver {
  display: none; position: absolute; top: 1rem; left: 1rem; width: 40px; height: 40px;
  align-items: center; justify-content: center; border-radius: 10px;
  background: #e8e8fb; color: #1b1b3a; text-decoration: none;
}

/* Celular: pantalla completa, con flecha para volver */
@media (max-width: 600px) {
  .tarjeta-login { border: 0; border-radius: 0; box-shadow: none; min-height: 100vh; padding: 1.25rem; }
  .volver { display: inline-flex; }
}
</style>
