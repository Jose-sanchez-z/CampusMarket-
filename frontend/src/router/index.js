import { createRouter, createWebHistory } from 'vue-router'
import RegistroView from '@/views/RegistroView.vue'
import { useAuth } from '@/stores/auth'

// =====================================================================
// Router base (S1-T03).
// Rutas públicas del Sprint 1: registro, verificación y login.
// meta.publica se deja marcada para que E05 (S1-T05) agregue la
// protección de rutas privadas con un guard global.
// S1-T05: meta.layout = 'publico' usa el layout con encabezado;
// sin meta.layout se usa el layout de autenticación.
// =====================================================================
const routes = [
  {
    path: '/',
    redirect: { name: 'registro' },
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
     {
    // Tras iniciar sesión se llega al catálogo.
    path: '/inicio',
    name: 'inicio',
    redirect: { name: 'catalogo' },
  },
  {
    // S2-T05: catálogo de productos (solo con sesión iniciada).
    path: '/catalogo',
    name: 'catalogo',
    component: () => import('@/views/CatalogoView.vue'),
    meta: { publica: false, layout: 'publico', titulo: 'Catálogo' },
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: { name: 'registro' },
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
  // Con sesión iniciada no tiene sentido ver registro ni login.
  if (to.meta.soloInvitado && estaAutenticado.value) return { name: 'inicio' }
})

// Título de la pestaña según la ruta.
router.afterEach((to) => {
  document.title = to.meta.titulo ? `${to.meta.titulo} · CampusMarket` : 'CampusMarket'
})

export default router