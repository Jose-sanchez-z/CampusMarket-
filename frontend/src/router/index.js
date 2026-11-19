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
    // Pantalla de ingreso del token: se completa en S1-T04 (E04).
    path: '/verificacion',
    name: 'verificacion',
    component: () => import('@/views/VerificacionView.vue'),
    meta: { publica: true, titulo: 'Verificar cuenta' },
  },
  {
    // Pantalla de login: la construye E05 en S1-T06.
    path: '/login',
    name: 'login',
    component: () => import('@/views/LoginView.vue'),
    meta: { publica: true, soloInvitado: true, titulo: 'Iniciar sesión' },
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
    redirect: { name: 'registro' },
  },
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
})

// S1-T05: protección de rutas según el estado de autenticación.
router.beforeEach((to) => {
  const { estaAutenticado } = useAuth()
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