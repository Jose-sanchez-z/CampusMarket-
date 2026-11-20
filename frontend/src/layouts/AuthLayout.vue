<!-- S1-T05 · Layout del flujo de autenticación: contenido centrado y pie institucional.
     La tarjeta con el formulario la pone cada pantalla (BaseCard). -->
<template>
  <div class="layout-auth">
    <main class="centro">
      <slot />
    </main>
    <footer class="pie">Autenticación institucional</footer>
  </div>
</template>

<style scoped>
.layout-auth {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--cm-esp-lg);
  background: var(--cm-fondo);
}
.centro { width: 100%; display: flex; justify-content: center; }
.pie { margin-top: var(--cm-esp-lg); font-size: var(--cm-body-sm); color: var(--cm-texto-suave); }

/* Celular: menos margen y sin pie */
@media (max-width: 600px) {
  .layout-auth { padding: 0; justify-content: flex-start; }
  .pie { display: none; }
}
</style>
