import { createRouter, createWebHistory } from 'vue-router'
import RegistroView from '@/views/RegistroView.vue'
import { useAuth } from '@/stores/auth'

// =====================================================================
// Router base (S1-T03).
// Rutas públicas del Sprint 1: bienvenida, registro, verificación y login.
// meta.publica se deja marcada para que E05 (S1-T05) agregue la
// protección de rutas privadas con un guard global.
// S1-T05: meta.layout = 'publico' usa el layout con encabezado;
// sin meta.layout se usa el layout de autenticación.
// =====================================================================
const routes = [
  {
    // Pantalla de bienvenida: botones hacia /registro y /login.
    path: '/',
    name: 'bienvenida',
    component: () => import('@/views/BienvenidaView.vue'),
    meta: { publica: true, soloInvitado: true, layout: 'publico', titulo: 'Bienvenido' },
  },
  {
    path: '/registro',
    name: 'registro',
    component: RegistroView,
    meta: { publica: true, soloInvitado: true, titulo: 'Crear cuenta' },
  },
  {
    // "¡Registro exitoso!" (S1-T03): se muestra solo al venir del registro.
    // Si se entra sin ?registro=ok, se pasa directo a la pantalla del código.
    path: '/verificacion',
    name: 'verificacion',
    component: () => import('@/views/VerificacionView.vue'),
    meta: { publica: true, titulo: 'Verificar cuenta' },
    beforeEnter: (to) =>
      to.query.registro === 'ok'
        ? true
        : { name: 'verificar-codigo', query: { correo: to.query.correo }, replace: true },
  },
  {
    // Pantalla de ingreso del código de 6 dígitos (S1-T04, HU-02).
    path: '/verificacion/codigo',
    name: 'verificar-codigo',
    component: () => import('@/views/CodigoVerificacionView.vue'),
    meta: { publica: true, titulo: 'Verifica tu correo' },
  },
  {
    // Pantalla de login: la construye E05 en S1-T06.
    path: '/login',
    name: 'login',
    component: () => import('@/views/LoginView.vue'),
    meta: { publica: true, soloInvitado: true, titulo: 'Iniciar sesión' },
  },
  {
    // Recuperar contraseña, paso 1: pedir el código por correo (S2-T03, HU-04).
    path: '/recuperar-contrasena',
    name: 'recuperar-contrasena',
    component: () => import('@/views/RecuperarContrasenaView.vue'),
    meta: { publica: true, soloInvitado: true, titulo: 'Recuperar contraseña' },
  },
  {
    // Recuperar contraseña, paso 2: código + nueva contraseña (S2-T03, HU-04).
    // Acepta ?token=... por si más adelante el correo trae un enlace.
    path: '/recuperar-contrasena/nueva',
    name: 'nueva-contrasena',
    component: () => import('@/views/NuevaContrasenaView.vue'),
    meta: { publica: true, soloInvitado: true, titulo: 'Crear nueva contraseña' },
  },
  {
    // S1-T05: página privada de ejemplo (solo con sesión iniciada).
    path: '/inicio',
    name: 'inicio',
    component: () => import('@/views/InicioView.vue'),
    meta: { publica: false, layout: 'publico', titulo: 'Inicio' },
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: { name: 'bienvenida' },
  },
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
})

// S1-T05: protección de rutas según el estado de autenticación.
router.beforeEach(async (to) => {
  const { estaAutenticado, restaurarSesion } = useAuth()
  await restaurarSesion() // pregunta al backend solo la primera vez
  // Ruta privada sin sesión: se envía al login.
  if (!to.meta.publica && !estaAutenticado.value) return { name: 'login' }
  // Con sesión iniciada no tiene sentido ver bienvenida, registro ni login.
  if (to.meta.soloInvitado && estaAutenticado.value) return { name: 'inicio' }
})

// Título de la pestaña según la ruta.
router.afterEach((to) => {
  document.title = to.meta.titulo ? `${to.meta.titulo} · CampusMarket` : 'CampusMarket'
})

export default router