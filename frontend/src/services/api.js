// Cliente HTTP base. La sesión viaja en la cookie HttpOnly CM_SESION, por eso credentials: 'include'.
const BASE = import.meta.env.VITE_API_URL ?? 'http://localhost:8080'

export class ApiError extends Error {
  constructor(status, mensaje) {
    super(mensaje)
    this.status = status
  }
}

export async function api(ruta, { method = 'GET', body } = {}) {
  let res
  try {
    res = await fetch(BASE + ruta, {
      method,
      credentials: 'include',
      headers: body ? { 'Content-Type': 'application/json' } : undefined,
      body: body ? JSON.stringify(body) : undefined,
    })
  } catch {
    throw new ApiError(0, 'No se pudo conectar con el servidor')
  }
  const datos = await res.json().catch(() => null)
  if (!res.ok) throw new ApiError(res.status, datos?.mensaje ?? 'Error inesperado')
  return datos
}
