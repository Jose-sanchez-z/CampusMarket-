<script setup>
// =====================================================================
// Pantalla de registro — HU-01 (S1-T03, responsable E04).
// Reglas:
//  - CA-01-01: si la API responde 202 se redirige a /verificar SIN iniciar
//    sesión (no se guarda nada en localStorage/sessionStorage).
//  - CA-01-02: si el correo ya existe (409) se muestra un mensaje claro y
//    no se crea otra cuenta.
// =====================================================================
import { computed, nextTick, reactive, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { registrarUsuario } from '@/services/authService'
import {
  normalizarCorreo,
  normalizarNombre,
  reglasPassword,
  validarRegistro,
  formularioValido,
  NOMBRE_MAX,
  CORREO_MAX
} from '@/utils/validaciones'
import logoClaro from '@/assets/img/logo-claro.png'
import logoOscuro from '@/assets/img/logo-oscuro.png'

const router = useRouter()

const DOMINIO = (import.meta.env.VITE_DOMINIO_INSTITUCIONAL || 'unipamplona.edu.co').replace(/^@/, '')
const PASSWORD_MIN = Number(import.meta.env.VITE_PASSWORD_MIN) || 8
const CAMPOS = ['nombre', 'correo', 'password', 'confirmacion', 'terminos']

const form = reactive({
  nombre: '',
  correo: '',
  password: '',
  confirmacion: '',
  terminos: false
})

// Mensajes de error por campo y campos que el usuario ya tocó.
const errores = reactive({ nombre: '', correo: '', password: '', confirmacion: '', terminos: '' })
const tocados = reactive({ nombre: false, correo: false, password: false, confirmacion: false, terminos: false })

const cargando = ref(false)
const enviado = ref(false)
const verPassword = ref(false)
const verConfirmacion = ref(false)
// Error general devuelto por la API: { tipo: 'duplicado' | 'general', mensaje }
const errorApi = ref(null)

// Referencias a los campos para mover el foco al primer error.
const nombreRef = ref(null)
const correoRef = ref(null)
const passwordRef = ref(null)
const confirmacionRef = ref(null)
const terminosRef = ref(null)
const refs = {
  nombre: nombreRef,
  correo: correoRef,
  password: passwordRef,
  confirmacion: confirmacionRef,
  terminos: terminosRef
}

const reglas = computed(() => reglasPassword(form.password, PASSWORD_MIN))

function calcularErrores() {
  return validarRegistro(form, { dominio: DOMINIO, passwordMin: PASSWORD_MIN })
}

/** Muestra el error de un campo solo si ya se tocó o si se intentó enviar. */
function revisarCampo(campo) {
  const nuevos = calcularErrores()
  for (const c of CAMPOS) {
    if (tocados[c] || enviado.value) errores[c] = nuevos[c]
  }
  // Si la contraseña cambia, la confirmación debe volver a revisarse.
  if (campo === 'password' && form.confirmacion) {
    errores.confirmacion = nuevos.confirmacion
  }
}

function alSalir(campo) {
  tocados[campo] = true
  revisarCampo(campo)
}

function alEscribir(campo) {
  if (campo === 'correo' && errorApi.value?.tipo === 'duplicado') {
    errorApi.value = null
  }
  if (tocados[campo] || enviado.value) revisarCampo(campo)
}

function idError(campo) {
  return `error-${campo}`
}

async function enfocarPrimerError() {
  await nextTick()
  const campo = CAMPOS.find((c) => errores[c])
  refs[campo]?.value?.focus()
}

/** Aplica errores por campo si el backend los devuelve en un 400. */
function aplicarErroresDeCampo(datos) {
  const lista = datos?.errores
  if (!lista) return false
  const pares = Array.isArray(lista)
    ? lista.map((e) => [e.campo, e.mensaje])
    : Object.entries(lista)
  let aplicado = false
  for (const [campo, mensaje] of pares) {
    if (campo in errores && mensaje) {
      errores[campo] = mensaje
      aplicado = true
    }
  }
  return aplicado
}

function mensajePorEstado(error) {
  switch (error?.status) {
    case 0:
      return 'No pudimos conectar con el servidor. Verifica tu conexión e intenta de nuevo.'
    case 400:
      return error.datos?.mensaje
        ? `Revisa los datos ingresados: ${error.datos.mensaje}`
        : 'Revisa los datos ingresados e intenta de nuevo.'
    case 429:
      return 'Hiciste demasiados intentos. Espera unos minutos e intenta de nuevo.'
    default:
      return `Ocurrió un error inesperado${error?.status ? ` (código ${error.status})` : ''}. Intenta de nuevo en unos minutos.`
  }
}

async function enviar() {
  if (cargando.value) return // evita doble envío
  enviado.value = true
  errorApi.value = null

  // Normalizar antes de validar y enviar.
  form.nombre = normalizarNombre(form.nombre)
  form.correo = normalizarCorreo(form.correo)

  Object.assign(errores, calcularErrores())
  if (!formularioValido(errores)) {
    await enfocarPrimerError()
    return
  }

  cargando.value = true
  try {
    await registrarUsuario({
      nombre: form.nombre,
      correo: form.correo,
      password: form.password
    })
    // Respuesta 2xx (202 según contrato): cuenta PENDIENTE y sin sesión.
    // Solo se pasa el correo a la pantalla de verificación; no se guarda nada.
    const correo = form.correo
    form.password = ''
    form.confirmacion = ''
    await router.push({ name: 'verificacion', query: { correo, registro: 'ok' } })
  } catch (error) {
    if (error?.status === 409) {
      errorApi.value = {
        tipo: 'duplicado',
        mensaje: 'Este correo institucional ya está registrado en CampusMarket. Inicia sesión o recupera tu contraseña.'
      }
      errores.correo = 'Este correo ya tiene una cuenta asociada.'
      await nextTick()
      correoRef.value?.focus()
    } else {
      const porCampo = error?.status === 400 && aplicarErroresDeCampo(error.datos)
      errorApi.value = { tipo: 'general', mensaje: mensajePorEstado(error) }
      if (porCampo) await enfocarPrimerError()
    }
  } finally {
    cargando.value = false
  }
}
</script>

<template>
  <main class="cm-pagina-acceso">
    <section class="cm-tarjeta registro" aria-labelledby="titulo-registro">
      <header class="registro__encabezado">
        <div>
          <h1 id="titulo-registro" class="registro__titulo">Crear cuenta.</h1>
          <p class="registro__subtitulo">
            Acceso exclusivo con correo institucional certificado (@{{ DOMINIO }})
          </p>
        </div>
        <picture>
          <source :srcset="logoOscuro" media="(prefers-color-scheme: dark)" />
          <img :src="logoClaro" alt="CampusMarket" class="registro__logo" width="96" height="68" />
        </picture>
      </header>

      <!-- Error general de la API (409 duplicado, 400, sin conexión...) -->
      <div v-if="errorApi" class="alerta alerta--error" role="alert">
        <p class="alerta__titulo">
          {{ errorApi.tipo === 'duplicado' ? 'El correo ya está registrado' : 'No se pudo crear la cuenta' }}
        </p>
        <p class="alerta__texto">{{ errorApi.mensaje }}</p>
        <div v-if="errorApi.tipo === 'duplicado'" class="alerta__acciones">
          <RouterLink :to="{ name: 'login', query: { correo: form.correo } }" class="alerta__boton">
            Iniciar sesión con este correo
          </RouterLink>
        </div>
      </div>

      <!-- Regla del flujo (CA-01-01) -->
      <div v-else class="alerta alerta--info">
        <p class="alerta__texto">
          <strong>Importante.</strong> Al completar el formulario pasarás a verificar tu cuenta con el
          código que enviaremos a tu correo institucional.
        </p>
      </div>

      <form class="registro__form" novalidate @submit.prevent="enviar">
        <div class="registro__grid">
          <!-- Nombre -->
          <div class="campo" :class="{ 'campo--error': errores.nombre }">
            <label for="nombre" class="campo__etiqueta">Nombre y apellidos</label>
            <input
              id="nombre"
              ref="nombreRef"
              v-model="form.nombre"
              type="text"
              class="campo__input"
              autocomplete="name"
              :maxlength="NOMBRE_MAX"
              placeholder="Ej. Camilo Torres Cruz"
              :disabled="cargando"
              :aria-invalid="Boolean(errores.nombre)"
              :aria-describedby="errores.nombre ? idError('nombre') : undefined"
              @blur="alSalir('nombre')"
              @input="alEscribir('nombre')"
            />
            <p v-if="errores.nombre" :id="idError('nombre')" class="campo__error">{{ errores.nombre }}</p>
          </div>

          <!-- Correo -->
          <div class="campo" :class="{ 'campo--error': errores.correo }">
            <label for="correo" class="campo__etiqueta">Correo institucional</label>
            <input
              id="correo"
              ref="correoRef"
              v-model="form.correo"
              type="email"
              class="campo__input"
              autocomplete="email"
              inputmode="email"
              :maxlength="CORREO_MAX"
              :placeholder="`usuario@${DOMINIO}`"
              :disabled="cargando"
              :aria-invalid="Boolean(errores.correo)"
              :aria-describedby="errores.correo ? idError('correo') : undefined"
              @blur="alSalir('correo')"
              @input="alEscribir('correo')"
            />
            <p v-if="errores.correo" :id="idError('correo')" class="campo__error">{{ errores.correo }}</p>
          </div>

          <!-- Contraseña -->
          <div class="campo" :class="{ 'campo--error': errores.password }">
            <label for="password" class="campo__etiqueta">Contraseña</label>
            <div class="campo__con-boton">
              <input
                id="password"
                ref="passwordRef"
                v-model="form.password"
                :type="verPassword ? 'text' : 'password'"
                class="campo__input"
                autocomplete="new-password"
                :disabled="cargando"
                :aria-invalid="Boolean(errores.password)"
                :aria-describedby="errores.password ? `reglas-password ${idError('password')}` : 'reglas-password'"
                @blur="alSalir('password')"
                @input="alEscribir('password')"
              />
              <button
                type="button"
                class="campo__ver"
                :aria-pressed="verPassword"
                :disabled="cargando"
                @click="verPassword = !verPassword"
              >
                {{ verPassword ? 'Ocultar' : 'Mostrar' }}<span class="sr-only"> contraseña</span>
              </button>
            </div>
            <p v-if="errores.password" :id="idError('password')" class="campo__error">{{ errores.password }}</p>
          </div>

          <!-- Confirmación -->
          <div class="campo" :class="{ 'campo--error': errores.confirmacion }">
            <label for="confirmacion" class="campo__etiqueta">Confirmar contraseña</label>
            <div class="campo__con-boton">
              <input
                id="confirmacion"
                ref="confirmacionRef"
                v-model="form.confirmacion"
                :type="verConfirmacion ? 'text' : 'password'"
                class="campo__input"
                autocomplete="new-password"
                :disabled="cargando"
                :aria-invalid="Boolean(errores.confirmacion)"
                :aria-describedby="errores.confirmacion ? idError('confirmacion') : undefined"
                @blur="alSalir('confirmacion')"
                @input="alEscribir('confirmacion')"
              />
              <button
                type="button"
                class="campo__ver"
                :aria-pressed="verConfirmacion"
                :disabled="cargando"
                @click="verConfirmacion = !verConfirmacion"
              >
                {{ verConfirmacion ? 'Ocultar' : 'Mostrar' }}<span class="sr-only"> confirmación</span>
              </button>
            </div>
            <p v-if="errores.confirmacion" :id="idError('confirmacion')" class="campo__error">
              {{ errores.confirmacion }}
            </p>
          </div>
        </div>

        <!-- Requisitos de la contraseña en tiempo real -->
        <div id="reglas-password" class="requisitos">
          <p class="requisitos__titulo">Requisitos de seguridad</p>
          <ul class="requisitos__lista">
            <li
              v-for="regla in reglas"
              :key="regla.id"
              class="requisitos__item"
              :class="{ 'requisitos__item--ok': regla.cumple }"
            >
              <span aria-hidden="true">{{ regla.cumple ? '✓' : '○' }}</span>
              {{ regla.texto }}
              <span class="sr-only">{{ regla.cumple ? '(cumplido)' : '(pendiente)' }}</span>
            </li>
          </ul>
        </div>

        <!-- Términos -->
        <div class="campo campo--check" :class="{ 'campo--error': errores.terminos }">
          <label class="check">
            <input
              ref="terminosRef"
              v-model="form.terminos"
              type="checkbox"
              :disabled="cargando"
              :aria-invalid="Boolean(errores.terminos)"
              :aria-describedby="errores.terminos ? idError('terminos') : undefined"
              @change="alSalir('terminos')"
            />
            <span>Acepto los términos de uso y la política de comercio seguro del marketplace estudiantil.</span>
          </label>
          <p v-if="errores.terminos" :id="idError('terminos')" class="campo__error">{{ errores.terminos }}</p>
        </div>

        <button type="submit" class="cm-boton" :disabled="cargando" :aria-busy="cargando">
          <template v-if="cargando">
            <span class="cm-spinner" aria-hidden="true"></span>
            Creando tu cuenta…
          </template>
          <template v-else>Crear cuenta y solicitar código de verificación →</template>
        </button>
      </form>

      <p class="registro__pie">
        ¿Ya tienes cuenta?
        <RouterLink :to="{ name: 'login' }">Iniciar sesión</RouterLink>
      </p>
    </section>
  </main>
</template>

<style scoped>
.registro__encabezado {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--cm-esp-md);
  margin-bottom: var(--cm-esp-lg);
}

.registro__titulo {
  margin: 0;
  font-size: var(--cm-titulo);
  line-height: 1.25;
  font-weight: 700;
}

.registro__subtitulo {
  margin: var(--cm-esp-xs) 0 0;
  color: var(--cm-texto-suave);
}

.registro__logo {
  width: 96px;
  height: auto;
  flex-shrink: 0;
}

.registro__form {
  display: flex;
  flex-direction: column;
  gap: var(--cm-esp-md);
}

.registro__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--cm-esp-md) var(--cm-esp-lg);
}

@media (max-width: 640px) {
  .registro__grid {
    grid-template-columns: 1fr;
  }

  .registro__logo {
    width: 72px;
  }
}

.registro__pie {
  margin: var(--cm-esp-lg) 0 0;
  text-align: center;
  color: var(--cm-texto-suave);
}

/* Alertas */
.alerta {
  border-radius: var(--cm-radio);
  padding: var(--cm-esp-md);
  margin-bottom: var(--cm-esp-lg);
}

.alerta--info {
  background: var(--cm-info-fondo);
  color: var(--cm-info-texto);
  border: 1px solid var(--cm-borde);
}

.alerta--error {
  background: var(--cm-error-fondo);
  color: var(--cm-error);
  border: 1px solid var(--cm-error-borde);
}

.alerta__titulo {
  margin: 0 0 var(--cm-esp-xs);
  font-weight: 700;
}

.alerta__texto {
  margin: 0;
}

.alerta__acciones {
  margin-top: var(--cm-esp-sm);
}

.alerta__boton {
  display: inline-block;
  padding: var(--cm-esp-xs) var(--cm-esp-sm);
  border-radius: var(--cm-radio);
  background: var(--cm-primario);
  color: #ffffff;
  font-size: var(--cm-texto-sm);
}

/* Campos */
.campo {
  display: flex;
  flex-direction: column;
  gap: var(--cm-esp-xs);
}

.campo__etiqueta {
  font-size: var(--cm-texto-sm);
  font-weight: 600;
}

.campo__input {
  width: 100%;
  min-height: 44px;
  padding: var(--cm-esp-sm) var(--cm-esp-md);
  border: 1px solid var(--cm-borde);
  border-radius: var(--cm-radio);
  background: var(--cm-campo);
  color: var(--cm-texto);
  font: inherit;
}

.campo__input:focus {
  border-color: var(--cm-primario);
}

.campo__input:disabled {
  opacity: 0.6;
}

.campo--error .campo__input {
  border-color: var(--cm-error-borde);
  background: var(--cm-error-fondo);
}

.campo__error {
  margin: 0;
  color: var(--cm-error);
  font-size: var(--cm-texto-sm);
}

.campo__con-boton {
  position: relative;
}

.campo__con-boton .campo__input {
  padding-right: 5.5rem;
}

.campo__ver {
  position: absolute;
  top: 50%;
  right: var(--cm-esp-sm);
  transform: translateY(-50%);
  border: none;
  background: transparent;
  color: var(--cm-primario);
  font: inherit;
  font-size: var(--cm-texto-sm);
  font-weight: 600;
  cursor: pointer;
  padding: var(--cm-esp-xs);
}

/* Requisitos de contraseña */
.requisitos {
  padding: var(--cm-esp-md);
  border: 1px solid var(--cm-borde);
  border-radius: var(--cm-radio-lg);
  background: var(--cm-campo);
}

.requisitos__titulo {
  margin: 0 0 var(--cm-esp-sm);
  font-size: 0.6875rem;
  font-weight: 700;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  color: var(--cm-texto-suave);
}

.requisitos__lista {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--cm-esp-xs);
  margin: 0;
  padding: 0;
  list-style: none;
}

@media (max-width: 640px) {
  .requisitos__lista {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

.requisitos__item {
  font-size: var(--cm-texto-sm);
  color: var(--cm-texto-suave);
}

.requisitos__item--ok {
  color: var(--cm-primario);
  font-weight: 600;
}

/* Casilla de términos */
.check {
  display: flex;
  align-items: flex-start;
  gap: var(--cm-esp-sm);
  font-size: var(--cm-texto-sm);
  cursor: pointer;
}

.check input {
  width: 16px;
  height: 16px;
  margin-top: 1px;
  accent-color: var(--cm-primario);
}
</style>
