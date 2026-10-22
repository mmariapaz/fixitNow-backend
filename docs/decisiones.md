# Decisiones técnicas iniciales

- **Java 21 y Spring Boot 3.3.4:** se mantienen las versiones de referencia del
  backend histórico `Fixitapp` para que el grupo trabaje sobre una base conocida.
  Se revisó esa referencia sin copiar su código.
- **Maven Wrapper:** permite ejecutar los mismos comandos de Maven en distintos
  equipos sin instalar Maven manualmente. Se usa Spring Web para exponer la API
  REST y Spring Boot Starter Test para verificarla.
- **Separación de responsabilidades:** `CategoriaController` recibe la petición
  HTTP y delega en `CategoriaService`. El servicio conserva el catálogo inicial
  y devuelve los datos representados por `Categoria`.
- **Datos temporales:** las seis categorías están en una lista inmutable en
  memoria. Este incremento no tiene base de datos, autenticación ni operaciones
  para modificar el catálogo. Los datos se reconstruyen en cada arranque.
- **Solicitudes preliminares en memoria:** las solicitudes creadas a través de
  `POST /api/v1/solicitudes` se almacenan en una colección en memoria (`CopyOnWriteArrayList`)
  y se les asigna un identificador secuencial único (`AtomicLong`). Nacen siempre con estado
  `BORRADOR`. Los borradores se pierden al reiniciar el servidor. Esta versión no identifica
  al cliente, no asocia direcciones ni publica solicitudes para profesionales.
- **Validación en dos etapas:**
  - El DTO de entrada valida la sintaxis y formato con Bean Validation (`@NotNull`, `@NotBlank`,
    `@Size(max = 150)` y el enum `NivelUrgencia`).
  - El servicio (`SolicitudService`) comprueba la regla de negocio consultando `CategoriaService`
    para verificar que `categoriaId` exista en el catálogo de oficios existente. Si no existe
    o la entrada es inválida, se devuelve HTTP 400 y no se almacena ningún borrador.

