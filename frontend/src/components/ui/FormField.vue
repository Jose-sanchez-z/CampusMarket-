<script setup>
// Grupo etiqueta + control + mensaje de error. Deja el id, aria-describedby y
// el estado a los BaseInput que lleve dentro (y los expone también al slot).
import { computed, provide } from 'vue'

const props = defineProps({
  id: { type: String, required: true }, // id del control (enlaza label for="")
  label: { type: String, default: '' }, // sin label no se dibuja la etiqueta (p. ej. casillas)
  error: { type: String, default: '' },
  errorIcono: { type: String, default: 'close' }, // '' = sin icono
  estado: { type: String, default: '' }, // texto corto a la derecha de la etiqueta ("Válida")
  estadoIcono: { type: String, default: '' },
  valid: { type: Boolean, default: false },
  ayudaId: { type: String, default: '' } // id de un texto de ayuda a anunciar junto al control
})

const errorId = computed(() => `${props.id}-error`)
const invalid = computed(() => Boolean(props.error))
const describedby = computed(
  () => [props.ayudaId, invalid.value ? errorId.value : ''].filter(Boolean).join(' ') || undefined
)

provide('cm-form-field', {
  id: computed(() => props.id),
  describedby,
  invalid,
  valid: computed(() => props.valid)
})
</script>

<template>
  <div class="campo" :class="{ 'campo--error': invalid, 'campo--valido': valid && !invalid }">
    <label v-if="label" :for="id" class="campo__etiqueta">
      <span>{{ label }}</span>
      <span v-if="estado" class="campo__estado">
        <span v-if="estadoIcono" class="material-symbols-outlined" aria-hidden="true">{{ estadoIcono }}</span>
        {{ estado }}
      </span>
    </label>

    <slot :id="id" :describedby="describedby" :invalid="invalid" />

    <p v-if="error" :id="errorId" class="campo__error" aria-live="polite">
      <span v-if="errorIcono" class="material-symbols-outlined" aria-hidden="true">{{ errorIcono }}</span>{{ error }}
    </p>
  </div>
</template>

<style scoped>
.campo { display: flex; flex-direction: column; gap: var(--cm-esp-xs); }

.campo__etiqueta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--cm-esp-sm);
  font-size: var(--cm-label-md);
  line-height: 16px;
  font-weight: 500;
  color: var(--cm-texto);
}

.campo__estado {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: var(--cm-label-sm);
  line-height: 14px;
  font-weight: 600;
}
.campo__estado .material-symbols-outlined { font-size: 14px; }

.campo__error {
  display: flex;
  align-items: flex-start;
  gap: 4px;
  margin: 0;
  font-size: var(--cm-body-sm);
  line-height: 16px;
  color: var(--cm-error);
}
.campo__error .material-symbols-outlined { font-size: 14px; margin-top: 1px; }

/* Error: etiqueta roja en negrita */
.campo--error .campo__etiqueta { color: var(--cm-error); font-weight: 700; }

/* Válido: texto de estado en azul */
.campo--valido .campo__estado { color: var(--cm-primario); }
</style>
