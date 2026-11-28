// =====================================================================
// Servicio de productos (S2-T05, E05).
// La ruta real y los campos de la API están por confirmar con el contrato
// de S2-T11 (backend). Con VITE_API_SIMULADA=true se usan datos de ejemplo.
// Para probar el estado de error en modo simulado, escribe "error" en el buscador.
// =====================================================================
import { API_URL, peticion, ApiError } from './api'

const SIMULADA = import.meta.env.VITE_API_SIMULADA === 'true'

// Categorías del filtro (según el mockup HU-07)
export const CATEGORIAS_FILTRO = [
  { id: 'todos', nombre: 'Todos' },
  { id: 'libros', nombre: 'Libros y Apuntes' },
  { id: 'tecnologia', nombre: 'Tecnología' },
  { id: 'accesorios', nombre: 'Accesorios' },
  { id: 'snacks', nombre: 'Snacks' },
]

export async function listarProductos({ busqueda = '', categoria = 'todos' } = {}) {
  if (SIMULADA) return listarSimulado({ busqueda, categoria })

  // RUTA PROVISIONAL: ajustar cuando exista el contrato de productos
  const params = new URLSearchParams()
  if (busqueda) params.set('busqueda', busqueda)
  if (categoria !== 'todos') params.set('categoria', categoria)
  const consulta = params.toString()
  const { datos } = await peticion(`/productos${consulta ? '?' + consulta : ''}`)
  if (!Array.isArray(datos)) return []
  return datos.map((producto) => ({
    ...producto,
    imagen: producto.imagenPrincipal ? API_URL + producto.imagenPrincipal : producto.imagen,
  }))
}

// ---------------------------------------------------------------------
// Datos de ejemplo
// ---------------------------------------------------------------------
const PRODUCTOS = [
  { id: 1, titulo: 'Cálculo Multivariable Stewart', descripcion: '8va Edición · Buen estado', precio: 45000, categoria: 'libros' },
  { id: 2, titulo: 'Calculadora Gráfica TI-84', descripcion: 'Incluye cable USB · Como nueva', precio: 280000, categoria: 'tecnologia' },
  { id: 3, titulo: 'Morral Antirrobo 15.6', descripcion: 'Impermeable · Puerto USB', precio: 95000, categoria: 'accesorios' },
  { id: 4, titulo: 'Bata de Laboratorio', descripcion: 'Talla M · 100% Algodón', precio: 38000, categoria: 'accesorios' },
  { id: 5, titulo: 'Mouse Inalámbrico Logitech', descripcion: 'Bluetooth · Batería incluida', precio: 65000, categoria: 'tecnologia' },
  { id: 6, titulo: 'Apuntes Física Mecánica', descripcion: 'Guía completa + parciales', precio: 18000, categoria: 'libros' },
  { id: 7, titulo: 'Pack Brownies Artesanales x4', descripcion: 'Entrega en plazoleta central', precio: 16000, categoria: 'snacks' },
  { id: 8, titulo: 'Adaptador USB-C Multipuerto', descripcion: 'HDMI 4K + 3 USB 3.0', precio: 72000, categoria: 'tecnologia' },
]

const esperar = (ms) => new Promise((resolver) => setTimeout(resolver, ms))
const normalizar = (texto) => texto.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase()

async function listarSimulado({ busqueda, categoria }) {
  await esperar(700)
  if (normalizar(busqueda) === 'error') {
    throw new ApiError(0, null, 'No fue posible conectar con el servidor.')
  }
  const texto = normalizar(busqueda)
  return PRODUCTOS.filter(
    (p) =>
      (categoria === 'todos' || p.categoria === categoria) &&
      (!texto || normalizar(p.titulo).includes(texto))
  )
}