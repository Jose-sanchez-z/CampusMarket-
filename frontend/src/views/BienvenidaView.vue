<script setup>
// =====================================================================
// Pantalla de bienvenida (ruta "/"), según el mockup "pantalla general compu".
// Usa el layout con encabezado (meta.layout = 'publico') y los componentes
// base de '@/components/ui'. Solo lleva estilo propio de la composición.
// =====================================================================
import { BaseButton, BaseCard } from '@/components/ui'
import logo from '@/assets/img/logo-claro.png'

// Textos y cifras de la pantalla: se ajustan aquí sin tocar el template.
// Dominio de correo: Gmail (decisión del equipo; el diseño original dice .edu).
const TEXTOS = {
  insignia: 'CampusMarket 2026',
  titulo: 'Compra y venta segura entre estudiantes',
  descripcion:
    'El marketplace universitario verificado para intercambiar tecnología, libros técnicos y suministros con validación oficial y puntos de entrega vigilados en campus.',
  crearCuenta: 'Crear cuenta universitaria',
  iniciarSesion: 'Iniciar sesión',
  cifradoTitulo: 'Cifrado de transacciones P2P',
  cifradoTexto: 'Autenticación tokenizada vinculada directamente a la identidad de cada estudiante.'
}

const ESTADISTICAS = [
  { valor: '12.4k+', etiqueta: 'Estudiantes activos' },
  { valor: '100%', etiqueta: 'Correos @gmail.com validados' },
  { valor: '$0', etiqueta: 'Comisión por retiro físico' }
]
</script>

<template>
  <div class="bienvenida">
    <section class="bienvenida__texto" aria-labelledby="titulo-bienvenida">
      <h1 id="titulo-bienvenida" class="bienvenida__titulo">{{ TEXTOS.titulo }}</h1>
      <p class="bienvenida__descripcion">{{ TEXTOS.descripcion }}</p>

      <div class="bienvenida__acciones">
        <BaseButton :to="{ name: 'registro' }" icon="arrow_forward">
          {{ TEXTOS.crearCuenta }}
        </BaseButton>
        <BaseButton :to="{ name: 'login' }" variant="secondary">
          {{ TEXTOS.iniciarSesion }}
        </BaseButton>
      </div>

      <ul class="bienvenida__estadisticas">
        <li v-for="dato in ESTADISTICAS" :key="dato.etiqueta" class="estadistica">
          <span class="estadistica__valor">{{ dato.valor }}</span>
          <span class="estadistica__etiqueta">{{ dato.etiqueta }}</span>
        </li>
      </ul>
    </section>

    <aside class="bienvenida__visual">
      <span class="bienvenida__insignia">{{ TEXTOS.insignia }}</span>
      <BaseCard tag="div" class="bienvenida__tarjeta">
        <img :src="logo" alt="CampusMarket" class="bienvenida__logo" width="200" height="200" />
        <h2 class="bienvenida__cifrado-titulo">{{ TEXTOS.cifradoTitulo }}</h2>
        <p class="bienvenida__cifrado-texto">{{ TEXTOS.cifradoTexto }}</p>
      </BaseCard>
    </aside>
  </div>
</template>

<style scoped>
.bienvenida {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(0, 1fr);
  align-items: center;
  gap: var(--cm-esp-2xl);
  padding: var(--cm-esp-xl) 0;
}

.bienvenida__titulo {
  margin: 0 0 var(--cm-esp-md);
  font-size: clamp(2rem, 4vw, 2.75rem);
  line-height: 1.15;
  font-weight: 700;
  color: var(--cm-texto);
}

.bienvenida__descripcion {
  margin: 0 0 var(--cm-esp-lg);
  max-width: 520px;
  font-size: var(--cm-texto-lg);
  line-height: 24px;
  color: var(--cm-texto-suave);
}

.bienvenida__acciones {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cm-esp-md);
  margin-bottom: var(--cm-esp-xl);
}

.bienvenida__estadisticas {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--cm-esp-md);
  margin: 0;
  padding: 0;
  list-style: none;
}

.estadistica {
  display: flex;
  flex-direction: column;
  gap: var(--cm-esp-xs);
  padding: var(--cm-esp-md);
  border-radius: var(--cm-radio-lg);
  background: var(--cm-superficie);
  border: 1px solid var(--cm-borde);
}

.estadistica__valor {
  font-size: 1.5rem;
  line-height: 1.2;
  font-weight: 700;
  color: var(--cm-primario);
}

.estadistica__etiqueta {
  font-size: var(--cm-body-sm);
  line-height: 16px;
  color: var(--cm-texto-suave);
}

.bienvenida__visual {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--cm-esp-sm);
}

.bienvenida__insignia {
  align-self: flex-end;
  padding: var(--cm-esp-xs) var(--cm-esp-sm);
  border-radius: var(--cm-radio);
  background: var(--cm-primario);
  color: var(--cm-sobre-primario);
  font-size: var(--cm-label-sm);
  line-height: 14px;
  font-weight: 600;
}

.bienvenida__tarjeta {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  gap: var(--cm-esp-md);
}

.bienvenida__logo {
  width: 200px;
  height: 200px;
  object-fit: contain;
}

.bienvenida__cifrado-titulo {
  margin: 0;
  font-size: var(--cm-texto-lg);
  line-height: 24px;
  font-weight: 700;
  color: var(--cm-texto);
}

.bienvenida__cifrado-texto {
  margin: 0;
  max-width: 320px;
  font-size: var(--cm-body-sm);
  line-height: 16px;
  color: var(--cm-texto-suave);
}

/* Tablet y celular: una sola columna */
@media (max-width: 900px) {
  .bienvenida {
    grid-template-columns: 1fr;
    gap: var(--cm-esp-xl);
  }
  .bienvenida__visual { order: -1; }
}

@media (max-width: 480px) {
  .bienvenida__estadisticas { grid-template-columns: 1fr; }
  .bienvenida__acciones > * { flex: 1 1 100%; }
}
</style>
