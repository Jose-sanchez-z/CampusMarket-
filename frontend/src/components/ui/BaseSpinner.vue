<script setup>
// Indicador de carga. Toma el color del texto que lo rodea (currentColor).
// - decorativo: úsalo dentro de otro elemento que ya avisa que está cargando (p. ej. BaseButton).
// - sin decorativo: se anuncia a lectores de pantalla con el texto de `etiqueta`.
defineProps({
  size: { type: String, default: 'md', validator: (v) => ['sm', 'md', 'lg'].includes(v) },
  etiqueta: { type: String, default: 'Cargando' },
  decorativo: { type: Boolean, default: false }
})
</script>

<template>
  <span
    class="cm-spinner"
    :class="`cm-spinner--${size}`"
    :role="decorativo ? undefined : 'status'"
    :aria-hidden="decorativo ? 'true' : undefined"
  >
    <span v-if="!decorativo" class="sr-only">{{ etiqueta }}</span>
  </span>
</template>

<style scoped>
.cm-spinner {
  display: inline-block;
  width: 18px;
  height: 18px;
  flex-shrink: 0;
  border: 2px solid color-mix(in srgb, currentColor 30%, transparent);
  border-top-color: currentColor;
  border-radius: 50%;
  animation: cm-girar 0.8s linear infinite;
}
.cm-spinner--sm { width: 14px; height: 14px; }
.cm-spinner--lg { width: 32px; height: 32px; border-width: 3px; }

@keyframes cm-girar {
  to { transform: rotate(360deg); }
}

@media (prefers-reduced-motion: reduce) {
  .cm-spinner { animation-duration: 2s; }
}
</style>
