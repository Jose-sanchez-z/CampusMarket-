// =====================================================================
// Servicio de autenticación (contrato: docs/api/auth-contrato.yaml).
//
// HU-01 Registro (S1-T03)
//   POST /auth/registro  { nombre, correo, password }
//   202 { mensaje: "Código enviado" } -> cuenta PENDIENTE, sin sesión
//   400 datos inválidos · 409 correo ya registrado
//
// HU-02 Verificación y reenvío (S1-T04) — backend S1-T10 (E07)
//   POST /auth/verificar      { correo, token }  (token = 6 dígitos)
//   200 { mensaje: "Cuenta activada" }
//   422 { mensaje: "Código inválido o vencido" }  (incorrecto, vencido o usado)
//   429 demasiados intentos (5 fallos en 15 min) · 400 datos inválidos
//   POST /auth/reenviartoken  { correo }
//   200 { mensaje: "Nuevo código enviado" } (igual exista o no el correo)
//   429 demasiados reenvíos (3 en 15 min)
//
// Login y logout los agrega E05 en S1-T06.
// Recuperación de contraseña (HU-04): al final del archivo (S2-T03).
// =====================================================================
import { post, ApiError } from './api'

const SIMULADA = import.meta.env.VITE_API_SIMULADA === 'true'

/**
 * Solicita el registro de una cuenta nueva.
 * La confirmación de contraseña NO se envía: solo se valida en el cliente.
 */
export async function registrarUsuario({ nombre, correo, password }) {
  const cuerpo = { nombre, correo, password }
  if (SIMULADA) return registroSimulado(cuerpo)
  return post('/auth/registro', cuerpo)
}

// ---------------------------------------------------------------------
// Modo simulado (VITE_API_SIMULADA=true): permite probar la pantalla
// mientras el backend no está listo. Responde 409 si el correo empieza
// por "duplicado"; en otro caso responde 202.
// ---------------------------------------------------------------------
function registroSimulado({ correo }) {
  return new Promise((resolve, reject) => {
    setTimeout(() => {
      if (correo.startsWith('duplicado')) {
        reject(new ApiError(409, { mensaje: 'El correo ya está registrado' }))
      } else {
        resolve({ status: 202, datos: { mensaje: 'Código enviado' } })
      }
    }, 900)
  })
}

/** Verifica el código de 6 dígitos y activa la cuenta (CA-02-01). */
export async function verificarCuenta({ correo, token }) {
  const cuerpo = { correo, token }
  if (SIMULADA) return verificacionSimulada(cuerpo)
  return post('/auth/verificar', cuerpo)
}

/** Pide un código nuevo: el backend invalida el anterior y envía otro. */
export async function reenviarCodigo({ correo }) {
  if (SIMULADA) return reenvioSimulado()
  return post('/auth/reenviartoken', { correo })
}

// ---------------------------------------------------------------------
// Modo simulado de verificación (VITE_API_SIMULADA=true):
//   123456 -> 200 (cuenta activada)
//   999999 -> 429 (demasiados intentos)
//   cualquier otro -> 422 (código inválido o vencido)
// El reenvío siempre responde 200.
// ---------------------------------------------------------------------
function verificacionSimulada({ token }) {
  return new Promise((resolve, reject) => {
    setTimeout(() => {
      if (token === '123456') {
        resolve({ status: 200, datos: { mensaje: 'Cuenta activada' } })
      } else if (token === '999999') {
        reject(new ApiError(429, { mensaje: 'Demasiados intentos. Intenta de nuevo más tarde' }))
      } else {
        reject(new ApiError(422, { mensaje: 'Código inválido o vencido' }))
      }
    }, 900)
  })
}

function reenvioSimulado() {
  return new Promise((resolve) => {
    setTimeout(() => resolve({ status: 200, datos: { mensaje: 'Nuevo código enviado' } }), 700)
  })
}

// =====================================================================
// HU-04 Recuperación de contraseña (S2-T03) — backend S2-T09 (E07)
//   POST /auth/recuperacion/solicitar  { correo }
//   200 { mensaje } SIEMPRE igual, exista o no el correo (CA-04-01)
//   400 correo vacío o con formato inválido
//   (el límite de 3 solicitudes/hora no cambia la respuesta: sigue siendo 200)
//
//   POST /auth/recuperacion/confirmar  { token, nuevaContrasena }
//   200 contraseña cambiada y TODAS las sesiones revocadas (CA-04-02)
//   400 token inválido, vencido o usado: la clave NO cambia (CA-04-03)
//   422 la contraseña no cumple (8 a 72): el token NO se gasta
//   429 demasiados intentos fallidos desde la IP
// =====================================================================

/** Pide el token de recuperación. La respuesta no revela si el correo existe. */
export async function solicitarRecuperacion({ correo }) {
  if (SIMULADA) return solicitudSimulada()
  return post('/auth/recuperacion/solicitar', { correo })
}

/** Cambia la contraseña con el token recibido por correo. */
export async function confirmarRecuperacion({ token, nuevaContrasena }) {
  const cuerpo = { token, nuevaContrasena }
  if (SIMULADA) return confirmacionSimulada(cuerpo)
  return post('/auth/recuperacion/confirmar', cuerpo)
}

// ---------------------------------------------------------------------
// Modo simulado de recuperación (VITE_API_SIMULADA=true):
//   Solicitar: siempre 200 con el mensaje genérico.
//   Confirmar: token que empieza por "vencido" -> 400 · por "limite" -> 429
//   · cualquier otro token de 43 caracteres -> 200.
// ---------------------------------------------------------------------
function solicitudSimulada() {
  return new Promise((resolve) => {
    setTimeout(
      () => resolve({ status: 200, datos: { mensaje: 'Si el correo está registrado, te enviamos un token de recuperación' } }),
      900
    )
  })
}

function confirmacionSimulada({ token }) {
  return new Promise((resolve, reject) => {
    setTimeout(() => {
      if (token.startsWith('vencido')) {
        reject(new ApiError(400, { mensaje: 'Token inválido o vencido' }))
      } else if (token.startsWith('limite')) {
        reject(new ApiError(429, { mensaje: 'Demasiados intentos. Intenta de nuevo más tarde' }))
      } else {
        resolve({ status: 200, datos: { mensaje: 'Contraseña actualizada. Inicia sesión de nuevo' } })
      }
    }, 900)
  })
}
