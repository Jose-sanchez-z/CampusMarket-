import { obtenerYo } from '@/services/productosApi'

// Uso: router.beforeEach(guardSesion). Protege las rutas con meta: { requiereSesion: true }.
export async function guardSesion(to) {
  if (!to.meta.requiereSesion) return true
  try {
    await obtenerYo()
    return true
  } catch (e) {
    if (e.status === 401 || e.status === 403) {
      return { path: '/login', query: { redirect: to.fullPath } }
    }
    return true // error de red: deja pasar; el formulario mostrará el error al enviar
  }
}
