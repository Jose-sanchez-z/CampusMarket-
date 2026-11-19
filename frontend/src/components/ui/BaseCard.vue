<script setup>
// Tarjeta blanca de las pantallas de acceso. Clases y atributos
// (class, aria-labelledby...) pasan al elemento raíz.
defineProps({
  tag: { type: String, default: 'section' },
  size: { type: String, default: 'lg', validator: (v) => ['sm', 'lg'].includes(v) } // sm=520px, lg=760px
})
</script>

<template>
  <component :is="tag" class="cm-tarjeta" :class="`cm-tarjeta--${size}`">
    <slot />
  </component>
</template>

<style scoped>
.cm-tarjeta {
  width: 100%;
  max-width: var(--cm-ancho-tarjeta);
  background: var(--cm-superficie);
  border-radius: var(--cm-radio-lg);
  box-shadow: var(--cm-sombra);
  padding: var(--cm-esp-xl);
}
.cm-tarjeta--sm { max-width: var(--cm-ancho-tarjeta-sm); }

@media (max-width: 480px) {
  .cm-tarjeta { padding: var(--cm-esp-lg) var(--cm-esp-md); }
}
</style>
