// =====================================================================
// Validaciones del lado del cliente para el registro (S1-T03, HU-01).
// Son funciones puras (sin Vue) para poder probarlas con `npm test`.
// Importante: estas validaciones mejoran la experiencia del usuario, pero
// la validación definitiva la hace siempre el backend (S1-T09).
// Cada validador devuelve un mensaje de error o '' si el valor es válido.
// =====================================================================

// Límites tomados de la migración V1 (tabla usuarios).
export const NOMBRE_MAX = 120
export const CORREO_MAX = 255

// Formato general de correo (mismo criterio que el CHECK de la base de datos).
const FORMATO_CORREO = /^[^@\s]+@[^@\s]+\.[^@\s]+$/

/** Quita espacios y pasa a minúsculas: la BD solo acepta correos en minúsculas. */
export function normalizarCorreo(correo) {
  return String(correo ?? '').trim().toLowerCase()
}

/** Quita espacios al inicio y al final, y deja un solo espacio entre palabras. */
export function normalizarNombre(nombre) {
  return String(nombre ?? '').trim().replace(/\s+/g, ' ')
}

export function validarNombre(nombre) {
  const valor = normalizarNombre(nombre)
  if (!valor) return 'Escribe tu nombre completo.'
  if (valor.length < 3) return 'El nombre es demasiado corto.'
  if (valor.length > NOMBRE_MAX) return `El nombre no puede superar ${NOMBRE_MAX} caracteres.`
  return ''
}

/**
 * Valida el formato del correo. Si se indica `dominio`, además exige que el
 * correo sea de ese dominio; si viene vacío, se acepta cualquier correo válido.
 */
export function validarCorreo(correo, dominio) {
  const valor = normalizarCorreo(correo)
  const dominioEsperado = String(dominio ?? '').trim().toLowerCase().replace(/^@/, '')
  if (!valor) return 'Escribe tu correo electrónico.'
  if (valor.length > CORREO_MAX) return 'El correo es demasiado largo.'
  if (!FORMATO_CORREO.test(valor)) return 'El formato del correo no es válido.'
  if (dominioEsperado && !valor.endsWith(`@${dominioEsperado}`)) {
    return `Usa tu correo institucional (@${dominioEsperado}).`
  }
  return ''
}

/**
 * Reglas de seguridad de la contraseña (las mismas que muestra el diseño).
 * Devuelve la lista de reglas con su estado para pintarlas en tiempo real.
 */
export function reglasPassword(password, minimo = 8) {
  const valor = String(password ?? '')
  return [
    { id: 'longitud', texto: `Mín. ${minimo} caracteres`, cumple: valor.length >= minimo },
    { id: 'mayuscula', texto: 'Una mayúscula', cumple: /[A-ZÁÉÍÓÚÑ]/.test(valor) },
    { id: 'numero', texto: 'Un número', cumple: /\d/.test(valor) },
    { id: 'especial', texto: 'Carácter especial', cumple: /[^A-Za-z0-9ÁÉÍÓÚÑáéíóúñ\s]/.test(valor) }
  ]
}

export function validarPassword(password, minimo = 8) {
  const valor = String(password ?? '')
  if (!valor) return 'Escribe una contraseña.'
  if (/\s/.test(valor)) return 'La contraseña no puede contener espacios.'
  const pendientes = reglasPassword(valor, minimo).filter((regla) => !regla.cumple)
  if (pendientes.length) {
    return `La contraseña debe tener: ${pendientes.map((r) => r.texto.toLowerCase()).join(', ')}.`
  }
  return ''
}

export function validarConfirmacion(password, confirmacion) {
  if (!confirmacion) return 'Confirma tu contraseña.'
  if (password !== confirmacion) {
    return 'Las contraseñas no coinciden. Por favor asegúrate de que sean idénticas.'
  }
  return ''
}

/**
 * Valida todo el formulario y devuelve un objeto { campo: mensaje }.
 * `requiereTerminos` = false cuando la vista no muestra la casilla de términos
 * (el mockup del celular no la incluye).
 */
export function validarRegistro(datos, { dominio, passwordMin = 8, requiereTerminos = true } = {}) {
  return {
    nombre: validarNombre(datos.nombre),
    correo: validarCorreo(datos.correo, dominio),
    password: validarPassword(datos.password, passwordMin),
    confirmacion: validarConfirmacion(datos.password, datos.confirmacion),
    terminos: !requiereTerminos || datos.terminos ? '' : 'Debes aceptar los términos de uso para continuar.'
  }
}

/** true si el objeto de errores no tiene ningún mensaje. */
export function formularioValido(errores) {
  return Object.values(errores).every((mensaje) => !mensaje)
}
