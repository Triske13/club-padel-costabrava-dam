# Club Pádel Costa Brava — Proyecto final DAM

Sistema cliente-servidor para gestionar socios, pistas, bloqueos y reservas de un club de pádel ficticio en Roses (Girona).

## Aplicaciones

- **Android (Kotlin y Jetpack Compose):** activación o recuperación por código de correo, acceso de socio, calendario, disponibilidad, reservas propias, cancelación, cambio de contraseña y cierre de sesión.
- **Windows (Java 21 y JavaFX):** acceso ADMIN, gestión de socios, agenda por pista, bloqueos, creación y cancelación de reservas, consulta de reservas actuales e histórico por fecha, y gestión de la propia contraseña.
- **Servidor (Java 21 y Spring Boot):** API REST/JSON, autenticación y roles, reglas de negocio, transacciones, acceso a PostgreSQL y envío de correo.
- **Base de datos (PostgreSQL):** esquema versionado con Flyway; cuatro pistas y 14 festivos de Roses correspondientes a 2026.

Los clientes se comunican con el servidor; solo este accede directamente a PostgreSQL.
El servidor usa la zona horaria Europe/Madrid para calcular los horarios y los plazos de reserva.

## Estado del proyecto

El servidor Spring Boot y PostgreSQL están desplegados en Railway. El
[diagnóstico de la API](https://padel-spring-production.up.railway.app/api/diagnostico)
permite comprobar que el servicio responde y tiene acceso a las cuatro pistas.

Se ha verificado el inicio de sesión como administrador y como socio, así como
la recepción del código de activación por correo. En la prueba integrada se
creó una reserva desde el APK firmado de Android y se comprobó que aparecía en
la aplicación de administración de Windows.

El código fuente conserva las direcciones locales por defecto. El APK de
entrega y el paquete portátil de Windows están configurados para conectarse a
Railway.

Los datos iniciales incluyen los festivos de Roses de 2026.

## Estructura

| Ruta | Contenido |
| --- | --- |
| `server/` | Servidor, migraciones Flyway y pruebas JUnit. |
| `android/` | Cliente móvil para USER. |
| `desktop/` | Cliente Windows para ADMIN. |
| `database/esquema.sql` | Esquema SQL de referencia; no se ejecuta además de Flyway. |
| `docs/` | Requisitos, modelo de datos y documentación. |

## Documentación

- [Requisitos consolidados](docs/requisitos.md)
- [Documentación técnica](docs/documentacion-tecnica.md)
- [Contrato de la API REST](docs/api-rest.md)
- [Puesta en marcha y pruebas locales](docs/puesta-en-marcha.md)
- [Manual de usuario](docs/manual-usuario.md)
- [Modelo entidad-relación editable](docs/modelo-er.drawio)
- [Esquema SQL de referencia](database/esquema.sql)

## Inicio rápido

Instalar Java 21 y PostgreSQL, crear una base de datos vacía, configurar `server/config/application-local.properties` y las variables `APP_ADMIN_EMAIL` y `APP_ADMIN_PASSWORD` para la primera instalación. Desde `server/`:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

Flyway aplica las migraciones al arrancar. `http://localhost:8080/api/diagnostico` debe indicar cuatro pistas. Para ejecutar Windows y Android y comprobar cada módulo, seguir la [guía de puesta en marcha](docs/puesta-en-marcha.md).

Las credenciales, archivos locales y datos personales reales no deben añadirse al repositorio.