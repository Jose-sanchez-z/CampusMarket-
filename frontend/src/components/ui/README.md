# Componentes base (S1-T07)

Caja de piezas reutilizable de CampusMarket. **Regla:** las vistas no escriben
colores ni medidas; usan estos componentes y las variables `--cm-*` de
`src/assets/styles/tokens.css`.

```js
import { BaseButton, BaseInput, FormField, BaseAlert, BaseSpinner, BaseCard } from '@/components/ui'
```

## Ejemplo completo (formulario de login)

```vue
<BaseCard size="sm" aria-labelledby="titulo">
  <h1 id="titulo">Iniciar sesión</h1>

  <BaseAlert v-if="error" tipo="error" titulo="No se pudo iniciar sesión">{{ error }}</BaseAlert>

  <form novalidate @submit.prevent="enviar">
    <FormField id="correo" label="Correo electrónico" :error="errores.correo">
      <BaseInput v-model="form.correo" type="email" icon="mail" autocomplete="email" />
    </FormField>

    <FormField id="password" label="Contraseña" :error="errores.password">
      <BaseInput v-model="form.password" type="password" icon="key" autocomplete="current-password" />
    </FormField>

    <BaseButton type="submit" block :loading="cargando">Entrar</BaseButton>
  </form>
</BaseCard>
```

## Referencia rápida

| Componente | Props principales | Notas |
|---|---|---|
| `BaseButton` | `variant` (`primary`/`secondary`), `size` (`md`/`sm`/`xs`), `type` (por defecto `button`), `to`, `icon`, `loading`, `disabled`, `block` | Con `to` se comporta como enlace. `loading` muestra spinner, bloquea el clic y pone `aria-busy`. Para enviar formularios usa `type="submit"`. |
| `BaseInput` | `v-model`, `type`, `icon`, `iconRight`, `invalid`, `valid` | Dentro de `FormField` toma solo el `id`, el `aria-describedby` y el estado de error. Cualquier otro atributo (`placeholder`, `autocomplete`, `maxlength`, `@blur`…) va al `<input>`. Expone `.focus()`. |
| `FormField` | `id` (obligatorio), `label`, `error`, `errorIcono`, `estado`, `estadoIcono`, `valid`, `ayudaId` | Enlaza `label for`, muestra el error con `aria-live` y marca `aria-invalid` en el control. Sin `label` sirve para casillas (usar el slot: `v-slot="{ describedby, invalid }"`). |
| `BaseAlert` | `tipo` (`info`/`error`/`exito`), `titulo`, `icono` | Slot por defecto = texto; slot `acciones` = botones. El error usa `role="alert"`. |
| `BaseSpinner` | `size` (`sm`/`md`/`lg`), `etiqueta`, `decorativo` | Hereda el color del texto. Suelto se anuncia como `role="status"`. |
| `BaseCard` | `size` (`lg` 760px / `sm` 520px), `tag` | Acepta `class` y `aria-*`. |

## Accesibilidad incluida

- Etiqueta siempre asociada al control (`for` / `id`).
- Foco visible (anillo de 3px) en botones, enlaces y campos.
- Todo se maneja con teclado (son `<button>`, `<a>` e `<input>` nativos).
- Errores enlazados con `aria-describedby` y `aria-invalid`.
- `prefers-reduced-motion` ralentiza el spinner.

## Cómo agregar una pieza nueva

1. Crea `NombreDeLaPieza.vue` en esta carpeta, con estilos `scoped` que solo usen `var(--cm-...)`.
2. Expórtala en `index.js`.
3. Agrégala a la tabla de arriba.
