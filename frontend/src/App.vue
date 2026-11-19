<script setup>
// Componente raíz (S1-T05): elige el layout según meta.layout de la ruta.
// 'publico' = con encabezado; cualquier otra = layout de autenticación.
import { computed } from 'vue'
import { RouterView, useRoute } from 'vue-router'
import PublicLayout from '@/layouts/PublicLayout.vue'
import AuthLayout from '@/layouts/AuthLayout.vue'

const route = useRoute()
const layout = computed(() => (route.meta.layout === 'publico' ? PublicLayout : AuthLayout))
</script>

<template>
  <component :is="layout">
    <RouterView />
  </component>
</template>