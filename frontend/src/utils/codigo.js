// =====================================================================
// Utilidades del código de verificación (S1-T04, HU-02).
// Funciones puras para poder probarlas con `npm test`.
// =====================================================================

export const LARGO_CODIGO = 6

/** Deja solo dígitos y como máximo 6 (sirve para pegar "482 915" o "482-915"). */
export function soloDigitos(texto, maximo = LARGO_CODIGO) {
  return String(texto ?? '').replace(/\D/g, '').slice(0, maximo)
}

/** Une las 6 casillas en un solo código. */
export function unirCodigo(digitos) {
  return (digitos ?? []).map((d) => soloDigitos(d, 1)).join('')
}

/** true si el código tiene exactamente 6 dígitos (mismo patrón que el backend: \d{6}). */
export function codigoCompleto(codigo) {
  return new RegExp(`^\\d{${LARGO_CODIGO}}$`).test(String(codigo ?? ''))
}

/** Reparte un texto pegado en las casillas, empezando por `desde`. */
export function repartirDigitos(digitos, texto, desde = 0) {
  const nuevos = [...digitos]
  const pegados = soloDigitos(texto, LARGO_CODIGO - desde)
  for (let i = 0; i < pegados.length; i++) nuevos[desde + i] = pegados[i]
  return { digitos: nuevos, siguiente: Math.min(desde + pegados.length, LARGO_CODIGO - 1) }
}

/** Milisegundos que le quedan al código (nunca negativo). */
export function tiempoRestante(enviadoEn, minutosVigencia, ahora = Date.now()) {
  return Math.max(0, enviadoEn + minutosVigencia * 60_000 - ahora)
}

/** Formatea milisegundos como mm:ss (ej. 14:59). */
export function formatearTiempo(ms) {
  const totalSegundos = Math.ceil(Math.max(0, ms) / 1000)
  const minutos = Math.floor(totalSegundos / 60)
  const segundos = totalSegundos % 60
  return `${String(minutos).padStart(2, '0')}:${String(segundos).padStart(2, '0')}`
}
