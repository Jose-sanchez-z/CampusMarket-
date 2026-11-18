// Pruebas de las validaciones del registro (S1-T03).
// Ejecutar con: npm test   (usa el ejecutor de pruebas nativo de Node, sin dependencias)
import { test } from 'node:test'
import assert from 'node:assert/strict'
import {
  normalizarCorreo,
  normalizarNombre,
  validarNombre,
  validarCorreo,
  validarPassword,
  validarConfirmacion,
  validarRegistro,
  formularioValido,
  reglasPassword
} from '../src/utils/validaciones.js'

const DOMINIO = 'unipamplona.edu.co'

test('normaliza el correo a minúsculas y sin espacios', () => {
  assert.equal(normalizarCorreo('  Camilo.Torres@UniPamplona.edu.co '), 'camilo.torres@unipamplona.edu.co')
})

test('normaliza el nombre quitando espacios sobrantes', () => {
  assert.equal(normalizarNombre('  Camilo   Torres  '), 'Camilo Torres')
})

test('nombre vacío o muy corto es inválido', () => {
  assert.notEqual(validarNombre('   '), '')
  assert.notEqual(validarNombre('Al'), '')
  assert.equal(validarNombre('Camilo Torres'), '')
})

test('correo institucional válido', () => {
  assert.equal(validarCorreo('ctorres@unipamplona.edu.co', DOMINIO), '')
  assert.equal(validarCorreo('  CTorres@UNIPAMPLONA.EDU.CO ', DOMINIO), '')
})

test('correo de otro dominio es rechazado', () => {
  assert.match(validarCorreo('ctorres@gmail.com', DOMINIO), /institucional/)
  assert.match(validarCorreo('ctorres@unipamplona.edu.co.fake.com', DOMINIO), /institucional/)
  assert.match(validarCorreo('ctorres@fakeunipamplona.edu.co', DOMINIO), /institucional/)
})

test('correo con formato inválido es rechazado', () => {
  assert.notEqual(validarCorreo('sin-arroba', DOMINIO), '')
  assert.notEqual(validarCorreo('', DOMINIO), '')
})

test('contraseña debe cumplir todas las reglas', () => {
  assert.equal(validarPassword('Campus@2026', 8), '')
  assert.notEqual(validarPassword('corta1!', 8), '') // corta y sin mayúscula
  assert.notEqual(validarPassword('SinNumero!', 8), '')
  assert.notEqual(validarPassword('SinEspecial1', 8), '')
  assert.notEqual(validarPassword('con espacio A1!', 8), '')
})

test('las reglas de contraseña se marcan en tiempo real', () => {
  const reglas = reglasPassword('Campus@2026', 8)
  assert.ok(reglas.every((r) => r.cumple))
  assert.equal(reglasPassword('', 8).filter((r) => r.cumple).length, 0)
})

test('la confirmación debe ser igual a la contraseña', () => {
  assert.equal(validarConfirmacion('Campus@2026', 'Campus@2026'), '')
  assert.match(validarConfirmacion('Campus@2026', 'Campus@2024'), /no coinciden/)
  assert.notEqual(validarConfirmacion('Campus@2026', ''), '')
})

test('formulario completo válido', () => {
  const errores = validarRegistro(
    {
      nombre: 'Camilo Torres',
      correo: 'ctorres@unipamplona.edu.co',
      password: 'Campus@2026',
      confirmacion: 'Campus@2026',
      terminos: true
    },
    { dominio: DOMINIO, passwordMin: 8 }
  )
  assert.ok(formularioValido(errores))
})

test('formulario sin aceptar términos es inválido', () => {
  const errores = validarRegistro(
    {
      nombre: 'Camilo Torres',
      correo: 'ctorres@unipamplona.edu.co',
      password: 'Campus@2026',
      confirmacion: 'Campus@2026',
      terminos: false
    },
    { dominio: DOMINIO, passwordMin: 8 }
  )
  assert.equal(formularioValido(errores), false)
  assert.notEqual(errores.terminos, '')
})
