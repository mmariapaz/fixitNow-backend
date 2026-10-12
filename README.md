# FixIt Now — Backend

Repositorio backend de FixIt Now.

Este primer incremento ofrece un catálogo público de oficios. La rama principal es `main`.

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

La consulta `GET http://localhost:8080/api/v1/categorias` devuelve HTTP 200 y un JSON
con identificador y nombre para Plomería, Electricidad, Gas, Cerrajería,
Albañilería y Pintura. Por ejemplo:

```bash
curl -i http://localhost:8080/api/v1/categorias
```

Para compilar y ejecutar las pruebas automatizadas:

```bash
./mvnw test
```

El catálogo está definido temporalmente en memoria: no hay base de datos ni
posibilidad de persistir cambios. Ver [API](docs/api.md) y
[decisiones técnicas](docs/decisiones.md).
