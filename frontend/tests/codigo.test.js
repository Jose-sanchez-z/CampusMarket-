// Pruebas de las utilidades del código de verificación (S1-T04).
import { test } from 'node:test'
import assert from 'node:assert/strict'
import {
  soloDigitos,
  unirCodigo,
  codigoCompleto,
  repartirDigitos,
  tiempoRestante,
  formatearTiempo
} from '../src/utils/codigo.js'

test('deja solo dígitos y como máximo 6', () => {
  assert.equal(soloDigitos('482 915'), '482915')
  assert.equal(soloDigitos('48-29-15-77'), '482915')
  assert.equal(soloDigitos('abc'), '')
  assert.equal(soloDigitos('7a', 1), '7')
})

test('une las casillas y valida el código completo (\\d{6}, como el backend)', () => {
  assert.equal(unirCodigo(['4', '8', '2', '9', '1', '5']), '482915')
  assert.ok(codigoCompleto('482915'))
  assert.equal(codigoCompleto(unirCodigo(['4', '8', '', '9', '1', '5'])), false)
  assert.equal(codigoCompleto('48291a'), false)
  assert.equal(codigoCompleto('4829151'), false)
})

test('al pegar el código completo se llenan las 6 casillas', () => {
  const { digitos, siguiente } = repartirDigitos(['', '', '', '', '', ''], 'Tu código: 482915')
  assert.deepEqual(digitos, ['4', '8', '2', '9', '1', '5'])
  assert.equal(siguiente, 5)
})

test('al pegar desde la mitad se respetan las casillas anteriores', () => {
  const { digitos } = repartirDigitos(['4', '8', '', '', '', ''], '2915', 2)
  assert.deepEqual(digitos, ['4', '8', '2', '9', '1', '5'])
})

test('tiempo restante del código de 15 minutos', () => {
  const enviado = 1_000_000
  assert.equal(tiempoRestante(enviado, 15, enviado), 15 * 60_000)
  assert.equal(tiempoRestante(enviado, 15, enviado + 60_000), 14 * 60_000)
  assert.equal(tiempoRestante(enviado, 15, enviado + 16 * 60_000), 0)
})

test('formato mm:ss del contador', () => {
  assert.equal(formatearTiempo(15 * 60_000), '15:00')
  assert.equal(formatearTiempo(14 * 60_000 + 59_000), '14:59')
  assert.equal(formatearTiempo(500), '00:01')
  assert.equal(formatearTiempo(0), '00:00')
})
