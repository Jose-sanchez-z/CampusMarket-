<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { listarCategorias, crearProducto } from '@/services/productosApi'

const router = useRouter()

const form = reactive({ titulo: '', descripcion: '', categoriaId: '', precio: '' })
const imagen = ref(null) // File elegido; aún no se envía (ver S2-T08)
const errores = reactive({})
const errorGeneral = ref('')
const enviando = ref(false)

const categorias = ref([])
const cargandoCategorias = ref(true)
const errorCategorias = ref(false)

async function cargarCategorias() {
  cargandoCategorias.value = true
  errorCategorias.value = false
  try {
    categorias.value = await listarCategorias()
  } catch {
    errorCategorias.value = true
  } finally {
    cargandoCategorias.value = false
  }
}
onMounted(cargarCategorias)

function alElegirImagen(e) {
  imagen.value = e.target.files?.[0] ?? null
}

function validar() {
  Object.keys(errores).forEach((k) => delete errores[k])
  if (!form.titulo.trim()) errores.titulo = 'El título no puede estar vacío ni tener solo espacios.'
  else if (form.titulo.trim().length > 150) errores.titulo = 'El título no puede superar 150 caracteres.'
  if (!form.descripcion.trim()) errores.descripcion = 'La descripción no puede estar vacía ni tener solo espacios.'
  if (!form.categoriaId) errores.categoriaId = 'Selecciona una categoría.'
  const precio = String(form.precio).trim()
  if (!precio) errores.precio = 'El precio es obligatorio.'
  else if (!/^\d+(\.\d{1,2})?$/.test(precio) || Number(precio) <= 0)
    errores.precio = 'Ingresa un precio numérico mayor que cero.'
  if (!imagen.value) errores.imagen = 'Selecciona una imagen.'
  return Object.keys(errores).length === 0
}

// El backend responde { mensaje } sin indicar el campo: lo deducimos del texto.
function campoDe(mensaje) {
  const m = mensaje.toLowerCase()
  if (m.includes('título') || m.includes('titulo')) return 'titulo'
  if (m.includes('descripci')) return 'descripcion'
  if (m.includes('categor')) return 'categoriaId'
  if (m.includes('precio')) return 'precio'
  return null
}

async function publicar() {
  if (enviando.value) return // evita publicaciones duplicadas
  errorGeneral.value = ''
  if (!validar()) return
  enviando.value = true
  try {
    await crearProducto({
      titulo: form.titulo.trim(),
      descripcion: form.descripcion.trim(),
      categoriaId: Number(form.categoriaId),
      precio: Number(String(form.precio).trim()),
    })
    router.push({ path: '/catalogo', query: { publicado: '1' } })
  } catch (e) {
    if (e.status === 401) {
      router.push({ path: '/login', query: { redirect: '/productos/nuevo' } })
    } else if (e.status === 403) {
      errorGeneral.value = e.message || 'Tu cuenta debe estar activa para publicar.'
    } else if (e.status === 400 || e.status === 422) {
      const campo = campoDe(e.message)
      if (campo) errores[campo] = e.message
      else errorGeneral.value = e.message
    } else {
      errorGeneral.value = e.message
    }
  } finally {
    enviando.value = false
  }
}
</script>

<template>
  <main class="nuevo">
    <form class="tarjeta" novalidate @submit.prevent="publicar">
      <p class="eyebrow">NUEVA PUBLICACIÓN · HU-07</p>
      <h1>Publicar un producto en el campus</h1>
      <p class="sub">Completa los detalles técnicos y de estado para que otros estudiantes encuentren tu artículo.</p>

      <div v-if="errorGeneral" class="alerta" role="alert">{{ errorGeneral }}</div>

      <div class="campo">
        <label for="titulo">Título del producto <span class="rq">*</span></label>
        <input id="titulo" v-model="form.titulo" :class="{ mal: errores.titulo }" :disabled="enviando"
               placeholder="Ej. Calculadora Científica Casio fx-991LAX casi nueva" />
        <p v-if="errores.titulo" class="error">{{ errores.titulo }}</p>
        <p v-else class="ayuda">Sé específico: incluye marca, modelo o edición.</p>
      </div>

      <div class="fila">
        <div class="campo">
          <label for="categoria">Categoría <span class="rq">*</span></label>
          <select id="categoria" v-model="form.categoriaId" :class="{ mal: errores.categoriaId }"
                  :disabled="enviando || cargandoCategorias || errorCategorias">
            <option value="" disabled>
              {{ cargandoCategorias ? 'Cargando categorías…' : errorCategorias ? 'No se pudieron cargar' : 'Selecciona una categoría' }}
            </option>
            <option v-for="c in categorias" :key="c.id" :value="c.id">{{ c.nombre }}</option>
          </select>
          <p v-if="errorCategorias" class="error">
            No se pudieron cargar las categorías.
            <button type="button" class="enlace" @click="cargarCategorias">Reintentar</button>
          </p>
          <p v-else-if="errores.categoriaId" class="error">{{ errores.categoriaId }}</p>
        </div>
        <div class="campo">
          <label for="precio">Precio (COP) <span class="rq">*</span></label>
          <input id="precio" v-model="form.precio" inputmode="decimal" :class="{ mal: errores.precio }"
                 :disabled="enviando" placeholder="$ Ej. 85000" />
          <p v-if="errores.precio" class="error">{{ errores.precio }}</p>
        </div>
      </div>

      <div class="campo">
        <label for="descripcion">Descripción <span class="rq">*</span></label>
        <textarea id="descripcion" v-model="form.descripcion" rows="4" :class="{ mal: errores.descripcion }"
                  :disabled="enviando"
                  placeholder="Describe el estado físico, tiempo de uso, accesorios incluidos y puntos de entrega en el campus…" />
        <p v-if="errores.descripcion" class="error">{{ errores.descripcion }}</p>
      </div>

      <div class="campo">
        <label for="imagen">Fotografía del producto <span class="rq">*</span> <span class="ayuda">· JPG, PNG o WEBP</span></label>
        <input id="imagen" type="file" accept="image/jpeg,image/png,image/webp" :class="{ mal: errores.imagen }"
               :disabled="enviando" @change="alElegirImagen" />
        <p v-if="errores.imagen" class="error">{{ errores.imagen }}</p>
      </div>

      <div class="acciones">
        <button type="button" class="btn" :disabled="enviando" @click="router.push('/catalogo')">Cancelar</button>
        <button type="submit" class="btn primario" :disabled="enviando">
          {{ enviando ? 'Publicando…' : 'Publicar producto' }}
        </button>
      </div>
    </form>
  </main>
</template>

<style scoped>
.nuevo { max-width: 800px; margin: 32px auto; padding: 0 16px; font-family: Inter, sans-serif; color: #1a1c2b; }
.tarjeta { background: #fff; border-radius: 12px; padding: 32px; box-shadow: 0 1px 8px #1a1c2b10; }
.eyebrow { margin: 0; font-size: 11px; font-weight: 700; letter-spacing: .08em; color: #1d5cff; }
h1 { margin: 6px 0 8px; font-size: 32px; font-weight: 800; }
.sub { margin: 0 0 24px; color: #44475a; font-size: 15px; }
.campo { margin-bottom: 20px; }
.fila { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
label { display: block; font-size: 14px; font-weight: 600; margin-bottom: 8px; }
input, select, textarea { box-sizing: border-box; width: 100%; background: #eef0fb; border: 2px solid transparent;
  border-radius: 6px; padding: 12px 16px; font: 15px Inter, sans-serif; color: #1a1c2b; }
input.mal, select.mal, textarea.mal { border-color: #b3261e; background: #fff5f4; }
.rq { color: #b3261e; }
.error { margin: 6px 0 0; font-size: 13px; font-weight: 500; color: #b3261e; }
.ayuda { margin: 6px 0 0; font-size: 12px; font-weight: 400; color: #5c5f73; }
label .ayuda { margin: 0; }
.alerta { background: #ffd9d6; color: #8c1d18; border-radius: 10px; padding: 16px; margin-bottom: 24px; font-size: 14px; }
.acciones { display: flex; justify-content: flex-end; gap: 12px; }
.btn { border: 0; border-radius: 6px; padding: 14px 28px; font: 600 15px Inter, sans-serif; background: #e6e8f6; color: #44475a; cursor: pointer; }
.btn.primario { background: #1d5cff; color: #fff; }
.btn:disabled { opacity: .6; cursor: not-allowed; }
.enlace { background: none; border: 0; padding: 0; color: #1d5cff; font: inherit; text-decoration: underline; cursor: pointer; }
@media (max-width: 640px) { .fila { grid-template-columns: 1fr; } }
</style>
