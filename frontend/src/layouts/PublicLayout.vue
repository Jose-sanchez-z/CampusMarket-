<!-- S1-T05 · Layout con encabezado: logo, navegación y acceso (PC y celular) -->
<script setup>
import { ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { useAuth } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const { estaAutenticado, cerrarSesion } = useAuth()
const logo = import.meta.env.BASE_URL + 'logo.png'
const menuAbierto = ref(false)

// Al cambiar de página en celular, el menú se cierra solo
watch(() => route.fullPath, () => { menuAbierto.value = false })

async function salir() {
  menuAbierto.value = false
 await cerrarSesion()
  router.push({ name: 'login' })
}
</script>

<template>
  <div class="layout-publico">
    <header class="cabecera" :class="{ 'menu-abierto': menuAbierto }">
      <RouterLink :to="{ name: 'inicio' }" class="marca">
        <img :src="logo" alt="" width="32" height="32" />
        <span>CampusMarket</span>
      </RouterLink>

      <button
        type="button"
        class="hamburguesa"
        aria-label="Menú"
        :aria-expanded="menuAbierto"
        @click="menuAbierto = !menuAbierto"
      >
        {{ menuAbierto ? '✕' : '☰' }}
      </button>

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
.hamburguesa {
  display: none; width: 44px; height: 44px; border: 0; border-radius: 10px;
  background: var(--suave); color: var(--texto); font-size: 1.25rem; cursor: pointer;
}
.contenido { flex: 1; width: 100%; max-width: 1100px; margin: 0 auto; padding: 2rem 1.5rem; }

/* Celular: la navegación y el acceso se esconden detrás del botón ☰ */
@media (max-width: 768px) {
  .cabecera { flex-wrap: wrap; padding: 0.75rem 1rem; }
  .hamburguesa { display: inline-flex; align-items: center; justify-content: center; }
  .navegacion, .acciones { display: none; width: 100%; }
  .insignia { display: none; }
  .menu-abierto .navegacion { display: flex; flex-direction: column; gap: 1rem; padding-top: 0.75rem; }
  .menu-abierto .acciones { display: flex; padding-top: 0.75rem; }
  .boton { min-height: 44px; display: inline-flex; align-items: center; justify-content: center; width: 100%; }
  .contenido { padding: 1.5rem 1rem; }
}
</style>