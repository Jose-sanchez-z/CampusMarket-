<script setup>
// Botón base. Con `to` se dibuja como enlace del router (misma apariencia).
import { computed } from 'vue'
import { RouterLink } from 'vue-router'
import BaseSpinner from './BaseSpinner.vue'

const props = defineProps({
  variant: { type: String, default: 'primary', validator: (v) => ['primary', 'secondary'].includes(v) },
  size: { type: String, default: 'md', validator: (v) => ['xs', 'sm', 'md'].includes(v) },
  type: { type: String, default: 'button' },
  to: { type: [String, Object], default: null },
  icon: { type: String, default: '' }, // icono Material Symbols a la derecha
  loading: { type: Boolean, default: false },
  disabled: { type: Boolean, default: false },
  block: { type: Boolean, default: false } // ocupa todo el ancho
})

const etiqueta = computed(() => (props.to ? RouterLink : 'button'))
const atributos = computed(() =>
  props.to
    ? { to: props.to }
    : { type: props.type, disabled: props.disabled || props.loading, 'aria-busy': props.loading || undefined }
)
</script>

<template>
  <component
    :is="etiqueta"
    v-bind="atributos"
    class="cm-boton"
    :class="[
      `cm-boton--${variant}`,
      `cm-boton--${size}`,
      { 'cm-boton--bloque': block, 'cm-boton--cargando': loading }
    ]"
  >
    <span><slot /></span>
    <BaseSpinner v-if="loading" size="sm" decorativo />
    <span v-else-if="icon" class="material-symbols-outlined" aria-hidden="true">{{ icon }}</span>
  </component>
</template>

<style scoped>
.cm-boton {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: var(--cm-esp-xs);
  padding: var(--cm-esp-md);
  border: none;
  border-radius: var(--cm-radio-lg);
  font-family: inherit;
  font-size: var(--cm-label-lg);
  line-height: 20px;
  font-weight: 600;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
  cursor: pointer;
  text-decoration: none;
  transition: background-color 0.15s ease;
}
.cm-boton:hover { text-decoration: none; }
.cm-boton .material-symbols-outlined { font-size: 18px; }

/* Tamaños */
.cm-boton--sm { padding: var(--cm-esp-sm) var(--cm-esp-md); }
.cm-boton--xs {
  padding: var(--cm-esp-xs) var(--cm-esp-sm);
  border-radius: var(--cm-radio);
  font-size: var(--cm-label-sm);
  line-height: 14px;
  box-shadow: none;
}
.cm-boton--bloque { display: flex; width: 100%; }

/* Variantes */
.cm-boton--primary { background: var(--cm-primario); color: var(--cm-sobre-primario); }
.cm-boton--primary:hover:not(:disabled) { background: var(--cm-primario-hover); }
.cm-boton--secondary { background: var(--cm-secundario-fondo); color: var(--cm-texto); }
.cm-boton--secondary:hover:not(:disabled) { filter: brightness(0.95); }

/* Deshabilitado del diseño (fondo lila, texto gris) */
.cm-boton:disabled {
  background: var(--cm-superficie-alta);
  color: var(--cm-texto-suave);
  cursor: not-allowed;
}

/* Mientras carga se mantiene azul */
.cm-boton--cargando:disabled {
  background: var(--cm-primario);
  color: var(--cm-sobre-primario);
  opacity: 0.85;
  cursor: progress;
}
</style>
