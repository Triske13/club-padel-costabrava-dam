package com.clubpadelcostabrava.server.bloqueo;

import com.clubpadelcostabrava.server.calendario.HorarioDia;
import com.clubpadelcostabrava.server.calendario.HorarioService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.NoSuchElementException;

/** Aplica las reglas de negocio para consultar, crear y retirar bloqueos de pista. */
@Service
public class BloqueoService {

    private final BloqueoRepository bloqueos;
    private final HorarioService horarios;

    public BloqueoService(BloqueoRepository bloqueos, HorarioService horarios) {
        this.bloqueos = bloqueos;
        this.horarios = horarios;
    }

    /** Devuelve los bloqueos de una fecha; la fecha es obligatoria. */
    public List<BloqueoRespuesta> listarPorFecha(LocalDate fecha) {
        if (fecha == null) {
            throw new IllegalArgumentException("Indica una fecha.");
        }

        return bloqueos.listarPorFecha(fecha);
    }

    /**
     * Valida el bloqueo y ocupa cada una de sus horas en una sola transacción.
     * Si alguna hora ya está ocupada, se deshace toda la creación.
     */
    @Transactional
    public BloqueoRespuesta crear(CrearBloqueoPeticion datos, int adminId) {

        if (datos == null
                || datos.pistaId() == null
                || datos.fecha() == null
                || datos.horaInicio() == null
                || datos.numeroHoras() == null) {
            throw new IllegalArgumentException("Faltan datos obligatorios del bloqueo");
        }

        if (datos.pistaId() <= 0 || datos.numeroHoras() <= 0) {
            throw new IllegalArgumentException("Los datos no son válidos");
        }

        HorarioDia horario = horarios.calcular(datos.fecha());

        if (!horario.cabe(datos.horaInicio(), datos.numeroHoras())) {
            throw new IllegalArgumentException("El bloqueo debe ocupar horas completas dentro del horario de apertura");
        }

        LocalDateTime inicio = LocalDateTime.of(datos.fecha(), datos.horaInicio());
        LocalDateTime ahora = LocalDateTime.now(ZoneId.of("Europe/Madrid"));

        if (!inicio.isAfter(ahora)) {
            throw new IllegalArgumentException("El bloqueo debe comenzar en el futuro.");
        }

        if (!bloqueos.existePista(datos.pistaId())) {
            throw new NoSuchElementException("Pista no encontrada");
        }

        String descripcion = datos.descripcion();

        if (descripcion != null
                && (descripcion.isBlank() || descripcion.length() > 200)) {
            throw new IllegalArgumentException("La descripción no puede estar en blanco ni superar los 200 caracteres.");
        }

        int bloqueoId = bloqueos.insertarBloqueo(datos, adminId);

        for (int indice = 0; indice < datos.numeroHoras(); indice++) {
            LocalTime hora = datos.horaInicio().plusHours(indice);

            bloqueos.insertarOcupacion(
                    datos.pistaId(),
                    datos.fecha(),
                    hora,
                    bloqueoId
            );
        }

        return new BloqueoRespuesta(
                bloqueoId,
                datos.pistaId(),
                datos.fecha(),
                datos.horaInicio(),
                datos.numeroHoras(),
                datos.descripcion()
        );
    }

    /**
     * Retira un bloqueo que aún no ha comenzado.
     * La base de datos elimina también sus ocupaciones asociadas.
     */
    @Transactional
    public void retirar(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("El ID del bloqueo no es válido.");
        }

        LocalDateTime inicio = bloqueos.buscarInicioPorId(id)
                .orElseThrow(() ->
                        new NoSuchElementException("Bloqueo no encontrado.")
                );

        LocalDateTime ahora = LocalDateTime.now(ZoneId.of("Europe/Madrid"));

        if (!inicio.isAfter(ahora)) {
            throw new IllegalStateException(
                    "No se puede retirar un bloqueo que ya ha comenzado."
            );
        }

        int filasEliminadas = bloqueos.eliminarPorId(id);

        if (filasEliminadas == 0) {
            throw new NoSuchElementException("Bloqueo no encontrado.");
        }
    }
}