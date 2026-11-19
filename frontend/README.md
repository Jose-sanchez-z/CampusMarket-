# CampusMarket — Frontend

Vue 3 + Vite + Vue Router. Corre en `http://localhost:5173` y consume la API de Spring Boot en `http://localhost:8080`.

## Requisitos

- Node.js 20.19 o superior (recomendado: LTS 22).
- npm (viene con Node).

## Cómo ejecutarlo

```bash
cd frontend
npm install       # solo la primera vez
npm run dev       # abre http://localhost:5173
```

Otros comandos:

| Comando | Qué hace |
|---|---|
| `npm test` | Ejecuta las pruebas de validaciones del registro |
| `npm run build` | Genera la versión de producción en `dist/` |
| `npm run preview` | Sirve localmente la versión compilada |

## Variables de entorno (`.env`)

| Variable | Valor por defecto | Para qué sirve |
|---|---|---|
| `VITE_API_URL` | `http://localhost:8080` | URL base de la API |
| `VITE_DOMINIO_INSTITUCIONAL` | *(vacío)* | Vacío = acepta cualquier correo válido (Gmail, Hotmail…). Si se pone un dominio, solo acepta ese |
| `VITE_PASSWORD_MIN` | `8` | Longitud mínima de la contraseña |
| `VITE_API_SIMULADA` | `false` | `true` para probar la pantalla sin backend |

Para valores personales crea un `.env.local` (no se sube a git).

**Modo simulado:** con `VITE_API_SIMULADA=true` el registro no llama a la API: responde `202` y, si el correo empieza por `duplicado` (por ejemplo `duplicado@gmail.com`), responde `409`. Sirve para maquetar mientras el backend no está listo. La evidencia final de la tarea se toma con la API real.

## Estructura

```
frontend/
├── index.html
├── vite.config.js
├── .env                      # variables compartidas (sin secretos)
├── public/                   # archivos estáticos (favicon)
├── src/
│   ├── main.js               # arranque de la app
│   ├── App.vue               # componente raíz (el layout lo agrega S1-T05)
│   ├── router/index.js       # rutas: /registro, /verificar, /login
│   ├── services/
│   │   ├── api.js            # cliente HTTP (credentials: 'include')
│   │   └── authService.js    # llamadas de autenticación
│   ├── utils/validaciones.js # validaciones del cliente
│   ├── views/                # pantallas
│   └── assets/               # estilos base y logos
├── tests/                    # pruebas (node --test)
└── Diseno/                   # bocetos de diseño (S1-T01)
```

## Reglas del Sprint 1

- **Vista de celular (≤ 767 px):** registro y "¡Registro exitoso!" siguen `Diseno/boceto cel blanco.png` (P1.3 y Grupo 3). Para probarla: en Chrome presiona `F12` y luego `Ctrl + Shift + M`, y elige un teléfono (por ejemplo iPhone 12 Pro). Para verla en tu propio celular (misma red WiFi): `npm run dev -- --host` y abre en el teléfono la dirección `Network` que muestra la terminal.
- La pantalla de registro replica los mockups de `Diseno/` ("crear cuenta pantalla compu.png" y "error al crear cuenta.png"). Los textos están juntos en la constante `TEXTOS` de `RegistroView.vue`.
- La tipografía Inter y los iconos Material Symbols se cargan desde Google Fonts (`index.html`), igual que en el diseño: se necesita internet para verlos.
- La interfaz usa siempre el **modo claro**. Los colores del modo oscuro quedan definidos en `base.css` bajo `:root[data-tema='oscuro']` por si más adelante se agrega un botón para cambiar de tema.

- La sesión viaja **solo** en la cookie HttpOnly del servidor. Nunca guardes tokens en `localStorage` ni `sessionStorage`.
- Todas las peticiones salen con `credentials: 'include'`.
- El backend debe habilitar CORS para `http://localhost:5173` con `allowCredentials = true`.

## Estado de las pantallas

| Ruta | Tarea | Estado |
|---|---|---|
| `/registro` | S1-T03 (E04) | Listo |
| `/verificar` | S1-T04 (E04) | Solo mensaje de "código enviado"; falta el formulario del token |
| `/login` | S1-T06 (E05) | Marcador de posición |
