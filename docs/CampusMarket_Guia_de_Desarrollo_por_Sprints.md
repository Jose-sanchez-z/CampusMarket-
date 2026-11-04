CampusMarket
Guía de desarrollo por sprints: historias de usuario y tareas detalladas


Documento de trabajo dirigido al equipo de desarrollo. Reúne, sprint por sprint, las historias de usuario
con sus criterios de aceptación y cada tarea técnica explicada paso a paso, para que quien la reciba
entienda qué construir, por qué y cuándo está terminada.

Este documento es de trabajo y no sustituye al SRS, PRD ni TRD.
 1. Información general
 CampusMarket es una plataforma para que estudiantes de una misma comunidad universitaria compren
 y vendan artículos entre sí: se registran con correo institucional, publican productos con fotografías, se
 contactan, manifiestan interés, confirman ventas y se califican.


 Metodología y alcance
 • Se trabaja con Scrum, en 4 sprints de 1 semana cada uno.
 • Los story points estiman complejidad relativa, no horas. Los puntos de historia no se convierten
   automáticamente en horas.
 • Prioridad Debe significa incluido en el MVP. Prioridad Debería se negocia si hay riesgo de tiempo;
   solo HU-15 es 'Debería'.
 • En total hay 15 historias de usuario (HU-01 a HU-15, 59 puntos) y 56 tareas técnicas (14 por
   sprint).
 • Cada sprint reparte 63 horas efectivas entre E03 y E09 (9 horas por persona). Son 252 horas en
   total. Esta cifra supone 12 horas brutas semanales por desarrollador y 3 horas de ceremonias y
   coordinación; no es disponibilidad confirmada.


 Equipo
 • E01: Product Owner. Decide prioridades y resuelve dudas sobre las reglas del negocio.
 • E02: Scrum Master. Facilita las ceremonias y desbloquea al equipo.
 • E03: Diseño UX/UI y QA.
 • E04: Frontend, identidad (registro, verificación, login, recuperación, perfil).
 • E05: Frontend, catálogo (layout, rutas, catálogo, categorías, filtros).
 • E06: Frontend, integración (componentes, conexión con la API, ventas y calificaciones).
 • E07: Backend, identidad (autenticación, tokens, perfil, interés y venta).
 • E08: Backend, negocio (sesión, productos, categorías, descuentos, calificaciones, moderación).
 • E09: Backend y base de datos (migraciones, integridad, pruebas de datos y despliegue).


 Cómo leer cada tarea
 Cada tarea comienza con un bloque gris con sus datos de control, seguido de una explicación en cuatro
 partes (Qué se busca, Qué debes hacer, Reglas que no puedes saltarte y Listo cuando). Estos son los
 campos y partes:
 • Historia(s): las historias de usuario que la tarea ayuda a completar. Búscalas en la sección del sprint.
 • Componente: el área del sistema (Diseño, Frontend, Backend, BD, Integración, QA, DevOps).
 • Responsable y Colaboradores: quién la hace y con quién debe coordinarse. Si dudas, empieza por
   preguntarle a un colaborador.
 • Estimación: horas efectivas propuestas. Día objetivo: día de la semana del sprint en que debe estar
   lista (D1 es el primer día y D5 el último).
 • Dependencia: lo que debe existir antes de empezar. Entregable / evidencia: lo que debes mostrar
   como prueba (PR/commit más una captura o una prueba ejecutada). Estado inicial: Pendiente.
 • Qué se busca: el propósito de la tarea en lenguaje sencillo.
 • Qué debes hacer: los pasos concretos.
 • Reglas que no puedes saltarte: los criterios de aceptación y reglas del negocio que la tarea debe
   respetar (recuadro amarillo).




CampusMarket - Guía de desarrollo por sprints                                                        Página 2
 • Listo cuando: la condición para dar la tarea por terminada (recuadro verde).


 Resumen de los sprints
 Sprint 1: Registro, token de verificación e inicio de sesión. Historias: HU-01, HU-02, HU-03 (13 puntos).
 14 tareas, 63 horas efectivas.
 Sprint 2: Recuperación, perfil, categorías y publicación con fotografías. Historias: HU-04, HU-05, HU-06,
 HU-07, HU-08 (21 puntos). 14 tareas, 63 horas efectivas.
 Sprint 3: Catálogo, descuentos, interés, venta y calificaciones. Historias: HU-09, HU-10, HU-11, HU-12,
 HU-13, HU-14 (22 puntos). 14 tareas, 63 horas efectivas.
 Sprint 4: Moderación negociable, pruebas, correcciones y demostración. Historias: HU-15 (3 puntos). 14
 tareas, 63 horas efectivas.




CampusMarket - Guía de desarrollo por sprints                                                        Página 3
 Sprint 1 - Semana 1
 Objetivo: Registro, token de verificación e inicio de sesión.
 Carga: 63 horas efectivas, 14 tareas, 3 historia(s) de usuario (13 puntos).
 Contexto: Este sprint construye la base de todo lo demás: cuentas, sesión y base de datos. Si algo falla
 aquí, se atrasan los otros tres sprints.


 Historias de usuario del sprint
 HU-01 - Solicitar registro
 Historia: Como estudiante nuevo, quiero registrarme con mi correo institucional, para crear una cuenta
 verificada.
 Épica: Identidad | Prioridad: Debe | Story points: 5 | Sprint: 1
 Requisito(s): RF-AUT-01 | Flujo: F01 | API: POST /auth/registro
 Dependencias funcionales: Ninguna funcional; infraestructura y contrato de datos mínimo.

 Criterios de aceptación (Dado / Cuando / Entonces)
 • CA-01-01. Dado un correo institucional válido y no registrado, cuando envío el formulario completo,
   entonces se crea una cuenta PENDIENTE y se responde 202 sin iniciar sesión.
 • CA-01-02. Dado un correo ya registrado, cuando intento registrarlo de nuevo, entonces se rechaza el
   duplicado sin crear otra cuenta.
 Evidencia esperada: registro del caso CP-01 con resultado, captura o prueba automatizada; demo frontend + API +
 BD en el Sprint Review.

 HU-02 - Verificar cuenta y reenviar código
 Historia: Como estudiante registrado, quiero confirmar un token y solicitar otro si vence, para activar
 mi acceso.
 Épica: Identidad | Prioridad: Debe | Story points: 5 | Sprint: 1
 Requisito(s): RF-AUT-02, RF-AUT-03 | Flujo: F02/F03 | API: POST /auth/verificar; POST /auth/reenviartoken
 Dependencias funcionales: HU-01 o componentes previos del sprint.

 Criterios de aceptación (Dado / Cuando / Entonces)
 • CA-02-01. Dado una cuenta PENDIENTE y un token vigente, cuando ingreso el token correcto, entonces la
   cuenta queda ACTIVA y el token queda inutilizado.
 • CA-02-02. Dado un token vencido o usado, cuando lo intento validar, entonces la cuenta no se activa y
   aparece un error controlado.
 Evidencia esperada: registro del caso CP-02 con resultado, captura o prueba automatizada; demo frontend + API +
 BD en el Sprint Review.




CampusMarket - Guía de desarrollo por sprints                                                            Página 4
 HU-03 - Iniciar y cerrar sesión
 Historia: Como estudiante verificado, quiero autenticarme y salir de mi sesión, para acceder a mis
 funciones protegidas.
 Épica: Identidad | Prioridad: Debe | Story points: 3 | Sprint: 1
 Requisito(s): RF-AUT-04 | Flujo: F04 | API: POST /auth/login; POST /auth/logout
 Dependencias funcionales: HU-02 o componentes previos del sprint.

 Criterios de aceptación (Dado / Cuando / Entonces)
 • CA-03-01. Dado una cuenta ACTIVA con credenciales correctas, cuando inicio sesión, entonces se crea una
   cookie de sesión HttpOnly.
 • CA-03-02. Dado una cuenta PENDIENTE, cuando intento ingresar, entonces se deniega el acceso.
 • CA-03-03. Dado una sesión autenticada, cuando cierro sesión, entonces la sesión se revoca.
 Evidencia esperada: registro del caso CP-03 con resultado, captura o prueba automatizada; demo frontend + API +
 BD en el Sprint Review.




CampusMarket - Guía de desarrollo por sprints                                                            Página 5
 Sprint 1: tareas técnicas
 S1-T01 - Bocetos de registro, verificación y login
   Historia(s): HU-01/HU-02
   Componente: Diseño
   Responsable: E03
   Colaboradores: E04/E05
   Estimación: 4h
   Día objetivo: D2
   Dependencia: Backlog aprobado y objetivos del sprint
   Entregable / evidencia: PR/commit + captura o prueba ejecutada
   Estado inicial: Pendiente

 Qué se busca
 Que los desarrolladores de frontend tengan una referencia visual clara de las tres primeras pantallas y
 no tengan que adivinar nada.
 Qué debes hacer
 • Dibujar el boceto (wireframe) de registro: nombre, correo institucional, contraseña y confirmación de
   contraseña.
 • Dibujar la pantalla de verificación: campo para el token y botón de 'reenviar código'.
 • Dibujar la pantalla de login.
 • Para cada pantalla, mostrar sus estados: normal, cargando, error y éxito.
 • Definir los textos de los mensajes: correo duplicado, token vencido o usado, credenciales incorrectas
   y cuenta pendiente.

   Reglas que no puedes saltarte: Tras registrarse, el usuario no entra a la plataforma: pasa a la
   pantalla de verificación (CA-01-01). Un token vencido o usado muestra un error controlado, no una
   pantalla rota (CA-02-02).

   Listo cuando: E04 y E05 pueden construir las pantallas sin hacerte preguntas y los bocetos están en
   una carpeta compartida.


 S1-T02 - Guía visual y prototipo navegable
   Historia(s): HU-03
   Componente: Diseño
   Responsable: E03
   Colaboradores: E04/E05
   Estimación: 5h
   Día objetivo: D4
   Dependencia: Backlog aprobado y objetivos del sprint
   Entregable / evidencia: PR/commit + captura o prueba ejecutada
   Estado inicial: Pendiente

 Qué se busca
 Definir cómo se ve CampusMarket (colores, letras, botones) para que todo el equipo construya con el
 mismo estilo, y mostrar el recorrido completo del usuario.




CampusMarket - Guía de desarrollo por sprints                                                       Página 6
 Qué debes hacer
 • Definir la paleta de colores, la tipografía, los espaciados y el estilo de botones, campos de texto y
   alertas.
 • Armar un prototipo clicable (Figma o similar) que una registro, verificación, login y una pantalla de
   inicio básica.
 • Incluir las variantes del login: credenciales incorrectas, cuenta pendiente (CA-03-02) y sesión cerrada.
 • Entregar la guía en un formato fácil de consultar (valores hexadecimales, tamaños en px/rem).

   Listo cuando: E06 puede construir los componentes base (S1-T07) leyendo solo tu guía, y el
   prototipo recorre el flujo completo.


 S1-T03 - Estructura de frontend y formulario registro
   Historia(s): HU-01
   Componente: Frontend
   Responsable: E04
   Colaboradores: E07
   Estimación: 5h
   Día objetivo: D3
   Dependencia: SQL + endpoints acordados
   Entregable / evidencia: PR/commit + captura o prueba ejecutada
   Estado inicial: Pendiente

 Qué se busca
 Dejar montado el proyecto de frontend y la primera pantalla funcional.
 Qué debes hacer
 • Crear la estructura del proyecto (carpetas, router base, variable de entorno con la URL de la API).
 • Construir el formulario de registro según el boceto de S1-T01.
 • Validar en el cliente: correo con formato institucional, contraseña mínima y confirmación igual.
 • Enviar los datos a POST /auth/registro y manejar la respuesta.

   Reglas que no puedes saltarte: Si la API responde 202, rediriges a la pantalla de verificación y no
   inicias sesión (CA-01-01). Si el correo ya existe, muestras un mensaje claro sin crear otra cuenta
   (CA-01-02).

   Listo cuando: Puedes registrar un usuario real contra la API y ver el mensaje correcto en los casos
   de éxito y de duplicado.




CampusMarket - Guía de desarrollo por sprints                                                         Página 7
 S1-T04 - Pantalla de ingreso de token
   Historia(s): HU-02
   Componente: Frontend
   Responsable: E04
   Colaboradores: E07
   Estimación: 4h
   Día objetivo: D4
   Dependencia: SQL + endpoints acordados
   Entregable / evidencia: PR/commit + captura o prueba ejecutada
   Estado inicial: Pendiente

 Qué se busca
 Que el usuario pueda activar su cuenta con el código que le llegó al correo.
 Qué debes hacer
 • Crear la pantalla con el campo del token y el botón 'Verificar', que llama a POST /auth/verificar.
 • Agregar el botón 'Reenviar código', que llama a POST /auth/reenviartoken.
 • Mostrar los estados: cargando, éxito (invitar a iniciar sesión) y errores.
 • Evitar que el usuario pulse 'reenviar' muchas veces seguidas (por ejemplo, desactivar el botón unos
   segundos).

   Reglas que no puedes saltarte: Un token vencido o ya usado muestra un error controlado y la
   cuenta no se activa (CA-02-02).

   Listo cuando: Con un token correcto la cuenta queda activa, y con uno vencido o repetido ves el
   mensaje de error esperado.


 S1-T05 - Layout, rutas y estados de autenticación
   Historia(s): HU-03
   Componente: Frontend
   Responsable: E05
   Colaboradores: E08
   Estimación: 5h
   Día objetivo: D3
   Dependencia: SQL + endpoints acordados
   Entregable / evidencia: PR/commit + captura o prueba ejecutada
   Estado inicial: Pendiente

 Qué se busca
 La estructura general de la aplicación y el control de qué puede ver quien está o no autenticado.
 Qué debes hacer
 • Construir el layout general (encabezado, navegación, contenido).
 • Definir las rutas públicas (registro, verificación, login) y las protegidas.
 • Crear un estado global de autenticación (autenticado / no autenticado / cargando).
 • Redirigir al login si alguien intenta entrar a una ruta protegida sin sesión.
 • Acordar con E08 cómo saber si hay sesión activa (por ejemplo, un endpoint de consulta de sesión o la
   respuesta del login).




CampusMarket - Guía de desarrollo por sprints                                                        Página 8
   Reglas que no puedes saltarte: La sesión vive en una cookie HttpOnly (CA-03-01), así que el
   JavaScript no puede leerla. Nunca guardes tokens de sesión en localStorage. Las peticiones deben
   enviarse con credenciales incluidas (credentials: 'include').

   Listo cuando: Una ruta protegida redirige al login sin sesión y funciona con sesión, incluso al
   recargar la página.


 S1-T06 - Pantalla login y mensajes de error
   Historia(s): HU-03
   Componente: Frontend
   Responsable: E05
   Colaboradores: E08
   Estimación: 4h
   Día objetivo: D4
   Dependencia: SQL + endpoints acordados
   Entregable / evidencia: PR/commit + captura o prueba ejecutada
   Estado inicial: Pendiente

 Qué se busca
 Que un estudiante verificado pueda entrar y salir, y que cada error se entienda.
 Qué debes hacer
 • Construir el formulario de login que llama a POST /auth/login.
 • Distinguir los errores: credenciales incorrectas y cuenta pendiente. En el segundo caso, ofrecer ir a
   verificar.
 • Agregar el botón 'Cerrar sesión', que llama a POST /auth/logout y actualiza el estado global.
 • Evitar mensajes que revelen demasiado (por ejemplo, no confirmar si el correo existe).

   Reglas que no puedes saltarte: Una cuenta PENDIENTE no entra (CA-03-02). Al cerrar sesión, la
   sesión se revoca en el servidor (CA-03-03).

   Listo cuando: Puedes iniciar y cerrar sesión, y al volver atrás no accedes a pantallas protegidas.


 S1-T07 - Sistema de componentes y estilos
   Historia(s): HU-01/HU-03
   Componente: Frontend
   Responsable: E06
   Colaboradores: E08/E07
   Estimación: 4h
   Día objetivo: D3
   Dependencia: SQL + endpoints acordados
   Entregable / evidencia: PR/commit + captura o prueba ejecutada
   Estado inicial: Pendiente

 Qué se busca
 Una 'caja de piezas' reutilizable para que nadie repita botones y campos en cada pantalla.




CampusMarket - Guía de desarrollo por sprints                                                        Página 9
 Qué debes hacer
 • Traducir la guía visual de E03 a variables de estilo (colores, tipografías, espaciados).
 • Crear los componentes base: botón, campo de texto, grupo campo + etiqueta + error, alerta,
   indicador de carga y tarjeta.
 • Hacerlos accesibles (etiquetas asociadas, foco visible, uso con teclado).
 • Documentar brevemente cómo se usa cada uno.

   Reglas que no puedes saltarte: E04 y E05 los necesitan el D3, así que entrega una primera
   versión usable pronto y mejórala después.

   Listo cuando: E04 y E05 usan tus componentes en sus pantallas sin copiar estilos.


 S1-T08 - Integrar frontend con API de acceso
   Historia(s): HU-01/HU-02/HU-03
   Componente: Integración
   Responsable: E06
   Colaboradores: E08/E07
   Estimación: 5h
   Día objetivo: D5
   Dependencia: SQL + endpoints acordados
   Entregable / evidencia: PR/commit + captura o prueba ejecutada
   Estado inicial: Pendiente

 Qué se busca
 Comprobar que frontend, API y base de datos funcionan juntos de punta a punta.
 Qué debes hacer
 • Crear una capa única para llamar a la API (base URL, credenciales incluidas, manejo uniforme de
   errores).
 • Conectar las pantallas de registro, verificación y login a esa capa.
 • Probar el flujo completo: registro, verificación, login y logout.
 • Registrar y reportar los errores de integración (CORS, cookies, formatos de respuesta) a los dueños de
   cada parte.
 • Preparar el recorrido para la demo del Sprint Review.

   Reglas que no puedes saltarte: La cookie de sesión debe funcionar entre frontend y backend
   (revisa CORS y los atributos de la cookie).

   Listo cuando: El flujo completo corre en un entorno compartido y puede mostrarse en la demo.




CampusMarket - Guía de desarrollo por sprints                                                    Página 10
 S1-T09 - API de registro, generación y envío de token
   Historia(s): HU-01
   Componente: Backend
   Responsable: E07
   Colaboradores: E09
   Estimación: 5h
   Día objetivo: D3
   Dependencia: Modelo ER y contratos de autenticación
   Entregable / evidencia: PR/commit + captura o prueba ejecutada
   Estado inicial: Pendiente

 Qué se busca
 El endpoint que crea cuentas nuevas y envía el código de verificación.
 Qué debes hacer
 • Implementar POST /auth/registro.
 • Validar los campos y que el correo sea institucional.
 • Rechazar correos ya registrados sin crear otra cuenta.
 • Guardar la contraseña con hash seguro (bcrypt o argon2), nunca en texto plano.
 • Crear el usuario en estado PENDIENTE.
 • Generar un token aleatorio con fecha de vencimiento, guardarlo (idealmente como hash) y enviarlo
   por correo.
 • Responder 202 sin iniciar sesión.

   Reglas que no puedes saltarte: CA-01-01 (202, cuenta PENDIENTE, sin sesión) y CA-01-02
   (duplicado rechazado).

   Listo cuando: Registras un correo nuevo y llega el token, y un segundo intento con el mismo correo
   es rechazado.


 S1-T10 - Verificación, reenvío y controles de intentos
   Historia(s): HU-02
   Componente: Backend
   Responsable: E07
   Colaboradores: E09
   Estimación: 4h
   Día objetivo: D4
   Dependencia: Modelo ER y contratos de autenticación
   Entregable / evidencia: PR/commit + captura o prueba ejecutada
   Estado inicial: Pendiente

 Qué se busca
 Activar cuentas de forma segura y evitar abusos con el token.
 Qué debes hacer
 • Implementar POST /auth/verificar: si el token es correcto y vigente, la cuenta pasa a ACTIVA y el
   token se marca como usado.
 • Implementar POST /auth/reenviartoken: invalidar el token anterior, generar uno nuevo y enviarlo.
 • Limitar los intentos fallidos y la frecuencia de reenvío (por usuario y por IP si es posible).
 • Devolver errores controlados con códigos HTTP consistentes.



CampusMarket - Guía de desarrollo por sprints                                                       Página 11
   Reglas que no puedes saltarte: Un token usado o vencido nunca activa la cuenta (CA-02-02). Un
   token correcto queda inutilizado (CA-02-01).

   Listo cuando: Las pruebas cubren token correcto, vencido, repetido, reenvío y exceso de intentos.


 S1-T11 - API login, logout y middleware de sesión
   Historia(s): HU-03
   Componente: Backend
   Responsable: E08
   Colaboradores: E09
   Estimación: 5h
   Día objetivo: D3
   Dependencia: Modelo ER y contratos de autenticación
   Entregable / evidencia: PR/commit + captura o prueba ejecutada
   Estado inicial: Pendiente

 Qué se busca
 El mecanismo de sesión sobre el que se apoyan todos los endpoints protegidos de los demás sprints.
 Qué debes hacer
 • Implementar POST /auth/login: verificar el hash de la contraseña y permitir el acceso solo a cuentas
   ACTIVAS.
 • Crear un registro de sesión en base de datos y enviar una cookie HttpOnly (con Secure y SameSite
   configurados).
 • Implementar POST /auth/logout, que revoca la sesión.
 • Crear el middleware que lee la cookie, valida que la sesión exista, no esté vencida ni revocada, y
   deja disponible al usuario actual.
 • Definir cómo se identifica el rol (estudiante / administrador), porque se necesitará desde el Sprint 2.

   Reglas que no puedes saltarte: CA-03-01 (cookie HttpOnly), CA-03-02 (PENDIENTE denegado),
   CA-03-03 (logout revoca).

   Listo cuando: Puedes proteger un endpoint de prueba con tu middleware y funciona con y sin
   sesión.


 S1-T12 - Pruebas API y contrato de autenticación
   Historia(s): HU-01/HU-03
   Componente: Backend
   Responsable: E08
   Colaboradores: E09
   Estimación: 4h
   Día objetivo: D5
   Dependencia: Modelo ER y contratos de autenticación
   Entregable / evidencia: PR/commit + captura o prueba ejecutada
   Estado inicial: Pendiente

 Qué se busca
 Demostrar con pruebas que la autenticación hace lo que dicen los criterios de aceptación, y dejar
 documentado el contrato.




CampusMarket - Guía de desarrollo por sprints                                                        Página 12
 Qué debes hacer
 • Escribir pruebas automatizadas para los criterios CA-01-01, CA-01-02, CA-03-01, CA-03-02 y CA-03-03.
 • Documentar cada endpoint de autenticación: ruta, método, cuerpo, respuestas y códigos de error
   (OpenAPI, Postman u otro formato acordado).
 • Compartir el documento con el equipo de frontend.

   Listo cuando: Las pruebas pasan y el documento de contrato está accesible para todos.


 S1-T13 - Migración de usuarios, tokens y sesiones
   Historia(s): HU-01/HU-03
   Componente: BD
   Responsable: E09
   Colaboradores: E07/E08
   Estimación: 5h
   Día objetivo: D2
   Dependencia: Backlog aprobado y objetivos del sprint
   Entregable / evidencia: PR/commit + captura o prueba ejecutada
   Estado inicial: Pendiente

 Qué se busca
 Las tablas que todo el equipo de backend necesita. Es la tarea más crítica del sprint porque vence el D2.
 Qué debes hacer
 • Crear una migración repetible (no cambios a mano) con las tablas de usuarios, tokens de
   verificación y sesiones.
 • Usuarios: id, correo único, nombre, hash de contraseña, estado (PENDIENTE/ACTIVA), rol y fechas.
 • Tokens: usuario, token (o su hash), vencimiento, usado/no usado.
 • Sesiones: id, usuario, vencimiento, revocada.
 • Agregar claves foráneas, restricciones de unicidad (correo) e índices.
 • Avisar a E07 y E08 apenas esté lista.

   Listo cuando: Cualquier compañero puede levantar la base desde cero con un solo comando.


 S1-T14 - Integridad de BD, datos de prueba y smoke test
   Historia(s): HU-02/HU-03
   Componente: BD/QA
   Responsable: E09
   Colaboradores: E07/E08
   Estimación: 4h
   Día objetivo: D5
   Dependencia: Backlog aprobado y objetivos del sprint
   Entregable / evidencia: PR/commit + captura o prueba ejecutada
   Estado inicial: Pendiente

 Qué se busca
 Verificar que la base protege los datos y que el equipo tiene datos para probar.




CampusMarket - Guía de desarrollo por sprints                                                     Página 13
 Qué debes hacer
 • Crear datos de prueba: usuarios en cada estado (PENDIENTE y ACTIVA), tokens vigentes, vencidos y
   usados.
 • Probar que las restricciones funcionan (por ejemplo, un correo duplicado falla a nivel de base de
   datos).
 • Escribir un smoke test: un script rápido que confirma que la base levanta y que el flujo básico
   responde.
 • Documentar cómo cargar los datos de prueba.

   Listo cuando: El smoke test se ejecuta sin errores y el equipo usa tus datos de prueba.




CampusMarket - Guía de desarrollo por sprints                                                        Página 14
 Sprint 2 - Semana 2
 Objetivo: Recuperación, perfil, categorías y publicación con fotografías.
 Carga: 63 horas efectivas, 14 tareas, 5 historia(s) de usuario (21 puntos).
 Contexto: Aparecen los productos y las imágenes. Este sprint contiene la historia más grande (HU-07, 8
 puntos), así que la coordinación entre frontend, backend y base de datos es clave.


 Historias de usuario del sprint
 HU-04 - Recuperar contraseña mediante token
 Historia: Como estudiante, quiero restablecer mi contraseña por un token temporal, para recuperar
 acceso sin ayuda administrativa.
 Épica: Identidad | Prioridad: Debe | Story points: 5 | Sprint: 2
 Requisito(s): RF-AUT-05 | Flujo: F05 | API: POST /auth/recuperacion/solicitar; POST
 /auth/recuperacion/confirmar
 Dependencias funcionales: HU-03 o componentes previos del sprint.

 Criterios de aceptación (Dado / Cuando / Entonces)
 • CA-04-01. Dado un correo registrado, cuando solicito restablecer acceso, entonces se envía un token y la
   pantalla muestra una respuesta genérica.
 • CA-04-02. Dado un token válido y una nueva contraseña aceptable, cuando confirmo la recuperación,
   entonces se guarda el hash de la nueva clave y se revocan las sesiones.
 • CA-04-03. Dado un token expirado, cuando intento usarlo, entonces el sistema no modifica la clave.
 Evidencia esperada: registro del caso CP-04 con resultado, captura o prueba automatizada; demo frontend + API +
 BD en el Sprint Review.

 HU-05 - Administrar perfil y contactos
 Historia: Como estudiante autenticado, quiero actualizar mis datos y medios de contacto públicos, para
 hacer posible la comunicación segura.
 Épica: Perfil | Prioridad: Debe | Story points: 3 | Sprint: 2
 Requisito(s): RF-USR-01, RF-USR-02 | Flujo: F06 | API: GET/PATCH /usuarios/yo; POST/DELETE
 /usuarios/yo/contactos
 Dependencias funcionales: HU-04 o componentes previos del sprint.

 Criterios de aceptación (Dado / Cuando / Entonces)
 • CA-05-01. Dado mi cuenta ACTIVA, cuando guardo nombre y contacto válido, entonces los datos
   autorizados aparecen en mi perfil.
 • CA-05-02. Dado un contacto marcado como no público, cuando alguien consulta mi producto, entonces el
   contacto privado no se devuelve.
 Evidencia esperada: registro del caso CP-05 con resultado, captura o prueba automatizada; demo frontend + API +
 BD en el Sprint Review.




CampusMarket - Guía de desarrollo por sprints                                                           Página 15
 HU-06 - Gestionar categorías
 Historia: Como administrador, quiero crear y desactivar categorías, para mantener el catálogo
 organizado.
 Épica: Catálogo | Prioridad: Debe | Story points: 2 | Sprint: 2
 Requisito(s): RF-CAT-01 | Flujo: F09 | API: GET /categorias; POST/PATCH /admin/categorias
 Dependencias funcionales: HU-05 o componentes previos del sprint.

 Criterios de aceptación (Dado / Cuando / Entonces)
 • CA-06-01. Dado un administrador autenticado, cuando creo una categoría única, entonces queda
   disponible para nuevas publicaciones.
 • CA-06-02. Dado un estudiante común, cuando intento crear una categoría, entonces recibo 403.
 Evidencia esperada: registro del caso CP-06 con resultado, captura o prueba automatizada; demo frontend + API +
 BD en el Sprint Review.

 HU-07 - Publicar producto con fotografías
 Historia: Como vendedor, quiero registrar un artículo con precio, categoría e imagen, para ofrecerlo a
 otros estudiantes.
 Épica: Publicaciones | Prioridad: Debe | Story points: 8 | Sprint: 2
 Requisito(s): RF-PRD-01 | Flujo: F07 | API: POST /productos
 Dependencias funcionales: HU-06 o componentes previos del sprint.

 Criterios de aceptación (Dado / Cuando / Entonces)
 • CA-07-01. Dado un vendedor ACTIVO, cuando publico con título, descripción, categoría, precio y una
   imagen válida, entonces el producto DISPONIBLE aparece en el catálogo.
 • CA-07-02. Dado una imagen que excede el tamaño definido, cuando intento cargarla, entonces el servicio
   devuelve 422 y no almacena el archivo inválido.
 Evidencia esperada: registro del caso CP-07 con resultado, captura o prueba automatizada; demo frontend + API +
 BD en el Sprint Review.

 HU-08 - Editar y retirar una publicación
 Historia: Como vendedor, quiero actualizar o desactivar mis productos, para mantener información
 confiable.
 Épica: Publicaciones | Prioridad: Debe | Story points: 3 | Sprint: 2
 Requisito(s): RF-PRD-02 | Flujo: F08 | API: PATCH /productos/{id}; PATCH /productos/{id}/estado
 Dependencias funcionales: HU-07 o componentes previos del sprint.

 Criterios de aceptación (Dado / Cuando / Entonces)
 • CA-08-01. Dado que soy el dueño de un producto DISPONIBLE, cuando actualizo su precio, entonces se
   guarda el cambio.
 • CA-08-02. Dado que no soy el propietario, cuando intento editarlo, entonces el servicio responde 403 o
   404 y no modifica datos.
 Evidencia esperada: registro del caso CP-08 con resultado, captura o prueba automatizada; demo frontend + API +
 BD en el Sprint Review.




CampusMarket - Guía de desarrollo por sprints                                                           Página 16
 Sprint 2: tareas técnicas
 S2-T01 - Wireframes catálogo, producto y contactos
   Historia(s): HU-05/HU-07
   Componente: Diseño
   Responsable: E03
   Colaboradores: E04/E05
   Estimación: 5h
   Día objetivo: D2
   Dependencia: Backlog aprobado y objetivos del sprint
   Entregable / evidencia: PR/commit + captura o prueba ejecutada
   Estado inicial: Pendiente

 Qué se busca
 Dibujar las pantallas nuevas del sprint antes de que se programen.
 Qué debes hacer
 • Boceto del perfil con la lista de contactos y la opción de marcar cada contacto como público o
   privado.
 • Boceto del formulario de publicación: título, descripción, categoría, precio y carga de imagen.
 • Boceto del catálogo con tarjetas de producto (imagen, título, precio, categoría).
 • Estados de error y vacíos (sin productos, imagen demasiado grande).

   Reglas que no puedes saltarte: Un contacto privado nunca se muestra a otros (CA-05-02). El
   formulario debe permitir ver errores de imagen (CA-07-02).

   Listo cuando: E04 y E05 pueden construir las pantallas con tus bocetos.


 S2-T02 - Revisión UX de formulario y recuperación
   Historia(s): HU-04/HU-08
   Componente: Diseño/QA
   Responsable: E03
   Colaboradores: E04/E05
   Estimación: 4h
   Día objetivo: D5
   Dependencia: Backlog aprobado y objetivos del sprint
   Entregable / evidencia: PR/commit + captura o prueba ejecutada
   Estado inicial: Pendiente

 Qué se busca
 Revisar con ojo de usuario lo que ya construyó el equipo y reportar mejoras.
 Qué debes hacer
 • Probar las pantallas de recuperación de contraseña y de edición/retiro de producto.
 • Revisar con una lista de verificación: claridad de textos, errores comprensibles, consistencia visual,
   uso en móvil.
 • Registrar cada hallazgo con una captura, la pantalla afectada y su prioridad.
 • Confirmar que la pantalla de recuperación muestra siempre una respuesta genérica (CA-04-01).

   Listo cuando: Entregas la lista de incidencias priorizada y los responsables saben qué corregir.




CampusMarket - Guía de desarrollo por sprints                                                       Página 17
 S2-T03 - Pantalla de solicitud/confirmación recuperación
   Historia(s): HU-04
   Componente: Frontend
   Responsable: E04
   Colaboradores: E07
   Estimación: 5h
   Día objetivo: D3
   Dependencia: SQL + endpoints acordados
   Entregable / evidencia: PR/commit + captura o prueba ejecutada
   Estado inicial: Pendiente

 Qué se busca
 Que un usuario que olvidó su contraseña pueda recuperarla sin ayuda de un administrador.
 Qué debes hacer
 • Pantalla de solicitud: campo de correo que llama a POST /auth/recuperacion/solicitar.
 • Pantalla de confirmación: token, nueva contraseña y confirmación, que llama a POST
   /auth/recuperacion/confirmar.
 • Mostrar siempre el mismo mensaje tras solicitar, exista o no el correo.
 • Manejar el error de token vencido.
 • Al terminar con éxito, llevar al login y avisar que debe iniciar sesión de nuevo.

   Reglas que no puedes saltarte: Respuesta genérica (CA-04-01). Con token vencido no se cambia
   la clave (CA-04-03).

   Listo cuando: El flujo completo funciona y los mensajes no revelan si un correo está registrado.


 S2-T04 - Pantalla de perfil y medios de contacto
   Historia(s): HU-05
   Componente: Frontend
   Responsable: E04
   Colaboradores: E07
   Estimación: 4h
   Día objetivo: D5
   Dependencia: SQL + endpoints acordados
   Entregable / evidencia: PR/commit + captura o prueba ejecutada
   Estado inicial: Pendiente

 Qué se busca
 Que el estudiante administre sus datos y decida qué contactos son visibles.
 Qué debes hacer
 • Mostrar el perfil con GET /usuarios/yo.
 • Permitir editar el nombre con PATCH /usuarios/yo.
 • Agregar y eliminar contactos con POST y DELETE /usuarios/yo/contactos.
 • Incluir el interruptor público/privado por contacto.
 • Validar el formato del contacto (por ejemplo, número de WhatsApp) antes de enviarlo.

   Reglas que no puedes saltarte: Solo se muestran los datos autorizados (CA-05-01).




CampusMarket - Guía de desarrollo por sprints                                                     Página 18
   Listo cuando: Puedes editar tu perfil, agregar un contacto y verlo reflejado tras recargar.


 S2-T05 - Vista de catálogo y tarjetas
   Historia(s): HU-07
   Componente: Frontend
   Responsable: E05
   Colaboradores: E08
   Estimación: 5h
   Día objetivo: D3
   Dependencia: SQL + endpoints acordados
   Entregable / evidencia: PR/commit + captura o prueba ejecutada
   Estado inicial: Pendiente

 Qué se busca
 Que los productos publicados aparezcan en una vista de catálogo.
 Qué debes hacer
 • Crear el componente de tarjeta de producto (imagen, título, precio, categoría).
 • Construir la cuadrícula del catálogo que consume GET /productos.
 • Incluir estados de carga, vacío y error.
 • Dejar la estructura lista para añadir filtros y paginación en el Sprint 3.

   Reglas que no puedes saltarte: Un producto recién publicado y DISPONIBLE debe aparecer en el
   catálogo (CA-07-01).

   Listo cuando: Se ve la lista de productos reales de la API con un diseño consistente.


 S2-T06 - Pantalla categorías y navegación
   Historia(s): HU-06
   Componente: Frontend
   Responsable: E05
   Colaboradores: E08
   Estimación: 4h
   Día objetivo: D5
   Dependencia: SQL + endpoints acordados
   Entregable / evidencia: PR/commit + captura o prueba ejecutada
   Estado inicial: Pendiente

 Qué se busca
 Que se puedan ver las categorías y que el administrador las gestione.
 Qué debes hacer
 • Mostrar la lista de categorías con GET /categorias y navegación por categoría.
 • Crear la pantalla de administración para crear y desactivar categorías (POST/PATCH
   /admin/categorias).
 • Mostrar los controles de administración solo a los administradores.
 • Mostrar el error cuando un estudiante común recibe 403.

   Reglas que no puedes saltarte: Ocultar un botón no es seguridad: la protección real la hace el
   backend (CA-06-02), pero tu pantalla debe manejar ese 403.




CampusMarket - Guía de desarrollo por sprints                                                    Página 19
   Listo cuando: Un administrador crea y desactiva categorías, y un estudiante común no ve esas
   opciones.


 S2-T07 - Formulario producto y validaciones
   Historia(s): HU-07
   Componente: Frontend
   Responsable: E06
   Colaboradores: E08/E07
   Estimación: 5h
   Día objetivo: D3
   Dependencia: SQL + endpoints acordados
   Entregable / evidencia: PR/commit + captura o prueba ejecutada
   Estado inicial: Pendiente

 Qué se busca
 El formulario con el que un vendedor publica un artículo.
 Qué debes hacer
 • Campos: título, descripción, categoría (cargada con GET /categorias), precio e imagen.
 • Validar en el cliente: campos obligatorios, precio numérico y mayor que cero.
 • Enviar a POST /productos (probablemente como multipart/form-data; confirma el formato con E08).
 • Mostrar los errores que devuelva la API junto al campo correspondiente.

   Reglas que no puedes saltarte: CA-07-01: con datos válidos, el producto queda DISPONIBLE y
   aparece en el catálogo.

   Listo cuando: Publicas un producto y lo ves en el catálogo.


 S2-T08 - Subida de imagen y edición básica
   Historia(s): HU-07/HU-08
   Componente: Frontend
   Responsable: E06
   Colaboradores: E08/E07
   Estimación: 4h
   Día objetivo: D5
   Dependencia: SQL + endpoints acordados
   Entregable / evidencia: PR/commit + captura o prueba ejecutada
   Estado inicial: Pendiente

 Qué se busca
 Una carga de imagen cómoda y la edición/retiro de publicaciones propias.
 Qué debes hacer
 • Selector de imagen con vista previa y validación previa de tipo y tamaño.
 • Manejar el error 422 del servidor (imagen demasiado grande) con un mensaje claro.
 • Formulario de edición (PATCH /productos/{id}) y acción de retirar (PATCH /productos/{id}/estado).
 • Mostrar estas acciones solo al dueño del producto.

   Reglas que no puedes saltarte: CA-07-02 (imagen grande, 422) y CA-08-01 (el dueño puede
   cambiar el precio).




CampusMarket - Guía de desarrollo por sprints                                                    Página 20
   Listo cuando: Subes una imagen válida, una inválida muestra el error correcto y puedes editar y
   retirar tu producto.


 S2-T09 - API token recuperación y rotación de clave
   Historia(s): HU-04
   Componente: Backend
   Responsable: E07
   Colaboradores: E09
   Estimación: 5h
   Día objetivo: D3
   Dependencia: Modelo ER y contratos de autenticación
   Entregable / evidencia: PR/commit + captura o prueba ejecutada
   Estado inicial: Pendiente

 Qué se busca
 El mecanismo seguro para recuperar el acceso.
 Qué debes hacer
 • POST /auth/recuperacion/solicitar: si el correo existe, generar un token con vencimiento y enviarlo.
   Siempre responder lo mismo exista o no el correo.
 • POST /auth/recuperacion/confirmar: validar el token, aplicar la política de contraseña, guardar el hash
   de la nueva clave y marcar el token como usado.
 • Revocar todas las sesiones del usuario al cambiar la clave.
 • Con token expirado o usado, no tocar la contraseña.

   Reglas que no puedes saltarte: CA-04-01, CA-04-02 y CA-04-03.

   Listo cuando: Las pruebas cubren token válido, expirado, usado y sesiones revocadas.


 S2-T10 - API de perfil y privacidad de contactos
   Historia(s): HU-05
   Componente: Backend
   Responsable: E07
   Colaboradores: E09
   Estimación: 4h
   Día objetivo: D5
   Dependencia: Modelo ER y contratos de autenticación
   Entregable / evidencia: PR/commit + captura o prueba ejecutada
   Estado inicial: Pendiente

 Qué se busca
 Gestionar el perfil y garantizar que los contactos privados no se filtren.
 Qué debes hacer
 • GET y PATCH /usuarios/yo: permitir cambiar solo los campos autorizados (nada de rol o estado).
 • POST y DELETE /usuarios/yo/contactos: validar el formato y que el contacto pertenezca al usuario.
 • Crear una función reutilizable que devuelva solo los contactos públicos. El Sprint 3 la usará en el
   detalle de producto (HU-10).




CampusMarket - Guía de desarrollo por sprints                                                       Página 21
   Reglas que no puedes saltarte: Un contacto privado jamás sale en respuestas públicas
   (CA-05-02).

   Listo cuando: Hay una prueba que consulta como otro usuario y confirma que el contacto privado
   no aparece.


 S2-T11 - API productos, categorías y titularidad
   Historia(s): HU-06/HU-07
   Componente: Backend
   Responsable: E08
   Colaboradores: E09
   Estimación: 5h
   Día objetivo: D3
   Dependencia: Modelo ER y contratos de autenticación
   Entregable / evidencia: PR/commit + captura o prueba ejecutada
   Estado inicial: Pendiente

 Qué se busca
 Los endpoints de categorías y de creación de productos, con control de quién es el dueño.
 Qué debes hacer
 • GET /categorias (público) y POST/PATCH /admin/categorias (solo ADMIN; los demás reciben 403).
 • Evitar categorías duplicadas.
 • POST /productos: exigir vendedor ACTIVO, validar título, descripción, categoría activa y precio, y crear
   el producto en estado DISPONIBLE asociado al usuario de la sesión.
 • Crear una función reutilizable de verificación de titularidad ('¿este producto es de este usuario?').

   Reglas que no puedes saltarte: CA-06-01, CA-06-02 y CA-07-01.

   Listo cuando: Pruebas con administrador, estudiante y creación de productos pasan.


 S2-T12 - Edición y subida segura de imágenes
   Historia(s): HU-07/HU-08
   Componente: Backend
   Responsable: E08
   Colaboradores: E09
   Estimación: 4h
   Día objetivo: D5
   Dependencia: Modelo ER y contratos de autenticación
   Entregable / evidencia: PR/commit + captura o prueba ejecutada
   Estado inicial: Pendiente

 Qué se busca
 Editar productos solo siendo el dueño y recibir imágenes sin abrir huecos de seguridad.




CampusMarket - Guía de desarrollo por sprints                                                       Página 22
 Qué debes hacer
 • PATCH /productos/{id} y PATCH /productos/{id}/estado: si no eres el propietario, responder 403 o 404
   sin modificar nada.
 • Subida de imágenes: validar el tipo real del archivo (no solo la extensión), limitar el tamaño y
   responder 422 si excede.
 • Guardar el archivo con un nombre generado (no el original) y en una ubicación controlada.
 • Si la imagen es inválida, no guardar nada (ni archivo ni fila en base de datos).

   Reglas que no puedes saltarte: CA-07-02, CA-08-01 y CA-08-02.

   Listo cuando: Pruebas con imagen válida, demasiado grande, tipo falso y usuario no propietario
   pasan.


 S2-T13 - Migraciones de contactos, categorías y productos
   Historia(s): HU-05/HU-07
   Componente: BD
   Responsable: E09
   Colaboradores: E07/E08
   Estimación: 5h
   Día objetivo: D2
   Dependencia: Backlog aprobado y objetivos del sprint
   Entregable / evidencia: PR/commit + captura o prueba ejecutada
   Estado inicial: Pendiente

 Qué se busca
 Las tablas del sprint, a tiempo para que backend pueda trabajar (vence el D2).
 Qué debes hacer
 • Crear migraciones para contactos (con la marca público/privado), categorías (nombre único,
   activa/inactiva), productos (dueño, categoría, precio, estado) e imágenes.
 • Agregar restricciones: precio mayor que cero, estados válidos, claves foráneas.
 • Cargar categorías iniciales de ejemplo.
 • Avisar a E07 y E08 apenas esté lista.

   Listo cuando: La base se levanta desde cero con las nuevas tablas y los datos iniciales.


 S2-T14 - Índices y pruebas de integridad / recuperación
   Historia(s): HU-04/HU-08
   Componente: BD/QA
   Responsable: E09
   Colaboradores: E07/E08
   Estimación: 4h
   Día objetivo: D5
   Dependencia: Backlog aprobado y objetivos del sprint
   Entregable / evidencia: PR/commit + captura o prueba ejecutada
   Estado inicial: Pendiente

 Qué se busca
 Que las consultas sean rápidas y que los datos queden consistentes.




CampusMarket - Guía de desarrollo por sprints                                                         Página 23
 Qué debes hacer
 • Verificar que existe la estructura para los tokens de recuperación (si falta, agregarla en una
   migración).
 • Crear índices útiles (por ejemplo, productos por categoría, estado y precio).
 • Probar integridad: no se puede borrar una categoría con productos, no existen productos huérfanos.
 • Probar el flujo de recuperación a nivel de datos (token usado, sesiones revocadas).

   Listo cuando: Las pruebas de integridad pasan y los índices están documentados.




CampusMarket - Guía de desarrollo por sprints                                                       Página 24
 Sprint 3 - Semana 3
 Objetivo: Catálogo, descuentos, interés, venta y calificaciones.
 Carga: 63 horas efectivas, 14 tareas, 6 historia(s) de usuario (22 puntos).
 Contexto: Es el sprint con más historias (HU-09 a HU-14). Aquí aparecen las reglas más delicadas del
 negocio, como la venta atómica y el precio con descuento.


 Historias de usuario del sprint
 HU-09 - Explorar catálogo con filtros
 Historia: Como visitante o estudiante, quiero buscar por texto, categoría y precio, para encontrar
 artículos disponibles.
 Épica: Descubrimiento | Prioridad: Debe | Story points: 5 | Sprint: 3
 Requisito(s): RF-SRH-01 | Flujo: F10 | API: GET /productos
 Dependencias funcionales: HU-08 o componentes previos del sprint.

 Criterios de aceptación (Dado / Cuando / Entonces)
 • CA-09-01. Dado que existen productos publicados, cuando filtro por categoría y rango de precios, entonces
   solo se devuelven productos DISPONIBLES que cumplen las condiciones.
 • CA-09-02. Dado que hay más resultados que el límite, cuando cambio de página, entonces recibo el
   siguiente conjunto sin duplicados de página.
 Evidencia esperada: registro del caso CP-09 con resultado, captura o prueba automatizada; demo frontend + API +
 BD en el Sprint Review.

 HU-10 - Consultar detalle y contactar vendedor
 Historia: Como comprador, quiero ver detalles y contactos autorizados, para negociar directamente.
 Épica: Descubrimiento | Prioridad: Debe | Story points: 3 | Sprint: 3
 Requisito(s): RF-CON-01 | Flujo: F11 | API: GET /productos/{id}
 Dependencias funcionales: HU-09 o componentes previos del sprint.

 Criterios de aceptación (Dado / Cuando / Entonces)
 • CA-10-01. Dado un producto DISPONIBLE con WhatsApp público, cuando abro su detalle, entonces veo
   nombre, fotos, precio final y opción de contacto.
 • CA-10-02. Dado un teléfono no autorizado para publicar, cuando consulto el producto, entonces ese
   número no aparece en la respuesta.
 Evidencia esperada: registro del caso CP-10 con resultado, captura o prueba automatizada; demo frontend + API +
 BD en el Sprint Review.




CampusMarket - Guía de desarrollo por sprints                                                           Página 25
 HU-11 - Aplicar descuento
 Historia: Como vendedor, quiero configurar un descuento porcentual para un producto, para
 promocionar mi artículo.
 Épica: Promociones | Prioridad: Debe | Story points: 3 | Sprint: 3
 Requisito(s): RF-DES-01 | Flujo: F12 | API: PUT/DELETE /productos/{id}/descuento
 Dependencias funcionales: HU-10 o componentes previos del sprint.

 Criterios de aceptación (Dado / Cuando / Entonces)
 • CA-11-01. Dado que soy propietario de un producto disponible, cuando registro un descuento del 20 por
   ciento, entonces se muestra el precio final calculado sin modificar el precio base.
 • CA-11-02. Dado que intento asignar un descuento del 110 por ciento, cuando guardo la promoción,
   entonces se rechaza con 422.
 Evidencia esperada: registro del caso CP-11 con resultado, captura o prueba automatizada; demo frontend + API +
 BD en el Sprint Review.

 HU-12 - Manifestar interés de compra
 Historia: Como comprador, quiero registrar mi interés por un artículo, para permitir al vendedor
 identificarme.
 Épica: Compraventa | Prioridad: Debe | Story points: 3 | Sprint: 3
 Requisito(s): RF-INT-01 | Flujo: F13 | API: POST /productos/{id}/intereses
 Dependencias funcionales: HU-11 o componentes previos del sprint.

 Criterios de aceptación (Dado / Cuando / Entonces)
 • CA-12-01. Dado un producto DISPONIBLE de otro estudiante, cuando pulso 'Estoy interesado', entonces se
   guarda mi interés una sola vez.
 • CA-12-02. Dado que soy el vendedor del mismo producto, cuando intento registrar interés, entonces la
   operación se rechaza.
 Evidencia esperada: registro del caso CP-12 con resultado, captura o prueba automatizada; demo frontend + API +
 BD en el Sprint Review.

 HU-13 - Confirmar una venta
 Historia: Como vendedor, quiero seleccionar a un interesado y marcar vendido, para cerrar la
 publicación y habilitar valoración.
 Épica: Compraventa | Prioridad: Debe | Story points: 5 | Sprint: 3
 Requisito(s): RF-VEN-01 | Flujo: F14 | API: POST /productos/{id}/venta
 Dependencias funcionales: HU-12 o componentes previos del sprint.

 Criterios de aceptación (Dado / Cuando / Entonces)
 • CA-13-01. Dado un comprador con interés registrado, cuando confirmo que le vendí el artículo, entonces
   en una transacción se crea la VENTA y se cambia el producto a VENDIDO.
 • CA-13-02. Dado que el producto ya está vendido, cuando intento confirmar otra venta, entonces se
   rechaza sin crear una segunda venta.
 Evidencia esperada: registro del caso CP-13 con resultado, captura o prueba automatizada; demo frontend + API +
 BD en el Sprint Review.




CampusMarket - Guía de desarrollo por sprints                                                           Página 26
 HU-14 - Calificar una compra registrada
 Historia: Como comprador vinculado a una venta, quiero valorar el producto con estrellas y comentario,
 para dejar referencia para la comunidad.
 Épica: Reputación | Prioridad: Debe | Story points: 3 | Sprint: 3
 Requisito(s): RF-CAL-01 | Flujo: F15 | API: POST /ventas/{id}/calificacion
 Dependencias funcionales: HU-13 o componentes previos del sprint.

 Criterios de aceptación (Dado / Cuando / Entonces)
 • CA-14-01. Dado que soy el comprador de una venta confirmada, cuando califico de 1 a 5, entonces se
   almacena una sola calificación asociada a la venta.
 • CA-14-02. Dado que no participo en la venta, cuando intento calificarla, entonces el servicio deniega la
   acción.
 Evidencia esperada: registro del caso CP-14 con resultado, captura o prueba automatizada; demo frontend + API +
 BD en el Sprint Review.




CampusMarket - Guía de desarrollo por sprints                                                           Página 27
 Sprint 3: tareas técnicas
 S3-T01 - Prototipos de oferta, interés, venta y rating
   Historia(s): HU-11/HU-14
   Componente: Diseño
   Responsable: E03
   Colaboradores: E04/E05
   Estimación: 5h
   Día objetivo: D2
   Dependencia: Backlog aprobado y objetivos del sprint
   Entregable / evidencia: PR/commit + captura o prueba ejecutada
   Estado inicial: Pendiente

 Qué se busca
 El diseño de las interacciones de compraventa antes de programarlas.
 Qué debes hacer
 • Panel de descuento del vendedor (porcentaje, vista previa del precio final).
 • Botón 'Estoy interesado' y sus estados.
 • Vista del vendedor con la lista de interesados y la acción 'marcar como vendido'.
 • Formulario de calificación (estrellas 1 a 5 y comentario).
 • Diseño del precio con descuento (precio base tachado y precio final).

   Listo cuando: Los desarrolladores tienen los prototipos y los estados de error (por ejemplo, producto
   ya vendido).


 S3-T02 - Pruebas exploratorias e incidencias UI
   Historia(s): HU-09/HU-13
   Componente: Diseño/QA
   Responsable: E03
   Colaboradores: E04/E05
   Estimación: 4h
   Día objetivo: D5
   Dependencia: Backlog aprobado y objetivos del sprint
   Entregable / evidencia: PR/commit + captura o prueba ejecutada
   Estado inicial: Pendiente

 Qué se busca
 Encontrar fallos de usabilidad en catálogo y venta antes de la demo.
 Qué debes hacer
 • Hacer sesiones de prueba exploratoria (sin guion rígido) sobre búsqueda, filtros, paginación, interés y
   venta.
 • Registrar cada incidencia: pasos para reproducirla, resultado esperado vs. real, captura y prioridad.
 • Probar en móvil y escritorio.
 • Volver a probar las incidencias corregidas.

   Listo cuando: Entregas el registro de incidencias y las críticas quedaron corregidas o asignadas.




CampusMarket - Guía de desarrollo por sprints                                                      Página 28
 S3-T03 - Vista detalle y acceso a contacto público
   Historia(s): HU-10
   Componente: Frontend
   Responsable: E04
   Colaboradores: E07
   Estimación: 4h
   Día objetivo: D3
   Dependencia: SQL + endpoints acordados
   Entregable / evidencia: PR/commit + captura o prueba ejecutada
   Estado inicial: Pendiente

 Qué se busca
 Que el comprador vea toda la información del producto y pueda contactar al vendedor.
 Qué debes hacer
 • Pantalla de detalle con GET /productos/{id}: nombre, galería de fotos, descripción, precio final y
   vendedor.
 • Botón de contacto (por ejemplo, un enlace a WhatsApp) solo si la API devuelve un contacto público.
 • Mensaje alternativo si no hay contacto público.
 • Estados de carga y de producto no encontrado.

   Reglas que no puedes saltarte: Muestra únicamente lo que devuelve la API. No intentes deducir
   contactos (CA-10-01, CA-10-02).

   Listo cuando: El detalle muestra el precio final y el contacto público, y no hay contacto privado
   visible.


 S3-T04 - Panel de descuentos del vendedor
   Historia(s): HU-11
   Componente: Frontend
   Responsable: E04
   Colaboradores: E07
   Estimación: 5h
   Día objetivo: D5
   Dependencia: SQL + endpoints acordados
   Entregable / evidencia: PR/commit + captura o prueba ejecutada
   Estado inicial: Pendiente

 Qué se busca
 Que el vendedor ponga y quite descuentos con claridad.
 Qué debes hacer
 • Panel visible solo para el dueño del producto.
 • Campo de porcentaje, con una vista previa del precio final calculado.
 • Guardar con PUT /productos/{id}/descuento y quitar con DELETE.
 • Mostrar el error 422 cuando el porcentaje no es válido (por ejemplo, 110).

   Reglas que no puedes saltarte: El precio base no se modifica: solo se muestra el precio final
   calculado (CA-11-01).




CampusMarket - Guía de desarrollo por sprints                                                      Página 29
   Listo cuando: Con 20 % ves el precio final correcto y con 110 % ves el error.


 S3-T05 - Búsqueda, filtros y paginación
   Historia(s): HU-09
   Componente: Frontend
   Responsable: E05
   Colaboradores: E08
   Estimación: 5h
   Día objetivo: D3
   Dependencia: SQL + endpoints acordados
   Entregable / evidencia: PR/commit + captura o prueba ejecutada
   Estado inicial: Pendiente

 Qué se busca
 Que se pueda encontrar un artículo rápido.
 Qué debes hacer
 • Barra de búsqueda por texto, selector de categoría y rango de precio.
 • Reflejar los filtros en la URL (para poder recargar y compartir).
 • Paginación conectada a GET /productos.
 • Evitar peticiones excesivas mientras el usuario escribe (debounce).
 • Mostrar un mensaje claro cuando no hay resultados.

   Reglas que no puedes saltarte: Solo se muestran productos DISPONIBLES (CA-09-01), y al
   cambiar de página no se repiten resultados (CA-09-02).

   Listo cuando: Puedes combinar texto, categoría y precio, y navegar por las páginas sin duplicados.


 S3-T06 - Refinamiento responsive y estados vacíos
   Historia(s): HU-09/HU-10
   Componente: Frontend
   Responsable: E05
   Colaboradores: E08
   Estimación: 4h
   Día objetivo: D5
   Dependencia: SQL + endpoints acordados
   Entregable / evidencia: PR/commit + captura o prueba ejecutada
   Estado inicial: Pendiente

 Qué se busca
 Que catálogo y detalle se vean bien en cualquier pantalla y manejen los casos vacíos.
 Qué debes hacer
 • Ajustar catálogo y detalle para móvil, tablet y escritorio.
 • Diseñar y programar los estados vacío, de carga (por ejemplo, esqueletos) y de error.
 • Corregir las incidencias de UI que reporte E03 (S3-T02).

   Listo cuando: Revisas a mano en tres tamaños de pantalla y no hay desbordes ni pantallas en
   blanco.




CampusMarket - Guía de desarrollo por sprints                                                    Página 30
 S3-T07 - Interés y confirmación de venta
   Historia(s): HU-12/HU-13
   Componente: Frontend
   Responsable: E06
   Colaboradores: E08/E07
   Estimación: 5h
   Día objetivo: D4
   Dependencia: SQL + endpoints acordados
   Entregable / evidencia: PR/commit + captura o prueba ejecutada
   Estado inicial: Pendiente

 Qué se busca
 El flujo que conecta a comprador y vendedor hasta cerrar la venta.
 Qué debes hacer
 • Botón 'Estoy interesado' (POST /productos/{id}/intereses), desactivado si ya lo pulsaste y oculto para
   el dueño.
 • Vista del vendedor con la lista de interesados.
 • Acción de seleccionar un interesado y confirmar con un cuadro de confirmación (POST
   /productos/{id}/venta).
 • Mostrar el estado 'Vendido' y manejar el error si el producto ya fue vendido.

   Reglas que no puedes saltarte: CA-12-01 (interés una sola vez), CA-12-02 (el vendedor no puede
   mostrar interés) y CA-13-02 (no hay segunda venta).

   Listo cuando: Completas el recorrido interés, selección y venta, y el producto aparece vendido.


 S3-T08 - Formulario de calificación
   Historia(s): HU-14
   Componente: Frontend
   Responsable: E06
   Colaboradores: E08/E07
   Estimación: 4h
   Día objetivo: D5
   Dependencia: SQL + endpoints acordados
   Entregable / evidencia: PR/commit + captura o prueba ejecutada
   Estado inicial: Pendiente

 Qué se busca
 Que el comprador deje su valoración tras la compra.
 Qué debes hacer
 • Formulario con estrellas (1 a 5) y comentario opcional.
 • Enviar a POST /ventas/{id}/calificacion.
 • Mostrar la opción solo al comprador de esa venta.
 • Manejar los errores: ya calificó, no participa en la venta.

   Reglas que no puedes saltarte: Una sola calificación por venta (CA-14-01) y no puede calificar
   quien no participó (CA-14-02).




CampusMarket - Guía de desarrollo por sprints                                                     Página 31
   Listo cuando: El comprador califica una vez y un segundo intento muestra el error esperado.


 S3-T09 - API de interés y validación de comprador
   Historia(s): HU-12
   Componente: Backend
   Responsable: E07
   Colaboradores: E09
   Estimación: 4h
   Día objetivo: D3
   Dependencia: Modelo ER y contratos de autenticación
   Entregable / evidencia: PR/commit + captura o prueba ejecutada
   Estado inicial: Pendiente

 Qué se busca
 Registrar el interés de compra con reglas claras.
 Qué debes hacer
 • Implementar POST /productos/{id}/intereses.
 • Rechazar si el producto no está DISPONIBLE o si el usuario es el vendedor.
 • Guardar el interés una sola vez por comprador y producto (repetirlo no debe duplicar).
 • Crear una consulta para que el vendedor vea sus interesados (acuerda la ruta con E06).

   Reglas que no puedes saltarte: CA-12-01 y CA-12-02.

   Listo cuando: Pruebas de interés válido, repetido y del propio vendedor pasan.


 S3-T10 - Transacción atómica de venta y autorización
   Historia(s): HU-13
   Componente: Backend
   Responsable: E07
   Colaboradores: E09
   Estimación: 5h
   Día objetivo: D5
   Dependencia: Modelo ER y contratos de autenticación
   Entregable / evidencia: PR/commit + captura o prueba ejecutada
   Estado inicial: Pendiente

 Qué se busca
 Cerrar una venta de forma segura: o se hace todo, o no se hace nada.
 Qué debes hacer
 • Implementar POST /productos/{id}/venta dentro de una transacción.
 • Verificar que quien llama es el dueño, que el producto está DISPONIBLE y que el comprador elegido
   tiene un interés registrado.
 • Crear la VENTA y cambiar el producto a VENDIDO en la misma transacción.
 • Bloquear la fila del producto durante la operación para evitar que dos peticiones simultáneas generen
   dos ventas.
 • Si algo falla, revertir todo.




CampusMarket - Guía de desarrollo por sprints                                                    Página 32
   Reglas que no puedes saltarte: CA-13-01 (venta y cambio de estado juntos) y CA-13-02 (si ya está
   vendido, se rechaza sin crear otra venta).

   Listo cuando: La prueba de dos intentos simultáneos termina con una sola venta.


 S3-T11 - API descuentos y precio final
   Historia(s): HU-11
   Componente: Backend
   Responsable: E08
   Colaboradores: E09
   Estimación: 4h
   Día objetivo: D3
   Dependencia: Modelo ER y contratos de autenticación
   Entregable / evidencia: PR/commit + captura o prueba ejecutada
   Estado inicial: Pendiente

 Qué se busca
 Manejar el descuento sin alterar el precio original.
 Qué debes hacer
 • PUT /productos/{id}/descuento y DELETE /productos/{id}/descuento, solo para el dueño de un
   producto disponible.
 • Validar el porcentaje: 110 debe responder 422. Confirma con el Product Owner si 100 % es válido y
   qué pasa con 0 o negativos.
 • Calcular el precio final: precio_base x (1 - porcentaje/100), sin modificar el precio base.
 • Incluir el precio final en las respuestas de producto (catálogo y detalle).

   Reglas que no puedes saltarte: CA-11-01 y CA-11-02.

   Listo cuando: Con 20 % el precio final sale correcto y el base queda intacto.


 S3-T12 - API calificaciones y catálogo con filtros
   Historia(s): HU-09/HU-14
   Componente: Backend
   Responsable: E08
   Colaboradores: E09
   Estimación: 5h
   Día objetivo: D5
   Dependencia: Modelo ER y contratos de autenticación
   Entregable / evidencia: PR/commit + captura o prueba ejecutada
   Estado inicial: Pendiente

 Qué se busca
 El endpoint de búsqueda del catálogo y el de calificaciones.




CampusMarket - Guía de desarrollo por sprints                                                    Página 33
 Qué debes hacer
 • GET /productos con parámetros de texto, categoría, precio mínimo/máximo, página y límite.
 • Devolver solo productos DISPONIBLES y con un orden estable (por ejemplo, fecha e id) para que
   las páginas no repitan resultados.
 • POST /ventas/{id}/calificacion: solo el comprador de la venta, valor entre 1 y 5, y una sola
   calificación por venta.
 • Responder con el código correcto cuando alguien ajeno intenta calificar.

   Reglas que no puedes saltarte: CA-09-01, CA-09-02, CA-14-01 y CA-14-02.

   Listo cuando: Las pruebas de filtros, paginación y calificación válida/inválida pasan.


 S3-T13 - Migraciones descuentos, intereses y ventas
   Historia(s): HU-11/HU-13
   Componente: BD
   Responsable: E09
   Colaboradores: E07/E08
   Estimación: 5h
   Día objetivo: D2
   Dependencia: Backlog aprobado y objetivos del sprint
   Entregable / evidencia: PR/commit + captura o prueba ejecutada
   Estado inicial: Pendiente

 Qué se busca
 Las tablas para descuentos, intereses y ventas, a tiempo para backend (D2).
 Qué debes hacer
 • Descuentos: producto, porcentaje con restricción de rango.
 • Intereses: producto y comprador, con restricción de unicidad para que no se repita.
 • Ventas: producto, comprador, vendedor, fecha, con restricción para que un producto tenga una sola
   venta.
 • Agregar claves foráneas e índices de consulta.

   Listo cuando: La base se levanta con las tablas nuevas y las restricciones impiden duplicados.


 S3-T14 - Calificaciones y pruebas transaccionales
   Historia(s): HU-13/HU-14
   Componente: BD/QA
   Responsable: E09
   Colaboradores: E07/E08
   Estimación: 4h
   Día objetivo: D5
   Dependencia: Backlog aprobado y objetivos del sprint
   Entregable / evidencia: PR/commit + captura o prueba ejecutada
   Estado inicial: Pendiente

 Qué se busca
 La tabla de calificaciones y la comprobación de que la venta es segura.




CampusMarket - Guía de desarrollo por sprints                                                       Página 34
 Qué debes hacer
 • Crear la tabla de calificaciones: una por venta (unicidad), estrellas entre 1 y 5, comentario.
 • Probar la transacción de venta: éxito, reversión ante error y dos intentos simultáneos.
 • Verificar que ninguna venta quede sin producto marcado como VENDIDO, ni al revés.

   Listo cuando: Las pruebas transaccionales demuestran que no hay ventas duplicadas ni estados
   incoherentes.




CampusMarket - Guía de desarrollo por sprints                                                       Página 35
 Sprint 4 - Semana 4
 Objetivo: Moderación negociable, pruebas, correcciones y demostración.
 Carga: 63 horas efectivas, 14 tareas, 1 historia(s) de usuario (3 puntos).
 Contexto: Casi no hay funcionalidad nueva (solo HU-15, que es negociable). Este sprint sirve para pulir,
 probar, asegurar y preparar la demostración.


 Historias de usuario del sprint
 HU-15 - Moderar publicaciones
 Historia: Como administrador, quiero desactivar publicaciones inapropiadas, para mantener el catálogo
 adecuado.
 Épica: Administración | Prioridad: Debería (negociable) | Story points: 3 | Sprint: 4
 Requisito(s): RF-ADM-01 | Flujo: F16 | API: PATCH /admin/productos/{id}/estado
 Dependencias funcionales: HU-14 o componentes previos del sprint.

 Criterios de aceptación (Dado / Cuando / Entonces)
 • CA-15-01. Dado que soy administrador autenticado, cuando desactivo una publicación, entonces deja de
   aparecer en el catálogo.
 • CA-15-02. Dado que soy estudiante, cuando intento desactivar un artículo ajeno, entonces se responde
   403.
 Evidencia esperada: registro del caso CP-15 con resultado, captura o prueba automatizada; demo frontend + API +
 BD en el Sprint Review.




CampusMarket - Guía de desarrollo por sprints                                                           Página 36
 Sprint 4: tareas técnicas
 S4-T01 - Pruebas UX y accesibilidad básica
   Historia(s): HU-01/HU-14
   Componente: Diseño/QA
   Responsable: E03
   Colaboradores: E04/E05
   Estimación: 5h
   Día objetivo: D3
   Dependencia: Backlog aprobado y objetivos del sprint
   Entregable / evidencia: PR/commit + captura o prueba ejecutada
   Estado inicial: Pendiente

 Qué se busca
 Verificar que la aplicación es usable y accesible en lo básico.
 Qué debes hacer
 • Recorrer el flujo principal (del registro a la calificación) como un usuario nuevo.
 • Revisar con una lista: contraste de colores, etiquetas en campos, navegación con teclado, foco visible,
   mensajes de error comprensibles y adaptación a móvil.
 • Registrar los hallazgos con captura y prioridad, y asignarlos.

   Listo cuando: Entregas el informe y las incidencias críticas tienen responsable.


 S4-T02 - Manual visual y checklist de demostración
   Historia(s): TODAS
   Componente: Diseño
   Responsable: E03
   Colaboradores: E04/E05
   Estimación: 4h
   Día objetivo: D5
   Dependencia: Backlog aprobado y objetivos del sprint
   Entregable / evidencia: PR/commit + captura o prueba ejecutada
   Estado inicial: Pendiente

 Qué se busca
 Dejar documentado el estilo final y un guion para la demostración.
 Qué debes hacer
 • Actualizar la guía visual con lo que realmente se construyó (componentes, colores, reglas de uso).
 • Armar la checklist de la demo: orden de los pasos, cuentas de prueba, datos necesarios y resultado
   esperado en cada paso.
 • Incluir un plan B por si algo falla en vivo (capturas o video de respaldo).

   Listo cuando: Alguien que no conoce el proyecto puede ejecutar la demo siguiendo tu checklist.




CampusMarket - Guía de desarrollo por sprints                                                     Página 37
 S4-T03 - Corregir formularios y errores de autenticación
   Historia(s): HU-01/HU-04
   Componente: Frontend
   Responsable: E04
   Colaboradores: E07
   Estimación: 5h
   Día objetivo: D3
   Dependencia: SQL + endpoints acordados
   Entregable / evidencia: PR/commit + captura o prueba ejecutada
   Estado inicial: Pendiente

 Qué se busca
 Dejar impecables registro, verificación, login y recuperación.
 Qué debes hacer
 • Revisar las incidencias de UX y pruebas de los sprints anteriores sobre estas pantallas y corregirlas.
 • Unificar los mensajes de error y las validaciones.
 • Probar los casos límite: espacios en blanco, contraseñas largas, doble clic en enviar, pérdida de
   conexión.

   Listo cuando: Las incidencias asignadas están cerradas y verificadas.


 S4-T04 - Pruebas de regresión de frontend
   Historia(s): TODAS
   Componente: Frontend/QA
   Responsable: E04
   Colaboradores: E07
   Estimación: 4h
   Día objetivo: D5
   Dependencia: SQL + endpoints acordados
   Entregable / evidencia: PR/commit + captura o prueba ejecutada
   Estado inicial: Pendiente

 Qué se busca
 Confirmar que lo correcto en sprints anteriores no se dañó con los cambios nuevos.
 Qué debes hacer
 • Preparar una lista de pruebas que cubra las pantallas de las historias HU-01 a HU-15.
 • Ejecutarla completa (manual o automatizada) y registrar el resultado de cada caso.
 • Reportar los fallos al responsable de la pantalla y volver a probar tras la corrección.

   Listo cuando: Hay un registro con resultado por historia y sin fallos críticos abiertos.




CampusMarket - Guía de desarrollo por sprints                                                       Página 38
 S4-T05 - Interfaz mínima de moderación
   Historia(s): HU-15
   Componente: Frontend
   Responsable: E05
   Colaboradores: E08
   Estimación: 5h
   Día objetivo: D3
   Dependencia: SQL + endpoints acordados
   Entregable / evidencia: PR/commit + captura o prueba ejecutada
   Estado inicial: Pendiente

 Qué se busca
 Que el administrador pueda desactivar publicaciones inapropiadas. Esta historia es 'Debería', así que se
 puede negociar si falta tiempo.
 Qué debes hacer
 • Crear una pantalla sencilla, solo para administradores, con la lista de publicaciones y un botón
   'Desactivar'.
 • Llamar a PATCH /admin/productos/{id}/estado.
 • Mostrar confirmación y manejar el error 403.

   Reglas que no puedes saltarte: Al desactivar, la publicación deja de aparecer en el catálogo
   (CA-15-01).

   Listo cuando: El administrador desactiva un producto y desaparece del catálogo.


 S4-T06 - Ajustes de catálogo y responsive
   Historia(s): HU-09/HU-15
   Componente: Frontend/QA
   Responsable: E05
   Colaboradores: E08
   Estimación: 4h
   Día objetivo: D5
   Dependencia: SQL + endpoints acordados
   Entregable / evidencia: PR/commit + captura o prueba ejecutada
   Estado inicial: Pendiente

 Qué se busca
 El pulido final del catálogo.
 Qué debes hacer
 • Corregir las incidencias pendientes de catálogo, filtros y diseño adaptable.
 • Verificar que un producto desactivado por moderación ya no aparece.
 • Revisar el rendimiento percibido (carga de imágenes, tiempos de respuesta).

   Listo cuando: El catálogo funciona bien en móvil y escritorio, y las incidencias asignadas están
   cerradas.




CampusMarket - Guía de desarrollo por sprints                                                         Página 39
 S4-T07 - Pruebas E2E de publicación a calificación
   Historia(s): HU-07/HU-14
   Componente: Integración/QA
   Responsable: E06
   Colaboradores: E08/E07
   Estimación: 5h
   Día objetivo: D4
   Dependencia: SQL + endpoints acordados
   Entregable / evidencia: PR/commit + captura o prueba ejecutada
   Estado inicial: Pendiente

 Qué se busca
 Probar de punta a punta el recorrido más importante del producto.
 Qué debes hacer
 • Definir el escenario: un vendedor publica con foto, un comprador lo encuentra y muestra interés, el
   vendedor confirma la venta y el comprador califica.
 • Ejecutarlo (idealmente automatizado) y guardar la evidencia.
 • Probar también variantes negativas: producto ya vendido, usuario no autorizado.
 • Reportar los defectos con pasos para reproducirlos.

   Listo cuando: El escenario completo pasa sin intervención manual y queda la evidencia.


 S4-T08 - Corrección integración y preparación demo
   Historia(s): TODAS
   Componente: Integración
   Responsable: E06
   Colaboradores: E08/E07
   Estimación: 4h
   Día objetivo: D5
   Dependencia: SQL + endpoints acordados
   Entregable / evidencia: PR/commit + captura o prueba ejecutada
   Estado inicial: Pendiente

 Qué se busca
 Que todo funcione junto en el entorno donde se hará la demo.
 Qué debes hacer
 • Corregir los fallos de integración que quedaron abiertos.
 • Configurar el entorno de demo (variables, URLs, cookies, CORS).
 • Preparar los datos de demo según la checklist de E03.
 • Hacer un ensayo completo y anotar los problemas.

   Listo cuando: Se hizo un ensayo de punta a punta sin errores.




CampusMarket - Guía de desarrollo por sprints                                                    Página 40
 S4-T09 - Pruebas negativas de token, permisos y sesión
   Historia(s): HU-01/HU-04
   Componente: Backend/Seguridad
   Responsable: E07
   Colaboradores: E09
   Estimación: 5h
   Día objetivo: D3
   Dependencia: Modelo ER y contratos de autenticación
   Entregable / evidencia: PR/commit + captura o prueba ejecutada
   Estado inicial: Pendiente

 Qué se busca
 Intentar romper el sistema como lo haría alguien malintencionado.
 Qué debes hacer
 • Probar tokens vencidos, usados, alterados y reutilizados (verificación y recuperación).
 • Probar el exceso de intentos y de reenvíos.
 • Probar el acceso sin cookie, con sesión revocada y con sesión de otro usuario.
 • Probar permisos: un estudiante intentando acciones de administrador o editando productos ajenos.
 • Documentar cada hallazgo con severidad.

   Listo cuando: Entregas el informe y los hallazgos graves tienen corrección asignada (S4-T10).


 S4-T10 - Correcciones de seguridad y evidencias
   Historia(s): TODAS
   Componente: Backend/QA
   Responsable: E07
   Colaboradores: E09
   Estimación: 4h
   Día objetivo: D5
   Dependencia: Modelo ER y contratos de autenticación
   Entregable / evidencia: PR/commit + captura o prueba ejecutada
   Estado inicial: Pendiente

 Qué se busca
 Corregir lo que encontró S4-T09 y juntar las pruebas de que todo cumple.
 Qué debes hacer
 • Corregir los hallazgos de seguridad priorizados.
 • Reunir la evidencia de los casos CP-01 a CP-15 (resultado, captura o prueba automatizada) para el
   Sprint Review.
 • Volver a ejecutar las pruebas negativas tras cada corrección.

   Listo cuando: Los hallazgos críticos están cerrados y la evidencia está organizada por caso.




CampusMarket - Guía de desarrollo por sprints                                                      Página 41
 S4-T11 - API de moderación y permisos ADMIN
   Historia(s): HU-15
   Componente: Backend
   Responsable: E08
   Colaboradores: E09
   Estimación: 4h
   Día objetivo: D3
   Dependencia: Modelo ER y contratos de autenticación
   Entregable / evidencia: PR/commit + captura o prueba ejecutada
   Estado inicial: Pendiente

 Qué se busca
 El endpoint de moderación, protegido para administradores.
 Qué debes hacer
 • Implementar PATCH /admin/productos/{id}/estado para desactivar publicaciones.
 • Responder 403 a cualquier usuario que no sea administrador.
 • Asegurar que el producto desactivado no sale en el catálogo ni en las búsquedas.

   Reglas que no puedes saltarte: CA-15-01 y CA-15-02.

   Listo cuando: Pruebas con administrador y con estudiante pasan, y el producto desactivado
   desaparece del catálogo.


 S4-T12 - Corregir defectos y finalizar contrato API
   Historia(s): TODAS
   Componente: Backend
   Responsable: E08
   Colaboradores: E09
   Estimación: 5h
   Día objetivo: D5
   Dependencia: Modelo ER y contratos de autenticación
   Entregable / evidencia: PR/commit + captura o prueba ejecutada
   Estado inicial: Pendiente

 Qué se busca
 Cerrar el backend con un contrato de API estable y sin defectos conocidos graves.
 Qué debes hacer
 • Corregir los defectos pendientes reportados por QA y por integración.
 • Revisar que todos los endpoints usen códigos de estado y formatos de error consistentes.
 • Actualizar y dejar como versión final la documentación de la API.

   Listo cuando: La documentación coincide con el comportamiento real y no quedan defectos críticos
   abiertos.




CampusMarket - Guía de desarrollo por sprints                                                  Página 42
 S4-T13 - Auditar constraints e integridad SQL
   Historia(s): TODAS
   Componente: BD/QA
   Responsable: E09
   Colaboradores: E07/E08
   Estimación: 5h
   Día objetivo: D3
   Dependencia: Backlog aprobado y objetivos del sprint
   Entregable / evidencia: PR/commit + captura o prueba ejecutada
   Estado inicial: Pendiente

 Qué se busca
 Confirmar que la base de datos protege las reglas del negocio por sí sola.
 Qué debes hacer
 • Revisar todas las tablas: claves primarias y foráneas, unicidad, restricciones de rango (precio,
   descuento, estrellas) y estados válidos.
 • Buscar datos huérfanos o incoherentes (por ejemplo, producto VENDIDO sin venta).
 • Agregar las restricciones que falten mediante migraciones.
 • Documentar el resultado de la auditoría.

   Listo cuando: Entregas la auditoría y las correcciones necesarias quedaron aplicadas.


 S4-T14 - Scripts de despliegue, backup y smoke tests
   Historia(s): TODAS
   Componente: BD/DevOps
   Responsable: E09
   Colaboradores: E07/E08
   Estimación: 4h
   Día objetivo: D5
   Dependencia: Backlog aprobado y objetivos del sprint
   Entregable / evidencia: PR/commit + captura o prueba ejecutada
   Estado inicial: Pendiente

 Qué se busca
 Poder desplegar el sistema de forma repetible y recuperarlo si algo sale mal.
 Qué debes hacer
 • Crear scripts para desplegar (migraciones, variables de entorno, arranque).
 • Crear el procedimiento de respaldo (backup) y restauración, y probar que restaurar realmente
   funciona.
 • Ampliar el smoke test para cubrir los flujos principales después de un despliegue.
 • Documentar el paso a paso.

   Listo cuando: Alguien del equipo puede desplegar desde cero y restaurar un respaldo siguiendo tu
   documento.




CampusMarket - Guía de desarrollo por sprints                                                         Página 43
 Puntos por confirmar con el Product Owner
 Estos puntos no están definidos en el material original y conviene aclararlos antes o durante el sprint
 indicado.
 • HU-11 (Sprint 3): el documento solo dice que 110 % se rechaza con 422. Falta definir si 100 % es
   válido y qué pasa con 0 o valores negativos.
 • HU-04 (Sprint 2): ninguna tarea de base de datos crea de forma explícita la tabla de tokens de
   recuperación. S2-T14 pide verificar que exista; confirma quién la crea.
 • HU-03 (Sprint 1): la interfaz necesita saber si hay una sesión activa, pero no hay un endpoint de
   'consultar sesión' definido. E05 y E08 deben acordarlo en S1-T05 y S1-T11.
 • HU-07 / HU-09 (Sprints 2 y 3): el catálogo simple (S2-T05) se entrega con HU-07 y el catálogo con
   filtros y paginación llega con HU-09 en el Sprint 3. Conviene que E05 y E08 mantengan el mismo
   contrato de GET /productos entre ambos sprints.




CampusMarket - Guía de desarrollo por sprints                                                       Página 44
