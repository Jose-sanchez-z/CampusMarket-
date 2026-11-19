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
// S1-T07: la vista usa los componentes de '@/components/ui' en lugar de
// estilos propios; aquí solo queda el estilo específico del registro.
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
import { BaseAlert, BaseButton, BaseCard, BaseInput, FormField } from '@/components/ui'
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
// (BaseInput expone focus(); la casilla es un <input> nativo.)
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

// Mientras carga, BaseButton dibuja su propio spinner.
const iconoBoton = computed(() => {
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
    <BaseCard class="registro" aria-labelledby="titulo-registro">
      <!-- Encabezado: título, subtítulo y logo -->
      <header class="registro__encabezado">
        <div>
          <h1 id="titulo-registro" class="registro__titulo">{{ TEXTOS.titulo }}</h1>
          <p class="registro__subtitulo">{{ TEXTOS.subtitulo }}</p>
        </div>
        <img :src="logo" alt="CampusMarket" class="registro__logo" width="64" height="64" />
      </header>

      <!-- Conflicto 409: correo ya registrado (CA-01-02) -->
      <BaseAlert v-if="conflicto" tipo="error" icono="warning" :titulo="TEXTOS.conflictoTitulo">
        {{ errorApi.mensaje }}
        <template #acciones>
          <BaseButton size="xs" :to="{ name: 'login', query: { correo: form.correo } }">
            {{ TEXTOS.conflictoIniciar }}
          </BaseButton>
          <!-- La recuperación de contraseña se construye en S2-T03; por ahora lleva al login. -->
          <BaseButton size="xs" variant="secondary" :to="{ name: 'login', query: { correo: form.correo } }">
            {{ TEXTOS.conflictoOlvido }}
          </BaseButton>
        </template>
      </BaseAlert>

      <!-- Otros errores de la API (400, sin conexión, 429...) -->
      <BaseAlert v-else-if="errorApi" tipo="error" :titulo="TEXTOS.errorTitulo">
        {{ errorApi.mensaje }}
      </BaseAlert>

      <!-- Regla del flujo (CA-01-01) -->
      <BaseAlert v-else tipo="info" :titulo="TEXTOS.reglaTitulo">{{ TEXTOS.reglaTexto }}</BaseAlert>

      <form class="registro__form" novalidate @submit.prevent="enviar">
        <div class="registro__grid">
          <FormField id="nombre" :label="TEXTOS.nombre" :error="errores.nombre">
            <BaseInput
              ref="nombreRef"
              v-model="form.nombre"
              icon="person"
              autocomplete="name"
              :maxlength="NOMBRE_MAX"
              :placeholder="TEXTOS.nombreEjemplo"
              :disabled="cargando"
              @blur="alSalir('nombre')"
              @input="alEscribir('nombre')"
            />
          </FormField>

          <FormField
            id="correo"
            :label="TEXTOS.correo"
            :error="errores.correo"
            :error-icono="conflicto ? '' : 'close'"
            :estado="conflicto ? TEXTOS.conflictoEtiqueta : ''"
          >
            <BaseInput
              ref="correoRef"
              v-model="form.correo"
              type="email"
              icon="mail"
              :icon-right="conflicto ? 'cancel' : ''"
              autocomplete="email"
              inputmode="email"
              :maxlength="CORREO_MAX"
              :placeholder="TEXTOS.correoEjemplo"
              :disabled="cargando"
              @blur="alSalir('correo')"
              @input="alEscribir('correo')"
            />
          </FormField>

          <FormField
            id="password"
            :label="TEXTOS.password"
            :error="errores.password"
            :valid="passwordValida"
            :estado="passwordValida ? TEXTOS.passwordValida : ''"
            :estado-icono="passwordValida ? 'check_circle' : ''"
            ayuda-id="reglas-password"
          >
            <BaseInput
              ref="passwordRef"
              v-model="form.password"
              type="password"
              icon="key"
              :icon-right="passwordValida ? 'verified' : ''"
              autocomplete="new-password"
              placeholder="••••••••••••"
              :disabled="cargando"
              @blur="alSalir('password')"
              @input="alEscribir('password')"
            />
          </FormField>

          <FormField
            id="confirmacion"
            :label="TEXTOS.confirmacion"
            :error="errores.confirmacion"
            :estado="noCoincide ? TEXTOS.noCoincide : ''"
            :estado-icono="noCoincide ? 'error' : ''"
          >
            <BaseInput
              ref="confirmacionRef"
              v-model="form.confirmacion"
              type="password"
              icon="lock_reset"
              :icon-right="noCoincide ? 'error' : ''"
              autocomplete="new-password"
              placeholder="••••••••••••"
              :disabled="cargando"
              @blur="alSalir('confirmacion')"
              @input="alEscribir('confirmacion')"
            />
          </FormField>
        </div>

        <!-- Requisitos de seguridad en tiempo real (específico de esta pantalla) -->
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

        <!-- Términos: FormField sin etiqueta, solo para el mensaje de error y la accesibilidad -->
        <FormField id="terminos" :error="errores.terminos" v-slot="{ describedby, invalid }">
          <div class="check">
            <input
              id="terminos"
              ref="terminosRef"
              v-model="form.terminos"
              type="checkbox"
              :disabled="cargando"
              :aria-invalid="invalid"
              :aria-describedby="describedby"
              @change="alSalir('terminos')"
            />
            <label for="terminos" :class="{ 'check__etiqueta--error': invalid }">
              {{ TEXTOS.terminosAntes }}<a href="#" @click.prevent>{{ TEXTOS.terminosEnlace }}</a>{{ TEXTOS.terminosDespues }}
            </label>
          </div>
        </FormField>

        <BaseButton
          type="submit"
          block
          :loading="cargando"
          :disabled="botonBloqueado"
          :icon="iconoBoton"
        >
          {{ textoBoton }}
        </BaseButton>
      </form>

      <p class="registro__pie">
        {{ TEXTOS.pieTexto }}
        <RouterLink :to="{ name: 'login' }">{{ TEXTOS.pieEnlace }}</RouterLink>
      </p>
    </BaseCard>
  </main>
</template>

<style scoped>
/* Solo lo propio del registro: composición de la pantalla, requisitos y casilla de términos.
   Botones, campos, alertas y tarjeta vienen de '@/components/ui'. */
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

.check__etiqueta--error {
  color: var(--cm-error);
}
</style>
