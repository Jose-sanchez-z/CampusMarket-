// =====================================================================
// Servicio de sesión — HU-03 (S1-T06, E05).
// Contrato (docs/api/auth-contrato.yaml):
//   POST /auth/login   { correo, password } -> 200 { id, nombre, rol } + cookie CM_SESION
//                      400 campos vacíos · 401 credenciales · 403 cuenta PENDIENTE
//   GET  /auth/yo      -> 200 { id, nombre, correo, rol } · 401 sin sesión
//   POST /auth/logout  -> 200 { mensaje } · 401 no autenticado
// Va en un archivo aparte para no chocar con authService.js (E04).
// La sesión viaja en la cookie HttpOnly: aquí NO se guarda ningún token.
// =====================================================================
import { post, peticion, ApiError } from './api'

const SIMULADA = import.meta.env.VITE_API_SIMULADA === 'true'
let usuarioSimulado = null

export async function iniciarSesionApi({ correo, password }) {
  if (SIMULADA) return loginSimulado({ correo, password })
  return post('/auth/login', { correo, password })
}

export async function consultarSesion() {
  if (SIMULADA) return yoSimulado()
  return peticion('/auth/yo')
}

export async function cerrarSesionApi() {
  if (SIMULADA) {
    usuarioSimulado = null
    return { status: 200, datos: { mensaje: 'Sesión cerrada' } }
  }
  return post('/auth/logout')
}

// ---------------------------------------------------------------------
// Modo simulado (VITE_API_SIMULADA=true), para probar sin backend:
//   activa@uni.edu + Clave123!  -> entra
//   correo que empieza por "pendiente" -> 403
//   cualquier otro -> 401
// ---------------------------------------------------------------------
const esperar = (ms) => new Promise((resolver) => setTimeout(resolver, ms))

async function loginSimulado({ correo, password }) {
  await esperar(700)
  if (!correo || !password) throw new ApiError(400, { mensaje: 'Faltan campos' })
  if (correo.startsWith('pendiente')) {
    throw new ApiError(403, { mensaje: 'Cuenta pendiente de verificación' })
  }
  if (correo.startsWith('activa') && password === 'Clave123!') {
    usuarioSimulado = { id: 1, nombre: 'Ana Activa', correo, rol: 'ESTUDIANTE' }
    return { status: 200, datos: { id: 1, nombre: 'Ana Activa', rol: 'ESTUDIANTE' } }
  }
  throw new ApiError(401, { mensaje: 'Credenciales incorrectas' })
}

async function yoSimulado() {
  await esperar(200)
  if (!usuarioSimulado) throw new ApiError(401, { mensaje: 'No autenticado' })
  return { status: 200, datos: usuarioSimulado }
}