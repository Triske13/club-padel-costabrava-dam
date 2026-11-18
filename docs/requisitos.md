# Club Pádel Costa Brava — requisitos consolidados


## 1. Marco académico obligatorio

- Proyecto individual de gestión de un club de pádel mediante un sistema cliente-servidor funcional.
- Dos clientes de tipos distintos: uno de usuarios y otro de escritorio para el personal. No habrá cliente web. La comunicación entre los clientes y el servidor será mediante API REST y JSON.
- Lógica de negocio y acceso seguro a los datos en el servidor; base de datos relacional con modelos conceptual y relacional; autenticación y control de roles ADMIN y USER.
- Repositorio accesible con un historial de commits representativo; backlog en una herramienta de gestión y captura del tablero.
- Despliegue funcional accesible para la comprobación.

## 2. Alcance y arquitectura acordados

- Nombre de trabajo: **Club Pádel Costa Brava**. Club de demostración en Roses, Girona, CP 17480, con **cuatro pistas** de iguales reglas y turnos.
- Cliente de socios: aplicación Android nativa en **Kotlin**, probada con emulador y teléfono Android real. No se desarrollará iOS.
- Cliente de administración: aplicación de escritorio para **Windows**, en **Java con JavaFX**.
- Servidor: **Java con Spring Boot**, API REST/JSON, autenticación, autorización, disponibilidad y reglas de reserva. Base de datos **PostgreSQL**.
- Ambos clientes se comunican con el servidor por HTTPS. No acceden directamente a la base de datos ni se comunican entre sí. El servidor vuelve a comprobar las reglas al guardar una operación.
- Código en el repositorio GitHub `club-padel-costabrava-dam`; trabajo organizado en GitHub Projects.
- **Fuera del sistema:** cuotas y pagos, registro de otros jugadores, decisión sobre participación de no socios, cliente web e iOS. La inscripción presencial del socio es un supuesto del caso práctico, sin gestión económica en la aplicación.

## 3. Socios, acceso y administración

- Los estados administrativos del socio son únicamente **ACTIVO** e **INACTIVO**. La activación de su cuenta Android es un dato distinto de ese estado. Una ficha de socio puede existir aunque todavía no tenga contraseña Android.
- Administración crea y edita manualmente la ficha del socio tras la inscripción presencial. Puede consultar su historial e inactivar o reactivar la ficha. No se borra físicamente la ficha como operación ordinaria. El socio puede consultar sus reservas y cambiar su propia contraseña. Nombre, apellidos, teléfono, dirección, población y correo son obligatorios.
- Un socio ACTIVO puede activar su cuenta Android introduciendo el correo que consta en su ficha. El sistema envía un código temporal de un solo uso para establecer su contraseña. La respuesta inicial no revela si el correo existe. Un socio INACTIVO no puede activar ni utilizar la cuenta.
- Socios activos con cuenta activada y el administrador pueden iniciar sesión y cambiar su contraseña indicando la actual. La recuperación usa un **código de seis cifras** enviado por correo. **El correo se emplea para establecer o recuperar contraseñas y para avisar de cancelaciones hechas por ADMIN o provocadas por la inactivación**. No se envían otros avisos.
- El código caduca a los **15 minutos**, permite **tres intentos fallidos**, es de un solo uso y sustituye al anterior de la misma cuenta. Se admite como máximo una solicitud por minuto y tres por hora por correo; hay además un límite de 30 por hora por IP. La respuesta de solicitud no revela si la cuenta existe y sigue siendo genérica cuando se supera el límite por correo.
- La contraseña tiene entre **10 y 64 caracteres**, sin combinación obligatoria de letras, números o símbolos; se permiten también espacios. No se recorta ni se exige cambio periódico. Se guarda mediante un hash seguro. El inicio de sesión admite cinco errores en quince minutos por correo y cincuenta por IP; al alcanzar el límite se pausa quince minutos. Una recuperación correcta borra la pausa del correo, pero no el límite general por IP.
- El servidor entrega un token de sesión aleatorio, conservado en la memoria de un único servidor durante **ocho horas**. El cierre de sesión, cambio o recuperación de contraseña e inactivación invalidan los tokens afectados. Un reinicio del servidor cierra todas las sesiones. El inicio de sesión responde igual si el correo no existe, la contraseña falla, la cuenta no está activada o el socio está inactivo.
- Solo ADMIN puede modificar el correo del socio, en una operación separada de la edición de la ficha, tras comprobar presencialmente su identidad y volver a introducir su propia contraseña. El nuevo correo debe ser único. El cambio invalida las sesiones y el código pendiente del socio; una cuenta ya activada conserva su contraseña y accede con el nuevo correo. No se exige verificación automática del nuevo correo en esta versión.
- Durante la instalación se crea de forma segura **una cuenta ADMIN inicial**, sin contraseña fija en el código. La aplicación no ofrece alta ni gestión de otras cuentas administrativas. El servidor distingue ADMIN y USER y comprueba los permisos en cada operación.
- Al inactivar un socio se invalidan sus sesiones y se elimina su código pendiente, aunque aún no haya caducado; se impiden reservas nuevas por ambos clientes y se cancelan sus reservas futuras. **Si se ha cancelado al menos una**, se intenta enviar un único correo que resume esas reservas y da como motivo el cambio de estado del socio; si no tenía reservas futuras, no se envía correo. Una reserva en curso no se cancela automáticamente; administración puede cancelarla con motivo. El socio inactivo no puede entrar en Android ni consultar sus reservas. La reactivación requiere decisión de administración y no restaura reservas canceladas. Si la cuenta Android ya estaba activada, conserva sus credenciales tras reactivarse; puede recuperarlas por correo si las olvidó.

### Permisos

| Acción | Socio activo | Socio inactivo | ADMIN |
| --- | --- | --- | --- |
| Activar cuenta e iniciar sesión en Android | Sí | No | No aplica |
| Consultar las reservas propias | Sí, con sesión | No | Sí, para recepción y gestión |
| Consultar la ficha de un socio | No | No | Sí |
| Editar datos de la ficha del socio | No | No | Sí |
| Cambiar el correo del socio, con nueva comprobación de contraseña ADMIN | No | No | Sí |
| Cambiar o recuperar contraseña propia | Sí | No obtiene acceso mientras esté inactivo | Sí |
| Consultar calendario y disponibilidad | Sí, con sesión | No | Sí |
| Crear una reserva para un socio activo | Solo para sí mismo | No | Sí, aunque el socio no haya activado Android |
| Cancelar una reserva | Las propias según RN-09 | No | Las futuras o en curso, con motivo |
| Crear o cancelar bloqueos manuales de pista | No | No | Sí |
| Inactivar o reactivar socios | No | No | Sí |

## 4. Horarios, festivos y mantenimiento

| Tipo de día | Horario habitual | Último inicio de 1 h | Último inicio de 2 h |
| --- | --- | --- | --- |
| Lunes a viernes | 08:00–23:00 | 22:00 | 21:00 |
| Sábado | 08:00–20:00 | 19:00 | 18:00 |
| Domingo o festivo | 08:00–16:00 | 15:00 | 14:00 |

- Un festivo prevalece sobre el día de la semana. Para la demostración se carga una **lista local de fechas festivas aplicables a Roses**. El servidor consulta esa lista al calcular disponibilidad: una fecha que no figure en ella sigue el horario de su día de la semana. No hay proveedor externo, importación automática, validación anual ni pantalla administrativa para modificar festivos. Los datos iniciales documentarán qué años están cubiertos; la lista se verificará para las fechas de la demostración.
- El mantenimiento diario se realiza fuera del horario comercial y no ocupa franjas de reserva.
- **No hay mantenimiento recurrente.** Administración crea bloqueos manuales de una o varias horas completas y consecutivas para una pista, tanto hoy como en una fecha futura, siempre dentro del horario de ese día y sobre turnos todavía libres y no iniciados. Puede cancelarlos antes de que empiecen: se borra el bloqueo y sus ocupaciones, sin conservar un registro de esa cancelación. No existe una regla de 14 días para crear o cancelar estos bloqueos.
- **No hay función de cierre extraordinario del club o de la pista que desplace reservas.** Los bloqueos manuales no cancelan reservas ya creadas. Administración puede cancelar individualmente una reserva futura o en curso con un motivo. La única cancelación automática de varias reservas ocurre al inactivar a su titular, según RN-11.

## 5. Reservas y calendario Android

- Una reserva identifica una pista, un socio titular, un inicio a la hora en punto y una duración elegida de **una o dos horas consecutivas**. Es una sola reserva, incluso cuando ocupa dos turnos horarios; debe caber entera en el horario de la fecha. El titular es el socio para quien se crea, incluso cuando administración la registra en recepción. No se registran los demás jugadores.
- Un socio ACTIVO puede tener como máximo **dos reservas futuras**. Una reserva de dos horas cuenta como **una** dentro de ese límite. Una reserva deja de contar como futura al empezar, como ya estaba acordado. No puede crear otra reserva cuyo período se solape con una propia. Sí puede tener dos reservas contiguas, incluso de dos horas cada una, porque no se solapan y no hay límite adicional por día. Solo se pueden reservar **14 fechas de calendario en total: hoy y los 13 días siguientes**; una reserva para hoy debe empezar en el futuro.
- Las reservas válidas se **confirman al crearse**; no hay aprobación posterior ni estados de revisión. Los estados funcionales son **CONFIRMADA** y **CANCELADA**. Una confirmada ocupa la pista durante todo su período; una cancelada la libera. El servidor impide que cualquiera de sus horas se solape con otra reserva o con un bloqueo, incluso ante solicitudes simultáneas.
- El socio puede cancelar la **reserva completa** hasta el final del día anterior a su inicio. Además dispone de **diez minutos desde la creación** para corregir cualquier reserva, tanto si se creó en Android como en recepción, siempre antes de que empiece. Para ejercer esta acción directamente necesita una cuenta Android activa; sin ella, recepción tramita la cancelación con las reglas de ADMIN. Android explica este límite al reservar para hoy. Administración puede cancelar una reserva futura o en curso con un **motivo visible para el socio**; se intenta enviarle un correo con una plantilla sencilla que indica pista, fecha, hora de inicio y fin, y motivo, incluso si el socio solicitó la cancelación en recepción. La cancelación pasa la reserva completa a CANCELADA y elimina todas sus filas de ocupación, incluso si ya había empezado. Las horas todavía no iniciadas vuelven a estar disponibles; las ya iniciadas no se vuelven a ofrecer porque su comienzo ha pasado. El historial de la reserva permanece en su propia fila. No hay cancelación parcial de una de sus horas. Las reservas terminadas no se cancelan.
- Android conserva una **vista mensual de calendario**. La marca de disponibilidad refleja las pistas y horas, no el cupo personal del socio. Un día dentro del plazo que todavía tiene turnos por empezar se marca como completo si todas las opciones de **una hora** restantes están ocupadas en las cuatro pistas; **sigue siendo consultable**, aunque no se puede reservar en él. Puede ser seleccionable aunque no tenga dos horas consecutivas disponibles. Para hoy solo cuentan turnos que aún no han empezado: si ya no queda ninguno, se indica «sin turnos futuros», no «completo» por ocupación. Fechas pasadas o fuera de la ventana no se pueden abrir y se distinguen de un día completo. Si el socio ya tiene dos reservas futuras, se le indica que ha alcanzado su límite y no se le permite crear otra, aunque el calendario muestre horas libres. Al abrir un día consultable, muestra las cuatro pistas y sus turnos como libres, reservados o bloqueados por mantenimiento; una hora físicamente libre que ya empezó figura como pasada. Permite elegir una o dos horas solo si todas las horas de esa opción están libres y aún no han empezado. No muestra la identidad del titular de otras reservas ni la descripción interna del bloqueo.
- Android permite consultar y cancelar reservas propias. Las cancelaciones administrativas se reflejan en los datos devueltos por el servidor y generan un intento de aviso por correo. La inactivación también se comunica por correo cuando provoca cancelaciones futuras, ya que el socio deja de poder entrar en la aplicación.

## 6. Funciones de los clientes

### Android para socios

- Activación por correo y código de un solo uso, inicio de sesión, cambio y recuperación de contraseña.
- Calendario mensual con días completos señalados y consulta diaria por pista y turno.
- Selección de inicio y duración de una o dos horas consecutivas; reserva, lista de reservas propias, cancelación completa permitida y mensajes claros cuando el servidor rechaza una operación por plazo, límite u ocupación. El inicio de sesión fallido muestra un mensaje genérico, incluso si el socio está inactivo.

### Windows para administración

- Inicio de sesión, cambio y recuperación de contraseña del ADMIN inicial.
- Alta, edición, consulta rápida, inactivación y reactivación de socios.
- Consulta de pistas, horarios, bloqueos manuales y disponibilidad.
- Creación presencial de reservas de una o dos horas para socios activos, consulta de reservas y cancelación completa y motivada de reservas futuras o en curso. La interfaz muestra si el intento de enviar el aviso de cancelación ha fallado.

## 7. Decisiones técnicas pendientes de validación

| Tema | Acordado | Pendiente |
| --- | --- | --- |
| Correo | Dirección gratuita para establecer y recuperar contraseñas y para avisar de cancelaciones hechas por ADMIN o causadas por la inactivación. | Elegir proveedor, probar la entrega real, remitente, límites y tratamiento de fallos. **Brevo es solo candidato.** |
| Festivos | Lista local cargada con datos de demostración, sin integración externa. | Seleccionar y verificar las fechas aplicables a Roses para las fechas cubiertas por la demostración. |
| Alojamiento | API y PostgreSQL accesibles para comprobar el sistema; desarrollo local. | Decidir plan gratuito o de pago tras una prueba. |
| API y seguridad | Contrato inicial B1-05 con rutas, datos, errores y token aleatorio en memoria durante ocho horas; contraseña de 10 a 64 caracteres, código de seis cifras durante quince minutos y límites de solicitudes e intentos. | Elegir y probar durante la implementación un codificador de contraseñas que admita íntegra la longitud permitida y configurar correctamente la identificación de IP tras el alojamiento. |
| Datos y despliegue | PostgreSQL, modelos conceptual y relacional, esquema versionado, campos obligatorios del socio y cuenta ADMIN inicial. | Migraciones, copias de seguridad y procedimiento seguro de alta inicial del ADMIN. |
| Correo de la ficha | Solo ADMIN puede cambiarlo, con comprobación presencial de identidad, nueva contraseña ADMIN, correo único e invalidación de sesiones y código pendientes del socio. | Probar el procedimiento con un correo real y comprobar la entrega del proveedor elegido. |

## 8. Reglas de negocio formalizadas — B1-03

La fecha y la hora se interpretan en **Europe/Madrid**. Una reserva «futura» aún no ha empezado; una «en curso» ya empezó y no terminó; una «terminada» llegó a su hora de fin. El servidor aplica las reglas al crear o cancelar, no se fía únicamente de lo mostrado por los clientes.

| ID | Regla verificable |
| --- | --- |
| RN-01 | Una reserva dura una o dos horas completas y consecutivas en una sola pista; comienza a la hora en punto y termina dentro del horario aplicable a la misma fecha. Cuenta como una reserva con independencia de su duración. |
| RN-02 | El horario es lunes a viernes 08:00–23:00, sábados 08:00–20:00 y domingos o festivos 08:00–16:00. Una fecha incluida en la lista local de festivos se trata como festiva aunque caiga entre semana. |
| RN-03 | La ventana contiene **14 fechas en total: hoy y los 13 días naturales siguientes**. Una reserva para hoy solo se crea antes de su hora de inicio. |
| RN-04 | Cada socio activo puede tener hasta dos reservas confirmadas cuyo período no haya empezado; una reserva de dos horas cuenta una vez. Las canceladas o iniciadas no cuentan. Ninguna reserva nueva del socio puede solaparse, ni parcialmente, con otra reserva propia confirmada. Se permiten dos reservas contiguas, incluso dos de dos horas, sin límite adicional por día. |
| RN-05 | Cada una de las horas que abarque una reserva debe estar libre en la misma pista: no puede solaparse con otra reserva confirmada ni con un bloqueo manual. El servidor comprueba todas las horas y guarda la reserva completa de forma consistente ante solicitudes simultáneas; no admite reservar solo la parte libre de una opción de dos horas. |
| RN-06 | Android y recepción aplican las mismas reglas. ADMIN puede reservar para un socio activo que todavía no haya activado Android; no puede hacerlo para uno inactivo. |
| RN-07 | Una reserva válida de una o dos horas queda CONFIRMADA al crearse como una sola operación, sin aprobación posterior. El titular es el socio para quien se hace, no necesariamente quien la introdujo en el sistema. |
| RN-08 | ADMIN solo crea bloqueos manuales en turnos futuros, no iniciados, completos, dentro del horario aplicable y libres de reservas o bloqueos. Puede abarcar varios turnos consecutivos de una misma pista y fecha. ADMIN puede cancelar un bloqueo antes de su inicio: se eliminan su fila y sus ocupaciones, sin crear reservas automáticamente ni conservar un registro de la cancelación. |
| RN-09 | Un socio activo puede cancelar directamente por Android una reserva propia completa en cualquier momento anterior a la fecha de inicio. Si empieza hoy, solo puede cancelarla durante los diez minutos siguientes a su creación, inclusive, y siempre antes de su inicio. El plazo de corrección se aplica con independencia de si la reserva se creó en Android o recepción; para ejercerlo directamente debe tener su cuenta Android activada. Sin ella, recepción tramita la cancelación como ADMIN. Fuera de esos casos solo administración puede cancelarla. No hay cancelación parcial de una reserva de dos horas. |
| RN-10 | ADMIN puede cancelar una reserva completa futura o en curso con un motivo obligatorio y visible para el socio, incluso si el socio lo solicita en recepción. Se registran el motivo, quién actuó y cuándo. La cancelación elimina todas las filas de ocupación de la reserva, tanto si es futura como si está en curso; los turnos todavía no iniciados pueden volver a reservarse y los ya iniciados no se ofrecen. El registro completo pasa a CANCELADA y conserva su historial. Tras guardar, se intenta enviar un correo al titular con plantilla sencilla: pista, fecha, hora de inicio y fin, y motivo. Si el envío falla, la reserva sigue CANCELADA, se registra el fallo y Windows informa a ADMIN. Una reserva terminada conserva su historial y no se cancela. |
| RN-11 | Al inactivar a un socio se invalidan sus sesiones, se elimina su código temporal pendiente y se cancelan todas sus reservas futuras con la inactivación registrada como motivo. Solo si se canceló alguna se intenta enviar **un correo** con la lista de reservas canceladas y el motivo genérico «cambio de estado del socio». Un fallo de envío no deshace la inactivación ni las cancelaciones; se registra y Windows informa a ADMIN. Las reservas ya iniciadas no se cancelan automáticamente; ADMIN puede cancelarlas según RN-10. La reactivación no restaura reservas canceladas. |
| RN-12 | Solo un socio activo puede activar su cuenta Android, iniciar sesión y reservar. La ficha de socio y su cuenta USER se crean juntas; la cuenta puede permanecer sin contraseña hasta que el socio la active. ADMIN puede reservar para ese socio si está activo. |
| RN-13 | El código de seis cifras para establecer o recuperar contraseña caduca a los quince minutos, permite tres fallos, es de un solo uso y tiene límites de solicitudes. No se revela en la respuesta inicial si un correo pertenece a una ficha. Fuera de esos códigos, el correo solo se utiliza para avisar de cancelaciones hechas por ADMIN o causadas por la inactivación. |
| RN-14 | El ADMIN inicial se crea durante la instalación sin contraseña fija en el código; no existe creación ni gestión de otras cuentas ADMIN dentro de la aplicación. El servidor separa permisos ADMIN y USER. |
| RN-15 | Android muestra la disponibilidad física de las pistas, separada del límite personal de dos reservas. Una fecha reservable con turnos todavía por empezar está completa cuando ninguna pista tiene siquiera una hora libre entre esos turnos; puede abrirse para consulta. Si hoy ya no quedan turnos futuros, se indica así y no como «completo» por ocupación. Puede haber días seleccionables sin períodos de dos horas consecutivas libres. Un socio que haya alcanzado su cupo ve las horas libres, pero no puede reservar. Las fechas fuera del plazo se distinguen de las completas. |
