// =====================================================================
// Indica si la pantalla es de celular, para mostrar la vista móvil
// del mockup ("boceto cel blanco.png", 390 px de ancho).
// Se actualiza sola si cambia el tamaño de la ventana o se gira el teléfono.
// =====================================================================
import { onBeforeUnmount, onMounted, ref } from 'vue'

export const CONSULTA_MOVIL = '(max-width: 767px)'

export function useEsMovil(consulta = CONSULTA_MOVIL) {
  const mq = typeof window !== 'undefined' && window.matchMedia ? window.matchMedia(consulta) : null
  const esMovil = ref(mq ? mq.matches : false)

  const actualizar = (evento) => {
    esMovil.value = evento.matches
  }

  onMounted(() => mq?.addEventListener?.('change', actualizar))
  onBeforeUnmount(() => mq?.removeEventListener?.('change', actualizar))

  return esMovil
}
