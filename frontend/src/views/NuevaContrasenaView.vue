<script setup>
// =====================================================================
// Recuperar contraseña, paso 2: código + nueva contraseña (S2-T03, HU-04, E04).
// Diseño:
//   - Computador: frontend/Diseno/Sprint2/"Recuperación de contraseña (HU-04).png"
//     (panel derecho, estados Normal y Claves desiguales) y
//     "Error contraseña HU-04.png"; vencido y éxito según el lienzo del zip.
//   - Celular (≤ 767 px): mockup de 390 px del zip de Yari
//     ("PANTALLA 2: Crear nueva contraseña", vista móvil).
// API (S2-T09, E07):
//   POST /auth/recuperacion/confirmar { token, nuevaContrasena }
//   200 cambiada + sesiones revocadas | 400 token inválido/vencido/usado
//   422 contraseña fuera de política | 429 demasiados intentos
// Reglas:
//  - CA-04-02: con token válido se cambia la clave y se cierran todas las
//    sesiones, así que al terminar se lleva al LOGIN (no se "entra" directo).
//  - CA-04-03: con token vencido la clave no cambia y se muestra un error
//    controlado con la opción de pedir un código nuevo.
// Ajustes frente al mockup, por orden del Scrum Master (se manda el backend):
//  - El token son 43 caracteres (no 6 casillas): un solo campo, pegable.
//  - Vigencia de 30 min; botón "Cambiar contraseña" (antes "...y entrar").
//  - Celular: el checklist no muestra caracteres de la clave ("(C)", "(2025)").
// =====================================================================
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { FormField, BaseInput, BaseButton } from '@/components/ui'
import { confirmarRecuperacion, solicitarRecuperacion } from '@/services/authService'
import { reglasPassword, formularioValido, PASSWORD_MAX } from '@/utils/validaciones'
import { normalizarToken, validarNuevaContrasena, nivelPassword, reglasMovil } from '@/utils/recuperacion'
import { useEsMovil } from '@/composables/useEsMovil'
import { tiempoRestante, formatearTiempo } from '@/utils/codigo'
import logo from '@/assets/img/logo-claro.png'
import '@/assets/styles/recuperacion.css'

const route = useRoute()
const router = useRouter()
const esMovil = useEsMovil()

const PASSWORD_MIN = Number(import.meta.env.VITE_PASSWORD_MIN) || 8
const MINUTOS_VIGENCIA = Number(import.meta.env.VITE_RECUPERACION_MINUTOS) || 30
const SEGUNDOS_BLOQUEO_REENVIO = Number(import.meta.env.VITE_REENVIO_SEGUNDOS) || 60
const SEGUNDOS_REDIRECCION = 3

const TEXTOS = {
  paso: 'Paso 2 de 2 • Verificación segura',
  titulo: 'Crear nueva contraseña',
  textoConCorreo: 'Ingresa el código que enviamos a tu correo personal',
  textoSinCorreo: 'Ingresa el código de recuperación que te enviamos por correo.',
  token: 'Código de seguridad (Token)',
  tokenEjemplo: 'Pega aquí el código del correo',
  expiraEn: (t) => `Expira en ${t} min`,
  expirado: 'Tiempo cumplido',
  revisaSpam: 'Revisa tu bandeja o carpeta de spam.',
  noRecibiste: '¿No recibiste el código?',
  reenviar: 'Reenviar',
  reenviarEn: (t) => `Reenviar (${t})`,
  reenviado: 'Listo. Te llegará un código nuevo; el anterior deja de servir.',
  password: 'Nueva contraseña',
  nivel: (n) => `Nivel: ${n}`,
  requisitos: 'Tu clave debe incluir:',
  confirmacion: 'Confirmar nueva contraseña',
  coinciden: 'Coinciden',
  noCoinciden: 'No coinciden',
  verPassword: 'Mostrar contraseña',
  ocultarPassword: 'Ocultar contraseña',
  boton: 'Cambiar contraseña',
  botonCargando: 'Guardando...',
  volver: 'Volver a Iniciar Sesión',
  sesiones:
    'Tu seguridad es primordial en CampusMarket. Todas tus sesiones activas en otros dispositivos se cerrarán por protección una vez actualizada la contraseña.',
  // Errores
  noCoincidenTitulo: 'Las contraseñas no coinciden',
  noCoincidenTexto: 'Por favor asegúrate de que ambos campos tengan exactamente los mismos caracteres antes de confirmar.',
  vencidoTitulo: 'El código de seguridad caducó o no es válido',
  vencidoTexto: `Por seguridad de tu cuenta, los códigos son de un solo uso y duran ${MINUTOS_VIGENCIA} minutos. Tu contraseña no se cambió.`,
  vencidoBoton: 'Solicitar nuevo código',
  politica: `La contraseña debe tener entre ${PASSWORD_MIN} y ${PASSWORD_MAX} caracteres.`,
  demasiados: 'Demasiados intentos. Espera unos minutos e intenta de nuevo.',
  sinConexion: 'Ocurrió una interrupción temporal de conexión. Por favor reintenta en unos instantes.',
  // Éxito
  exitoTitulo: 'Contraseña restablecida con éxito',
  exitoTexto: 'Por tu seguridad cerramos todas tus sesiones abiertas. Inicia sesión con tu nueva contraseña.',
  exitoCaja: '¡Credencial renovada!',
  exitoRedirige: (s) => `Redirigiendo a inicio de sesión en ${s}s...`,
  exitoBoton: 'Ir a iniciar sesión',
  atras: 'Volver'
}

// Textos del mockup de celular (390 px)
const TEXTOS_MOVIL = {
  textoConCorreo: 'Ingresa el código que te enviamos y define una contraseña robusta para tu cuenta de CampusMarket.',
  textoSinCorreo: 'Ingresa el código que te enviamos y define una contraseña robusta para tu cuenta de CampusMarket.',
  token: 'Código de seguridad',
  confirmacion: 'Confirmar contraseña',
  coinciden: 'Coincide',
  cancelar: 'Cancelar y regresar'
}

const T = computed(() => (esMovil.value ? { ...TEXTOS, ...TEXTOS_MOVIL } : TEXTOS))

// ---------------------------------------------------------------------
// Estado
// ---------------------------------------------------------------------
const correo = ref(typeof window.history.state?.correo === 'string' ? window.history.state.correo : '')
const enviadoEn = ref(Number(window.history.state?.enviadoEn) || 0) // 0 = no sabemos cuándo se envió

// 'normal' | 'vencido' | 'exito'
const estado = ref('normal')
const guardando = ref(false)
const form = ref({
  token: typeof route.query.token === 'string' ? normalizarToken(route.query.token) : '',
  password: '',
  confirmacion: ''
})
const errores = ref({ token: '', password: '', confirmacion: '' })
const intentoEnviar = ref(false)
const errorGeneral = ref('')
const aviso = ref('')
const verPassword = ref(false)
const reenviando = ref(false)
const bloqueoReenvio = ref(0)
const segundosRedireccion = ref(SEGUNDOS_REDIRECCION)

const tokenRef = ref(null)
const passwordRef = ref(null)
const confirmacionRef = ref(null)

const ahora = ref(Date.now())
let intervalo = null
let temporizadorRedireccion = null

onMounted(async () => {
  intervalo = window.setInterval(() => {
    ahora.value = Date.now()
    if (bloqueoReenvio.value > 0) bloqueoReenvio.value -= 1
  }, 1000)
  // Si el código vino en la URL (?token=), se quita para que no quede en el historial.
  if (route.query.token) router.replace({ query: {}, state: window.history.state })
  await nextTick()
  ;(form.value.token ? passwordRef : tokenRef).value?.focus()
})

onBeforeUnmount(() => {
  window.clearInterval(intervalo)
  window.clearInterval(temporizadorRedireccion)
})

// ---------------------------------------------------------------------
// Contador "Expira en" (solo si sabemos cuándo se pidió el código)
// ---------------------------------------------------------------------
const restante = computed(() => (enviadoEn.value ? tiempoRestante(enviadoEn.value, MINUTOS_VIGENCIA, ahora.value) : null))
const textoExpira = computed(() => {
  if (restante.value === null) return ''
  return restante.value === 0 ? TEXTOS.expirado : TEXTOS.expiraEn(formatearTiempo(restante.value))
})

// ---------------------------------------------------------------------
// Contraseña: reglas, nivel y coincidencia en tiempo real
// ---------------------------------------------------------------------
const reglas = computed(() =>
  esMovil.value ? reglasMovil(form.value.password, PASSWORD_MIN) : reglasPassword(form.value.password, PASSWORD_MIN)
)
const nivel = computed(() => nivelPassword(form.value.password, PASSWORD_MIN))
const claseNivel = computed(() =>
  nivel.value.nivel <= 1 ? 'rec__nivel--bajo' : nivel.value.nivel === 2 ? 'rec__nivel--medio' : ''
)
const hayConfirmacion = computed(() => form.value.confirmacion.length > 0)
const coinciden = computed(() => hayConfirmacion.value && form.value.confirmacion === form.value.password)
const mostrarBannerNoCoinciden = computed(
  () => intentoEnviar.value && hayConfirmacion.value && !coinciden.value && estado.value === 'normal'
)

/** Vuelve a validar un campo si ya tenía error (para que el error se quite al corregir). */
function revalidar(campo) {
  errorGeneral.value = ''
  if (!errores.value[campo] && !intentoEnviar.value) return
  const nuevos = validarNuevaContrasena(form.value, { passwordMin: PASSWORD_MIN })
  errores.value[campo] = nuevos[campo]
  if (campo === 'password' && hayConfirmacion.value) errores.value.confirmacion = nuevos.confirmacion
}

function alSalirToken() {
  form.value.token = normalizarToken(form.value.token)
  if (form.value.token) {
    errores.value.token = validarNuevaContrasena(form.value, { passwordMin: PASSWORD_MIN }).token
  }
}

// Si el usuario cambia el código después de "vencido", se vuelve al formulario normal.
watch(
  () => form.value.token,
  () => {
    if (estado.value === 'vencido') estado.value = 'normal'
  }
)

// ---------------------------------------------------------------------
// Enviar (POST /auth/recuperacion/confirmar)
// ---------------------------------------------------------------------
async function guardar() {
  if (guardando.value) return
  intentoEnviar.value = true
  errorGeneral.value = ''
  aviso.value = ''
  form.value.token = normalizarToken(form.value.token)
  errores.value = validarNuevaContrasena(form.value, { passwordMin: PASSWORD_MIN })
  if (!formularioValido(errores.value)) {
    const primero = errores.value.token ? tokenRef : errores.value.password ? passwordRef : confirmacionRef
    primero.value?.focus()
    return
  }

  guardando.value = true
  try {
    await confirmarRecuperacion({ token: form.value.token, nuevaContrasena: form.value.password })
    // CA-04-02: clave cambiada y sesiones revocadas -> hay que iniciar sesión de nuevo.
    form.value = { token: '', password: '', confirmacion: '' }
    estado.value = 'exito'
    iniciarRedireccion()
  } catch (error) {
    if (error?.status === 400) {
      // CA-04-03: token inválido, vencido o usado. La contraseña NO cambió.
      estado.value = 'vencido'
    } else if (error?.status === 422) {
      // Política de contraseña: el token no se gastó, puede corregir y reintentar.
      errores.value.password = error.datos?.mensaje || TEXTOS.politica
      passwordRef.value?.focus()
    } else if (error?.status === 429) {
      errorGeneral.value = TEXTOS.demasiados
    } else {
      errorGeneral.value = TEXTOS.sinConexion
    }
  } finally {
    guardando.value = false
  }
}

// ---------------------------------------------------------------------
// Pedir un código nuevo (mismo endpoint de la solicitud)
// ---------------------------------------------------------------------
const puedeReenviar = computed(() => !reenviando.value && !guardando.value && bloqueoReenvio.value === 0)
const textoReenviar = computed(() =>
  bloqueoReenvio.value > 0 ? TEXTOS.reenviarEn(formatearTiempo(bloqueoReenvio.value * 1000)) : TEXTOS.reenviar
)

async function pedirCodigoNuevo() {
  // Sin correo conocido (p. ej. se abrió la página directo), se va al paso 1.
  if (!correo.value) {
    router.push({ name: 'recuperar-contrasena' })
    return
  }
  if (!puedeReenviar.value) return
  reenviando.value = true
  errorGeneral.value = ''
  aviso.value = ''
  try {
    await solicitarRecuperacion({ correo: correo.value })
    enviadoEn.value = Date.now()
    ahora.value = Date.now()
    window.history.replaceState({ ...window.history.state, enviadoEn: enviadoEn.value }, '')
    form.value.token = ''
    errores.value.token = ''
    estado.value = 'normal'
    aviso.value = TEXTOS.reenviado
    bloqueoReenvio.value = SEGUNDOS_BLOQUEO_REENVIO
    await nextTick()
    tokenRef.value?.focus()
  } catch {
    errorGeneral.value = TEXTOS.sinConexion
  } finally {
    reenviando.value = false
  }
}

// ---------------------------------------------------------------------
// Éxito: cuenta regresiva y paso al login
// ---------------------------------------------------------------------
function iniciarRedireccion() {
  segundosRedireccion.value = SEGUNDOS_REDIRECCION
  temporizadorRedireccion = window.setInterval(() => {
    segundosRedireccion.value -= 1
    if (segundosRedireccion.value <= 0) irAlLogin()
  }, 1000)
}

/** Flecha de la barra superior del celular: vuelve al paso 1. */
function atras() {
  if (window.history.state?.back) router.back()
  else router.push({ name: 'recuperar-contrasena', state: { correo: correo.value } })
}

function irAlLogin() {
  window.clearInterval(temporizadorRedireccion)
  router.replace({ name: 'login' })
}
</script>

<template>
  <div class="rec-pagina" :class="{ 'rec-movil': esMovil }">
    <div class="rec">
      <!-- ============================ ÉXITO ============================ -->
      <section v-if="estado === 'exito'" class="rec__tarjeta" aria-labelledby="rec-titulo">
        <header class="rec__cabecera">
          <div class="rec__icono-estado rec__icono-estado--exito" aria-hidden="true">
            <span class="material-symbols-outlined">task_alt</span>
          </div>
          <h1 id="rec-titulo" class="rec__titulo">{{ T.exitoTitulo }}</h1>
          <p class="rec__texto">{{ T.exitoTexto }}</p>
        </header>

        <div class="rec__alerta rec__alerta--exito" role="status">
          <span class="material-symbols-outlined" aria-hidden="true">check_circle</span>
          <div class="rec__flex">
            <strong>{{ T.exitoCaja }}</strong>
            <p>{{ T.exitoRedirige(Math.max(segundosRedireccion, 0)) }}</p>
            <div class="rec__progreso" :style="{ '--rec-duracion': `${SEGUNDOS_REDIRECCION}s` }" aria-hidden="true">
              <span></span>
            </div>
          </div>
        </div>

        <BaseButton class="rec__boton" icon="login" @click="irAlLogin">{{ T.exitoBoton }}</BaseButton>
      </section>

      <!-- ===================== FORMULARIO ===================== -->
      <section v-else class="rec__tarjeta" aria-labelledby="rec-titulo">
        <div class="rec__superior">
          <!-- Celular: barra con flecha y logo -->
          <div v-if="esMovil" class="rec-movil__barra">
            <button type="button" class="rec-movil__volver" :aria-label="T.atras" :disabled="guardando" @click="atras">
              <span class="material-symbols-outlined" aria-hidden="true">arrow_back</span>
            </button>
            <img :src="logo" alt="CampusMarket" class="rec-movil__logo rec-movil__logo--sm" width="28" height="28" />
            <span class="rec-movil__espacio" aria-hidden="true"></span>
          </div>

          <header class="rec__cabecera">
            <img v-if="!esMovil" :src="logo" alt="CampusMarket" class="rec__logo" width="88" height="88" />
            <span v-if="!esMovil" class="rec__chip">
              <span class="material-symbols-outlined" aria-hidden="true">verified_user</span>{{ T.paso }}
            </span>
            <h1 id="rec-titulo" class="rec__titulo">{{ T.titulo }}</h1>
            <p class="rec__texto">
              <template v-if="correo && !esMovil">{{ T.textoConCorreo }}<br /><span class="rec__correo">{{ correo }}</span></template>
              <template v-else-if="esMovil">{{ T.textoConCorreo }}</template>
              <template v-else>{{ T.textoSinCorreo }}</template>
            </p>
          </header>

          <!-- Error arriba: contraseñas distintas (mockup "Claves desiguales") -->
          <div v-if="mostrarBannerNoCoinciden" class="rec__alerta" role="alert">
            <span class="material-symbols-outlined" aria-hidden="true">error</span>
            <p><strong>{{ T.noCoincidenTitulo }}</strong>{{ T.noCoincidenTexto }}</p>
          </div>

          <!-- CA-04-03: token vencido, usado o inválido -->
          <div v-if="estado === 'vencido'" class="rec__alerta rec__alerta--ambar" role="alert">
            <span class="material-symbols-outlined" aria-hidden="true">alarm_off</span>
            <div class="rec__flex">
              <strong>{{ T.vencidoTitulo }}</strong>
              <p>{{ T.vencidoTexto }}</p>
              <BaseButton
                size="sm"
                class="rec__boton"
                icon="send"
                :loading="reenviando"
                :disabled="!puedeReenviar"
                @click="pedirCodigoNuevo"
              >
                {{ bloqueoReenvio > 0 ? textoReenviar : T.vencidoBoton }}
              </BaseButton>
            </div>
          </div>

          <div v-if="errorGeneral" class="rec__alerta" role="alert">
            <span class="material-symbols-outlined" aria-hidden="true">warning</span>
            <p>{{ errorGeneral }}</p>
          </div>
          <p v-if="aviso" class="rec__alerta rec__alerta--exito" role="status">
            <span class="material-symbols-outlined" aria-hidden="true">mark_email_read</span>
            <span>{{ aviso }}</span>
          </p>

          <form id="rec-form-nueva" class="rec__form" novalidate @submit.prevent="guardar">
            <!-- Código de seguridad -->
            <FormField
              id="rec-token"
              :label="T.token"
              :error="errores.token"
              error-icono="error"
              :estado="textoExpira"
              :estado-icono="textoExpira ? 'timer' : ''"
              class="rec__estado-ambar"
            >
              <BaseInput
                ref="tokenRef"
                v-model="form.token"
                icon="key"
                class="rec__token"
                :placeholder="T.tokenEjemplo"
                autocomplete="one-time-code"
                autocapitalize="off"
                spellcheck="false"
                maxlength="80"
                :disabled="guardando"
                @blur="alSalirToken"
                @input="revalidar('token')"
              />
            </FormField>
            <div class="rec__fila-ayuda">
              <p class="rec__ayuda">{{ T.revisaSpam }}</p>
              <p class="rec__ayuda rec__derecha">
                {{ T.noRecibiste }}
                <button
                  type="button"
                  class="rec__enlace rec__enlace--chico"
                  :disabled="!!correo && !puedeReenviar"
                  @click="pedirCodigoNuevo"
                >
                  {{ textoReenviar }}
                </button>
              </p>
            </div>

            <!-- Nueva contraseña -->
            <FormField
              id="rec-password"
              :label="T.password"
              :error="errores.password"
              error-icono="error"
              :estado="nivel.texto && !esMovil ? T.nivel(nivel.texto) : ''"
              :class="nivel.nivel >= 3 ? 'rec__estado-ok' : nivel.texto ? 'rec__estado-mal' : ''"
              ayuda-id="rec-requisitos"
            >
              <div class="rec__con-ojo">
                <BaseInput
                  ref="passwordRef"
                  v-model="form.password"
                  :type="verPassword ? 'text' : 'password'"
                  :icon="esMovil ? '' : 'lock'"
                  :valid="esMovil && nivel.nivel === 4"
                  autocomplete="new-password"
                  placeholder="••••••••••••"
                  :maxlength="PASSWORD_MAX + 8"
                  :disabled="guardando"
                  @input="revalidar('password')"
                />
                <button
                  type="button"
                  class="rec__ojo"
                  :aria-label="verPassword ? T.ocultarPassword : T.verPassword"
                  :aria-pressed="verPassword"
                  @click="verPassword = !verPassword"
                >
                  <span class="material-symbols-outlined" aria-hidden="true">{{ verPassword ? 'visibility_off' : 'visibility' }}</span>
                </button>
              </div>
            </FormField>
            <div v-if="!esMovil" class="rec__barra" :class="`rec__barra--${nivel.nivel}`" aria-hidden="true">
              <span :style="{ width: `${nivel.nivel * 25}%` }"></span>
            </div>

            <div id="rec-requisitos" class="rec__requisitos">
              <p v-if="!esMovil" class="rec__requisitos-titulo">{{ T.requisitos }}</p>
              <p v-else class="sr-only">{{ T.requisitos }}</p>
              <ul>
                <li v-for="regla in reglas" :key="regla.id" :class="{ rec__ok: regla.cumple }">
                  <span class="material-symbols-outlined" aria-hidden="true">{{
                    regla.cumple ? 'check_circle' : esMovil ? 'circle' : 'radio_button_unchecked'
                  }}</span>
                  <span>{{ regla.texto }}</span>
                  <span class="sr-only">{{ regla.cumple ? '(cumplido)' : '(pendiente)' }}</span>
                </li>
              </ul>
            </div>

            <!-- Confirmación -->
            <FormField
              id="rec-confirmacion"
              :label="T.confirmacion"
              :error="errores.confirmacion && !mostrarBannerNoCoinciden ? errores.confirmacion : ''"
              error-icono="error"
              :estado="hayConfirmacion ? (coinciden ? T.coinciden : T.noCoinciden) : ''"
              :estado-icono="hayConfirmacion ? (coinciden ? 'done_all' : 'close') : ''"
              :class="hayConfirmacion ? (coinciden ? 'rec__estado-ok' : 'rec__estado-mal') : ''"
            >
              <div class="rec__con-ojo">
                <BaseInput
                  ref="confirmacionRef"
                  v-model="form.confirmacion"
                  :type="verPassword ? 'text' : 'password'"
                  :icon="esMovil ? '' : 'lock_reset'"
                  :class="{ 'rec__input-verde': esMovil && coinciden }"
                  :invalid="mostrarBannerNoCoinciden || (esMovil && hayConfirmacion && !coinciden)"
                  autocomplete="new-password"
                  placeholder="••••••••••••"
                  :maxlength="PASSWORD_MAX + 8"
                  :disabled="guardando"
                  @input="revalidar('confirmacion')"
                />
                <button
                  type="button"
                  class="rec__ojo"
                  :aria-label="verPassword ? T.ocultarPassword : T.verPassword"
                  :aria-pressed="verPassword"
                  @click="verPassword = !verPassword"
                >
                  <span class="material-symbols-outlined" aria-hidden="true">{{ verPassword ? 'visibility_off' : 'visibility' }}</span>
                </button>
              </div>
            </FormField>

            <BaseButton v-if="!esMovil" type="submit" class="rec__boton" icon="arrow_forward" :loading="guardando">
              {{ guardando ? T.botonCargando : T.boton }}
            </BaseButton>
          </form>
        </div>

        <!-- Celular: acciones abajo (mockup "Guardar..." / "Cancelar y regresar") -->
        <div v-if="esMovil" class="rec-movil__inferior">
          <BaseButton type="submit" form="rec-form-nueva" class="rec__boton" :loading="guardando">
            <span v-if="!guardando" class="material-symbols-outlined rec-movil__icono-boton" aria-hidden="true">lock</span>
            {{ guardando ? T.botonCargando : T.boton }}
          </BaseButton>
          <RouterLink :to="{ name: 'login' }" class="rec-movil__cancelar">{{ T.cancelar }}</RouterLink>
        </div>

        <template v-else>
          <div class="rec__centro">
            <RouterLink :to="{ name: 'login' }" class="rec__enlace">
              <span class="material-symbols-outlined" aria-hidden="true">arrow_back</span>{{ T.volver }}
            </RouterLink>
          </div>

          <div class="rec__caja">
            <span class="material-symbols-outlined" aria-hidden="true">devices</span>
            <p>{{ T.sesiones }}</p>
          </div>
        </template>
      </section>
    </div>
  </div>
</template>
