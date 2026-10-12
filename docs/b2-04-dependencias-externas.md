# B2-04 · Prueba de dependencias externas

**Fecha:** 12 de octubre de 2026

## Correo

- **Proveedor candidato:** cuenta Gmail gratuita dedicada al proyecto.
- **Prueba realizada:** conexión a `smtp.gmail.com:587` correcta. Una prueba desde Spring Boot terminó con `BUILD SUCCESS` y el correo llegó a la bandeja de entrada de la cuenta.
- La contraseña de aplicación se guarda en `server/config/application-local.properties`, archivo excluido de Git. La clase utilizada para la prueba se eliminó después; se conserva la dependencia de correo en el servidor.

## Alojamiento provisional

- **API:** Render ofrece un servicio web gratuito y permite desplegar aplicaciones Java mediante Docker. Su servicio gratuito se suspende tras 15 minutos sin tráfico y puede tardar aproximadamente un minuto en reactivarse. [Docker en Render](https://render.com/docs/docker) · [Límites del plan gratuito](https://render.com/docs/free)
- **Base de datos:** PostgreSQL gratuito de Render caduca a los 30 días y no ofrece copias de seguridad. Neon anuncia 1 GB gratuito por proyecto como posible alternativa; todavía no hemos probado la conexión con nuestra API. [Render PostgreSQL](https://render.com/docs/free) · [Plan gratuito de Neon](https://neon.com/blog/neon-free-plan-1-gb-per-project)
- **Limitación detectada:** Render gratuito bloquea las conexiones salientes a los puertos SMTP habituales. El envío con Gmail está comprobado **en local**, pero esa configuración no funcionaría allí sin cambiar el método de envío o el alojamiento. [Límites de Render](https://render.com/docs/free)

## Decisiones pendientes

- Elegir alojamiento definitivo y decidir entre un plan gratuito o de pago.
- Resolver el envío de correos desde la API alojada.
- Probar el despliegue y la conexión con PostgreSQL antes de la entrega.