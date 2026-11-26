// =====================================================================
// Validaciones de la recuperación de contraseña (S2-T03, HU-04).
// Funciones puras (sin Vue) para poder probarlas con `npm test`.
//
// Contrato (S2-T09, E07 — docs/api/auth-contrato.yaml):
//   - El token de recuperación NO es de 6 dígitos: son 43 caracteres
//     base64 url-safe (letras, números, "-" y "_").
//   - La nueva contraseña debe tener entre 8 y 72 caracteres. En el cliente
//     se exigen además las mismas reglas del registro (mayúscula, número y
//     carácter especial), que son las que muestra el diseño de Yari.
// =====================================================================
import { validarPassword, validarConfirmacion, reglasPassword } from './validaciones.js'

// Largo del token que genera el backend (32 bytes en base64 sin relleno).
export const TOKEN_LARGO = 43
const FORMATO_TOKEN = /^[A-Za-z0-9_-]+$/

/**
 * Limpia el token pegado desde el correo: quita espacios, saltos de línea
 * y comillas que a veces se copian sin querer.
 */
export function normalizarToken(token) {
  return String(token ?? '').replace(/[\s"'«»]/g, '')
}

export function validarToken(token) {
  const valor = normalizarToken(token)
  if (!valor) return 'Pega el código que te enviamos al correo.'
  if (!FORMATO_TOKEN.test(valor)) return 'El código solo tiene letras, números, guiones (-) y guiones bajos (_).'
  if (valor.length !== TOKEN_LARGO) {
    return `El código debe tener ${TOKEN_LARGO} caracteres (tiene ${valor.length}). Cópialo completo desde el correo.`
  }
  return ''
}

/** Valida el formulario de "Crear nueva contraseña" y devuelve { campo: mensaje }. */
export function validarNuevaContrasena(datos, { passwordMin = 8 } = {}) {
  return {
    token: validarToken(datos.token),
    password: validarPassword(datos.password, passwordMin),
    confirmacion: validarConfirmacion(datos.password, datos.confirmacion)
  }
}

/**
 * Nivel de seguridad de la contraseña para la barra del diseño
 * ("Nivel: Muy segura"). Se basa en cuántas reglas cumple.
 * @returns {{ nivel: 0|1|2|3|4, texto: string }}
 */
export function nivelPassword(password, minimo = 8) {
  const valor = String(password ?? '')
  if (!valor) return { nivel: 0, texto: '' }
  const cumplidas = reglasPassword(valor, minimo).filter((regla) => regla.cumple).length
  const TEXTOS = ['Muy débil', 'Débil', 'Media', 'Segura', 'Muy segura']
  return { nivel: cumplidas, texto: TEXTOS[cumplidas] }
}
