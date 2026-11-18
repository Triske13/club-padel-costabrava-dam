# Contrato de la API REST

El servidor local utiliza `http://localhost:8080`. La API desplegada
utiliza `https://padel-spring-production.up.railway.app`.

Las peticiones con cuerpo JSON utilizan `Content-Type: application/json`.
Las rutas protegidas requieren la cabecera `Authorization: Bearer <token>`.
El token se obtiene en el inicio de sesión. Los roles `USER` y `ADMIN`
corresponden a socios y personal de administración, respectivamente.

## Rutas

| Método | Ruta | Acceso | Operación |
| --- | --- | --- | --- |
| GET | `/api/diagnostico` | Público | Comprobar el estado del servicio y la lectura de pistas. |
| POST | `/api/auth/login` | Público | Iniciar sesión y obtener el token. |
| POST | `/api/auth/codigo` | Público | Solicitar un código de activación o recuperación. |
| POST | `/api/auth/contrasena` | Público | Establecer una contraseña mediante un código temporal. |
| POST | `/api/auth/logout` | USER, ADMIN | Cerrar la sesión actual. |
| PUT | `/api/mi-cuenta/contrasena` | USER, ADMIN | Cambiar la contraseña propia. |
| GET | `/api/calendario` | USER, ADMIN | Consultar el calendario mensual con `mes` en formato `AAAA-MM`. |
| GET | `/api/disponibilidad` | USER, ADMIN | Consultar las pistas y sus turnos para una `fecha`. |
| GET | `/api/mis-reservas` | USER | Consultar las reservas propias. |
| POST | `/api/mis-reservas` | USER | Crear una reserva propia. |
| POST | `/api/mis-reservas/{id}/cancelacion` | USER | Cancelar una reserva propia. |
| GET | `/api/admin/socios` | ADMIN | Listar socios, con el filtro opcional `buscar`. |
| GET | `/api/admin/socios/{id}` | ADMIN | Consultar la ficha de un socio. |
| POST | `/api/admin/socios` | ADMIN | Dar de alta un socio. |
| PUT | `/api/admin/socios/{id}` | ADMIN | Editar la ficha de un socio. |
| PUT | `/api/admin/socios/{id}/correo` | ADMIN | Cambiar el correo del socio. |
| PUT | `/api/admin/socios/{id}/estado` | ADMIN | Activar o inactivar un socio. |
| GET | `/api/admin/reservas` | ADMIN | Consultar reservas con filtros opcionales `fecha`, `pistaId` y `socioId`. |
| GET | `/api/admin/reservas/pagina` | ADMIN | Consultar reservas paginadas mediante `pagina` y, para el histórico, `fecha`. |
| POST | `/api/admin/reservas` | ADMIN | Crear una reserva en nombre de un socio. |
| POST | `/api/admin/reservas/{id}/cancelacion` | ADMIN | Cancelar una reserva indicando el motivo. |
| GET | `/api/admin/bloqueos` | ADMIN | Consultar los bloqueos para una `fecha`. |
| POST | `/api/admin/bloqueos` | ADMIN | Crear un bloqueo de pista. |
| DELETE | `/api/admin/bloqueos/{id}` | ADMIN | Retirar un bloqueo que aún no ha empezado. |

`{id}` es el identificador del recurso. Las fechas se expresan como
`AAAA-MM-DD` y las horas como `HH:mm`. La paginación de reservas comienza
en `0` y devuelve hasta 50 registros en `reservas`, junto con el indicador
`hayMas`. Sin `fecha`, la ruta paginada muestra reservas desde hoy,
incluidas las canceladas. Para consultar el histórico se indica una
fecha anterior a hoy.

## Ejemplo de reserva

`POST /api/mis-reservas` recibe el siguiente cuerpo. Sustituye la fecha
por una dentro del plazo y elige una pista y una hora disponibles.

```json
{
  "pistaId": 1,
  "fecha": "2026-11-19",
  "horaInicio": "10:00",
  "duracionHoras": 2
}
```

El servidor obtiene el socio de la sesión y devuelve `201 Created` con
la reserva. En la ruta de administración se añade `socioId` al cuerpo
para indicar el titular.

## Respuestas de error

| Código HTTP | Significado habitual |
| --- | --- |
| `400` | Datos incompletos, formato incorrecto o incumplimiento de una validación. |
| `401` | Credenciales o sesión no válidas. |
| `403` | El rol de la sesión carece del permiso necesario. |
| `404` | El recurso no se encuentra. |
| `409` | Conflicto con el estado actual, como una franja ocupada o el límite de reservas alcanzado. |
| `429` | Se ha superado un límite de solicitudes. |

Los errores de la aplicación incluyen `codigo` y `mensaje`. Un `202`
al solicitar un código indica que la API ha aceptado la petición.
La entrega del correo se comprueba en el buzón.