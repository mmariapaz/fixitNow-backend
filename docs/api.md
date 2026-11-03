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
modificar ni eliminar categorías.

## Registrar una solicitud preliminar de servicio

`POST /api/v1/solicitudes` permite cargar un borrador preliminar indicando la categoría del oficio, un título breve, una descripción de la necesidad y el nivel de urgencia (`BAJA`, `MEDIA`, `ALTA`, `EMERGENCIA`).

Requiere autenticación obligatoria con rol `CLIENTE`. La titularidad se determina exclusivamente a partir de la identidad autenticada por el token (no se acepta ningún identificador de cliente en el cuerpo).

Cabecera requerida: `Authorization: Bearer <accessToken>`.
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

- Si los datos son válidos, la categoría existe y la cuenta es `CLIENTE`: HTTP 201 Created, `Content-Type: application/json`.

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
- Si falta el token o no es válido: HTTP 401 Unauthorized.
- Si el usuario autenticado tiene rol `PROFESIONAL`: HTTP 403 Forbidden.

Las solicitudes se guardan asociadas a la cuenta del cliente autenticado (`clienteId`), de forma persistente en la base de datos local relacional con estado `BORRADOR`.

## Consultar el estado de una solicitud

`GET /api/v1/solicitudes/{id}/estado` permite consultar el estado actual de un borrador preliminar propio a partir de su identificador.

Requiere autenticación obligatoria con rol `CLIENTE`. Solo el titular que registró la solicitud puede consultar su estado.

Cabecera requerida: `Authorization: Bearer <accessToken>`.

Respuestas:

- Si la solicitud existe y pertenece al cliente autenticado: HTTP 200 OK, `Content-Type: application/json`. Por ejemplo:

```json
{
  "id": 1,
  "estado": "BORRADOR"
}
```

- Si la solicitud no existe o pertenece a otro cliente (o fue creada antes de B08 sin titular): HTTP 404 Not Found (sin revelar el motivo).
- Si falta el token o no es válido: HTTP 401 Unauthorized.
- Si el usuario autenticado tiene rol `PROFESIONAL`: HTTP 403 Forbidden.

La consulta es de solo lectura: no modifica el estado ni expone otros datos como el título, la descripción, la categoría, el nivel de urgencia o el propietario.

## Publicar una solicitud

`PATCH /api/v1/solicitudes/{id}/publicacion` permite completar un borrador preliminar propio asignándole una dirección propia y una fecha tentativa, pasando su estado a `PUBLICADA`.

Requiere autenticación obligatoria con rol `CLIENTE`.

Cabecera requerida: `Authorization: Bearer <accessToken>`.
Petición: `Content-Type: application/json`.

```json
{
  "direccionId": 1,
  "fechaTentativa": "2026-11-20T10:30:00"
}
```

Reglas y validaciones:
- `direccionId`: obligatorio, debe pertenecer al cliente autenticado.
- `fechaTentativa`: obligatoria, formato ISO 8601 local (`YYYY-MM-DDTHH:mm:ss`). No se exige que sea futura.
- Transición permitida exclusivamente: de estado `BORRADOR` a `PUBLICADA`.
- La operación es atómica: asocia dirección y fecha y actualiza el estado en una única transacción.

Respuestas:
- **HTTP 200 OK**: Solicitud publicada exitosamente.

```json
{
  "id": 1,
  "estado": "PUBLICADA",
  "direccionId": 1,
  "fechaTentativa": "2026-11-20T10:30:00",
  "nivelUrgencia": "ALTA"
}
```

- **HTTP 400 Bad Request**: Campos obligatorios faltantes o fecha mal formateada.
- **HTTP 401 Unauthorized**: Petición sin token o con token inválido.
- **HTTP 403 Forbidden**: Cuenta con rol `PROFESIONAL`.
- **HTTP 404 Not Found**: La solicitud o la dirección no existen, o pertenecen a otro cliente.
- **HTTP 409 Conflict**: La solicitud no se encuentra en estado `BORRADOR` (por ejemplo si ya fue publicada previamente).

## Registrar cuenta de usuario

`POST /api/v1/auth/registro` permite dar de alta una nueva cuenta de usuario con rol `CLIENTE` o `PROFESIONAL`.

Petición: `Content-Type: application/json`.

```json
{
  "correo": "usuario.ejemplo@correo.com",
  "contrasena": "claveSegura123",
  "rol": "CLIENTE"
}
```

Reglas y validaciones:
- `correo`: obligatorio, formato de correo válido. Se normaliza eliminando espacios en los extremos y convirtiendo a minúsculas (`trim` y `toLowerCase(Locale.ROOT)`). Posee restricción de unicidad en la base de datos.
- `contrasena`: obligatoria, longitud acordada entre 8 y 70 caracteres. Se almacena únicamente su hash seguro utilizando `BCryptPasswordEncoder`. Jamás se almacena ni se expone la contraseña en texto plano ni el hash.
- `rol`: obligatorio, valores permitidos exclusivamente: `CLIENTE` o `PROFESIONAL`.

Respuestas:
- **HTTP 201 Created**: Cuenta creada exitosamente.

```json
{
  "id": 1,
  "correo": "usuario.ejemplo@correo.com",
  "rol": "CLIENTE"
}
```

- **HTTP 400 Bad Request**: Campos obligatorios faltantes, correo inválido, contraseña menor a 8 caracteres o mayor a 70, o rol desconocido.
- **HTTP 409 Conflict**: El correo ya se encuentra registrado en el sistema (incluso si se ingresa con diferencias de mayúsculas o espacios).

## Iniciar sesión y obtener token JWT

`POST /api/v1/auth/login` permite autenticarse enviando credenciales de acceso para recibir un token JWT firmado.

Petición: `Content-Type: application/json`.

```json
{
  "correo": "usuario.ejemplo@correo.com",
  "contrasena": "claveSegura123"
}
```

Respuestas:
- **HTTP 200 OK**: Credenciales válidas. Devuelve el token JWT Bearer y el tiempo de expiración en segundos (15 minutos = 900 s).

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 900
}
```

- **HTTP 401 Unauthorized**: Correo no registrado o contraseña incorrecta. Se responde con un mensaje genérico para no revelar la existencia previa del correo.

## Consultar datos de la cuenta autenticada

`GET /api/v1/auth/me` es una ruta protegida que valida la identidad acreditada en el token JWT provisto en la cabecera `Authorization`.

Cabecera requerida: `Authorization: Bearer <accessToken>`.

Respuestas:
- **HTTP 200 OK**: Token válido. Devuelve los datos de la cuenta autenticada.

```json
{
  "id": 1,
  "correo": "usuario.ejemplo@correo.com",
  "rol": "CLIENTE"
}
```

- **HTTP 401 Unauthorized**: Token ausente, alterado, vencido o con firma no válida.

## Registrar una dirección propia

`POST /api/v1/direcciones` permite a una cuenta `CLIENTE` dar de alta una dirección asociada a su cuenta para servicios.

Requiere autenticación obligatoria con rol `CLIENTE`. La titularidad se determina exclusivamente a partir del token JWT (no se acepta ningún identificador en el cuerpo).

Cabecera requerida: `Authorization: Bearer <accessToken>`.
Petición: `Content-Type: application/json`.

```json
{
  "calle": "Av. Corrientes",
  "numero": "1234",
  "pisoDepto": "4B",
  "ciudad": "Buenos Aires",
  "codigoPostal": "C1043",
  "latitud": -34.6037,
  "longitud": -58.3816
}
```

Reglas y validaciones:
- `calle`: obligatoria, hasta 100 caracteres.
- `numero`: obligatorio, hasta 20 caracteres.
- `pisoDepto`: opcional, hasta 20 caracteres.
- `ciudad`: obligatoria, hasta 100 caracteres.
- `codigoPostal`: opcional, hasta 20 caracteres.
- `latitud`: obligatoria, valor numérico decimal entre -90.0 y 90.0.
- `longitud`: obligatoria, valor numérico decimal entre -180.0 y 180.0.

Respuestas:
- **HTTP 201 Created**: Dirección registrada exitosamente.

```json
{
  "id": 1,
  "calle": "Av. Corrientes",
  "numero": "1234",
  "pisoDepto": "4B",
  "ciudad": "Buenos Aires",
  "codigoPostal": "C1043",
  "latitud": -34.6037,
  "longitud": -58.3816
}
```

- **HTTP 400 Bad Request**: Campos obligatorios faltantes o coordenadas fuera de rango.
- **HTTP 401 Unauthorized**: Petición sin token o con token inválido.
- **HTTP 403 Forbidden**: Cuenta autenticada con rol `PROFESIONAL`.

## Consultar direcciones propias

`GET /api/v1/direcciones` permite a una cuenta `CLIENTE` consultar todas las direcciones asociadas a su cuenta.

Requiere autenticación obligatoria con rol `CLIENTE`.

Cabecera requerida: `Authorization: Bearer <accessToken>`.

Respuestas:
- **HTTP 200 OK**: Lista de direcciones del cliente autenticado (o `[]` si aún no registró ninguna).
- **HTTP 401 Unauthorized**: Petición sin token o inválido.
- **HTTP 403 Forbidden**: Cuenta con rol `PROFESIONAL`.

## Configurar o actualizar perfil profesional

`PUT /api/v1/profesionales/perfil` permite a una cuenta `PROFESIONAL` configurar inicialmente su perfil operativo o actualizarlo si ya existe (operación idempotente).

Requiere autenticación obligatoria con rol `PROFESIONAL`. El perfil queda indefectiblemente vinculado a la cuenta autenticada según el token JWT provisto.

Cabecera requerida: `Authorization: Bearer <accessToken>`.
Petición: `Content-Type: application/json`.

```json
{
  "categoriaIds": [1, 2],
  "matriculaLicencia": "MAT-2024-ABC",
  "descripcionExperiencia": "10 años en plomería y electricidad residencial.",
  "latitudBase": -34.6037,
  "longitudBase": -58.3816,
  "radioCoberturaKm": 15
}
```

Reglas y validaciones:
- `categoriaIds`: obligatorio, arreglo no vacío con los identificadores de categorías donde el profesional ofrece servicios. Cada identificador debe corresponder a una categoría existente en el catálogo. No se admiten identificadores duplicados.
- `matriculaLicencia`: opcional, texto de hasta 50 caracteres.
- `descripcionExperiencia`: opcional, texto descriptivo de hasta 1000 caracteres.
- `latitudBase`: obligatoria, valor numérico decimal entre -90.0 y 90.0.
- `longitudBase`: obligatoria, valor numérico decimal entre -180.0 y 180.0.
- `radioCoberturaKm`: obligatorio, valor entero entre 1 y 50 kilómetros.

Respuestas:
- **HTTP 200 OK**: Perfil creado o actualizado exitosamente.

```json
{
  "id": 1,
  "profesionalId": 3,
  "categoriaIds": [1, 2],
  "matriculaLicencia": "MAT-2024-ABC",
  "descripcionExperiencia": "10 años en plomería y electricidad residencial.",
  "latitudBase": -34.6037,
  "longitudBase": -58.3816,
  "radioCoberturaKm": 15
}
```

- **HTTP 400 Bad Request**: Campos obligatorios faltantes, coordenadas o radio fuera de rango, categorías inexistentes o duplicadas.
- **HTTP 401 Unauthorized**: Petición sin token o token inválido.
- **HTTP 403 Forbidden**: Cuenta autenticada con rol `CLIENTE`.

## Consultar perfil profesional propio

`GET /api/v1/profesionales/perfil` permite a un usuario con rol `PROFESIONAL` consultar los datos de su propio perfil de cobertura y especialidades.

Requiere autenticación obligatoria con rol `PROFESIONAL`.

Cabecera requerida: `Authorization: Bearer <accessToken>`.

Respuestas:
- **HTTP 200 OK**: Perfil configurado del profesional autenticado.
- **HTTP 401 Unauthorized**: Petición sin token o token inválido.
- **HTTP 403 Forbidden**: Cuenta autenticada con rol `CLIENTE`.
- **HTTP 404 Not Found**: La cuenta profesional aún no ha configurado su perfil.




