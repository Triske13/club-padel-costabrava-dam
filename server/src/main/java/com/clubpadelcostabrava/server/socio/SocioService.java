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

/** Aplica las reglas de negocio de socios y coordina los cambios de base de datos. */
@Service
public class SocioService {
    private final SocioRepository socios;
    private final CuentaRepository cuentas;
    private final PasswordEncoder passwordEncoder;
    private final SesionService sesiones;
    private final ReservaRepository reservas;

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

    /** Guarda ficha y cuenta en una misma transacción. */
    @Transactional
    public SocioRespuesta crear(CrearSocioPeticion datos) {
        CrearSocioPeticion limpio = validar(datos);
        int id = socios.insertarFicha(limpio);
        socios.insertarCuenta(id, limpio.correo());
        return socios.buscarPorId(id).orElseThrow();
    }

    /** Modifica la ficha, dejando el correo para su operación específica. */
    @Transactional
    public Optional<SocioRespuesta> editar(int id, EditarSocioPeticion datos) {
        EditarSocioPeticion limpio = validar(datos);
        int filas = socios.actualizarFicha(id, limpio);

        if (filas == 0) {
            return Optional.empty();
        }

        return socios.buscarPorId(id);
    }

    /** Cambia el correo y anula los códigos y sesiones anteriores de esa cuenta. */
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

    /** Inactiva o reactiva; al inactivar cancela reservas futuras y libera sus horas. */
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

    /** Quita espacios exteriores y comprueba presencia y longitud máxima. */
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

    /** Valida y normaliza los datos necesarios para crear un socio. */
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

    /** Valida los datos personales editables de un socio. */
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

    /** Acepta únicamente los dos estados definidos para los socios. */
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

    /** Compara la contraseña recibida con el hash del administrador autenticado. */
    private void comprobarContrasenaAdmin(int adminId, String contrasena) {
        boolean correcta = contrasena != null
                && cuentas.hashAdministrador(adminId)
                .map(hash -> passwordEncoder.matches(contrasena, hash))
                .orElse(false);

        if (!correcta) {
            throw new CredencialesInvalidasException();
        }
    }

    /** Convierte el correo a minúsculas y comprueba un formato básico. */
    private String normalizarCorreo(String valor) {
        String correo = obligatorio(valor, "correo", 255)
                .toLowerCase(Locale.ROOT);

        if (!correo.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException("El correo no tiene un formato válido.");
        }

        return correo;
    }

    /** Resultado usado para invalidar sesiones y enviar avisos fuera de la transacción. */
    public record ResultadoEstado(
            String estado,
            String correo,
            int cuentaId,
            boolean cambiado,
            List<ReservaRepository.ReservaCancelada> reservasCanceladas
    ) {}
}
