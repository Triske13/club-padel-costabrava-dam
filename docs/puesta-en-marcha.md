# Puesta en marcha

Esta guía explica cómo ejecutar en local el servidor y los clientes de Club Pádel Costa Brava. Las órdenes están escritas para PowerShell en Windows y las rutas parten de la raíz del repositorio.

## 1. Requisitos

- **JDK 21** para el servidor y el cliente Windows. Para generar el paquete portátil de Windows se necesita un JDK completo que incluya `jlink` y la carpeta `jmods`.
- **PostgreSQL** en ejecución y un usuario con permisos para crear tablas en la base de datos del proyecto.
- **Android Studio**, el SDK de Android correspondiente a `compileSdk = 37` y un emulador con Android API 26 o superior.
- **Una cuenta SMTP** para probar los códigos de acceso y los avisos por correo. El ejemplo usa Gmail con una contraseña de aplicación.

El repositorio incluye los ejecutables de Maven Wrapper y Gradle Wrapper.

## 2. Configurar el servidor en local

Crea una base de datos vacía en PostgreSQL, por ejemplo `club_padel_dev`. Crea después `server/config/application-local.properties` con este contenido y sustituye los valores de ejemplo por los de tu entorno.

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/club_padel_dev
spring.datasource.username=usuario_local
spring.datasource.password=contrasena_local

spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=tu_cuenta@gmail.com
spring.mail.password=contrasena_de_aplicacion
spring.mail.properties[mail.smtp.auth]=true
spring.mail.properties[mail.smtp.starttls.enable]=true
spring.mail.properties[mail.smtp.starttls.required]=true
spring.mail.properties[mail.smtp.connectiontimeout]=5000
spring.mail.properties[mail.smtp.timeout]=5000
spring.mail.properties[mail.smtp.writetimeout]=5000
```

Este archivo está excluido de Git. Si utilizas otro proveedor de correo, adapta el servidor, el puerto y los ajustes SMTP a su configuración.

En el primer arranque sobre una base de datos nueva, el servidor crea la cuenta de administración a partir de `APP_ADMIN_EMAIL` y `APP_ADMIN_PASSWORD`. Introduce la contraseña al ejecutar el siguiente bloque. Debe tener entre 10 y 64 caracteres.

```powershell
$env:APP_ADMIN_EMAIL = "admin@example.invalid"
$claveAdmin = Read-Host "Contraseña del administrador local" -AsSecureString
$env:APP_ADMIN_PASSWORD = [System.Net.NetworkCredential]::new("", $claveAdmin).Password
Remove-Variable claveAdmin
```

El correo de ejemplo sirve para iniciar sesión. Si quieres probar la recuperación de la cuenta de administración, usa una dirección a la que tengas acceso. Una vez creada la cuenta, cambiar estas variables no modifica sus credenciales.

## 3. Arrancar el servidor

Con PostgreSQL en ejecución, abre la carpeta `server` en la misma terminal donde has definido las variables y ejecuta:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

Flyway aplica las migraciones al arrancar. En una instalación nueva crea el esquema y carga las cuatro pistas y los festivos iniciales. El archivo `database/esquema.sql` sirve como referencia del esquema.

Abre [el diagnóstico local](http://localhost:8080/api/diagnostico) en el navegador. Si el servidor está en marcha y accede a la base de datos inicial, la respuesta incluirá `"estado":"OK"` y `"pistas":4`.

## 4. Ejecutar los clientes en local

### Windows

Abre otra terminal en la carpeta `desktop` y ejecuta:

```powershell
.\mvnw.cmd javafx:run
```

El cliente utiliza `http://localhost:8080` por defecto. Inicia sesión con la cuenta de administración creada en el primer arranque. Desde **Socios → Nuevo socio** puedes dar de alta una ficha de prueba para usar el cliente Android.

### Android

Abre la carpeta `android` como proyecto en Android Studio, sincroniza Gradle y ejecuta la aplicación sobre un emulador. El cliente usa `http://10.0.2.2:8080` por defecto para acceder al servidor que se ejecuta en el ordenador.

En la pantalla de acceso, pulsa **Activar cuenta o recuperar contraseña** e introduce el correo de la ficha creada desde Windows. Usa el código recibido para establecer la contraseña e iniciar sesión.

## 5. Conectar los clientes a otra URL

Para compilar e instalar una versión de prueba de Android que apunte a Railway, abre el emulador y ejecuta desde `android`:

```powershell
.\gradlew.bat "-PapiBaseUrl=https://padel-spring-production.up.railway.app" :app:installDebug
```

En Windows, la URL se puede indicar con la propiedad de Java `padel.api.baseUrl`. Si ya has generado el entorno portátil con `.\mvnw.cmd clean javafx:jlink`, puedes probarlo desde `desktop` con:

```powershell
.\target\app\bin\java.exe `
    "-Dpadel.api.baseUrl=https://padel-spring-production.up.railway.app" `
    -m "com.clubpadelcostabrava.desktop/com.clubpadelcostabrava.desktop.DesktopApplication"
```

La API local y la de Railway utilizan bases de datos distintas. Inicia sesión con una cuenta del entorno al que se conecta el cliente.

## 6. Ejecutar las pruebas del servidor

Desde la carpeta `server`, ejecuta:

```powershell
.\mvnw.cmd test
```