<script setup>
// S2-T05 · Vista de catálogo: buscador, filtros por categoría y estados
// (cargando, error, vacío, con productos). Acepta ?categoria=... en la dirección.
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ProductoCard from '@/components/ProductoCard.vue'
import { listarProductos, CATEGORIAS_FILTRO } from '@/services/productosService'

const route = useRoute()
const router = useRouter()

const estado = ref('cargando') // 'cargando' | 'listo' | 'vacio' | 'error'
const productos = ref([])
const busqueda = ref('')
const categoria = ref(
  CATEGORIAS_FILTRO.some((c) => c.id === route.query.categoria) ? route.query.categoria : 'todos'
)

let temporizador = null
let peticionActual = 0

async function cargar() {
  const numero = ++peticionActual
  estado.value = 'cargando'
  try {
    const lista = await listarProductos({ busqueda: busqueda.value.trim(), categoria: categoria.value })
    if (numero !== peticionActual) return // ya hay una búsqueda más nueva
    productos.value = lista
    estado.value = lista.length ? 'listo' : 'vacio'
  } catch {
    if (numero !== peticionActual) return
    estado.value = 'error'
  }
}

function elegirCategoria(id) {
  categoria.value = id
  router.replace({ query: id === 'todos' ? {} : { categoria: id } })
}

function limpiarFiltros() {
  busqueda.value = ''
  elegirCategoria('todos')
}

const pastilla = computed(() => {
  if (estado.value === 'error') return 'Error de conexión'
  if (estado.value === 'listo') {
    const n = productos.value.length
    return `${n} producto${n === 1 ? '' : 's'} disponible${n === 1 ? '' : 's'}`  }
  return ''
})

watch(categoria, cargar)
watch(busqueda, () => {
  clearTimeout(temporizador)
  temporizador = setTimeout(cargar, 300) // espera a que termine de escribir
})
onMounted(cargar)
onBeforeUnmount(() => clearTimeout(temporizador))
</script>

<template>
  <section>
    <div class="encabezado">
      <div>
        <p class="sobretitulo">01 / MERCADO ESTUDIANTIL</p>
        <h1>Catálogo de Productos</h1>
        <p class="intro">Encuentra libros, tecnología y accesorios verificados dentro del campus.</p>
      </div>
      <RouterLink :to="{ name: 'nuevo-producto' }" class="boton">Publicar producto</RouterLink>
      <span v-if="pastilla" class="pastilla">{{ pastilla }}</span>
    </div>

    <p v-if="route.query.publicado === '1'" role="status">Producto publicado.</p>

    <div class="barra">
      <label class="buscador">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
          <circle cx="11" cy="11" r="8" /><path d="m21 21-4.3-4.3" />
        </svg>
        <input v-model="busqueda" type="search" placeholder="Buscar por título, autor o palabra clave..." aria-label="Buscar productos" />
      </label>
      <div class="chips" role="group" aria-label="Categorías">
        <button
          v-for="c in CATEGORIAS_FILTRO"
          :key="c.id"
          type="button"
          class="chip"
          :class="{ activo: categoria === c.id }"
          :aria-pressed="categoria === c.id"
          @click="elegirCategoria(c.id)"
        >
          {{ c.nombre }}
        </button>
      </div>
    </div>

    <div v-if="estado === 'cargando'" class="cuadricula" aria-busy="true" aria-label="Cargando productos">
      <div v-for="n in 8" :key="n" class="esqueleto"></div>
    </div>

    <div v-else-if="estado === 'error'" class="aviso" role="alert">
      <div class="icono-aviso icono-error">
        <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
          <path d="m2 2 20 20" /><path d="M5.782 5.782A7 7 0 0 0 9 19h8.5a4.5 4.5 0 0 0 1.307-.193" /><path d="M21.532 16.5A4.5 4.5 0 0 0 17.5 10h-1.79A7.008 7.008 0 0 0 10 5.07" />
        </svg>
      </div>
      <h2>Problema de conexión</h2>
      <p>No pudimos cargar el catálogo en este momento. Intenta recargar la página.</p>
      <button type="button" class="boton" @click="cargar">↻ Reintentar</button>
    </div>

    <div v-else-if="estado === 'vacio'" class="aviso">
      <div class="icono-aviso" aria-hidden="true">🔍</div>
      <h2>No encontramos productos</h2>
      <p>Prueba con otra búsqueda o con otra categoría.</p>
      <button type="button" class="boton" @click="limpiarFiltros">Limpiar filtros</button>
    </div>

    <div v-else class="cuadricula">
      <ProductoCard v-for="p in productos" :key="p.id" :producto="p" />
    </div>
  </section>
</template>

<style scoped>
.encabezado { display: flex; justify-content: space-between; align-items: flex-end; gap: 1rem; flex-wrap: wrap; margin-bottom: 1.25rem; }
.sobretitulo { margin: 0 0 0.25rem; font-size: 0.75rem; font-weight: 700; letter-spacing: 0.05em; color: var(--primario); }
h1 { margin: 0; font-size: clamp(1.6rem, 4vw, 2rem); }
.intro { margin: 0.35rem 0 0; color: var(--texto-suave); }
.pastilla { padding: 0.5rem 0.9rem; border-radius: 10px; background: var(--suave); color: var(--texto); font-weight: 600; font-size: 0.9rem; }

.barra {
  display: flex; flex-wrap: wrap; align-items: center; gap: 1rem; margin-bottom: 1.5rem; padding: 1rem;
  background: var(--superficie); border: 1px solid var(--borde); border-radius: 12px;
}
.buscador {
  flex: 1; min-width: 240px; display: flex; align-items: center; gap: 0.6rem; padding: 0 0.9rem;
  min-height: 44px; border-radius: 10px; background: var(--suave); color: var(--texto-suave);
}
.buscador input { flex: 1; border: 0; outline: 0; background: transparent; font: inherit; color: var(--texto); }
.chips { display: flex; flex-wrap: wrap; gap: 0.5rem; }
.chip {
  min-height: 40px; padding: 0 0.9rem; border: 0; border-radius: 8px; background: var(--suave);
  color: var(--texto-suave); font: inherit; font-size: 0.85rem; font-weight: 600; cursor: pointer;
}
.chip.activo { background: var(--primario); color: #fff; }

.cuadricula { display: grid; grid-template-columns: repeat(auto-fill, minmax(250px, 1fr)); gap: 1.25rem; }
.esqueleto { height: 380px; border-radius: 12px; background: var(--suave); animation: pulso 1.2s ease-in-out infinite; }
@keyframes pulso { 50% { opacity: 0.5; } }

.aviso {
  max-width: 450px; margin: 3rem auto; padding: 2rem; text-align: center;
  background: var(--superficie); border-radius: 12px; box-shadow: 0 4px 16px rgba(27, 27, 58, 0.12);
}
.icono-aviso {
  width: 64px; height: 64px; margin: 0 auto 1rem; display: flex; align-items: center; justify-content: center;
  border-radius: 14px; background: var(--suave); font-size: 1.75rem;
}
.icono-error { background: #fddcd9; color: #8e1b1b; }
.aviso h2 { margin: 0 0 0.5rem; font-size: 1.25rem; }
.aviso p { margin: 0 0 1.25rem; color: var(--texto-suave); }
.boton {
  min-height: 44px; padding: 0 1.5rem; border: 0; border-radius: 8px; background: var(--primario);
  color: #fff; font: inherit; font-weight: 600; cursor: pointer;
}
</style>