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
