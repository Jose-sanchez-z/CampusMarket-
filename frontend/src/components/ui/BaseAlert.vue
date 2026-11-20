<script setup>
// Alerta en bloque: informativa, de error o de éxito.
// Slot por defecto = texto; slot `acciones` = botones debajo del texto.
import { computed } from 'vue'

const props = defineProps({
  tipo: { type: String, default: 'info', validator: (v) => ['info', 'error', 'exito'].includes(v) },
  titulo: { type: String, default: '' },
  icono: { type: String, default: '' } // si se omite, se usa el icono del tipo
})

const ICONOS = { info: 'info', error: 'error', exito: 'check_circle' }
const nombreIcono = computed(() => props.icono || ICONOS[props.tipo])
// Errores se anuncian de inmediato; el éxito de forma educada; info es contenido fijo.
const rol = computed(() => ({ error: 'alert', exito: 'status' })[props.tipo])
</script>

<template>
  <div class="aviso" :class="`aviso--${tipo}`" :role="rol">
    <span class="material-symbols-outlined aviso__icono" aria-hidden="true">{{ nombreIcono }}</span>
    <div class="aviso__cuerpo">
      <p v-if="titulo" class="aviso__titulo">{{ titulo }}</p>
      <p class="aviso__texto"><slot /></p>
      <div v-if="$slots.acciones" class="aviso__acciones"><slot name="acciones" /></div>
    </div>
  </div>
</template>

<style scoped>
.aviso {
  display: flex;
  align-items: flex-start;
  gap: var(--cm-esp-sm);
  padding: var(--cm-esp-md);
  border-radius: var(--cm-radio-lg);
}
.aviso__icono { font-size: 20px; margin-top: 2px; flex-shrink: 0; }
.aviso__cuerpo { display: flex; flex-direction: column; gap: 2px; }
.aviso__titulo { margin: 0; font-size: var(--cm-label-md); line-height: 16px; font-weight: 700; }
.aviso__texto { margin: 0; font-size: var(--cm-body-sm); line-height: 16px; }
.aviso__acciones { display: flex; flex-wrap: wrap; gap: var(--cm-esp-sm); margin-top: var(--cm-esp-sm); }

.aviso--info { background: var(--cm-info-fondo); color: var(--cm-info-texto); }
.aviso--info .aviso__titulo { font-weight: 600; }

.aviso--error { background: var(--cm-error-fondo); border: 1px solid var(--cm-error-borde); color: var(--cm-error); }
.aviso--error .aviso__icono { font-size: 22px; }

.aviso--exito { background: var(--cm-exito-fondo); border: 1px solid var(--cm-exito); color: var(--cm-exito); }
</style>
