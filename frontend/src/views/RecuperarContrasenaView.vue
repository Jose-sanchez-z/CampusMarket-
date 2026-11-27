<script setup>
// =====================================================================
// Recuperar contraseña, paso 1: solicitar el código (S2-T03, HU-04, E04).
// Diseño:
//   - Computador: frontend/Diseno/Sprint2/"Recuperación de contraseña (HU-04).png"
//     (estados: inicial, cargando, éxito CA-04-01 y error de formato).
//   - Celular (≤ 767 px): mockups de 390 px del zip de Yari
//     ("RECUPERAR CONTRASEÑA · RESET_PASSWORD" y "PANTALLA 1: Correo enviado").
// API (S2-T09, E07):
//   POST /auth/recuperacion/solicitar { correo } -> 200 siempre igual | 400
// Reglas:
//  - CA-04-01: tras solicitar se muestra SIEMPRE el mismo mensaje, exista
//    o no el correo. Nunca se dice "este correo no está registrado".
//  - Sin códigos técnicos en pantalla (directriz del diseño).
// Ajustes frente al mockup, por orden del Scrum Master (se manda el backend):
//  - El código vence en 30 min (no 15) y es un texto largo, no un enlace.
//  - Se acepta cualquier correo (decisión del Sprint 1), no solo @gmail.com.
//  - Celular: "Enviamos las instrucciones a" pasa a "Si el correo está
//    registrado, enviamos el código a" para no confirmar que la cuenta existe.
// =====================================================================
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { FormField, BaseInput, BaseButton } from '@/components/ui'
import { solicitarRecuperacion } from '@/services/authService'
import { validarCorreo, normalizarCorreo, CORREO_MAX } from '@/utils/validaciones'
import { formatearTiempo, tiempoRestante } from '@/utils/codigo'
import { useEsMovil } from '@/composables/useEsMovil'
import logo from '@/assets/img/logo-claro.png'
import '@/assets/styles/recuperacion.css'

const router = useRouter()
const esMovil = useEsMovil()

const DOMINIO = import.meta.env.VITE_DOMINIO_INSTITUCIONAL || ''
const MINUTOS_VIGENCIA = Number(import.meta.env.VITE_RECUPERACION_MINUTOS) || 30
const SEGUNDOS_BLOQUEO_REENVIO = Number(import.meta.env.VITE_REENVIO_SEGUNDOS) || 60

// Textos del mockup de computador (corregidos donde chocaban con el backend).
const TEXTOS = {
  titulo: 'Recuperar contraseña',
  texto: 'Ingresa tu correo personal asociado a tu cuenta de CampusMarket para restablecer el acceso de forma segura.',
  correo: 'Correo electrónico',
  correoPista: DOMINIO ? `Ej: @${DOMINIO}` : 'Ej: @gmail.com',
  correoEjemplo: DOMINIO ? `estudiante@${DOMINIO}` : 'estudiante.cm@gmail.com',
  ayuda: `Te enviaremos un código de un solo uso válido por ${MINUTOS_VIGENCIA} minutos.`,
  boton: 'Enviar código',
  botonCargando: 'Enviando...',
  volver: 'Volver a iniciar sesión',
  seguroTitulo: 'Conexión protegida',
  seguroTexto: 'Protección contra filtración de identidad estudiantil.',
  consejoTitulo: 'Consejo de seguridad CampusMarket',
  consejoTexto:
    'Ningún administrador o delegado de CampusMarket te solicitará tu código de recuperación ni tu contraseña por WhatsApp, correo ni llamadas. Mantén tus credenciales protegidas.',
  // Estado de éxito (CA-04-01): mismo mensaje exista o no el correo
  enviadoTitulo: '¡Revisa tu correo electrónico!',
  enviadoTexto: 'Si el correo está registrado, te enviamos un código para crear una nueva contraseña a',
  enviadoVigencia: `El código es de un solo uso y vence en ${MINUTOS_VIGENCIA} minutos por motivos de seguridad.`,
  tengoCodigo: 'Ya tengo el código',
  reenviar: 'Reenviar código',
  reenviarEn: (t) => `Reenviar código (disponible en ${t})`,
  reenviado: 'Listo. Si el correo está registrado, te llegará un código nuevo y el anterior dejará de servir.',
  otroCorreo: 'Usar otro correo',
  noLlegaTitulo: '¿No lo encuentras?',
  noLlegaTexto:
    'Revisa tu carpeta de spam, promociones o correo no deseado. Por seguridad solo se envían 3 códigos por hora a un mismo correo.',
  // Errores
  formato: 'Ingresa un correo electrónico válido.',
  sinConexion: 'Ocurrió una interrupción temporal de conexión. Por favor reintenta en unos instantes.',
  // Accesibilidad
  atras: 'Volver'
}

// Textos del mockup de celular (390 px)
const TEXTOS_MOVIL = {
  texto: 'Ingresa tu correo electrónico y te enviaremos un código para restablecer tu contraseña.',
  boton: 'Enviar código de recuperación',
  enviadoTitulo: '¡Revisa tu correo!',
  enviadoTexto: 'Si el correo está registrado, enviamos el código a:',
  validoPor: 'Código válido por:',
  minutos: 'min',
  spam: '¿No lo ves? Chequea tu carpeta de correo spam o promociones.',
  abrirGmail: 'Abrir Gmail',
  reenviar: 'Reenviar código',
  reenviarEn: (t) => `Reenviar código (${t})`
}

const T = computed(() => (esMovil.value ? { ...TEXTOS, ...TEXTOS_MOVIL } : TEXTOS))

// 'inicial' | 'enviado'
const estado = ref('inicial')
const correo = ref(typeof window.history.state?.correo === 'string' ? window.history.state.correo : '')
const tocado = ref(false)
const errorCorreo = ref('')
const errorGeneral = ref('')
const aviso = ref('')
const enviando = ref(false)
const correoEnviado = ref('')
const enviadoEn = ref(0)
const bloqueoReenvio = ref(0)
const correoRef = ref(null)
const ahora = ref(Date.now())

let intervalo = null
onMounted(() => {
  intervalo = window.setInterval(() => {
    ahora.value = Date.now()
    if (bloqueoReenvio.value > 0) bloqueoReenvio.value -= 1
  }, 1000)
  correoRef.value?.focus()
})
onBeforeUnmount(() => window.clearInterval(intervalo))

const correoValido = computed(() => tocado.value && !validarCorreo(correo.value, DOMINIO))
const iconoCorreo = computed(() => (errorCorreo.value ? 'warning' : correoValido.value ? 'check_circle' : ''))

// Celular: contador "Código válido por: 29:59 min"
const textoVigencia = computed(() =>
  formatearTiempo(tiempoRestante(enviadoEn.value || Date.now(), MINUTOS_VIGENCIA, ahora.value))
)
// Celular: "Abrir Gmail" solo si el correo es de Gmail (como el mockup)
const esGmail = computed(() => correoEnviado.value.endsWith('@gmail.com'))

function alSalir() {
  if (!correo.value.trim()) return
  tocado.value = true
  errorCorreo.value = validarCorreo(correo.value, DOMINIO) ? TEXTOS.formato : ''
}

function alEscribir() {
  errorGeneral.value = ''
  if (errorCorreo.value) errorCorreo.value = validarCorreo(correo.value, DOMINIO) ? TEXTOS.formato : ''
}

/** Llama a la API. Devuelve true si respondió 200 (mensaje genérico). */
async function pedirCodigo(destino) {
  errorGeneral.value = ''
  enviando.value = true
  try {
    await solicitarRecuperacion({ correo: destino })
    return true
  } catch (error) {
    if (error?.status === 400) {
      errorCorreo.value = TEXTOS.formato
      if (estado.value === 'enviado') estado.value = 'inicial'
    } else {
      // Sin conexión o error inesperado: mensaje amigable, sin códigos técnicos.
      errorGeneral.value = TEXTOS.sinConexion
    }
    return false
  } finally {
    enviando.value = false
  }
}

async function enviar() {
  if (enviando.value) return
  tocado.value = true
  aviso.value = ''
  errorCorreo.value = validarCorreo(correo.value, DOMINIO) ? TEXTOS.formato : ''
  if (errorCorreo.value) {
    correoRef.value?.focus()
    return
  }
  const destino = normalizarCorreo(correo.value)
  if (await pedirCodigo(destino)) {
    // CA-04-01: siempre el mismo estado, exista o no el correo.
    correoEnviado.value = destino
    enviadoEn.value = Date.now()
    ahora.value = Date.now()
    bloqueoReenvio.value = SEGUNDOS_BLOQUEO_REENVIO
    estado.value = 'enviado'
  }
}

const puedeReenviar = computed(() => !enviando.value && bloqueoReenvio.value === 0)
const textoReenviar = computed(() =>
  bloqueoReenvio.value > 0 ? T.value.reenviarEn(formatearTiempo(bloqueoReenvio.value * 1000)) : T.value.reenviar
)

async function reenviar() {
  if (!puedeReenviar.value) return
  aviso.value = ''
  if (await pedirCodigo(correoEnviado.value)) {
    enviadoEn.value = Date.now()
    ahora.value = Date.now()
    bloqueoReenvio.value = SEGUNDOS_BLOQUEO_REENVIO
    aviso.value = TEXTOS.reenviado
  }
}

function irANuevaContrasena() {
  // El correo y la hora de envío viajan en history.state (no en la URL ni en localStorage).
  router.push({ name: 'nueva-contrasena', state: { correo: correoEnviado.value, enviadoEn: enviadoEn.value } })
}

function usarOtroCorreo() {
  aviso.value = ''
  errorGeneral.value = ''
  estado.value = 'inicial'
}

/** Flecha de la barra superior del celular. */
function atras() {
  if (estado.value === 'enviado') usarOtroCorreo()
  else router.push({ name: 'login' })
}
</script>

<template>
  <div class="rec-pagina" :class="{ 'rec-movil': esMovil }">
    <div class="rec">
      <!-- ===================== PASO 1: FORMULARIO ===================== -->
      <section v-if="estado === 'inicial'" class="rec__tarjeta" aria-labelledby="rec-titulo">
        <div class="rec__superior">
          <!-- Celular: barra con flecha y logo -->
          <div v-if="esMovil" class="rec-movil__barra">
            <button type="button" class="rec-movil__volver" :aria-label="T.atras" :disabled="enviando" @click="atras">
              <span class="material-symbols-outlined" aria-hidden="true">arrow_back</span>
            </button>
            <img :src="logo" alt="CampusMarket" class="rec-movil__logo" width="32" height="32" />
            <span class="rec-movil__espacio" aria-hidden="true"></span>
          </div>

          <header class="rec__cabecera">
            <img v-if="!esMovil" :src="logo" alt="CampusMarket" class="rec__logo" width="88" height="88" />
            <h1 id="rec-titulo" class="rec__titulo">{{ T.titulo }}</h1>
            <p class="rec__texto">{{ T.texto }}</p>
          </header>

          <div v-if="errorGeneral" class="rec__alerta" role="alert">
            <span class="material-symbols-outlined" aria-hidden="true">wifi_off</span>
            <p>{{ errorGeneral }}</p>
          </div>

          <form id="rec-form-correo" class="rec__form" novalidate @submit.prevent="enviar">
            <FormField
              id="rec-correo"
              :label="T.correo"
              :error="errorCorreo"
              error-icono="error"
              :estado="esMovil ? '' : T.correoPista"
            >
              <BaseInput
                ref="correoRef"
                v-model="correo"
                type="email"
                :icon="esMovil ? 'mail' : 'alternate_email'"
                :icon-right="iconoCorreo"
                :valid="correoValido && !errorCorreo"
                autocomplete="email"
                inputmode="email"
                :maxlength="CORREO_MAX"
                :placeholder="T.correoEjemplo"
                :disabled="enviando"
                @blur="alSalir"
                @input="alEscribir"
              />
            </FormField>
            <p v-if="!esMovil" class="rec__ayuda">{{ T.ayuda }}</p>

            <BaseButton v-if="!esMovil" type="submit" class="rec__boton" icon="arrow_forward" :loading="enviando">
              {{ enviando ? T.botonCargando : T.boton }}
            </BaseButton>
          </form>
        </div>

        <!-- Celular: acciones abajo -->
        <div v-if="esMovil" class="rec-movil__inferior">
          <BaseButton type="submit" form="rec-form-correo" class="rec__boton" :loading="enviando">
            {{ enviando ? T.botonCargando : T.boton }}
          </BaseButton>
          <div class="rec__centro">
            <RouterLink :to="{ name: 'login' }" class="rec__enlace">
              <span class="material-symbols-outlined" aria-hidden="true">arrow_back</span>{{ T.volver }}
            </RouterLink>
          </div>
        </div>

        <!-- Computador: volver y caja de conexión protegida -->
        <template v-else>
          <div class="rec__centro">
            <RouterLink :to="{ name: 'login' }" class="rec__enlace">
              <span class="material-symbols-outlined" aria-hidden="true">arrow_back</span>{{ T.volver }}
            </RouterLink>
          </div>

          <div class="rec__caja">
            <span class="material-symbols-outlined" aria-hidden="true">lock</span>
            <p><strong>{{ T.seguroTitulo }}</strong>{{ T.seguroTexto }}</p>
          </div>
        </template>
      </section>

      <!-- ============ PASO 1: CÓDIGO ENVIADO (CA-04-01, genérico) ============ -->
      <!-- Celular: "PANTALLA 1: Correo enviado" (390 px) -->
      <section v-else-if="esMovil" class="rec__tarjeta" aria-labelledby="rec-titulo">
        <div class="rec__superior">
          <div class="rec-movil__barra">
            <button type="button" class="rec-movil__volver" :aria-label="T.atras" @click="atras">
              <span class="material-symbols-outlined" aria-hidden="true">arrow_back</span>
            </button>
            <img :src="logo" alt="CampusMarket" class="rec-movil__logo" width="32" height="32" />
            <span class="rec-movil__espacio" aria-hidden="true"></span>
          </div>

          <div class="rec-movil__enviado" role="status">
            <div class="rec-movil__ilustracion" aria-hidden="true">
              <span class="material-symbols-outlined">mark_email_read</span>
              <span class="rec-movil__insignia material-symbols-outlined">check</span>
            </div>
            <h1 id="rec-titulo" class="rec__titulo">{{ T.enviadoTitulo }}</h1>
            <p class="rec__texto">
              {{ T.enviadoTexto }}<br />
              <strong class="rec__correo">{{ correoEnviado }}</strong>
            </p>
          </div>

          <div class="rec-movil__pildora">
            <span class="material-symbols-outlined" aria-hidden="true">timelapse</span>
            <span>{{ T.validoPor }} <strong class="rec-movil__tiempo">{{ textoVigencia }} {{ T.minutos }}</strong></span>
          </div>

          <div class="rec-movil__nota">
            <span class="material-symbols-outlined" aria-hidden="true">help_outline</span>
            <p>{{ T.spam }}</p>
          </div>

          <p v-if="aviso" class="rec__alerta rec__alerta--exito" role="status">
            <span class="material-symbols-outlined" aria-hidden="true">mark_email_read</span>
            <span>{{ aviso }}</span>
          </p>
          <div v-if="errorGeneral" class="rec__alerta" role="alert">
            <span class="material-symbols-outlined" aria-hidden="true">wifi_off</span>
            <p>{{ errorGeneral }}</p>
          </div>
        </div>

        <div class="rec-movil__inferior">
          <BaseButton class="rec__boton" icon="arrow_forward" @click="irANuevaContrasena">{{ T.tengoCodigo }}</BaseButton>
          <a
            v-if="esGmail"
            class="rec-movil__secundario"
            href="https://mail.google.com"
            target="_blank"
            rel="noopener noreferrer"
          >
            <span class="material-symbols-outlined" aria-hidden="true">mail</span>
            <span>{{ T.abrirGmail }}</span>
          </a>
          <button type="button" class="rec-movil__reenviar" :disabled="!puedeReenviar" @click="reenviar">
            <span class="material-symbols-outlined" aria-hidden="true">refresh</span>{{ textoReenviar }}
          </button>
        </div>
      </section>

      <!-- Computador: estado de código enviado -->
      <section v-else class="rec__tarjeta" aria-labelledby="rec-titulo" role="status">
        <header class="rec__cabecera">
          <div class="rec__icono-estado" aria-hidden="true">
            <span class="material-symbols-outlined">mark_email_read</span>
          </div>
          <h1 id="rec-titulo" class="rec__titulo">{{ T.enviadoTitulo }}</h1>
          <p class="rec__texto">
            {{ T.enviadoTexto }}<br />
            <span class="rec__correo">{{ correoEnviado }}</span>
          </p>
        </header>

        <div class="rec__caja">
          <span class="material-symbols-outlined" aria-hidden="true">schedule</span>
          <p>{{ T.enviadoVigencia }}</p>
        </div>

        <p v-if="aviso" class="rec__alerta rec__alerta--exito" role="status">
          <span class="material-symbols-outlined" aria-hidden="true">mark_email_read</span>
          <span>{{ aviso }}</span>
        </p>
        <div v-if="errorGeneral" class="rec__alerta" role="alert">
          <span class="material-symbols-outlined" aria-hidden="true">wifi_off</span>
          <p>{{ errorGeneral }}</p>
        </div>

        <div class="rec__form">
          <BaseButton class="rec__boton" icon="arrow_forward" @click="irANuevaContrasena">{{ T.tengoCodigo }}</BaseButton>
          <BaseButton
            variant="secondary"
            class="rec__boton"
            icon="sync"
            :loading="enviando"
            :disabled="!puedeReenviar"
            @click="reenviar"
          >
            {{ textoReenviar }}
          </BaseButton>
        </div>

        <div class="rec__centro rec__centro--doble">
          <RouterLink :to="{ name: 'login' }" class="rec__enlace">
            <span class="material-symbols-outlined" aria-hidden="true">arrow_back</span>{{ T.volver }}
          </RouterLink>
          <button type="button" class="rec__enlace" @click="usarOtroCorreo">
            <span class="material-symbols-outlined" aria-hidden="true">edit</span>{{ T.otroCorreo }}
          </button>
        </div>

        <div class="rec__caja">
          <span class="material-symbols-outlined" aria-hidden="true">info</span>
          <p><strong>{{ T.noLlegaTitulo }}</strong>{{ T.noLlegaTexto }}</p>
        </div>
      </section>

      <!-- Consejo de seguridad bajo la tarjeta (solo computador, como el mockup) -->
      <aside v-if="!esMovil" class="rec__caja rec__consejo">
        <span class="material-symbols-outlined" aria-hidden="true">lightbulb</span>
        <p><strong>{{ T.consejoTitulo }}</strong>{{ T.consejoTexto }}</p>
      </aside>
    </div>
  </div>
</template>
