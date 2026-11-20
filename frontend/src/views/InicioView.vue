<!-- Página privada de ejemplo (S1-T05): solo se ve con sesión iniciada -->
<script setup>
import { useAuth } from '@/stores/auth'
const { usuario } = useAuth()
</script>

<template>
  <section>
    <h1>Inicio</h1>
    <p>Sesión iniciada{{ usuario?.nombre ? ' como ' + usuario.nombre : '' }}.</p>
  </section>
</template>