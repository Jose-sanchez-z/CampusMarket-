// =====================================================================
// Cliente HTTP mínimo (S1-T03).
// - Toma la URL base de VITE_API_URL (.env).
// - Envía SIEMPRE credentials: 'include' para que viaje la cookie HttpOnly
//   de sesión (CA-03-01). Nunca se guardan tokens en localStorage.
// - En S1-T08 E06 lo reemplaza por la capa única con Axios
//   (withCredentials: true); las vistas solo dependen de authService.js.
// =====================================================================

const API_URL = (import.meta.env.VITE_API_URL || 'http://localhost:8080').replace(/\/+$/, '')

/** Error de la API con el código HTTP y el cuerpo de la respuesta. */
export class ApiError extends Error {
  constructor(status, datos, mensaje) {
    super(mensaje || datos?.mensaje || `Error HTTP ${status}`)
    this.name = 'ApiError'
    this.status = status // 0 = no hubo respuesta (servidor apagado, CORS, sin red)
    this.datos = datos
  }
}

async function leerCuerpo(respuesta) {
  const texto = await respuesta.text()
  if (!texto) return null
  try {
    return JSON.parse(texto)
  } catch {
    return { mensaje: texto }
  }
}

/**
 * Hace una petición a la API.
 * @returns {Promise<{status:number, datos:any}>} si la respuesta es 2xx
 * @throws {ApiError} si la respuesta no es 2xx o no hubo conexión
 */
export async function peticion(ruta, { metodo = 'GET', cuerpo } = {}) {
  let respuesta
  try {
    respuesta = await fetch(`${API_URL}${ruta}`, {
      method: metodo,
      credentials: 'include',
      headers: {
        Accept: 'application/json',
        ...(cuerpo !== undefined ? { 'Content-Type': 'application/json' } : {})
      },
      body: cuerpo !== undefined ? JSON.stringify(cuerpo) : undefined
    })
  } catch {
    throw new ApiError(0, null, 'No fue posible conectar con el servidor.')
  }

  const datos = await leerCuerpo(respuesta)
  if (!respuesta.ok) {
    throw new ApiError(respuesta.status, datos)
  }
  return { status: respuesta.status, datos }
}

export function post(ruta, cuerpo) {
  return peticion(ruta, { metodo: 'POST', cuerpo })
}
