<script setup>
// =====================================================================
// Pantalla de registro — HU-01 (S1-T03, responsable E04).
// Réplica de los mockups de S1-T01 (frontend/Diseno):
//   - "crear cuenta pantalla compu.png"  -> estado normal
//   - "error al crear cuenta.png"        -> conflicto 409 (correo ya registrado)
//   - Stitch "estados de error"          -> contraseñas no coinciden
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
import logo from '@/assets/img/logo-claro.png'

const router = useRouter()

// Vacío = se acepta cualquier correo válido (Gmail, Hotmail, institucional...).
// Si el equipo decide exigir un dominio, basta con ponerlo en el .env.
const DOMINIO = (import.meta.env.VITE_DOMINIO_INSTITUCIONAL || '').trim().replace(/^@/, '')
const PASSWORD_MIN = Number(import.meta.env.VITE_PASSWORD_MIN) || 8
const CAMPOS = ['nombre', 'correo', 'password', 'confirmacion', 'terminos']

// ---------------------------------------------------------------------
// Textos de la pantalla: basados en el mockup, con correo Gmail en lugar de
// institucional (decisión del equipo; textos de la versión Gmail de Stitch).
// Si el diseño cambia, se ajustan aquí sin tocar el resto del código.
// ---------------------------------------------------------------------
const TEXTOS = {
  titulo: 'Crear cuenta.',
  subtitulo: 'Acceso exclusivo con correo electrónico certificado (@gmail.com)',
  reglaTitulo: 'Regla.',
  reglaTexto:
    'Al completar el formulario serás dirigido directamente a la verificación de tu token vía OTP.',
  nombre: 'Nombre y Apellidos',
  nombreEjemplo: 'Nicoll Arias',
  correo: 'Correo electrónico',
  correoEjemplo: 'narias@gmail.com',
  password: 'Contraseña',
  confirmacion: 'Confirmar contraseña',
  requisitos: 'Requisitos de seguridad en tiempo real',
  terminosAntes: 'Acepto los ',
  terminosEnlace: 'términos de uso',
  terminosDespues: ' y la política de comercio seguro del marketplace estudiantil.',
  boton: 'Crear cuenta y solicitar token de verificación',
  botonCargando: 'Creando tu cuenta…',
  botonBloqueado: 'Crear cuenta',
  botonConflicto: 'Crear cuenta (Conflicto de Identidad Activo)',
  pieTexto: '¿Ya tienes cuenta?',
  pieEnlace: 'Iniciar sesión',
  // Conflicto 409 ("error al crear cuenta.png")
  conflictoTitulo: 'Conflicto de Registro (HTTP 409 Conflict)',
  conflictoTexto:
    'Este correo electrónico ya está registrado en CampusMarket. Inicia sesión o recupera tu contraseña.',
  conflictoIniciar: 'Iniciar sesión con este correo',
  conflictoOlvido: '¿Olvidaste tu contraseña?',
  conflictoEtiqueta: '¡Ya registrado!',
  // Validación de contraseñas (Stitch "estados de error")
  passwordValida: 'Válida',
  noCoincide: 'No coincide',
  // Otros errores de la API
  errorTitulo: 'No se pudo crear la cuenta'
}

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

const conflicto = computed(() => errorApi.value?.tipo === 'duplicado')

// La contraseña se marca "Válida" cuando ya se tocó y cumple todas las reglas.
const passwordValida = computed(
  () => (tocados.password || enviado.value) && Boolean(form.password) && !errores.password
)

// Contraseñas distintas: etiqueta "No coincide" del diseño.
const noCoincide = computed(
  () => Boolean(errores.confirmacion) && Boolean(form.confirmacion) && form.confirmacion !== form.password
)

// El botón se bloquea (estilo del diseño) si hay conflicto o errores visibles tras intentar enviar.
const botonBloqueado = computed(
  () => conflicto.value || (enviado.value && Object.values(errores).some(Boolean))
)

const textoBoton = computed(() => {
  if (cargando.value) return TEXTOS.botonCargando
  if (conflicto.value) return TEXTOS.botonConflicto
  if (botonBloqueado.value) return TEXTOS.botonBloqueado
  return TEXTOS.boton
})

const iconoBoton = computed(() => {
  if (cargando.value) return 'progress_activity'
  if (conflicto.value) return ''
  if (botonBloqueado.value) return 'block'
  return 'arrow_forward'
})

/** Dominio del correo escrito, para el mensaje "El buzón @… ya tiene una credencial asociada." */
const dominioCorreo = computed(() => {
  const partes = normalizarCorreo(form.correo).split('@')
  return partes.length === 2 && partes[1] ? partes[1] : 'gmail.com'
})

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
  if (campo === 'correo' && conflicto.value) {
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
  if (cargando.value || conflicto.value) return // evita doble envío
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
      errorApi.value = { tipo: 'duplicado', mensaje: TEXTOS.conflictoTexto }
      errores.correo = `El buzón @${dominioCorreo.value} ya tiene una credencial asociada.`
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
      <!-- Encabezado: título, subtítulo y logo -->
      <header class="registro__encabezado">
        <div>
          <h1 id="titulo-registro" class="registro__titulo">{{ TEXTOS.titulo }}</h1>
          <p class="registro__subtitulo">{{ TEXTOS.subtitulo }}</p>
        </div>
        <img :src="logo" alt="CampusMarket" class="registro__logo" width="64" height="64" />
      </header>

      <!-- Conflicto 409: correo ya registrado (CA-01-02) -->
      <div v-if="conflicto" class="aviso aviso--error" role="alert">
        <span class="material-symbols-outlined aviso__icono" aria-hidden="true">warning</span>
        <div class="aviso__cuerpo">
          <p class="aviso__titulo">{{ TEXTOS.conflictoTitulo }}</p>
          <p class="aviso__texto">{{ errorApi.mensaje }}</p>
          <div class="aviso__acciones">
            <RouterLink :to="{ name: 'login', query: { correo: form.correo } }" class="aviso__boton aviso__boton--primario">
              {{ TEXTOS.conflictoIniciar }}
            </RouterLink>
            <!-- La recuperación de contraseña se construye en S2-T03; por ahora lleva al login. -->
            <RouterLink :to="{ name: 'login', query: { correo: form.correo } }" class="aviso__boton aviso__boton--secundario">
              {{ TEXTOS.conflictoOlvido }}
            </RouterLink>
          </div>
        </div>
      </div>

      <!-- Otros errores de la API (400, sin conexión, 429...) -->
      <div v-else-if="errorApi" class="aviso aviso--error" role="alert">
        <span class="material-symbols-outlined aviso__icono" aria-hidden="true">error</span>
        <div class="aviso__cuerpo">
          <p class="aviso__titulo">{{ TEXTOS.errorTitulo }}</p>
          <p class="aviso__texto">{{ errorApi.mensaje }}</p>
        </div>
      </div>

      <!-- Regla del flujo (CA-01-01) -->
      <div v-else class="aviso aviso--info">
        <span class="material-symbols-outlined aviso__icono" aria-hidden="true">info</span>
        <div class="aviso__cuerpo">
          <p class="aviso__titulo">{{ TEXTOS.reglaTitulo }}</p>
          <p class="aviso__texto">{{ TEXTOS.reglaTexto }}</p>
        </div>
      </div>

      <form class="registro__form" novalidate @submit.prevent="enviar">
        <div class="registro__grid">
          <!-- Nombre y Apellidos -->
          <div class="campo" :class="{ 'campo--error': errores.nombre }">
            <label for="nombre" class="campo__etiqueta">
              <span>{{ TEXTOS.nombre }}</span>
            </label>
            <div class="campo__control">
              <span class="material-symbols-outlined campo__icono" aria-hidden="true">person</span>
              <input
                id="nombre"
                ref="nombreRef"
                v-model="form.nombre"
                type="text"
                class="campo__input"
                autocomplete="name"
                :maxlength="NOMBRE_MAX"
                :placeholder="TEXTOS.nombreEjemplo"
                :disabled="cargando"
                :aria-invalid="Boolean(errores.nombre)"
                :aria-describedby="errores.nombre ? idError('nombre') : undefined"
                @blur="alSalir('nombre')"
                @input="alEscribir('nombre')"
              />
            </div>
            <p v-if="errores.nombre" :id="idError('nombre')" class="campo__error">
              <span class="material-symbols-outlined" aria-hidden="true">close</span>{{ errores.nombre }}
            </p>
          </div>

          <!-- Correo -->
          <div class="campo" :class="{ 'campo--error': errores.correo }">
            <label for="correo" class="campo__etiqueta">
              <span>{{ TEXTOS.correo }}</span>
              <span v-if="conflicto" class="campo__etiqueta-estado">{{ TEXTOS.conflictoEtiqueta }}</span>
            </label>
            <div class="campo__control">
              <span class="material-symbols-outlined campo__icono" aria-hidden="true">mail</span>
              <input
                id="correo"
                ref="correoRef"
                v-model="form.correo"
                type="email"
                class="campo__input"
                :class="{ 'campo__input--icono-derecha': conflicto }"
                autocomplete="email"
                inputmode="email"
                :maxlength="CORREO_MAX"
                :placeholder="TEXTOS.correoEjemplo"
                :disabled="cargando"
                :aria-invalid="Boolean(errores.correo)"
                :aria-describedby="errores.correo ? idError('correo') : undefined"
                @blur="alSalir('correo')"
                @input="alEscribir('correo')"
              />
              <span v-if="conflicto" class="material-symbols-outlined campo__icono-derecha" aria-hidden="true">cancel</span>
            </div>
            <p v-if="errores.correo" :id="idError('correo')" class="campo__error">
              <template v-if="!conflicto"><span class="material-symbols-outlined" aria-hidden="true">close</span></template>{{ errores.correo }}
            </p>
          </div>

          <!-- Contraseña -->
          <div class="campo" :class="{ 'campo--error': errores.password, 'campo--valido': passwordValida }">
            <label for="password" class="campo__etiqueta">
              <span>{{ TEXTOS.password }}</span>
              <span v-if="passwordValida" class="campo__etiqueta-estado">
                <span class="material-symbols-outlined" aria-hidden="true">check_circle</span>
                {{ TEXTOS.passwordValida }}
              </span>
            </label>
            <div class="campo__control">
              <span class="material-symbols-outlined campo__icono" aria-hidden="true">key</span>
              <input
                id="password"
                ref="passwordRef"
                v-model="form.password"
                type="password"
                class="campo__input"
                :class="{ 'campo__input--icono-derecha': passwordValida }"
                autocomplete="new-password"
                placeholder="••••••••••••"
                :disabled="cargando"
                :aria-invalid="Boolean(errores.password)"
                :aria-describedby="errores.password ? `reglas-password ${idError('password')}` : 'reglas-password'"
                @blur="alSalir('password')"
                @input="alEscribir('password')"
              />
              <span v-if="passwordValida" class="material-symbols-outlined campo__icono-derecha" aria-hidden="true">verified</span>
            </div>
            <p v-if="errores.password" :id="idError('password')" class="campo__error">
              <span class="material-symbols-outlined" aria-hidden="true">close</span>{{ errores.password }}
            </p>
          </div>

          <!-- Confirmar contraseña -->
          <div class="campo" :class="{ 'campo--error': errores.confirmacion }">
            <label for="confirmacion" class="campo__etiqueta">
              <span>{{ TEXTOS.confirmacion }}</span>
              <span v-if="noCoincide" class="campo__etiqueta-estado">
                <span class="material-symbols-outlined" aria-hidden="true">error</span>
                {{ TEXTOS.noCoincide }}
              </span>
            </label>
            <div class="campo__control">
              <span class="material-symbols-outlined campo__icono" aria-hidden="true">lock_reset</span>
              <input
                id="confirmacion"
                ref="confirmacionRef"
                v-model="form.confirmacion"
                type="password"
                class="campo__input"
                :class="{ 'campo__input--icono-derecha': noCoincide }"
                autocomplete="new-password"
                placeholder="••••••••••••"
                :disabled="cargando"
                :aria-invalid="Boolean(errores.confirmacion)"
                :aria-describedby="errores.confirmacion ? idError('confirmacion') : undefined"
                @blur="alSalir('confirmacion')"
                @input="alEscribir('confirmacion')"
              />
              <span v-if="noCoincide" class="material-symbols-outlined campo__icono-derecha" aria-hidden="true">error</span>
            </div>
            <p v-if="errores.confirmacion" :id="idError('confirmacion')" class="campo__error">
              <span class="material-symbols-outlined" aria-hidden="true">close</span>{{ errores.confirmacion }}
            </p>
          </div>
        </div>

        <!-- Requisitos de seguridad en tiempo real -->
        <div id="reglas-password" class="requisitos">
          <p class="requisitos__titulo">{{ TEXTOS.requisitos }}</p>
          <ul class="requisitos__lista">
            <li
              v-for="regla in reglas"
              :key="regla.id"
              class="requisitos__item"
              :class="{ 'requisitos__item--ok': regla.cumple }"
            >
              <span class="material-symbols-outlined" aria-hidden="true">check_circle</span>
              <span>{{ regla.texto }}</span>
              <span class="sr-only">{{ regla.cumple ? '(cumplido)' : '(pendiente)' }}</span>
            </li>
          </ul>
        </div>

        <!-- Términos -->
        <div class="campo campo--check" :class="{ 'campo--error': errores.terminos }">
          <div class="check">
            <input
              id="terminos"
              ref="terminosRef"
              v-model="form.terminos"
              type="checkbox"
              :disabled="cargando"
              :aria-invalid="Boolean(errores.terminos)"
              :aria-describedby="errores.terminos ? idError('terminos') : undefined"
              @change="alSalir('terminos')"
            />
            <label for="terminos">
              {{ TEXTOS.terminosAntes }}<a href="#" @click.prevent>{{ TEXTOS.terminosEnlace }}</a>{{ TEXTOS.terminosDespues }}
            </label>
          </div>
          <p v-if="errores.terminos" :id="idError('terminos')" class="campo__error">
            <span class="material-symbols-outlined" aria-hidden="true">close</span>{{ errores.terminos }}
          </p>
        </div>

        <button
          type="submit"
          class="cm-boton"
          :class="{ 'cm-boton--cargando': cargando }"
          :disabled="cargando || botonBloqueado"
          :aria-busy="cargando"
        >
          <span>{{ textoBoton }}</span>
          <span
            v-if="iconoBoton"
            class="material-symbols-outlined"
            :class="{ 'registro__girando': cargando }"
            aria-hidden="true"
          >{{ iconoBoton }}</span>
        </button>
      </form>

      <p class="registro__pie">
        {{ TEXTOS.pieTexto }}
        <RouterLink :to="{ name: 'login' }">{{ TEXTOS.pieEnlace }}</RouterLink>
      </p>
    </section>
  </main>
</template>

<style scoped>
/* Medidas tomadas del diseño (Stitch, escala Tailwind del proyecto):
   tarjeta 760px, padding 32px, separación vertical 24px, campos 16px. */
.registro {
  display: flex;
  flex-direction: column;
  gap: var(--cm-esp-lg);
}

.registro__encabezado {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--cm-esp-md);
}

.registro__titulo {
  margin: 0;
  font-size: var(--cm-headline-lg);
  line-height: 38px;
  font-weight: 700;
  color: var(--cm-texto);
}

.registro__subtitulo {
  margin: 0;
  font-size: var(--cm-body-md);
  line-height: 20px;
  color: var(--cm-texto-suave);
}

.registro__logo {
  width: 64px;
  height: 64px;
  object-fit: contain;
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
  gap: var(--cm-esp-md);
}

@media (max-width: 767px) {
  .registro__grid {
    grid-template-columns: 1fr;
  }
}

.registro__pie {
  margin: 0;
  text-align: center;
  font-size: var(--cm-body-sm);
  line-height: 16px;
  color: var(--cm-texto-suave);
}

.registro__pie a {
  font-size: var(--cm-label-md);
  font-weight: 500;
}

.registro__girando {
  animation: girar 1s linear infinite;
}

@keyframes girar {
  to {
    transform: rotate(360deg);
  }
}

/* ---------- Avisos (Regla / conflicto / error) ---------- */
.aviso {
  display: flex;
  align-items: flex-start;
  gap: var(--cm-esp-sm);
  padding: var(--cm-esp-md);
  border-radius: var(--cm-radio-lg);
}

.aviso__icono {
  font-size: 20px;
  margin-top: 2px;
  flex-shrink: 0;
}

.aviso__cuerpo {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.aviso__titulo {
  margin: 0;
  font-size: var(--cm-label-md);
  line-height: 16px;
  font-weight: 700;
}

.aviso__texto {
  margin: 0;
  font-size: var(--cm-body-sm);
  line-height: 16px;
}

.aviso--info {
  background: var(--cm-info-fondo);
  color: var(--cm-info-texto);
}

.aviso--info .aviso__titulo {
  font-weight: 600;
}

.aviso--error {
  background: var(--cm-error-fondo);
  border: 1px solid var(--cm-error-borde);
  color: var(--cm-error);
}

.aviso--error .aviso__icono {
  font-size: 22px;
}

.aviso__acciones {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cm-esp-sm);
  margin-top: var(--cm-esp-sm);
}

.aviso__boton {
  display: inline-block;
  padding: var(--cm-esp-xs) var(--cm-esp-sm);
  border-radius: var(--cm-radio);
  font-size: var(--cm-label-sm);
  line-height: 14px;
  font-weight: 600;
  text-decoration: none;
}

.aviso__boton:hover {
  text-decoration: none;
  filter: brightness(0.95);
}

.aviso__boton--primario {
  background: var(--cm-primario);
  color: #ffffff;
}

.aviso__boton--secundario {
  background: var(--cm-secundario-fondo);
  color: var(--cm-texto);
}

/* ---------- Campos ---------- */
.campo {
  display: flex;
  flex-direction: column;
  gap: var(--cm-esp-xs);
}

.campo__etiqueta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--cm-esp-sm);
  font-size: var(--cm-label-md);
  line-height: 16px;
  font-weight: 500;
  color: var(--cm-texto);
}

.campo__etiqueta-estado {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: var(--cm-label-sm);
  line-height: 14px;
  font-weight: 600;
}

.campo__etiqueta-estado .material-symbols-outlined {
  font-size: 14px;
}

.campo__control {
  position: relative;
  display: flex;
  align-items: center;
}

.campo__icono,
.campo__icono-derecha {
  position: absolute;
  font-size: 20px;
  color: var(--cm-texto-suave);
  pointer-events: none;
}

.campo__icono {
  left: 12px;
}

.campo__icono-derecha {
  right: 12px;
  font-size: 18px;
}

.campo__input {
  width: 100%;
  padding: var(--cm-esp-sm) var(--cm-esp-md) var(--cm-esp-sm) 40px;
  border: 1px solid transparent;
  border-radius: var(--cm-radio);
  background: var(--cm-campo);
  color: var(--cm-texto);
  font-family: inherit;
  font-size: var(--cm-body-md);
  line-height: 20px;
  outline: none;
}

.campo__input--icono-derecha {
  padding-right: 40px;
}

.campo__input::placeholder {
  color: var(--cm-texto-suave);
  opacity: 0.7;
}

.campo__input:focus {
  border-color: var(--cm-primario);
}

.campo__input:disabled {
  opacity: 0.6;
}

.campo__error {
  display: flex;
  align-items: flex-start;
  gap: 4px;
  margin: 0;
  font-size: var(--cm-body-sm);
  line-height: 16px;
  color: var(--cm-error);
}

.campo__error .material-symbols-outlined {
  font-size: 14px;
  margin-top: 1px;
}

/* Estado de error (diseño): etiqueta roja en negrita, borde e iconos rojos */
.campo--error .campo__etiqueta {
  color: var(--cm-error);
  font-weight: 700;
}

.campo--error .campo__input {
  border-color: var(--cm-error-borde);
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

.campo--error .campo__icono,
.campo--error .campo__icono-derecha {
  color: var(--cm-error);
}

/* Contraseña válida (diseño): borde, icono y etiqueta en azul */
.campo--valido .campo__etiqueta-estado,
.campo--valido .campo__icono,
.campo--valido .campo__icono-derecha {
  color: var(--cm-primario);
}

.campo--valido .campo__input {
  border-color: var(--cm-primario);
}

/* ---------- Requisitos de seguridad ---------- */
.requisitos {
  display: flex;
  flex-direction: column;
  gap: var(--cm-esp-xs);
  padding: var(--cm-esp-md);
  border-radius: var(--cm-radio-lg);
  background: var(--cm-campo);
}

.requisitos__titulo {
  margin: 0;
  font-size: var(--cm-label-sm);
  line-height: 14px;
  font-weight: 600;
  letter-spacing: 0.05em;
  text-transform: uppercase;
  color: var(--cm-texto-suave);
}

.requisitos__lista {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--cm-esp-xs);
  margin: 0;
  padding: 4px 0 0;
  list-style: none;
}

@media (max-width: 639px) {
  .requisitos__lista {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

.requisitos__item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: var(--cm-body-sm);
  line-height: 16px;
  color: var(--cm-texto-suave);
}

.requisitos__item .material-symbols-outlined {
  font-size: 16px;
  color: var(--cm-texto-suave);
  opacity: 0.5;
}

.requisitos__item--ok .material-symbols-outlined {
  color: var(--cm-primario);
  opacity: 1;
}

/* ---------- Términos ---------- */
.check {
  display: flex;
  align-items: center;
  gap: var(--cm-esp-sm);
}

.check input {
  width: 16px;
  height: 16px;
  margin: 0;
  flex-shrink: 0;
  accent-color: var(--cm-primario);
  cursor: pointer;
}

.check label {
  font-size: var(--cm-body-sm);
  line-height: 16px;
  color: var(--cm-texto-suave);
  cursor: pointer;
}

.check a {
  font-weight: 400;
}

.campo--check.campo--error .check label {
  color: var(--cm-error);
}
</style>
