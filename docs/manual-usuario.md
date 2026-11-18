# Manual de usuario

El cliente Android está destinado a los socios y el cliente Windows al
personal de recepción. Los paquetes de entrega ya están configurados
para conectarse a Railway. Para ejecutar el código fuente en local,
consulta la [guía de puesta en marcha](puesta-en-marcha.md).

## Android · Socios

1. Instala `Club-Padel-Costa-Brava-Android.apk` y abre la aplicación.
2. En **Acceso de socio**, introduce tu correo y contraseña. Si aún no
   tienes contraseña o la has olvidado, pulsa **Activar cuenta o recuperar
   contraseña**, solicita un código con el correo de tu ficha e introduce
   el código recibido y la nueva contraseña.
3. En el calendario, selecciona un día y una pista. Elige la hora y la
   duración entre las opciones disponibles. Puedes reservar para hoy y
   los trece días siguientes, con un máximo de dos reservas futuras confirmadas.
4. En **Menú → Mis reservas**, comprueba la reserva. Si aparece
   **Cancelar reserva**, puedes cancelar y confirmar la operación.
   Si la reserva es para hoy, tienes diez minutos desde que la creaste
   y debes cancelarla antes de que empiece. Para una fecha futura,
   puedes cancelarla hasta el día anterior.
5. En **Menú → Mi cuenta**, puedes cambiar la contraseña o cerrar sesión.

## Windows · Recepción

Descomprime `Club-Padel-Costa-Brava-Windows-portable.zip` y abre
`Club-Padel-Costa-Brava.exe` desde la carpeta extraída. El paquete incluye
el entorno Java. Inicia sesión con el correo y la contraseña de administración.

### Socios

Ve a **Socios → Nuevo socio** para dar de alta una ficha. Para modificarla,
selecciona el socio y pulsa **Editar seleccionado**. Usa **Cambiar correo**
para actualizar su dirección y confirma con tu contraseña de administrador.
Con **Cambiar estado** puedes activar o inactivar la ficha. Si la inactivación
afecta a reservas futuras, se cancelan y se intenta enviar un correo de resumen.

### Reservas

En **Reservas → Nueva reserva**, selecciona el socio, la fecha, la pista,
la hora y la duración. Pulsa **Crear** y comprueba que la reserva aparece
en Actuales.

**Actuales** muestra las reservas de hoy y de fechas futuras, incluidas las
canceladas. En **Histórico**, selecciona una fecha anterior a hoy para
consultar las reservas de ese día. Usa **Anterior** y **Siguiente** para
recorrer las páginas.

Para cancelar una reserva futura o en curso, selecciónala en **Actuales**,
introduce el motivo, pulsa **Cancelar reserva** y confirma. Las franjas
se liberan y la fila continúa visible con el estado **CANCELADA**. Si falla
el envío del correo de aviso, la aplicación muestra el resultado.

### Agenda y bloqueos

En **Agenda**, selecciona una fecha y pulsa **Consultar**. Abre la pestaña
de una pista y elige un turno libre para crear un bloqueo. Indica la
duración y pulsa **Crear bloqueo**.

Para retirar un bloqueo antes de que empiece, selecciónalo en
**Bloqueos de la fecha** y pulsa **Retirar bloqueo seleccionado**.

### Cuenta de administración

Usa **Mi cuenta** para cambiar tu contraseña o **Salir** para cerrar sesión.
Si has olvidado la contraseña y tienes acceso al buzón del administrador,
usa **He olvidado mi contraseña** en la pantalla de acceso.