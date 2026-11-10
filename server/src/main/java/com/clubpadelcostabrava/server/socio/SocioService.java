package com.clubpadelcostabrava.server.socio;

import com.clubpadelcostabrava.server.api.CredencialesInvalidasException;
import com.clubpadelcostabrava.server.auth.SesionService;
import com.clubpadelcostabrava.server.cuenta.CuentaRepository;
import com.clubpadelcostabrava.server.reserva.ReservaRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Aplica las reglas de gestión de socios y coordina los cambios
 * de ficha, cuenta, reservas y sesiones.
 */
@Service
public class SocioService {
    private final SocioRepository socios;
    private final CuentaRepository cuentas;
    private final PasswordEncoder passwordEncoder;
    private final SesionService sesiones;
    private final ReservaRepository reservas;

    /**
     * Recibe los componentes necesarios para gestionar socios y sus cuentas.
     *
     * @param socios          acceso a fichas de socios
     * @param cuentas         acceso a cuentas y códigos pendientes
     * @param passwordEncoder comprobación de la contraseña de ADMIN
     * @param sesiones        gestión de sesiones activas
     * @param reservas        acceso a las reservas del socio
     */
    public SocioService(
            SocioRepository socios,
            CuentaRepository cuentas,
            PasswordEncoder passwordEncoder,
            SesionService sesiones,
            ReservaRepository reservas
    ) {
        this.socios = socios;
        this.cuentas = cuentas;
        this.passwordEncoder = passwordEncoder;
        this.sesiones = sesiones;
        this.reservas = reservas;
    }

    /**
     * Crea la ficha del socio y su cuenta asociada en una misma transacción.
     *
     * @param datos datos personales y correo del nuevo socio
     * @return ficha del socio creado
     * @throws IllegalArgumentException si faltan datos o no son válidos
     */
    @Transactional
    public SocioRespuesta crear(CrearSocioPeticion datos) {
        CrearSocioPeticion limpio = validar(datos);
        int id = socios.insertarFicha(limpio);
        socios.insertarCuenta(id, limpio.correo());
        return socios.buscarPorId(id).orElseThrow();
    }

    /**
     * Modifica los datos personales de un socio sin cambiar su correo.
     *
     * @param id    identificador del socio
     * @param datos nuevos datos personales
     * @return ficha actualizada, o un Optional vacío si el socio no existe
     * @throws IllegalArgumentException si los datos no son válidos
     */
    @Transactional
    public Optional<SocioRespuesta> editar(int id, EditarSocioPeticion datos) {
        EditarSocioPeticion limpio = validar(datos);
        int filas = socios.actualizarFicha(id, limpio);

        if (filas == 0) {
            return Optional.empty();
        }

        return socios.buscarPorId(id);
    }

    /**
     * Cambia el correo de un socio tras comprobar la contraseña de ADMIN.
     * Elimina el código temporal pendiente e invalida las sesiones de la cuenta.
     *
     * @param socioId identificador del socio
     * @param adminId identificador de la cuenta administradora
     * @param datos   nuevo correo y contraseña de confirmación
     * @return ficha actualizada, o un Optional vacío si el socio no existe
     * @throws IllegalArgumentException       si los datos o el nuevo correo no son válidos
     * @throws CredencialesInvalidasException si la contraseña de ADMIN es incorrecta
     */
    @Transactional
    public Optional<SocioRespuesta> cambiarCorreo(
            int socioId, int adminId, CambiarCorreoPeticion datos
    ) {
        if (datos == null) {
            throw new IllegalArgumentException("Faltan los datos del cambio de correo.");
        }

        String nuevoCorreo = normalizarCorreo(datos.nuevoCorreo());
        Optional<SocioRespuesta> actual = socios.buscarPorId(socioId);
        if (actual.isEmpty()) {
            return Optional.empty();
        }

        comprobarContrasenaAdmin(adminId, datos.contrasenaAdmin());

        if (actual.get().correo().equalsIgnoreCase(nuevoCorreo)) {
            throw new IllegalArgumentException("El nuevo correo coincide con el actual.");
        }

        int cuentaId = socios.actualizarCorreo(socioId, nuevoCorreo).orElseThrow();
        cuentas.eliminarCodigoPendiente(cuentaId);
        sesiones.invalidarCuenta(cuentaId);

        return socios.buscarPorId(socioId);
    }

    /**
     * Activa o inactiva un socio. Al inactivarlo, cancela sus reservas
     * futuras y libera sus horas dentro de la misma transacción.
     * Repetir el estado actual no provoca nuevas cancelaciones.
     *
     * @param socioId identificador del socio
     * @param adminId identificador de la cuenta administradora
     * @param datos   estado solicitado
     * @return resultado del cambio, o un Optional vacío si el socio no existe
     * @throws IllegalArgumentException si el estado no es válido
     */
    @Transactional
    public Optional<ResultadoEstado> cambiarEstado(
            int socioId, int adminId, CambiarEstadoPeticion datos
    ) {
        String nuevoEstado = validarEstado(datos);
        Optional<SocioRespuesta> encontrado = socios.buscarPorIdParaCambio(socioId);

        if (encontrado.isEmpty()) {
            return Optional.empty();
        }

        SocioRespuesta socio = encontrado.get();
        int cuentaId = cuentas.idUsuarioPorSocio(socioId).orElseThrow();

        // Repetir el mismo estado no provoca nuevas cancelaciones.
        if (socio.estado().equals(nuevoEstado)) {
            return Optional.of(new ResultadoEstado(
                    nuevoEstado, socio.correo(), cuentaId, false, List.of()
            ));
        }

        socios.actualizarEstado(socioId, nuevoEstado);

        List<ReservaRepository.ReservaCancelada> canceladas = List.of();
        if (nuevoEstado.equals("INACTIVO")) {
            canceladas = reservas.cancelarFuturasDeSocio(socioId, adminId);
            cuentas.eliminarCodigoPendiente(cuentaId);
        }

        return Optional.of(new ResultadoEstado(
                nuevoEstado, socio.correo(), cuentaId, true, canceladas
        ));
    }

    /**
     * Quita los espacios exteriores y valida un campo obligatorio.
     *
     * @param valor  texto recibido
     * @param campo  nombre del campo usado en el mensaje de error
     * @param maximo número máximo de caracteres Unicode permitidos
     * @return texto limpio
     * @throws IllegalArgumentException si falta, está vacío o supera el máximo
     */
    private String obligatorio(String valor, String campo, int maximo) {
        if (valor == null) {
            throw new IllegalArgumentException("El campo " + campo + " es obligatorio.");
        }

        String limpio = valor.strip();
        if (limpio.isEmpty()) {
            throw new IllegalArgumentException("El campo " + campo + " es obligatorio.");
        }
        if (limpio.codePointCount(0, limpio.length()) > maximo) {
            throw new IllegalArgumentException(
                    "El campo " + campo + " supera " + maximo + " caracteres."
            );
        }

        return limpio;
    }

    /**
     * Valida y normaliza los datos necesarios para crear un socio.
     *
     * @param datos datos recibidos para el alta
     * @return petición con los textos limpios y el correo normalizado
     * @throws IllegalArgumentException si falta algún dato o no es válido
     */
    private CrearSocioPeticion validar(CrearSocioPeticion datos) {
        if (datos == null) {
            throw new IllegalArgumentException("Faltan los datos del socio.");
        }

        String correo = normalizarCorreo(datos.correo());

        return new CrearSocioPeticion(
                obligatorio(datos.nombre(), "nombre", 80),
                obligatorio(datos.apellidos(), "apellidos", 120),
                obligatorio(datos.telefono(), "teléfono", 25),
                obligatorio(datos.direccion(), "dirección", 200),
                obligatorio(datos.poblacion(), "población", 100),
                correo
        );
    }

    /**
     * Valida los datos personales que pueden modificarse sin cambiar el correo.
     *
     * @param datos datos recibidos para la edición
     * @return petición con los textos limpios
     * @throws IllegalArgumentException si falta algún dato o no es válido
     */
    private EditarSocioPeticion validar(EditarSocioPeticion datos) {
        if (datos == null) {
            throw new IllegalArgumentException("Faltan los datos del socio.");
        }

        return new EditarSocioPeticion(
                obligatorio(datos.nombre(), "nombre", 80),
                obligatorio(datos.apellidos(), "apellidos", 120),
                obligatorio(datos.telefono(), "teléfono", 25),
                obligatorio(datos.direccion(), "dirección", 200),
                obligatorio(datos.poblacion(), "población", 100)
        );
    }

    /**
     * Normaliza el estado solicitado y acepta solo ACTIVO o INACTIVO.
     *
     * @param datos petición de cambio de estado
     * @return estado válido en mayúsculas
     * @throws IllegalArgumentException si falta o tiene otro valor
     */
    private String validarEstado(CambiarEstadoPeticion datos) {
        if (datos == null || datos.estado() == null) {
            throw new IllegalArgumentException("El estado es obligatorio.");
        }

        String estado = datos.estado().strip().toUpperCase(Locale.ROOT);
        if (!estado.equals("ACTIVO") && !estado.equals("INACTIVO")) {
            throw new IllegalArgumentException("El estado debe ser ACTIVO o INACTIVO.");
        }

        return estado;
    }

    /**
     * Comprueba la contraseña de la cuenta administradora autenticada.
     *
     * @param adminId    identificador de la cuenta administradora
     * @param contrasena contraseña introducida para confirmar la operación
     * @throws CredencialesInvalidasException si no coincide con su hash
     */
    private void comprobarContrasenaAdmin(int adminId, String contrasena) {
        boolean correcta = contrasena != null
                && cuentas.hashAdministrador(adminId)
                .map(hash -> passwordEncoder.matches(contrasena, hash))
                .orElse(false);

        if (!correcta) {
            throw new CredencialesInvalidasException();
        }
    }

    /**
     * Quita los espacios exteriores, convierte el correo a minúsculas
     * y comprueba su longitud y un formato básico.
     *
     * @param valor correo recibido
     * @return correo normalizado
     * @throws IllegalArgumentException si falta o no tiene un formato válido
     */
    private String normalizarCorreo(String valor) {
        String correo = obligatorio(valor, "correo", 255)
                .toLowerCase(Locale.ROOT);

        if (!correo.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException("El correo no tiene un formato válido.");
        }

        return correo;
    }

    /**
     * Datos que necesita el controlador tras cambiar el estado del socio.
     *
     * @param estado             estado resultante del socio
     * @param correo             correo al que puede enviarse el aviso
     * @param cuentaId           identificador de la cuenta asociada
     * @param cambiado           indica si el estado realmente cambió
     * @param reservasCanceladas reservas canceladas al inactivar al socio
     */
    public record ResultadoEstado(
            String estado,
            String correo,
            int cuentaId,
            boolean cambiado,
            List<ReservaRepository.ReservaCancelada> reservasCanceladas
    ) {
    }
}
