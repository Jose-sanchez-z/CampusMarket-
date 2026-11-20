<script setup>
// =====================================================================
// Pantalla de ingreso del código — HU-02 (S1-T04, responsable E04).
// Diseño:
//   - Computador: "boceto compu blanco.png" (P4 OTP y P5 éxito) y
//     "boceto error compu blanco.png" (código incorrecto / expirado).
//   - Celular (≤ 767 px): "boceto cel blanco.png" (P1.4 y Grupo 4:
//     comprobando, código incorrecto, código vencido y verificado).
// API (S1-T10, E07):
//   POST /auth/verificar     { correo, token } -> 200 | 422 | 429 | 400
//   POST /auth/reenviartoken { correo }        -> 200 | 429
// Reglas:
//  - CA-02-01: con el código correcto la cuenta queda ACTIVA.
//  - CA-02-02: un código vencido o usado muestra un error controlado y la
//    cuenta no se activa.
//  - El backend responde 422 igual para "incorrecto" y "vencido"; el
//    frontend los distingue con el contador de 15 minutos.
//  - "Reenviar código" se bloquea unos segundos para evitar abusos.
//  - Nada se guarda en localStorage: la hora de envío viaja en history.state.
// =====================================================================
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { verificarCuenta, reenviarCodigo } from '@/services/authService'
import {
  LARGO_CODIGO,
  soloDigitos,
  unirCodigo,
  codigoCompleto,
  repartirDigitos,
  tiempoRestante,
  formatearTiempo
} from '@/utils/codigo'
import { useEsMovil } from '@/composables/useEsMovil'
import logo from '@/assets/img/logo-claro.png'

const route = useRoute()
const router = useRouter()
const esMovil = useEsMovil()

// Vigencia del código (backend: campusmarket.token.minutos-vigencia = 15).
const MINUTOS_VIGENCIA = Number(import.meta.env.VITE_TOKEN_MINUTOS) || 15
// Bloqueo del botón "Reenviar código" tras usarlo (anti-spam).
const SEGUNDOS_BLOQUEO_REENVIO = Number(import.meta.env.VITE_REENVIO_SEGUNDOS) || 60
// Celular: espera antes de ir al login tras verificar ("en 2 segundos").
const MS_REDIRECCION = 2000

// ---------------------------------------------------------------------
// Textos copiados de los mockups. Los de celular están en TEXTOS_MOVIL.
// ---------------------------------------------------------------------
const TEXTOS = {
  volver: 'Volver',
  paso: 'Paso 4 / 5',
  pasoExito: 'Paso 5 / 5',
  titulo: 'Verifica tu correo electrónico',
  enviadoA: 'Hemos enviado un código seguro de 6 dígitos a:',
  expiraEn: 'El código expirará en:',
  minutos: 'min',
  boton: 'Verificar cuenta y acceder',
  botonCargando: 'Comprobando código...',
  noRecibiste: '¿No recibiste el correo?',
  reenviar: 'Reenviar código',
  // Código incorrecto ("boceto error compu blanco.png")
  incorrectoTitulo: 'El código de verificación no es válido',
  incorrectoNoCoincide: 'El código ingresado no coincide con el enviado a:',
  incorrectoCaja: 'Código de verificación incorrecto',
  incorrectoTexto: 'Código incorrecto. Verifica los dígitos enviados a tu correo e intenta nuevamente.',
  incorrectoBoton: 'Reenviar Código',
  // Código expirado
  expiradoTitulo: 'Código Expirado',
  expiradoCaja: 'Tiempo límite superado',
  expiradoTexto: 'El código de seguridad ha caducado por límite de tiempo. Por tu seguridad, solicita uno nuevo.',
  expiradoBoton: 'Solicitar nuevo código',
  // Éxito (P5)
  exitoTitulo: '¡Tu cuenta ha sido verificada con éxito!',
  exitoTexto:
    'Ya formas parte de la red de intercambio académico de CampusMarket con identidad institucional confirmada.',
  exitoChip: 'Estudiante Activo 2026',
  exitoEntrar: 'Ir al inicio / Entrar a la tienda',
  exitoPerfil: 'Completar mi perfil de vendedor',
  // Mensajes propios (estados que el mockup no dibuja)
  faltanDigitos: 'Ingresa los 6 dígitos del código.',
  reenviado: 'Te enviamos un nuevo código. Revisa tu correo (también la carpeta de spam).',
  sinConexion: 'No pudimos conectar con el servidor. Verifica tu conexión e intenta de nuevo.',
  sinCorreoTitulo: 'No sabemos a qué correo enviamos tu código',
  sinCorreoTexto: 'Vuelve al registro para solicitar un código nuevo.',
  sinCorreoBoton: 'Volver al registro',
  digito: (n) => `Dígito ${n} de ${LARGO_CODIGO}`
}

const TEXTOS_MOVIL = {
  titulo: 'Verifica tu correo',
  enviadoA: 'Hemos enviado un código seguro de 6 dígitos a',
  expiraEn: 'El código expirará en',
  minutos: '',
  boton: 'Verificar cuenta',
  // Comprobando (BUSY_VERIFY)
  validandoTitulo: 'Validando',
  validandoTexto: 'Comprobando código criptográfico...',
  validandoPie: 'Sincronizando con el servidor central...',
  noRecibisteValidando: '¿No recibiste el código?',
  // Incorrecto (ERR_BAD_OTP)
  incorrectoTitulo: 'Verificación',
  incorrectoNoCoincide: 'Ingresa los 6 dígitos enviados a tu buzón.',
  incorrectoTexto: 'Código incorrecto. Verifica los dígitos e intenta nuevamente.',
  incorrectoBoton: 'Verificar cuenta',
  dificultades: '¿Dificultades?',
  // Vencido (ERR_EXPIRED)
  expiradoTitulo: 'Tiempo Expirado',
  expiradoCaja: 'Límite alcanzado',
  expiradoTexto: `El código de seguridad ha caducado por límite de tiempo (${MINUTOS_VIGENCIA} min). Por tu seguridad, los códigos temporales dejan de ser válidos tras ese lapso.`,
  codigoVencido: '¿Código vencido?',
  // Verificado (REDIRECT_STORE)
  exitoTitulo: '¡Código Verificado!',
  exitoTexto: 'Validación completada con éxito. Redirigiendo automáticamente a CampusMarket en 2 segundos...',
  exitoCargando: 'Cargando catálogo universitario...',
  exitoEntrar: 'Entrar ahora'
}

const T = computed(() => (esMovil.value ? { ...TEXTOS, ...TEXTOS_MOVIL } : TEXTOS))

// ---------------------------------------------------------------------
// Estado
// ---------------------------------------------------------------------
const correo = computed(() => (typeof route.query.correo === 'string' ? route.query.correo.trim().toLowerCase() : ''))
const nombre = ref(window.history.state?.nombre || '')

// 'normal' | 'validando' | 'incorrecto' | 'expirado' | 'verificado'
const estado = ref('normal')
const digitos = ref(Array(LARGO_CODIGO).fill(''))
const casillas = ref([])
const errorGeneral = ref('') // 429, sin conexión, 400...
const faltanDigitos = ref(false)
const aviso = ref('') // confirmación de reenvío
const reenviando = ref(false)
const bloqueoReenvio = ref(0) // segundos que faltan para poder reenviar
const casillaActiva = ref(0)

// Contador de vigencia del código
const enviadoEn = ref(Number(window.history.state?.enviadoEn) || Date.now())
const ahora = ref(Date.now())
const restante = computed(() => tiempoRestante(enviadoEn.value, MINUTOS_VIGENCIA, ahora.value))
const textoRestante = computed(() => formatearTiempo(restante.value))

const codigo = computed(() => unirCodigo(digitos.value))
const validando = computed(() => estado.value === 'validando')
const casillasBloqueadas = computed(() => validando.value || estado.value === 'expirado' || !correo.value)

let intervalo = null
let temporizadorRedireccion = null

onMounted(async () => {
  intervalo = window.setInterval(() => {
    ahora.value = Date.now()
    if (bloqueoReenvio.value > 0) bloqueoReenvio.value -= 1
  }, 1000)
  if (restante.value === 0) estado.value = 'expirado'
  await nextTick()
  if (correo.value && estado.value === 'normal') enfocar(0)
})

onBeforeUnmount(() => {
  window.clearInterval(intervalo)
  window.clearTimeout(temporizadorRedireccion)
})

// Si el tiempo se acaba mientras el usuario escribe, el código expira.
watch(restante, (ms) => {
  if (ms === 0 && (estado.value === 'normal' || estado.value === 'incorrecto')) {
    estado.value = 'expirado'
  }
})

// ---------------------------------------------------------------------
// Casillas del código
// ---------------------------------------------------------------------
function enfocar(indice) {
  const i = Math.max(0, Math.min(LARGO_CODIGO - 1, indice))
  casillas.value[i]?.focus()
  casillas.value[i]?.select?.()
}

/** Al cambiar el código después de un error, se vuelve al estado normal. */
function alCambiarCodigo() {
  faltanDigitos.value = false
  errorGeneral.value = ''
  if (estado.value === 'incorrecto') estado.value = 'normal'
}

function alEscribir(indice, evento) {
  const valor = soloDigitos(evento.target.value, LARGO_CODIGO)
  if (valor.length > 1) {
    // Autocompletado del teléfono o texto pegado en una casilla
    const { digitos: nuevos, siguiente } = repartirDigitos(digitos.value, valor, indice)
    digitos.value = nuevos
    alCambiarCodigo()
    enfocar(siguiente)
    return
  }
  const nuevos = [...digitos.value]
  nuevos[indice] = valor
  digitos.value = nuevos
  evento.target.value = valor
  alCambiarCodigo()
  if (valor && indice < LARGO_CODIGO - 1) enfocar(indice + 1)
}

function alPresionarTecla(indice, evento) {
  if (evento.key === 'Backspace' && !digitos.value[indice] && indice > 0) {
    evento.preventDefault()
    const nuevos = [...digitos.value]
    nuevos[indice - 1] = ''
    digitos.value = nuevos
    alCambiarCodigo()
    enfocar(indice - 1)
  } else if (evento.key === 'ArrowLeft') {
    evento.preventDefault()
    enfocar(indice - 1)
  } else if (evento.key === 'ArrowRight') {
    evento.preventDefault()
    enfocar(indice + 1)
  }
}

function alPegar(indice, evento) {
  evento.preventDefault()
  const texto = evento.clipboardData?.getData('text') ?? ''
  const { digitos: nuevos, siguiente } = repartirDigitos(digitos.value, texto, indice)
  digitos.value = nuevos
  alCambiarCodigo()
  enfocar(siguiente)
}

/** Al entrar a una casilla se selecciona su dígito, para que escribir lo reemplace. */
function alEnfocar(indice, evento) {
  casillaActiva.value = indice
  evento.target.select?.()
}

function limpiarCasillas() {
  digitos.value = Array(LARGO_CODIGO).fill('')
}

// ---------------------------------------------------------------------
// Verificar (POST /auth/verificar)
// ---------------------------------------------------------------------
async function verificar() {
  if (validando.value || estado.value === 'expirado' || !correo.value) return
  aviso.value = ''
  errorGeneral.value = ''
  if (!codigoCompleto(codigo.value)) {
    faltanDigitos.value = true
    enfocar(digitos.value.findIndex((d) => !d))
    return
  }

  estado.value = 'validando'
  try {
    await verificarCuenta({ correo: correo.value, token: codigo.value })
    estado.value = 'verificado' // CA-02-01: cuenta ACTIVA
    if (esMovil.value) {
      temporizadorRedireccion = window.setTimeout(irAlLogin, MS_REDIRECCION)
    }
  } catch (error) {
    if (error?.status === 422) {
      // CA-02-02: incorrecto, vencido o usado. Si ya pasó el tiempo, es vencido.
      estado.value = restante.value === 0 ? 'expirado' : 'incorrecto'
    } else if (error?.status === 429) {
      estado.value = 'normal'
      errorGeneral.value = error.datos?.mensaje || 'Demasiados intentos. Intenta de nuevo más tarde.'
    } else if (error?.status === 400) {
      estado.value = 'incorrecto'
    } else {
      estado.value = 'normal'
      errorGeneral.value = error?.status === 0 ? TEXTOS.sinConexion : mensajeInesperado(error)
    }
  }
}

// ---------------------------------------------------------------------
// Reenviar (POST /auth/reenviartoken) con bloqueo anti-spam
// ---------------------------------------------------------------------
const puedeReenviar = computed(
  () => Boolean(correo.value) && !reenviando.value && bloqueoReenvio.value === 0 && !validando.value
)

const textoReenviar = computed(() =>
  bloqueoReenvio.value > 0 ? `${T.value.reenviar} (${formatearTiempo(bloqueoReenvio.value * 1000)})` : T.value.reenviar
)

async function reenviar() {
  if (!puedeReenviar.value) return
  reenviando.value = true
  aviso.value = ''
  errorGeneral.value = ''
  try {
    await reenviarCodigo({ correo: correo.value })
    // Nuevo código: reinicia el contador, limpia las casillas y vuelve a empezar.
    enviadoEn.value = Date.now()
    ahora.value = Date.now()
    window.history.replaceState({ ...window.history.state, enviadoEn: enviadoEn.value }, '')
    limpiarCasillas()
    estado.value = 'normal'
    aviso.value = TEXTOS.reenviado
    bloqueoReenvio.value = SEGUNDOS_BLOQUEO_REENVIO
    await nextTick()
    enfocar(0)
  } catch (error) {
    if (error?.status === 429) {
      errorGeneral.value = error.datos?.mensaje || 'Demasiados intentos. Intenta de nuevo más tarde.'
      bloqueoReenvio.value = SEGUNDOS_BLOQUEO_REENVIO
    } else {
      errorGeneral.value = error?.status === 0 ? TEXTOS.sinConexion : mensajeInesperado(error)
    }
  } finally {
    reenviando.value = false
  }
}

function mensajeInesperado(error) {
  return `Ocurrió un error inesperado${error?.status ? ` (código ${error.status})` : ''}. Intenta de nuevo en unos minutos.`
}

/** Botón principal: verifica, salvo en "incorrecto" (escritorio) y "expirado", donde reenvía. */
function accionPrincipal() {
  if (estado.value === 'expirado' || (estado.value === 'incorrecto' && !esMovil.value)) reenviar()
  else verificar()
}

const textoBotonPrincipal = computed(() => {
  if (validando.value) return T.value.botonCargando
  if (estado.value === 'expirado') return T.value.expiradoBoton
  if (estado.value === 'incorrecto') return T.value.incorrectoBoton
  return T.value.boton
})

const iconoBotonPrincipal = computed(() => {
  if (estado.value === 'expirado') return esMovil.value ? 'refresh' : 'forward_to_inbox'
  if (estado.value === 'incorrecto' && !esMovil.value) return 'refresh'
  return esMovil.value ? '' : 'verified'
})

// ---------------------------------------------------------------------
// Navegación
// ---------------------------------------------------------------------
function volver() {
  if (window.history.state?.back) router.back()
  else router.push({ name: 'registro' })
}

function irAlLogin() {
  window.clearTimeout(temporizadorRedireccion)
  router.push({ name: 'login', query: { correo: correo.value } })
}

// Tarjeta del usuario en el éxito (P5): iniciales del nombre o del correo.
const iniciales = computed(() => {
  const base = nombre.value || correo.value.split('@')[0] || ''
  const partes = base.split(/[\s._-]+/).filter(Boolean)
  return ((partes[0]?.[0] || '') + (partes[1]?.[0] || '')).toUpperCase() || 'CM'
})
</script>

<template>
  <main class="cm-pagina-acceso codigo-pagina" :class="{ 'codigo-movil': esMovil }">
    <!-- ============================ ÉXITO ============================ -->
    <section
      v-if="estado === 'verificado'"
      class="codigo codigo--exito"
      aria-labelledby="titulo-codigo"
      role="status"
    >
      <!-- Celular: ¡Código Verificado! (REDIRECT_STORE) -->
      <template v-if="esMovil">
        <img :src="logo" alt="CampusMarket" class="exito-movil__logo" width="128" height="128" />
        <div class="exito-movil__cuerpo">
          <div class="exito-movil__icono" aria-hidden="true">
            <span class="material-symbols-outlined icono-relleno">check_circle</span>
          </div>
          <h1 id="titulo-codigo" class="codigo__titulo">{{ T.exitoTitulo }}</h1>
          <p class="exito-movil__texto">{{ T.exitoTexto }}</p>
          <div class="exito-movil__barra" aria-hidden="true"><span></span></div>
          <span class="exito-movil__cargando">{{ T.exitoCargando }}</span>
        </div>
        <button type="button" class="cm-boton exito-movil__boton" @click="irAlLogin">
          <span>{{ T.exitoEntrar }}</span>
          <span class="material-symbols-outlined" aria-hidden="true">login</span>
        </button>
      </template>

      <!-- Computador: P5 ¡Tu cuenta ha sido verificada con éxito! -->
      <template v-else>
        <div class="codigo__barra codigo__barra--derecha">
          <span class="codigo__paso">{{ T.pasoExito }}</span>
        </div>
        <div class="codigo__icono codigo__icono--exito" aria-hidden="true">
          <span class="material-symbols-outlined">check_circle</span>
        </div>
        <div class="codigo__textos">
          <h1 id="titulo-codigo" class="codigo__titulo codigo__titulo--xl">{{ T.exitoTitulo }}</h1>
          <p class="codigo__texto codigo__texto--angosto">{{ T.exitoTexto }}</p>
        </div>
        <div class="exito__tarjeta">
          <div class="exito__avatar" aria-hidden="true">{{ iniciales }}</div>
          <div class="exito__datos">
            <span v-if="nombre" class="exito__nombre">{{ nombre }}</span>
            <span class="exito__correo">{{ correo }}</span>
            <span class="exito__chip">{{ T.exitoChip }}</span>
          </div>
        </div>
        <div class="exito__acciones">
          <!-- La tienda requiere sesión: ambos botones llevan al login (S1-T06). -->
          <button type="button" class="cm-boton exito__boton" @click="irAlLogin">
            <span>{{ T.exitoEntrar }}</span>
            <span class="material-symbols-outlined" aria-hidden="true">shopping_bag</span>
          </button>
          <button type="button" class="exito__boton-secundario" @click="irAlLogin">{{ T.exitoPerfil }}</button>
        </div>
      </template>
    </section>

    <!-- ===================== SIN CORREO EN LA URL ===================== -->
    <section v-else-if="!correo" class="codigo" aria-labelledby="titulo-codigo">
      <div class="codigo__icono codigo__icono--gris" aria-hidden="true">
        <span class="material-symbols-outlined">mail</span>
      </div>
      <h1 id="titulo-codigo" class="codigo__titulo">{{ T.sinCorreoTitulo }}</h1>
      <p class="codigo__texto">{{ T.sinCorreoTexto }}</p>
      <RouterLink :to="{ name: 'registro' }" class="cm-boton codigo__boton-enlace">{{ T.sinCorreoBoton }}</RouterLink>
    </section>

    <!-- ===================== FORMULARIO DEL CÓDIGO ===================== -->
    <section v-else class="codigo" :class="`codigo--${estado}`" aria-labelledby="titulo-codigo">
      <div class="codigo__superior">
        <!-- Barra superior -->
        <div v-if="esMovil" class="movil__barra">
          <button type="button" class="movil__volver" :aria-label="T.volver" :disabled="validando" @click="volver">
            <span class="material-symbols-outlined" aria-hidden="true">arrow_back</span>
          </button>
          <img :src="logo" alt="CampusMarket" class="movil__logo" width="32" height="32" />
          <span class="movil__espacio" aria-hidden="true"></span>
        </div>
        <div v-else class="codigo__barra">
          <button type="button" class="codigo__volver" :disabled="validando" @click="volver">
            <span class="material-symbols-outlined" aria-hidden="true">arrow_back</span>
            <span>{{ T.volver }}</span>
          </button>
          <span v-if="estado === 'normal' || validando" class="codigo__paso">{{ T.paso }}</span>
        </div>

        <!-- Icono -->
        <div
          v-if="esMovil || estado === 'normal' || validando || estado === 'expirado'"
          class="codigo__icono"
          :class="{
            'codigo__icono--rojo': estado === 'incorrecto',
            'codigo__icono--ambar': estado === 'expirado' && esMovil,
            'codigo__icono--gris': estado === 'expirado' && !esMovil
          }"
          aria-hidden="true"
        >
          <span class="material-symbols-outlined">{{
            estado === 'expirado'
              ? esMovil ? 'hourglass_disabled' : 'hourglass_empty'
              : estado === 'incorrecto'
                ? 'lock_reset'
                : validando && esMovil
                  ? 'lock_clock'
                  : esMovil ? 'mark_email_unread' : 'mark_email_read'
          }}</span>
        </div>

        <!-- Título y texto -->
        <div class="codigo__textos">
          <h1 id="titulo-codigo" class="codigo__titulo">
            {{
              estado === 'expirado'
                ? T.expiradoTitulo
                : estado === 'incorrecto'
                  ? T.incorrectoTitulo
                  : validando && esMovil
                    ? T.validandoTitulo
                    : T.titulo
            }}
          </h1>
          <p v-if="validando && esMovil" class="codigo__texto">{{ T.validandoTexto }}</p>
          <p v-else-if="estado === 'incorrecto'" class="codigo__texto">
            {{ T.incorrectoNoCoincide }}
            <template v-if="!esMovil"><br /><strong class="codigo__correo">{{ correo }}</strong></template>
          </p>
          <p v-else-if="estado !== 'expirado'" class="codigo__texto">
            {{ T.enviadoA }}<br />
            <strong class="codigo__correo">{{ correo }}</strong>
          </p>
        </div>

        <!-- Escritorio: caja de "código incorrecto" arriba de las casillas -->
        <div v-if="estado === 'incorrecto' && !esMovil" class="caja caja--error" role="alert">
          <span class="material-symbols-outlined caja__icono" aria-hidden="true">error</span>
          <div>
            <p class="caja__titulo">{{ T.incorrectoCaja }}</p>
            <p class="caja__texto">{{ T.incorrectoTexto }}</p>
          </div>
        </div>

        <!-- Caja de "código expirado" -->
        <div v-if="estado === 'expirado'" class="caja" :class="esMovil ? 'caja--ambar' : 'caja--gris'" role="alert">
          <template v-if="esMovil">
            <p class="caja__titulo caja__titulo--con-icono">
              <span class="material-symbols-outlined" aria-hidden="true">schedule</span>{{ T.expiradoCaja }}
            </p>
            <p class="caja__texto">{{ T.expiradoTexto }}</p>
          </template>
          <template v-else>
            <span class="material-symbols-outlined caja__icono" aria-hidden="true">schedule</span>
            <div>
              <p class="caja__titulo">{{ T.expiradoCaja }}</p>
              <p class="caja__texto">{{ T.expiradoTexto }}</p>
            </div>
          </template>
        </div>

        <!-- Casillas del código -->
        <form class="codigo__form" novalidate @submit.prevent="accionPrincipal">
          <div
            class="casillas"
            :class="{
              'casillas--error': estado === 'incorrecto',
              'casillas--expirado': estado === 'expirado',
              'casillas--validando': validando
            }"
            role="group"
            aria-labelledby="titulo-codigo"
          >
            <input
              v-for="(digito, i) in digitos"
              :key="i"
              :ref="(el) => (casillas[i] = el)"
              :value="estado === 'expirado' && !esMovil ? '' : digito"
              class="casilla"
              :class="{ 'casilla--activa': esMovil && casillaActiva === i && estado === 'normal' }"
              type="text"
              inputmode="numeric"
              pattern="[0-9]*"
              maxlength="6"
              :placeholder="estado === 'expirado' && !esMovil ? '-' : ''"
              :autocomplete="i === 0 ? 'one-time-code' : 'off'"
              :aria-label="T.digito(i + 1)"
              :aria-invalid="estado === 'incorrecto'"
              :disabled="casillasBloqueadas"
              @input="alEscribir(i, $event)"
              @keydown="alPresionarTecla(i, $event)"
              @paste="alPegar(i, $event)"
              @focus="alEnfocar(i, $event)"
            />
          </div>

          <p v-if="faltanDigitos" class="codigo__falta" role="alert">{{ T.faltanDigitos }}</p>

          <!-- Contador (computador: píldora / celular: texto) -->
          <template v-if="estado === 'normal' || (validando && !esMovil)">
            <div v-if="!esMovil" class="contador" aria-live="off">
              <span class="material-symbols-outlined" aria-hidden="true">timer</span>
              <span>{{ T.expiraEn }} <strong>{{ textoRestante }} {{ T.minutos }}</strong></span>
            </div>
            <span v-else class="contador-movil">{{ T.expiraEn }} <strong>{{ textoRestante }}</strong></span>
          </template>

          <!-- Celular: enlace de reenvío bajo las casillas -->
          <p v-if="esMovil" class="reenvio">
            <span>{{
              validando
                ? T.noRecibisteValidando
                : estado === 'incorrecto'
                  ? T.dificultades
                  : estado === 'expirado'
                    ? T.codigoVencido
                    : T.noRecibiste
            }}</span>
            <button type="button" class="reenvio__enlace" :disabled="!puedeReenviar" @click="reenviar">
              {{ textoReenviar }}
            </button>
          </p>

          <!-- Celular: caja roja de "código incorrecto" bajo las casillas -->
          <div v-if="estado === 'incorrecto' && esMovil" class="caja caja--error-movil" role="alert">
            <span class="material-symbols-outlined" aria-hidden="true">warning</span>
            <span>{{ T.incorrectoTexto }}</span>
          </div>

          <!-- Avisos: reenvío correcto o error general (429, sin conexión) -->
          <p v-if="aviso" class="codigo__aviso" role="status">
            <span class="material-symbols-outlined" aria-hidden="true">mark_email_read</span>{{ aviso }}
          </p>
          <div v-if="errorGeneral" class="caja caja--error-movil" role="alert">
            <span class="material-symbols-outlined" aria-hidden="true">warning</span>
            <span>{{ errorGeneral }}</span>
          </div>

          <!-- Escritorio: botón principal dentro del formulario -->
          <template v-if="!esMovil">
            <button
              type="submit"
              class="cm-boton codigo__boton"
              :class="{ 'cm-boton--cargando': validando || reenviando }"
              :disabled="validando || reenviando || ((estado === 'expirado' || estado === 'incorrecto') && !puedeReenviar)"
              :aria-busy="validando || reenviando"
            >
              <span>{{ (estado === 'expirado' || estado === 'incorrecto') && bloqueoReenvio > 0 ? textoReenviar : textoBotonPrincipal }}</span>
              <span
                v-if="iconoBotonPrincipal || validando"
                class="material-symbols-outlined"
                :class="{ girando: validando }"
                aria-hidden="true"
              >{{ validando ? 'progress_activity' : iconoBotonPrincipal }}</span>
            </button>

            <p v-if="estado === 'normal' || validando" class="reenvio reenvio--escritorio">
              <span>{{ T.noRecibiste }}</span>
              <button type="button" class="reenvio__enlace" :disabled="!puedeReenviar" @click="reenviar">
                {{ textoReenviar }}
              </button>
            </p>
          </template>
        </form>
      </div>

      <!-- Celular: botón principal abajo -->
      <div v-if="esMovil" class="codigo__inferior">
        <button
          type="button"
          class="cm-boton codigo__boton"
          :class="{ 'cm-boton--cargando': validando || reenviando }"
          :disabled="validando || reenviando || (estado === 'expirado' && !puedeReenviar)"
          :aria-busy="validando || reenviando"
          @click="accionPrincipal"
        >
          <svg v-if="validando" class="spinner" fill="none" viewBox="0 0 24 24" aria-hidden="true">
            <circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" opacity="0.25"></circle>
            <path opacity="0.75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
          </svg>
          <span v-else-if="iconoBotonPrincipal" class="material-symbols-outlined" aria-hidden="true">{{ iconoBotonPrincipal }}</span>
          <span>{{ estado === 'expirado' && bloqueoReenvio > 0 ? textoReenviar : textoBotonPrincipal }}</span>
        </button>
        <span v-if="validando" class="codigo__pie-validando">{{ T.validandoPie }}</span>
      </div>
    </section>
  </main>
</template>

<style scoped>
/* =====================================================================
   COMPUTADOR — tarjeta de 560 px (P4) y 700 px (P5), centrada.
   ===================================================================== */
.codigo {
  width: 100%;
  max-width: 560px;
  display: flex;
  flex-direction: column;
  gap: var(--cm-esp-lg);
  padding: var(--cm-esp-xl);
  border-radius: var(--cm-radio-lg);
  background: var(--cm-superficie);
  box-shadow: var(--cm-sombra);
  text-align: center;
}

.codigo__superior {
  display: flex;
  flex-direction: column;
  gap: var(--cm-esp-lg);
}

.codigo--exito {
  max-width: 700px;
}

.codigo__barra {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.codigo__barra--derecha {
  justify-content: flex-end;
}

.codigo__volver {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 0;
  border: none;
  background: none;
  color: var(--cm-texto-suave);
  font-family: inherit;
  font-size: var(--cm-label-md);
  line-height: 16px;
  font-weight: 500;
  cursor: pointer;
}

.codigo__volver:hover:not(:disabled) {
  color: var(--cm-primario);
}

.codigo__volver .material-symbols-outlined {
  font-size: 18px;
}

.codigo__paso {
  font-size: 13px;
  line-height: 18px;
  font-weight: 500;
  color: var(--cm-texto-suave);
}

.codigo__icono {
  width: 64px;
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto;
  border: 1px solid rgba(0, 80, 212, 0.3);
  border-radius: 9999px;
  background: rgba(0, 80, 212, 0.12);
  color: var(--cm-primario);
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

.codigo__icono .material-symbols-outlined {
  font-size: 32px;
}

.codigo__icono--gris {
  border-color: transparent;
  background: var(--cm-superficie-alta);
  color: var(--cm-texto-suave);
}

.codigo__icono--gris .material-symbols-outlined {
  font-size: 36px;
}

.codigo__icono--exito {
  width: 80px;
  height: 80px;
}

.codigo__icono--exito .material-symbols-outlined {
  font-size: 44px;
}

.codigo__textos {
  display: flex;
  flex-direction: column;
  gap: var(--cm-esp-xs);
}

.codigo__titulo {
  margin: 0;
  font-size: var(--cm-headline-lg);
  line-height: 38px;
  font-weight: 700;
  color: var(--cm-texto);
}

.codigo__titulo--xl {
  font-size: 36px;
  line-height: 44px;
}

.codigo__texto {
  margin: 0;
  font-size: var(--cm-body-md);
  line-height: 20px;
  color: var(--cm-texto-suave);
}

.codigo__texto--angosto {
  max-width: 512px;
  margin: 0 auto;
}

.codigo__correo {
  color: var(--cm-texto);
  font-weight: 600;
}

/* Cajas de aviso */
.caja {
  display: flex;
  align-items: flex-start;
  gap: var(--cm-esp-sm);
  padding: var(--cm-esp-md);
  border-radius: var(--cm-radio-lg);
  text-align: left;
}

.caja__icono {
  font-size: 20px;
  margin-top: 2px;
  flex-shrink: 0;
}

.caja__titulo {
  margin: 0 0 2px;
  font-size: var(--cm-label-md);
  line-height: 16px;
  font-weight: 700;
}

.caja__texto {
  margin: 0;
  font-size: var(--cm-body-sm);
  line-height: 16px;
}

.caja--error {
  border: 1px solid var(--cm-error);
  color: var(--cm-error);
}

.caja--gris {
  border: 1px solid var(--cm-texto-suave);
  color: var(--cm-texto);
}

.caja--gris .caja__icono {
  font-size: 22px;
  color: var(--cm-texto-suave);
}

.caja--gris .caja__texto {
  color: var(--cm-texto-suave);
}

.caja--error-movil {
  align-items: center;
  padding: 12px;
  border: 1px solid #ef4444;
  border-radius: var(--cm-radio-lg);
  background: #ffffff;
  color: #dc2626;
  font-size: 12px;
  line-height: 1.375;
}

.caja--error-movil .material-symbols-outlined {
  font-size: 20px;
  flex-shrink: 0;
}

/* Formulario y casillas */
.codigo__form {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--cm-esp-lg);
}

.casillas {
  display: flex;
  justify-content: center;
  gap: var(--cm-esp-sm);
  margin: var(--cm-esp-xs) 0;
}

.casilla {
  width: 56px;
  height: 64px;
  padding: 0;
  border: 1px solid #cbd5e1;
  border-radius: var(--cm-radio-lg);
  background: var(--cm-campo);
  color: var(--cm-texto);
  font-family: inherit;
  font-size: 30px;
  line-height: 38px;
  font-weight: 700;
  text-align: center;
  box-shadow: inset 0 2px 4px rgba(0, 0, 0, 0.05);
  outline: none;
}

.casilla:focus {
  border-color: var(--cm-primario);
  background: var(--cm-superficie);
}

.casillas--error .casilla {
  border: 2px solid var(--cm-error);
  background: rgba(253, 185, 185, 0.2);
  color: var(--cm-error);
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

.casillas--expirado {
  opacity: 0.4;
}

.casillas--expirado .casilla {
  border-color: transparent;
  background: var(--cm-secundario-fondo);
}

.casillas--validando {
  opacity: 0.6;
}

.codigo__falta {
  margin: calc(var(--cm-esp-md) * -1) 0 0;
  font-size: var(--cm-body-sm);
  color: var(--cm-error);
}

/* Contador (píldora) */
.contador {
  display: inline-flex;
  align-items: center;
  gap: var(--cm-esp-xs);
  padding: var(--cm-esp-xs) var(--cm-esp-md);
  border: 1px solid var(--cm-borde);
  border-radius: 9999px;
  background: var(--cm-secundario-fondo);
  color: var(--cm-texto-suave);
  font-size: var(--cm-label-md);
  line-height: 16px;
  font-weight: 500;
}

.contador .material-symbols-outlined {
  font-size: 16px;
}

.contador strong {
  color: var(--cm-primario);
  font-size: 13px;
  font-weight: 700;
}

.codigo__boton {
  width: 100%;
}

.codigo__boton .girando,
.girando {
  animation: girar 1s linear infinite;
}

@keyframes girar {
  to {
    transform: rotate(360deg);
  }
}

.codigo__boton-enlace {
  text-decoration: none;
}

.codigo__boton-enlace:hover {
  text-decoration: none;
}

/* Reenvío */
.reenvio {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-wrap: wrap;
  gap: 4px;
  margin: 0;
  font-size: var(--cm-body-sm);
  line-height: 16px;
  color: var(--cm-texto-suave);
}

.reenvio--escritorio {
  align-self: flex-start;
  margin-top: calc(var(--cm-esp-sm) * -1);
}

.reenvio__enlace {
  padding: 0;
  border: none;
  background: none;
  color: var(--cm-primario);
  font-family: inherit;
  font-size: var(--cm-label-md);
  line-height: 16px;
  font-weight: 500;
  cursor: pointer;
}

.reenvio__enlace:hover:not(:disabled) {
  text-decoration: underline;
}

.reenvio__enlace:disabled {
  color: var(--cm-texto-suave);
  opacity: 0.7;
  cursor: not-allowed;
}

.codigo__aviso {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 0;
  padding: 8px 12px;
  border: 1px solid #a7f3d0;
  border-radius: var(--cm-radio-lg);
  background: #ecfdf5;
  color: #047857;
  font-size: var(--cm-body-sm);
  line-height: 16px;
  text-align: left;
}

.codigo__aviso .material-symbols-outlined {
  font-size: 18px;
}

/* Éxito en computador (P5) */
.exito__tarjeta {
  display: flex;
  align-items: center;
  gap: var(--cm-esp-lg);
  padding: var(--cm-esp-lg);
  border: 1px solid var(--cm-borde);
  border-radius: var(--cm-radio-lg);
  background: var(--cm-campo);
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.08);
  text-align: left;
}

.exito__avatar {
  width: 96px;
  height: 96px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 9999px;
  background: var(--cm-primario);
  color: #ffffff;
  font-size: 30px;
  font-weight: 700;
  box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.15);
}

.exito__datos {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: var(--cm-esp-xs);
}

.exito__nombre {
  font-size: 24px;
  line-height: 32px;
  font-weight: 600;
  color: var(--cm-texto);
}

.exito__correo {
  font-size: var(--cm-body-sm);
  color: var(--cm-texto-suave);
}

.exito__chip {
  margin-top: var(--cm-esp-xs);
  padding: 4px var(--cm-esp-sm);
  border: 1px solid var(--cm-borde);
  border-radius: var(--cm-radio);
  background: var(--cm-superficie-alta);
  color: var(--cm-texto);
  font-size: var(--cm-label-sm);
  line-height: 14px;
  font-weight: 600;
}

.exito__acciones {
  display: flex;
  justify-content: center;
  flex-wrap: wrap;
  gap: var(--cm-esp-md);
  padding-top: var(--cm-esp-sm);
}

.exito__boton {
  width: auto;
  padding: var(--cm-esp-md) var(--cm-esp-xl);
}

.exito__boton-secundario {
  padding: var(--cm-esp-md) var(--cm-esp-xl);
  border: 1px solid var(--cm-borde);
  border-radius: var(--cm-radio-lg);
  background: var(--cm-superficie-alta);
  color: var(--cm-texto);
  font-family: inherit;
  font-size: var(--cm-label-lg);
  line-height: 20px;
  font-weight: 600;
  cursor: pointer;
}

.exito__boton-secundario:hover {
  filter: brightness(0.97);
}

/* =====================================================================
   CELULAR (≤ 767 px) — "boceto cel blanco.png" P1.4 y Grupo 4.
   ===================================================================== */
.codigo-movil {
  align-items: stretch;
  padding: 0;
  min-height: 100vh;
  min-height: 100dvh;
  background: #ffffff;
}

.codigo-movil .codigo {
  max-width: none;
  min-height: 100vh;
  min-height: 100dvh;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 16px;
  border-radius: 0;
  box-shadow: none;
}

.codigo-movil .codigo__superior {
  gap: 12px;
}

.movil__barra {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.movil__volver {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  border: 1px solid #e2e8f0;
  border-radius: 9999px;
  background: #f1f5f9;
  color: #475569;
  cursor: pointer;
}

.movil__volver:disabled {
  color: #94a3b8;
}

.movil__volver .material-symbols-outlined {
  font-size: 18px;
}

.movil__logo {
  height: 32px;
  width: auto;
  object-fit: contain;
}

.movil__espacio {
  width: 32px;
}

.codigo-movil .codigo__icono {
  width: 56px;
  height: 56px;
  margin-top: 4px;
  border: 1px solid #bfdbfe;
  border-radius: 16px;
  background: #eff6ff;
  color: #2563eb;
  box-shadow: inset 0 2px 4px rgba(0, 0, 0, 0.05);
}

.codigo-movil .codigo--normal .codigo__icono {
  width: 64px;
  height: 64px;
}

.codigo-movil .codigo__icono .material-symbols-outlined {
  font-size: 30px;
}

.codigo-movil .codigo__icono--rojo {
  border-color: #fecaca;
  background: #fef2f2;
  color: #dc2626;
}

.codigo-movil .codigo__icono--ambar {
  border-color: #fde68a;
  background: #fffbeb;
  color: #d97706;
}

.codigo-movil .codigo__titulo {
  font-size: 20px;
  line-height: 28px;
  font-weight: 600;
  color: #0f172a;
}

.codigo-movil .codigo__texto {
  padding: 0 12px;
  font-size: 12px;
  line-height: 16px;
  color: #475569;
}

.codigo-movil .codigo--validando .codigo__texto,
.codigo-movil .codigo--incorrecto .codigo__texto {
  color: #64748b;
}

.codigo-movil .codigo__correo {
  color: #0f172a;
  font-size: 13px;
  font-weight: 600;
}

.codigo-movil .codigo__form {
  gap: 8px;
}

.codigo-movil .casillas {
  gap: 6px;
  margin: 4px 0;
}

.codigo-movil .casilla {
  width: 44px;
  height: 52px;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  background: #f8fafc;
  color: #1e293b;
  font-size: 24px;
  line-height: 32px;
  box-shadow: none;
}

.codigo-movil .casilla--activa,
.codigo-movil .casilla:focus {
  border: 2px solid var(--cm-primario);
  background: #eff6ff;
  color: var(--cm-primario);
}

.codigo-movil .casillas--validando {
  opacity: 0.6;
}

.codigo-movil .casillas--validando .casilla {
  width: 40px;
  height: 48px;
  background: #f1f5f9;
  color: #334155;
}

.codigo-movil .casillas--error .casilla {
  width: 40px;
  height: 48px;
  border: 2px solid #ef4444;
  background: #ffffff;
  color: #dc2626;
}

.codigo-movil .casillas--expirado {
  opacity: 0.5;
}

.codigo-movil .casillas--expirado .casilla {
  width: 40px;
  height: 48px;
  border: 1px solid #cbd5e1;
  background: #f1f5f9;
  color: #94a3b8;
  text-decoration: line-through;
}

.contador-movil {
  margin-top: 4px;
  font-size: 11px;
  line-height: 14px;
  font-weight: 600;
  color: #64748b;
}

.contador-movil strong {
  color: #1e293b;
}

.codigo-movil .reenvio {
  font-size: 12px;
  color: #64748b;
}

.codigo-movil .reenvio__enlace {
  color: #2563eb;
  font-size: 12px;
  font-weight: 600;
}

.codigo-movil .codigo--validando .reenvio__enlace {
  opacity: 0.6;
}

.caja--ambar {
  flex-direction: column;
  gap: 6px;
  padding: 14px;
  border: 1px solid #fde68a;
  background: #fffbeb;
}

.caja--ambar .caja__titulo {
  color: #92400e;
  font-size: 11px;
  line-height: 14px;
}

.caja__titulo--con-icono {
  display: flex;
  align-items: center;
  gap: 6px;
}

.caja__titulo--con-icono .material-symbols-outlined {
  font-size: 18px;
}

.caja--ambar .caja__texto {
  color: #b45309;
  font-size: 12px;
  line-height: 1.375;
}

.codigo-movil .codigo__falta {
  margin: 0;
}

.codigo__inferior {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding-bottom: 8px;
}

.codigo-movil .codigo__boton {
  height: 44px;
  padding: 0 16px;
  gap: 6px;
  border-radius: 8px;
  font-size: 12px;
  line-height: 16px;
  font-weight: 600;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -2px rgba(0, 0, 0, 0.1);
}

.codigo-movil .cm-boton--cargando:disabled {
  background: var(--cm-primario);
  color: #ffffff;
  opacity: 1;
  cursor: wait;
}

.spinner {
  width: 20px;
  height: 20px;
  animation: girar 1s linear infinite;
}

.codigo__pie-validando {
  font-size: 12px;
  color: #64748b;
}

/* Éxito en celular (¡Código Verificado!) */
.codigo-movil .codigo--exito {
  align-items: center;
  padding: 16px;
}

.exito-movil__logo {
  width: 128px;
  height: 128px;
  margin-top: 8px;
  object-fit: contain;
}

.exito-movil__cuerpo {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin: 8px 0 auto;
}

.exito-movil__icono {
  width: 80px;
  height: 80px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 8px;
  border: 1px solid #a7f3d0;
  border-radius: 9999px;
  background: #ecfdf5;
  color: #059669;
  box-shadow: inset 0 2px 4px rgba(0, 0, 0, 0.05);
}

.exito-movil__icono .material-symbols-outlined {
  font-size: 48px;
}

.icono-relleno {
  font-variation-settings: 'FILL' 1, 'wght' 400, 'GRAD' 0, 'opsz' 24;
}

.codigo-movil .codigo--exito .codigo__titulo {
  font-weight: 700;
}

.exito-movil__texto {
  max-width: 240px;
  margin: 8px 0 0;
  font-size: 12px;
  line-height: 16px;
  color: #475569;
}

.exito-movil__barra {
  width: 192px;
  height: 6px;
  margin-top: 16px;
  overflow: hidden;
  border: 1px solid #e2e8f0;
  border-radius: 9999px;
  background: #f1f5f9;
}

.exito-movil__barra span {
  display: block;
  height: 100%;
  width: 0;
  border-radius: 9999px;
  background: #059669;
  animation: llenar 2s linear forwards;
}

@keyframes llenar {
  to {
    width: 100%;
  }
}

.exito-movil__cargando {
  margin-top: 8px;
  font-size: 11px;
  color: #64748b;
}

.exito-movil__boton {
  height: 44px;
  margin-bottom: 8px;
  border-radius: 8px;
  background: #059669;
  font-size: 12px;
  gap: 8px;
}

.exito-movil__boton:hover:not(:disabled) {
  background: #047857;
}

@media (prefers-reduced-motion: reduce) {
  .exito-movil__barra span,
  .spinner,
  .girando {
    animation-duration: 0s;
  }
}
</style>
