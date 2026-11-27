<script setup>
// Tarjeta de producto (S2-T05), según HU-07_Catalogo.png.
import { computed } from 'vue'

const props = defineProps({ producto: { type: Object, required: true } })
defineEmits(['contactar'])

const CATEGORIAS = {
  libros: { nombre: 'Libros y Apuntes', icono: '📚', calida: false },
  tecnologia: { nombre: 'Tecnología', icono: '💻', calida: false },
  accesorios: { nombre: 'Accesorios', icono: '🎒', calida: true },
  snacks: { nombre: 'Snacks', icono: '🍪', calida: true },
}

const categoria = computed(
  () => CATEGORIAS[props.producto.categoria] ?? { nombre: props.producto.categoria, icono: '🛍️', calida: false }
)
const precio = computed(
  () =>
    '$' +
    new Intl.NumberFormat('es-CO', { maximumFractionDigits: 0, useGrouping: 'always' }).format(props.producto.precio) +
    ' COP'
)
</script>

<template>
  <article class="tarjeta">
    <div class="foto">
      <img v-if="producto.imagen" :src="producto.imagen" :alt="producto.titulo" loading="lazy" />
      <span v-else class="icono" aria-hidden="true">{{ categoria.icono }}</span>
      <span class="etiqueta" :class="{ calida: categoria.calida }">{{ categoria.nombre }}</span>
    </div>

    <div class="cuerpo">
      <h3>{{ producto.titulo }}</h3>
      <p v-if="producto.descripcion" class="detalle">{{ producto.descripcion }}</p>
      <p class="precio">{{ precio }}</p>
      <!-- El contacto con el vendedor se construye en el Sprint 3 -->
      <button type="button" class="contactar" @click="$emit('contactar', producto)">
        💬 Contactar vendedor
      </button>
    </div>
  </article>
</template>

<style scoped>
.tarjeta {
  display: flex; flex-direction: column; overflow: hidden;
  background: var(--superficie); border: 1px solid var(--borde); border-radius: 12px;
}
.foto {
  position: relative; height: 192px; display: flex; align-items: center; justify-content: center;
  background: linear-gradient(135deg, #eef0ff, #dfe3fa); font-size: 3.5rem;
}
.foto img { width: 100%; height: 100%; object-fit: cover; }
.etiqueta {
  position: absolute; top: 12px; left: 12px; padding: 0.2rem 0.65rem; border-radius: 999px;
  background: #e0e4ff; color: #0050cf; font-size: 0.72rem; font-weight: 600;
}
.etiqueta.calida { background: #ffe1d4; color: #a34a1e; }
.cuerpo { flex: 1; display: flex; flex-direction: column; gap: 0.35rem; padding: 1rem; }
h3 { margin: 0; font-size: 1.1rem; line-height: 1.3; }
.detalle { margin: 0; font-size: 0.8rem; color: var(--texto-suave); }
.precio { margin: auto 0 0.5rem; padding-top: 0.75rem; font-size: 1.3rem; font-weight: 700; color: var(--primario); }
.contactar {
  min-height: 44px; border: 0; border-radius: 8px; background: var(--primario);
  color: #fff; font: inherit; font-weight: 600; cursor: pointer;
}
</style>