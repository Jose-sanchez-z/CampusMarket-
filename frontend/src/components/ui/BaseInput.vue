<script setup>
// Campo de texto con icono opcional. Úsalo con v-model.
// Dentro de <FormField> toma solos el id, aria-describedby y el estado de error/válido.
// El resto de atributos (type, placeholder, autocomplete, maxlength, disabled,
// @blur, @input...) pasan directo al <input>.
import { computed, inject, ref } from 'vue'

defineOptions({ inheritAttrs: false })

const props = defineProps({
  type: { type: String, default: 'text' },
  icon: { type: String, default: '' }, // icono a la izquierda
  iconRight: { type: String, default: '' }, // icono de estado a la derecha
  invalid: { type: Boolean, default: false }, // fuerza el estado de error fuera de FormField
  valid: { type: Boolean, default: false }
})

const model = defineModel({ type: String, default: '' })
const campo = inject('cm-form-field', null)
const entrada = ref(null)

const inputId = computed(() => campo?.id.value)
const describedby = computed(() => campo?.describedby.value)
const esInvalido = computed(() => props.invalid || Boolean(campo?.invalid.value))
const esValido = computed(() => props.valid || Boolean(campo?.valid.value))

defineExpose({ focus: () => entrada.value?.focus() })
</script>

<template>
  <div class="entrada" :class="{ 'entrada--error': esInvalido, 'entrada--valido': esValido && !esInvalido }">
    <span v-if="icon" class="material-symbols-outlined entrada__icono" aria-hidden="true">{{ icon }}</span>
    <input
      :id="inputId"
      ref="entrada"
      v-model="model"
      :type="type"
      class="entrada__input"
      :class="{ 'entrada__input--derecha': iconRight, 'entrada__input--sin-icono': !icon }"
      :aria-invalid="esInvalido"
      :aria-describedby="describedby"
      v-bind="$attrs"
    />
    <span v-if="iconRight" class="material-symbols-outlined entrada__icono entrada__icono--derecha" aria-hidden="true">{{ iconRight }}</span>
  </div>
</template>

<style scoped>
.entrada { position: relative; display: flex; align-items: center; }

.entrada__icono {
  position: absolute;
  left: 12px;
  font-size: 20px;
  color: var(--cm-texto-suave);
  pointer-events: none;
}
.entrada__icono--derecha { left: auto; right: 12px; font-size: 18px; }

.entrada__input {
  width: 100%;
  padding: var(--cm-esp-sm) var(--cm-esp-md) var(--cm-esp-sm) 40px;
  border: 1px solid transparent;
  border-radius: var(--cm-radio);
  background: var(--cm-campo);
  color: var(--cm-texto);
  font-family: inherit;
  font-size: var(--cm-body-md);
  line-height: 20px;
  outline: none;
}
.entrada__input--sin-icono { padding-left: var(--cm-esp-md); }
.entrada__input--derecha { padding-right: 40px; }
.entrada__input::placeholder { color: var(--cm-texto-suave); opacity: 0.7; }
.entrada__input:disabled { opacity: 0.6; }

/* Foco: borde azul + anillo visible para teclado */
.entrada__input:focus { border-color: var(--cm-primario); }
.entrada__input:focus-visible { outline: 3px solid var(--cm-foco); outline-offset: 1px; }

/* Error: borde e iconos rojos */
.entrada--error .entrada__input { border-color: var(--cm-error-borde); box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05); }
.entrada--error .entrada__icono { color: var(--cm-error); }

/* Válido: borde e iconos azules */
.entrada--valido .entrada__input { border-color: var(--cm-primario); }
.entrada--valido .entrada__icono { color: var(--cm-primario); }
</style>
