# Decisiones técnicas

- **Java 21 y Spring Boot 3.3.4:** se mantienen las versiones de referencia del backend para garantizar compatibilidad y consistencia del entorno.
- **Maven Wrapper:** estandariza las ejecuciones de compilación, empaquetado y pruebas sin requerir instalación manual previa de Maven.
- **Separación de responsabilidades (Capas, DTOs, Entidad y Repositorio):**
  - **Controlador (`SolicitudController` / `CategoriaController`):** gestiona las solicitudes HTTP, aplica validaciones y mapea respuestas utilizando exclusivamente DTOs. **No expone entidades JPA directamente** para evitar acoplar la API a la estructura interna de persistencia ni exponer datos inadvertidamente.
  - **DTOs (`SolicitudRequest`, `SolicitudResponse`, `SolicitudEstadoResponse`):** definen los contratos formales de entrada y salida, asegurando la validación sintáctica mediante Bean Validation (`@NotNull`, `@NotBlank`, `@Size`) y la proyección precisa de atributos (por ejemplo, en la consulta de estado solo se exponen `id` y `estado`).
  - **Entidad JPA (`Solicitud`):** representa el estado persistente y modela la tabla `solicitudes` en la base relacional con `@Entity`, `@Table(name = "solicitudes")`, `@Id`, `@GeneratedValue(strategy = GenerationType.IDENTITY)` y atributos correspondientes.
  - **Repositorio (`SolicitudRepository`):** interfaz basada en Spring Data JPA (`JpaRepository<Solicitud, Long>`) que abstrae el acceso a datos y las operaciones CRUD sin acoplar la lógica de negocio a SQL específico o a un motor en particular.
  - **Servicio (`SolicitudService`):** orquesta las reglas de negocio, valida dependencias entre componentes y traduce entre entidades y DTOs.
- **Persistencia local de borradores (B05):**
  - Se sustituye el almacenamiento temporal en memoria (`CopyOnWriteArrayList` y `AtomicLong`) por persistencia local relacional mediante **Spring Data JPA** y **H2 Database en archivo**.
  - En desarrollo, la conexión apunta a `jdbc:h2:file:./data/fixitnowdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE` con `spring.jpa.hibernate.ddl-auto=update`. Esta configuración evita recrear o vaciar las tablas en cada arranque, garantizando que los borradores sobrevivan a reinicios del servidor.
  - La carpeta `data/` y los archivos `*.mv.db` / `*.trace.db` están expresamente ignorados en `.gitignore` para no versionar datos locales en Git.
- **Bases separadas para desarrollo y pruebas:**
  - El perfil de pruebas utiliza un archivo específico `src/test/resources/application.properties` con base de datos H2 en memoria (`jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1`) y `spring.jpa.hibernate.ddl-auto=create-drop`.
  - Esto garantiza aislamiento completo: las pruebas automatizadas se ejecutan en un entorno limpio, repetible y sin modificar ni depender de los datos generados durante el desarrollo local.
- **Relación con el catálogo de categorías:**
  - El catálogo de categorías sigue definiéndose en memoria (`CategoriaService`) como una colección inmutable fija.
  - Por esta razón, el campo `categoriaId` en la entidad `Solicitud` se almacena como un valor numérico (`Long`) y su validez se comprueba en el servicio contra el catálogo disponible, sin declarar una clave foránea (FK) relacional en esta etapa.
- **Alcance y limitaciones:**
  - Las solicitudes registradas permanecen en estado `BORRADOR` y no se publican a profesionales.
  - No se cuenta con autenticación, autorización ni control de titularidad sobre las solicitudes. La API no es apta para despliegue público ni para registrar información personal real hasta implementar mecanismos de seguridad adecuados.
- **Registro de usuarios y gestión segura de contraseñas (B06):**
  - **Entidad `Usuario`:** modela la tabla `usuarios` persistiendo `correo`, `contrasenaHash` y `rol` (`CLIENTE`, `PROFESIONAL`). El correo posee restricción formal de unicidad a nivel base de datos (`@Column(unique = true)`).
  - **Normalización de correos:** para evitar duplicados accidentales o ataques por variación de grafía, el correo se normaliza aplicando `trim()` y `toLowerCase(Locale.ROOT)` tanto en la deserialización temprana del DTO como en el servicio de negocio.
  - **Hashing de contraseñas:** se incorpora Spring Security (`spring-boot-starter-security`) utilizando `BCryptPasswordEncoder` para almacenar exclusivamente el hash de la contraseña. Jamás se persiste ni se expone la contraseña en texto claro ni su hash.
  - **Límites de contraseña:** se restringe la longitud a un mínimo de 8 caracteres y un máximo de 70 caracteres para prevenir vulnerabilidades de truncamiento en implementaciones basadas en BCrypt (límite de 72 bytes).
  - **Configuración de seguridad transitoria:** en esta etapa se mantiene una política permisiva sin login ni tokens activos (`permitAll()`), permitiendo la continuidad operativa de los endpoints existentes mientras se habilita el endpoint público de registro.
- **Inicio de sesión y validación de tokens JWT (B07):**
  - **Firma simétrica y algoritmo:** se utiliza HMAC-SHA256 (`HS256`) con una clave secreta simétrica provista externamente por la variable de entorno `JWT_SECRET`. Se exige una longitud mínima de 256 bits (32 caracteres); de faltar o ser menor en producción, la aplicación aborta el arranque protegiendo el sistema.
  - **Estructura del token:** los claims se limitan a `sub` (identificador numérico del usuario como String), `iss` (`"fixitnow-backend"`), `iat` (emisión) y `exp` (expiración a 15 minutos / 900 s). No se incluyen contraseñas, hashes ni el rol del usuario dentro del token.
  - **Validación con Spring Security nativo:** la validación y decodificación de tokens se realiza mediante el soporte nativo de Resource Server (`spring-boot-starter-oauth2-resource-server` con `NimbusJwtDecoder`), sin filtros propios ni criptografía ad-hoc.
  - **Claves ficticias aisladas para pruebas:** en `src/test/resources/application.properties` se configura una clave ficticia independiente para las pruebas automatizadas, garantizando que no se requieran variables de entorno secretas ni se expongan credenciales en Git.
  - **Comprobación de expiración:** la prueba de vencimiento de tokens se realiza controlando los instantes de emisión y expiración al generar un token de prueba en el pasado, verificando el rechazo con HTTP 401 de forma determinista y sin tiempos de espera.
  - **Alcance transitorio:** únicamente el endpoint `GET /api/v1/auth/me` se encuentra protegido por token en B07. Las rutas previas de categorías y solicitudes permanecen accesibles con fines de prueba local, sin atribuir borradores a cuentas todavía.
- **Protección y asociación de borradores al cliente (B08):**
  - **Identidad autoritativa desde JWT:** la identidad del cliente propietario se extrae estrictamente del claim `sub` del token Bearer validado en el `SecurityContext`. Se rechaza y no se confía en ningún campo `clienteId` enviado en el cuerpo de la petición.
  - **Autorización por roles y resolución en base confiable:** se implementa `JwtUserAuthenticationConverter` para resolver las autoridades (`ROLE_CLIENTE`, `ROLE_PROFESIONAL`) contra la entidad `Usuario` persistida. Los endpoints de solicitudes exigen formalmente rol `CLIENTE`, respondiendo HTTP 403 Forbidden a cuentas con rol `PROFESIONAL`.
  - **Comprobación de titularidad en el servicio:** para consultar el estado de una solicitud, se ejecuta `findByIdAndClienteId(id, clienteId)` garantizando que solo el creador pueda verla. Intentos de consulta por otros clientes devuelven HTTP 404 Not Found para no revelar la existencia del recurso.
  - **Preservación de borradores históricos sin propietario:** la columna `cliente_id` se incorpora como nullable en la tabla `solicitudes`. Los borradores creados antes de B08 conservan su registro intacto pero no son atribuidos a ningún usuario, respondiendo HTTP 404 a cualquier cliente que intente consultarlos.
- **Direcciones propias del cliente (B09):**
  - **Entidad `Direccion`:** modela la tabla relacional `direcciones` asociando cada registro al `clienteId` de la cuenta autenticada.
  - **Esquema acordado de longitudes y validaciones geográficas:** según lo acordado en la decisión D10, `calle` y `ciudad` se limitan a 100 caracteres; `numero`, `pisoDepto` y `codigoPostal` a 20 caracteres; y las coordenadas geográficas se validan con precisión decimal asegurando rangos válidos: latitud en [-90.0, 90.0] y longitud en [-180.0, 180.0].
  - **Aislamiento por identidad autenticada:** el endpoint `GET /api/v1/direcciones` filtra exclusivamente por `findAllByClienteId(clienteId)`. Ningún cliente puede ver ni consultar direcciones registradas por otros usuarios.
- **Publicación de solicitudes y operación transaccional (B10):**
  - **Doble validación de pertenencia:** antes de publicar, el servicio comprueba que tanto la solicitud preliminar como la dirección provista pertenezcan al cliente autenticado mediante consultas filtradas por `clienteId`. De no coincidir o no existir, responde HTTP 404 sin revelar la causa para evitar enumeración.
  - **Transición estricta y atomicidad:** la transición de estado solo admite `BORRADOR` a `PUBLICADA` en una única transacción `@Transactional`. Si la solicitud ya se encuentra publicada o en otro estado, responde HTTP 409 Conflict sin modificar dirección ni fecha.
  - **Formato temporal local:** la fecha tentativa se representa mediante `LocalDateTime` (ISO 8601 sin zona horaria, ej. `YYYY-MM-DDTHH:mm:ss`), cumpliendo la consigna de registrar el momento pactado local sin imponer que sea estrictamente futura en esta etapa preliminar.
- **Perfil profesional y especialidades (B11):**
  - **Entidad `PerfilProfesional` y unicidad:** se modela la tabla `perfiles_profesionales` con una columna `profesional_id` con restricción única (`unique = true`), asegurando exactamente un perfil por usuario profesional.
  - **Especialidades desacopladas del catálogo en memoria:** las especialidades se persisten mediante `@ElementCollection` de identificadores numéricos (`Set<Long>`) en la tabla secundaria `profesional_especialidades`. Esto permite mantener el catálogo de categorías en memoria (`CategoriaService`) sin forzar claves foráneas relacionales artificiales en JPA.
  - **Validación rigurosa de categorías:** al crear o actualizar el perfil, se valida que la lista no esté vacía, no contenga duplicados y que cada `categoriaId` exista efectivamente en el catálogo. De lo contrario, se rechaza la solicitud con HTTP 400 Bad Request.
  - **Idempotencia de creación y actualización:** el endpoint `PUT /api/v1/profesionales/perfil` implementa una semántica `upsert` limpia: si el perfil no existe lo crea, y si ya existe actualiza sus especialidades, matrícula, experiencia, coordenadas y radio de cobertura, respondiendo HTTP 200 OK con el DTO del perfil resultante.
  - **Consulta propia y control de acceso:** `GET /api/v1/profesionales/perfil` retorna los datos del profesional autenticado o HTTP 404 Not Found si aún no fue configurado. Se bloquea el acceso con HTTP 403 Forbidden para usuarios con rol `CLIENTE`.





