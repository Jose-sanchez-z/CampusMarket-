// S1-T05 · Estado de autenticación (sin Pinia: solo Vue)
// Estados posibles: sin sesión · pendiente de verificación · autenticado
import { reactive, computed } from 'vue'

const CLAVE = 'campusmarket_sesion'

// El almacenamiento del navegador puede fallar: siempre dentro de try/catch
function leerSesion() {
  try {
    return JSON.parse(localStorage.getItem(CLAVE))
  } catch {
    return null
  }
}

function guardarSesion(datos) {
  try {
    if (datos) localStorage.setItem(CLAVE, JSON.stringify(datos))
    else localStorage.removeItem(CLAVE)
  } catch {
    /* sin almacenamiento: la sesión dura solo mientras la pestaña esté abierta */
  }
}

const guardada = leerSesion()

const estado = reactive({
  token: guardada?.token ?? null,
  usuario: guardada?.usuario ?? null,
  correoPendiente: null, // correo que espera su código de verificación
})

const estaAutenticado = computed(() => !!estado.token)
const estaPendiente = computed(() => !estado.token && !!estado.correoPendiente)
const usuario = computed(() => estado.usuario)

// Se llama después de registrar la cuenta y antes de validar el código
function marcarPendiente(correo) {
  estado.correoPendiente = correo
}

// Se llama cuando el backend devuelve el token (login o verificación correcta)
function iniciarSesion({ token, usuario: datosUsuario }) {
  estado.token = token
  estado.usuario = datosUsuario ?? null
  estado.correoPendiente = null
  guardarSesion({ token, usuario: estado.usuario })
}

function cerrarSesion() {
  estado.token = null
  estado.usuario = null
  estado.correoPendiente = null
  guardarSesion(null)
}

export function useAuth() {
  return { estaAutenticado, estaPendiente, usuario, marcarPendiente, iniciarSesion, cerrarSesion }
}