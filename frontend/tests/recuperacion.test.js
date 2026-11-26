// Pruebas de la recuperación de contraseña (S2-T03, HU-04).
// Ejecutar con: npm test
import { test } from 'node:test'
import assert from 'node:assert/strict'
import {
  TOKEN_LARGO,
  normalizarToken,
  validarToken,
  validarNuevaContrasena,
  nivelPassword
} from '../src/utils/recuperacion.js'
import { formularioValido } from '../src/utils/validaciones.js'

// Token con el formato real del backend (43 caracteres base64 url-safe).
const TOKEN_OK = 'k3J9x0Qm1b7VtYc2sL8nPzW4aRu6HdEeFgIoXjMqNlA'

test('el token de ejemplo del contrato tiene el largo esperado', () => {
  assert.equal(TOKEN_OK.length, TOKEN_LARGO)
})

test('normalizarToken quita espacios, saltos de línea y comillas del pegado', () => {
  assert.equal(normalizarToken(`  "${TOKEN_OK.slice(0, 20)}\n${TOKEN_OK.slice(20)}" `), TOKEN_OK)
  assert.equal(normalizarToken(null), '')
})

test('validarToken acepta un token válido y también con guiones', () => {
  assert.equal(validarToken(TOKEN_OK), '')
  assert.equal(validarToken('A-_' + TOKEN_OK.slice(3)), '')
})

test('validarToken rechaza vacío, caracteres raros y largo incorrecto', () => {
  assert.match(validarToken(''), /Pega el código/)
  assert.match(validarToken(TOKEN_OK.slice(0, 42) + '+'), /solo tiene letras/)
  assert.match(validarToken(TOKEN_OK.slice(0, 10)), /43 caracteres \(tiene 10\)/)
  assert.match(validarToken('123456'), /43 caracteres/) // no es un código de 6 dígitos
})

test('validarNuevaContrasena: formulario correcto', () => {
  const errores = validarNuevaContrasena({ token: TOKEN_OK, password: 'Clave#2025', confirmacion: 'Clave#2025' })
  assert.ok(formularioValido(errores))
})

test('validarNuevaContrasena: contraseñas distintas y clave débil', () => {
  const errores = validarNuevaContrasena({ token: TOKEN_OK, password: 'clave', confirmacion: 'otra' })
  assert.notEqual(errores.password, '')
  assert.match(errores.confirmacion, /no coinciden/)
  assert.equal(errores.token, '')
})

test('validarNuevaContrasena respeta el máximo de 72 caracteres del backend', () => {
  const larga = 'Aa1#' + 'x'.repeat(70)
  const errores = validarNuevaContrasena({ token: TOKEN_OK, password: larga, confirmacion: larga })
  assert.match(errores.password, /72/)
})

test('nivelPassword sube con cada regla cumplida', () => {
  assert.deepEqual(nivelPassword(''), { nivel: 0, texto: '' })
  assert.equal(nivelPassword('abc').nivel, 0)
  assert.equal(nivelPassword('abcdefgh').nivel, 1)
  assert.equal(nivelPassword('Abcdefgh').nivel, 2)
  assert.equal(nivelPassword('Abcdefg1').nivel, 3)
  assert.deepEqual(nivelPassword('Abcdef1!'), { nivel: 4, texto: 'Muy segura' })
})
