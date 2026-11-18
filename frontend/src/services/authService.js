// =====================================================================
// Servicio de autenticación — HU-01 Registro (S1-T03).
// Contrato acordado (Guía Sprint 1):
//   POST /auth/registro  { nombre, correo, password }
//   202 Accepted         { mensaje: "Código enviado" }   -> cuenta PENDIENTE, sin sesión
//   400 Bad Request      datos inválidos
//   409 Conflict         correo ya registrado
// Los endpoints de verificar, reenviar, login y logout se agregan en
// S1-T04 (E04) y S1-T06 (E05).
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
