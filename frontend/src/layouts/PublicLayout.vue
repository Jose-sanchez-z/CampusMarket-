<!-- S1-T05 · Layout con encabezado: logo, navegación y acceso -->
<script setup>
import { RouterLink, useRouter } from 'vue-router'
import { useAuth } from '@/stores/auth'
import { BaseButton } from '@/components/ui'

const router = useRouter()
const { estaAutenticado, cerrarSesion } = useAuth()
const logo = import.meta.env.BASE_URL + 'logo.png'

function salir() {
  cerrarSesion()
  router.push({ name: 'login' })
}
</script>

<template>
  <div class="layout-publico">
    <header class="cabecera">
      <RouterLink :to="{ name: 'inicio' }" class="marca">
        <img :src="logo" alt="" width="32" height="32" />
        <span>CampusMarket</span>
      </RouterLink>

      <nav class="navegacion" aria-label="Principal">
        <a href="#" @click.prevent>Explorar</a>
        <a href="#" @click.prevent>Libros &amp; Apuntes</a>
        <a href="#" @click.prevent>Hardware &amp; PC</a>
        <a href="#" @click.prevent>Garantía Universitaria</a>
      </nav>

      <div class="acciones">
        <span class="insignia">Red Universitaria Activa</span>
        <BaseButton v-if="estaAutenticado" size="sm" @click="salir">Cerrar sesión</BaseButton>
        <BaseButton v-else size="sm" :to="{ name: 'login' }">Iniciar sesión</BaseButton>
      </div>
    </header>

    <main class="contenido">
      <slot />
    </main>
  </div>
</template>

<style scoped>
.layout-publico {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--cm-fondo);
  color: var(--cm-texto);
}
.cabecera {
  display: flex; align-items: center; justify-content: space-between; gap: var(--cm-esp-md);
  padding: 0.75rem var(--cm-esp-lg); background: var(--cm-superficie); border-bottom: 1px solid var(--cm-borde);
}
.marca { display: flex; align-items: center; gap: var(--cm-esp-sm); font-weight: 700; color: var(--cm-texto); text-decoration: none; }
.navegacion { display: flex; gap: 1.25rem; font-size: var(--cm-body-md); }
.navegacion a { color: var(--cm-primario); text-decoration: none; }
.acciones { display: flex; align-items: center; gap: 0.75rem; }
.insignia { font-size: var(--cm-body-sm); padding: 0.3rem 0.6rem; border-radius: var(--cm-radio-lg); background: var(--cm-secundario-fondo); color: var(--cm-texto-suave); }
.contenido { flex: 1; width: 100%; max-width: 1100px; margin: 0 auto; padding: var(--cm-esp-xl) var(--cm-esp-lg); }

/* Celular: se ocultan la navegación y la insignia */
@media (max-width: 768px) {
  .navegacion, .insignia { display: none; }
  .cabecera { padding: 0.75rem var(--cm-esp-md); }
  .contenido { padding: var(--cm-esp-lg) var(--cm-esp-md); }
}
</style>
