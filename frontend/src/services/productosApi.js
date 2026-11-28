import { peticion, post } from './api'

export async function listarCategorias() {
  const { datos } = await peticion('/categorias')
  return datos
}

export async function crearProducto(producto, imagen) {
  const formulario = new FormData()
  formulario.append('producto', new Blob([JSON.stringify(producto)], { type: 'application/json' }))
  formulario.append('archivo', imagen)
  const { datos } = await post('/productos', formulario)
  return datos
}
