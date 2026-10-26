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

## Registrar una solicitud preliminar de servicio

`POST /api/v1/solicitudes` permite cargar un borrador preliminar indicando la categoría del oficio, un título breve, una descripción de la necesidad y el nivel de urgencia (`BAJA`, `MEDIA`, `ALTA`, `EMERGENCIA`).

Petición: `Content-Type: application/json`.

```json
{
  "categoriaId": 1,
  "titulo": "Fuga de agua en lavamanos",
  "descripcion": "El caño de alimentación tiene una pérdida constante.",
  "nivelUrgencia": "ALTA"
}
```

Respuestas:

- Si los datos son válidos y la categoría existe: HTTP 201 Created, `Content-Type: application/json`.

```json
{
  "id": 1,
  "categoriaId": 1,
  "titulo": "Fuga de agua en lavamanos",
  "descripcion": "El caño de alimentación tiene una pérdida constante.",
  "nivelUrgencia": "ALTA",
  "estado": "BORRADOR"
}
```

- Si falta algún campo obligatorio, si los textos están en blanco, si el título supera los 150 caracteres, si el nivel de urgencia es desconocido o si el `categoriaId` no existe en el catálogo: HTTP 400 Bad Request.

Las solicitudes se guardan únicamente en memoria durante la ejecución con un identificador único generado y en estado `BORRADOR`. No persisten entre reinicios y no se publican a profesionales.

## Consultar el estado de una solicitud

`GET /api/v1/solicitudes/{id}/estado` permite consultar el estado actual de un borrador preliminar a partir de su identificador. Es público y no requiere autenticación.

Respuestas:

- Si la solicitud existe: HTTP 200 OK, `Content-Type: application/json`. Por ejemplo:

```json
{
  "id": 1,
  "estado": "BORRADOR"
}
```

- Si la solicitud no existe: HTTP 404 Not Found.

La consulta es de solo lectura: no modifica el estado ni expone otros datos como el título, la descripción, la categoría o el nivel de urgencia.

