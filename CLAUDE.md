# Contexto del Proyecto: CampusMarket

Este repositorio corresponde al proyecto de Arquitecturas Empresariales dividido en cuatro áreas principales:
- `/DataBase`: Scripts SQL, modelos relacionales y estructura de la base de datos.
- `/backend`: Lógica de negocio, APIs y conexión a la base de datos.
- `/frontend`: Interfaz de usuario y consumo de servicios del backend.
- `/docs`: Guía de desarrollo por sprints, requerimientos y mockups.

## Reglas para el Agente (Claude)
1. **Sincronización entre capas:** Lee todo el contexto cargado para asegurar que los nombres de tablas, campos JSON, rutas de API y variables coincidan exactamente entre `DataBase`, `backend` y `frontend`.
2. **Alcance por rol:** Responde y genera código únicamente para el área que el usuario indique al inicio de su mensaje (Frontend, Backend o Base de Datos). No modifiques código de otras capas a menos que se te pida explícitamente.
3. **Idioma y estándares:** Mantén los comentarios y documentación en español. Respeta las convenciones de nombres ya existentes en los archivos del repositorio.
