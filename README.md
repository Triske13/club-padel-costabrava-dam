# Club Pádel Costa Brava — Proyecto final DAM

Proyecto académico para gestionar socios, pistas, mantenimiento y reservas de un club de pádel situado en Roses (Girona).

## Aplicaciones previstas

- **Android (Kotlin):** activación de la cuenta del socio mediante un código enviado por correo, consulta del calendario y gestión de sus reservas.
- **Escritorio para Windows (Java y JavaFX):** administración de socios, reservas, pistas y bloqueos de mantenimiento.
- **Servidor (Java y Spring Boot):** API REST, autenticación y reglas de negocio.
- **Base de datos:** (PostgreSQL): almacenamiento de socios, cuentas, pistas, reservas, bloqueos y festivos.

Los dos clientes se comunicarán con el servidor. Solo el servidor accederá a la base de datos.

## Estructura del repositorio

- server/: código del servidor.
- android/: aplicación para socios.
- desktop/: aplicación de administración para Windows.
- database/: esquema SQL y futuras migraciones.
- docs/: documentación del proyecto.

## Estado

El diseño inicial está finalizado y la base técnica está en preparación. Las aplicaciones todavía no son ejecutables.

El prototipo contempla cuatro pistas, reservas de una o dos horas y horarios según el día de la semana o los festivos aplicables. El trabajo se organiza en GitHub Projects.

## Puesta en marcha

Las instrucciones para preparar PostgreSQL y arrancar el servidor y los clientes se añadirán durante B2-02 y B2-03.

## Alcance inicial

El prototipo tendrá cuatro pistas, reservas de una o dos horas, horarios según el día y los festivos. El trabajo se organizará en GitHub Projects.

## Documentación

- [Requisitos consolidados](docs/requisitos.md)
- [Modelo entidad-relación](docs/modelo-er.drawio)
- [Esquema de la base de datos](database/esquema.sql)

Las credenciales y configuraciones locales no deben añadirse al repositorio.