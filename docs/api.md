# API inicial

## Consultar categorías de oficios

`GET /api/v1/categorias` es público y no requiere parámetros ni autenticación.

Respuesta: HTTP 200, `Content-Type: application/json`.

```json
[
  {"id": 1, "nombre": "Plomería"},
  {"id": 2, "nombre": "Electricidad"},
  {"id": 3, "nombre": "Gas"},
  {"id": 4, "nombre": "Cerrajería"},
  {"id": 5, "nombre": "Albañilería"},
  {"id": 6, "nombre": "Pintura"}
]
```

## Consultar una categoría por identificador

`GET /api/v1/categorias/{id}` es público y no requiere autenticación.

Respuestas:

- Si la categoría existe: HTTP 200, `Content-Type: application/json`. Por ejemplo:

```json
{"id": 1, "nombre": "Plomería"}
```

- Si la categoría no existe: HTTP 404 Not Found.

El catálogo es fijo y permanece solo en memoria. Esta versión no permite crear,
modificar ni eliminar categorías, y no guarda datos entre ejecuciones.
