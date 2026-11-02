# FixIt Now — Backend

Repositorio backend de FixIt Now.

Este backend expone la API REST para el catálogo de oficios y el registro preliminar de solicitudes de servicio con persistencia local. La rama principal es `main`.

## Requisitos

- JDK 21.
- Conexión a Maven Central la primera vez que se descarguen Maven y las dependencias.

## Iniciar y comprobar

En Linux o macOS:

```bash
./mvnw spring-boot:run
```

En Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

### Endpoints principales

1. **Consultar categorías de oficios:**
   ```powershell
   curl.exe -i http://localhost:8080/api/v1/categorias
   ```
   Devuelve HTTP 200 con el listado de oficios disponibles. El catálogo permanece temporalmente en memoria.

2. **Registrar un borrador de solicitud preliminar:**
   ```powershell
   curl.exe -i -X POST http://localhost:8080/api/v1/solicitudes `
     -H "Content-Type: application/json" `
     -d '{"categoriaId": 1, "titulo": "Fuga de agua en lavamanos", "descripcion": "Perdida constante bajo la bacha.", "nivelUrgencia": "ALTA"}'
   ```
   Devuelve HTTP 201 Created con el identificador asignado y estado `BORRADOR`.

3. **Consultar el estado de un borrador preliminar:**
   ```powershell
   curl.exe -i http://localhost:8080/api/v1/solicitudes/1/estado
   ```
   Devuelve HTTP 200 OK con el identificador y el estado `BORRADOR` (o HTTP 404 si no existe).

## Persistencia local y reinicio

- **Base de datos de desarrollo:** se utiliza **Spring Data JPA** con **H2 en archivo**, almacenada localmente en `./data/fixitnowdb.mv.db`. Esta configuración permite que los borradores registrados sobrevivan tras detener y reiniciar la aplicación (`spring.jpa.hibernate.ddl-auto=update`).
- **Pruebas automatizadas independientes:** la suite de pruebas ejecuta una base H2 en memoria (`jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1`) con esquema limpio (`create-drop`), garantizando que las pruebas no alteren ni dependan de la base local de desarrollo.
- **Consola H2:** accesible en `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:file:./data/fixitnowdb`, usuario `sa`, contraseña vacía).

### Comprobación manual de supervivencia al reinicio

1. Iniciar la aplicación: `.\mvnw.cmd spring-boot:run`
2. Enviar un POST a `/api/v1/solicitudes` y registrar el `id` obtenido (ej. `1`).
3. Detener la aplicación (Ctrl+C en la consola).
4. Volver a iniciar la aplicación: `.\mvnw.cmd spring-boot:run`
5. Realizar `GET /api/v1/solicitudes/1/estado`. La solicitud responderá HTTP 200 con el estado `BORRADOR`, demostrando la persistencia efectiva entre reinicios.

## Pruebas automatizadas

Para compilar y ejecutar las pruebas:

En Linux o macOS:
```bash
./mvnw test
```

En Windows:
```powershell
.\mvnw.cmd test
```

## Limitaciones y fuera de alcance de esta etapa

- **Borradores locales:** las solicitudes son borradores preliminares y no están publicadas para profesionales.
- **Catálogo en memoria:** las categorías siguen cargadas en memoria y `categoriaId` no posee una clave foránea relacional en la tabla de solicitudes.
- **Seguridad y uso público:** aún no hay autenticación, autorización ni control de titularidad sobre las solicitudes. No ingresar datos personales reales ni desplegar esta API en entornos públicos.
- Ver detalles en [API](docs/api.md) y [decisiones técnicas](docs/decisiones.md).
