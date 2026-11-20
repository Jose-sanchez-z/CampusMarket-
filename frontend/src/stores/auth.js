// S1-T05/T06 · Estado de autenticación (sin Pinia, sin localStorage).
// La sesión vive en la cookie HttpOnly CM_SESION; aquí solo se guarda
// en memoria el usuario que responde GET /auth/yo.
import { reactive, computed } from 'vue'
import { consultarSesion, iniciarSesionApi, cerrarSesionApi } from '@/services/sesionService'

const estado = reactive({
  usuario: null, // { id, nombre, correo?, rol }
  correoPendiente: null, // correo de una cuenta que falta verificar
})

const estaAutenticado = computed(() => !!estado.usuario)
const estaPendiente = computed(() => !estado.usuario && !!estado.correoPendiente)
const usuario = computed(() => estado.usuario)

// Al cargar o recargar la página: ¿hay sesión? Se consulta una sola vez.
let consulta = null
function restaurarSesion() {
  if (!consulta) {
    consulta = consultarSesion()
      .then(({ datos }) => {
        estado.usuario = datos
      })
      .catch(() => {
        estado.usuario = null // 401 = sin sesión (o sin conexión con el servidor)
      })
  }
  return consulta
}

// Login: si el backend responde 200, ya hay cookie y se guarda el usuario.
// Si falla, lanza el error (ApiError) para que la pantalla lo muestre.
async function iniciarSesion({ correo, password }) {
  const { datos } = await iniciarSesionApi({ correo, password })
  estado.usuario = datos
  estado.correoPendiente = null
  return datos
}

async function cerrarSesion() {
  try {
    await cerrarSesionApi()
  } catch {
    /* si el servidor no responde, igual se limpia la vista */
  }
  estado.usuario = null
  estado.correoPendiente = null
}

function marcarPendiente(correo) {
  estado.correoPendiente = correo
}

export function useAuth() {
  return {
    estaAutenticado,
    estaPendiente,
    usuario,
    restaurarSesion,
    iniciarSesion,
    cerrarSesion,
    marcarPendiente,
  }
}