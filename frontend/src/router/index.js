import { createRouter, createWebHistory } from 'vue-router'
import RegistroView from '@/views/RegistroView.vue'

// =====================================================================
// Router base (S1-T03).
// Rutas públicas del Sprint 1: registro, verificación y login.
// meta.publica se deja marcada para que E05 (S1-T05) agregue la
// protección de rutas privadas con un guard global.
// =====================================================================
const routes = [
  {
    path: '/',
    redirect: { name: 'registro' }
  },
  {
    path: '/registro',
    name: 'registro',
    component: RegistroView,
    meta: { publica: true, titulo: 'Crear cuenta' }
  },
  {
    // "¡Registro exitoso!" (S1-T03): se muestra solo al venir del registro.
    // Si se entra sin ?registro=ok, se pasa directo a la pantalla del código.
    path: '/verificar',
    name: 'verificacion',
    component: () => import('@/views/VerificacionView.vue'),
    meta: { publica: true, titulo: 'Verificar cuenta' },
    beforeEnter: (to) =>
      to.query.registro === 'ok'
        ? true
        : { name: 'verificar-codigo', query: { correo: to.query.correo }, replace: true }
  },
  {
    // Pantalla de ingreso del código de 6 dígitos (S1-T04, HU-02).
    path: '/verificar/codigo',
    name: 'verificar-codigo',
    component: () => import('@/views/CodigoVerificacionView.vue'),
    meta: { publica: true, titulo: 'Verifica tu correo' }
  },
  {
    // Pantalla de login: la construye E05 en S1-T06.
    path: '/login',
    name: 'login',
    component: () => import('@/views/LoginView.vue'),
    meta: { publica: true, titulo: 'Iniciar sesión' }
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: { name: 'registro' }
  }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
})

// Título de la pestaña según la ruta.
router.afterEach((to) => {
  document.title = to.meta.titulo ? `${to.meta.titulo} · CampusMarket` : 'CampusMarket'
})

export default router
