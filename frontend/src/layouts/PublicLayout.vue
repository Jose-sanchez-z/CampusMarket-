<!-- S1-T05 · Layout con encabezado: logo, navegación y acceso -->
<script setup>
import { RouterLink, useRouter } from 'vue-router'
import { useAuth } from '@/stores/auth'

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
        <button v-if="estaAutenticado" type="button" class="boton" @click="salir">Cerrar sesión</button>
        <RouterLink v-else :to="{ name: 'login' }" class="boton">Iniciar sesión</RouterLink>
      </div>
    </header>

    <main class="contenido">
      <slot />
    </main>
  </div>
</template>

<style scoped>
.layout-publico {
  --primario: #0050cf;
  --superficie: #ffffff;
  --suave: #e8e8fb;
  --borde: #dcdcf0;
  --texto: #1b1b3a;
  --texto-suave: #5b5b7a;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f8f7ff;
  color: var(--texto);
}
.cabecera {
  display: flex; align-items: center; justify-content: space-between; gap: 1rem;
  padding: 0.75rem 1.5rem; background: var(--superficie); border-bottom: 1px solid var(--borde);
}
.marca { display: flex; align-items: center; gap: 0.5rem; font-weight: 700; color: var(--texto); text-decoration: none; }
.navegacion { display: flex; gap: 1.25rem; font-size: 0.85rem; }
.navegacion a { color: var(--primario); text-decoration: none; }
.acciones { display: flex; align-items: center; gap: 0.75rem; }
.insignia { font-size: 0.75rem; padding: 0.3rem 0.6rem; border-radius: 6px; background: var(--suave); color: var(--texto-suave); }
.boton {
  padding: 0.55rem 1rem; border: 0; border-radius: 8px; background: var(--primario); color: #fff;
  font: inherit; font-weight: 600; cursor: pointer; text-decoration: none;
}
.contenido { flex: 1; width: 100%; max-width: 1100px; margin: 0 auto; padding: 2rem 1.5rem; }

/* Celular: se ocultan la navegación y la insignia */
@media (max-width: 768px) {
  .navegacion, .insignia { display: none; }
  .cabecera { padding: 0.75rem 1rem; }
  .contenido { padding: 1.5rem 1rem; }
}
</style>