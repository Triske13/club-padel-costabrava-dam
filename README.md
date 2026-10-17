# Club Pádel Costa Brava — Proyecto final DAM

Proyecto académico para gestionar socios, pistas, mantenimiento y reservas de un club de pádel situado en Roses (Girona).


## Aplicaciones previstas

* **Android (Kotlin):** activación de la cuenta del socio mediante un código enviado por correo, consulta del calendario y gestión de sus reservas.
* **Escritorio para Windows (Java y JavaFX):** administración de socios, reservas, pistas y bloqueos de mantenimiento.
* **Servidor (Java y Spring Boot):** API REST, autenticación y reglas de negocio.
* **Base de datos (PostgreSQL):** almacenamiento de socios, cuentas, pistas, reservas, bloqueos y festivos.

Los dos clientes se comunicarán con el servidor. Solo el servidor accederá a la base de datos.


## Estructura del repositorio

* server/: código del servidor.
* android/: aplicación para socios.
* desktop/: aplicación de administración para Windows.
* database/: esquema SQL, las migraciones ejecutables están en server/src/main/resources/db/migration/; database/esquema.sql queda como referencia.
* docs/: documentación del proyecto.


## Estado

El servidor ya arranca; Android y escritorio ya arrancan y consultan la API, mientras sus funciones de reservas y administración siguen pendientes.

La migración V2 carga los 14 festivos de Roses correspondientes a 2026. La lista local todavía no cubre 2027, que queda fuera del alcance inicial. Fuente: calendario de fiestas laborales del Ayuntamiento de Roses.

El prototipo contempla cuatro pistas, reservas de una o dos horas y horarios según el día de la semana o los festivos aplicables.

El trabajo se organiza en GitHub Projects.


## Puesta en marcha del servidor

Instalar Java 21 o superior y PostgreSQL.



* Crear una base de datos vacía llamada club_padel_dev.



* Crear server/config/application-local.properties con la URL, el usuario y la contraseña de PostgreSQL. Este archivo está excluido de Git.



* Desde la carpeta server, ejecutar en PowerShell:



.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"



Flyway aplicará automáticamente las migraciones de server/src/main/resources/db/migration; no hay que ejecutar database/esquema.sql manualmente. Para comprobar la conexión, abrir http://localhost:8080/api/diagnostico. La respuesta inicial debe indicar 4 pistas.


## Ejecutar los clientes de demostración

El servidor debe estar en marcha antes de abrir los clientes.

- **Android:** abrir `android/` en Android Studio, seleccionar un emulador y pulsar Run. La app consulta `/api/diagnostico`. Su dirección local se define en `ApiConfig.kt`.
- **Windows:** abrir `desktop/` en IntelliJ IDEA, ejecutar `Launcher.main()` y pulsar «Comprobar servidor». Su dirección local se define en `ApiConfig.java`.


## Documentación

* [Requisitos consolidados](docs/requisitos.md)
* [Modelo entidad-relación](docs/modelo-er.drawio)
* [Esquema de la base de datos](database/esquema.sql)



Las credenciales y configuraciones locales no deben añadirse al repositorio.