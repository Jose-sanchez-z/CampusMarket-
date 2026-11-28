import { api } from '@/services/api'

// GET /categorias -> [{ id, nombre, activa }] (el backend ya devuelve solo las activas)
export const listarCategorias = () => api('/categorias')

// POST /productos (requiere sesión) -> 201 con el ProductoResponse; el producto nace DISPONIBLE (CA-07-01)
// OJO: hoy el backend recibe JSON { titulo, descripcion, categoriaId, precio }, no multipart,
// y todavía no tiene endpoint de imágenes. Cuando E08 lo publique (S2-T08), se sube aquí.
export const crearProducto = (datos) => api('/productos', { method: 'POST', body: datos })

// GET /auth/yo -> { id, nombre, correo, rol }; lanza ApiError 401 si no hay sesión
export const obtenerYo = () => api('/auth/yo')
