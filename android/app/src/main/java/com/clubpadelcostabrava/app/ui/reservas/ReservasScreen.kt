package com.clubpadelcostabrava.app.ui.reservas

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.clubpadelcostabrava.app.data.ApiCliente
import com.clubpadelcostabrava.app.data.ApiException
import com.clubpadelcostabrava.app.data.DisponibilidadDia
import com.clubpadelcostabrava.app.data.Reserva
import com.clubpadelcostabrava.app.data.Sesion
import kotlinx.coroutines.launch
import org.json.JSONException
import java.io.IOException
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeParseException

@Composable
fun ReservasScreen(sesion: Sesion, api: ApiCliente, onAbrirCuenta: () -> Unit, onSesionInvalida: () -> Unit) {
    var fechaTexto by remember {
        mutableStateOf(
            LocalDate.now(ZoneId.of("Europe/Madrid")).plusDays(1).toString()
        )
    }
    var dia by remember { mutableStateOf<DisponibilidadDia?>(null) }
    var cargando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    var pistaSeleccionadaId by remember { mutableStateOf<Int?>(null) }
    var reservando by remember { mutableStateOf(false) }
    var reservaCreada by remember { mutableStateOf<Reserva?>(null) }
    var misReservas by remember { mutableStateOf<List<Reserva>>(emptyList()) }
    var cargandoReservas by remember { mutableStateOf(false) }
    var reservasConsultadas by remember { mutableStateOf(false) }
    var cancelandoId by remember { mutableStateOf<Int?>(null) }
    var reservaPendiente by remember { mutableStateOf<Reserva?>(null) }
    var avisoCancelacion by remember { mutableStateOf<String?>(null) }

    fun mostrarError(e: IOException, mensajePorDefecto: String) {
        if (e is ApiException && e.estado == 401) {
            onSesionInvalida()
        } else {
            error = e.message ?: mensajePorDefecto
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Reservar pista", style = MaterialTheme.typography.headlineSmall)

        TextButton(onClick = onAbrirCuenta) {
            Text("Mi cuenta")
        }

        OutlinedTextField(
            value = fechaTexto,
            onValueChange = { nuevaFecha ->
                fechaTexto = nuevaFecha
                dia = null
                pistaSeleccionadaId = null
                reservaCreada = null
            },
            label = { Text("Fecha (AAAA-MM-DD)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                scope.launch {
                    cargando = true
                    reservaCreada = null
                    pistaSeleccionadaId = null
                    error = null
                    dia = null

                    try {
                        val fecha = LocalDate.parse(fechaTexto.trim())
                        val disponibilidad = api.consultarDisponibilidad(sesion.token, fecha)
                        dia = disponibilidad
                        pistaSeleccionadaId = disponibilidad.pistas.firstOrNull()?.id
                    } catch (e: DateTimeParseException) {
                        error = "Escribe la fecha como AAAA-MM-DD."
                    } catch (e: IOException) {
                        mostrarError(e, "No se pudo consultar la disponibilidad.")
                    } catch (e: JSONException) {
                        error = "La respuesta del servidor no es válida."
                    } finally {
                        cargando = false
                    }
                }
            },
            enabled = !cargando && !reservando && cancelandoId == null && fechaTexto.isNotBlank()
        ) {
            Text(if (cargando) "Consultando..." else "Ver disponibilidad")
        }

        error?.let { mensaje ->
            Text(mensaje, color = MaterialTheme.colorScheme.error)
        }

        avisoCancelacion?.let { mensaje ->
            Text(mensaje, color = MaterialTheme.colorScheme.primary)
        }

        reservaCreada?.let { reserva ->
            Text(
                "Reserva ${reserva.id} confirmada: ${reserva.pistaNombre}, " +
                        "${reserva.fecha} de ${reserva.horaInicio} a ${reserva.horaFin}",
                color = MaterialTheme.colorScheme.primary
            )
        }

        Button(
            onClick = {
                scope.launch {
                    cargandoReservas = true
                    error = null

                    try {
                        misReservas = api.listarMisReservas(sesion.token)
                        reservasConsultadas = true
                    } catch (e: IOException) {
                        mostrarError(e, "No se pudieron cargar las reservas.")
                    } catch (e: JSONException) {
                        error = "La respuesta del servidor no es válida."
                    } finally {
                        cargandoReservas = false
                    }
                }
            },
            enabled = !cargandoReservas && !reservando && cancelandoId == null
        ) {
            Text(if (cargandoReservas) "Cargando..." else "Mis reservas")
        }

        if (reservasConsultadas) {
            Text("Reservas encontradas: ${misReservas.size}")

            if (misReservas.isEmpty()) {
                Text("Todavía no tienes reservas.")
            }

            misReservas.forEach { reserva ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "Reserva ${reserva.id} · ${reserva.estado}",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text("${reserva.pistaNombre} · ${reserva.fecha}")
                        Text(
                            "${reserva.horaInicio}–${reserva.horaFin} " +
                                    "(${reserva.duracionHoras} h)"
                        )

                        if (reserva.estado == "CANCELADA") {
                            reserva.canceladaPor?.let { autor ->
                                Text("Cancelada por: $autor")
                            }
                            reserva.motivoCancelacion?.let { motivo ->
                                Text("Motivo: $motivo")
                            }
                        }

                        if (reserva.puedeCancelar) {
                            Button(
                                onClick = { reservaPendiente = reserva },
                                enabled = cancelandoId == null
                            ) {
                                Text("Cancelar reserva")
                            }
                        }
                    }
                }
            }
        }

        reservaPendiente?.let { pendiente ->
            AlertDialog(
                onDismissRequest = { reservaPendiente = null },
                title = { Text("Cancelar reserva") },
                text = {
                    Text(
                        "¿Cancelar completamente la reserva ${pendiente.id} " +
                                "de ${pendiente.fecha}, " +
                                "${pendiente.horaInicio}–${pendiente.horaFin}?"
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        reservaPendiente = null
                        cancelandoId = pendiente.id
                        error = null
                        avisoCancelacion = null

                        scope.launch {
                            try {
                                val cancelada = api.cancelarMiReserva(
                                    sesion.token,
                                    pendiente.id
                                )
                                misReservas = misReservas.map { actual ->
                                    if (actual.id == cancelada.id) cancelada else actual
                                }
                                avisoCancelacion =
                                    "Reserva ${cancelada.id} cancelada."
                                if (reservaCreada?.id == cancelada.id) {
                                    reservaCreada = null
                                }
                                dia = null
                                pistaSeleccionadaId = null
                            } catch (e: IOException) {
                                mostrarError(e, "No se pudo cancelar la reserva.")
                            } catch (e: JSONException) {
                                error = "La respuesta del servidor no es válida."
                            } finally {
                                cancelandoId = null
                            }
                        }
                    }) {
                        Text("Sí, cancelar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { reservaPendiente = null }) {
                        Text("Volver")
                    }
                }
            )
        }

        dia?.let { disponibilidad ->
            Text(
                "Horario: ${disponibilidad.horaApertura}–${disponibilidad.horaCierre}"
            )
            if (disponibilidad.fecha ==
                LocalDate.now(ZoneId.of("Europe/Madrid")).toString()
            ) {
                Text(
                    "Si reservas para hoy, podrás cancelarla durante los " +
                            "10 minutos posteriores a su creación y siempre antes del inicio."
                )
            }
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                disponibilidad.pistas.forEach { pista ->
                    FilterChip(
                        selected = pista.id == pistaSeleccionadaId,
                        onClick = { pistaSeleccionadaId = pista.id },
                        label = { Text(pista.nombre) }
                    )
                }
            }

            val pistaElegida = disponibilidad.pistas
                .firstOrNull { it.id == pistaSeleccionadaId }

            val franjasLibres = pistaElegida?.franjas
                ?.filter { it.duracionesDisponibles.isNotEmpty() }
                .orEmpty()

            if (franjasLibres.isEmpty()) {
                Text("No quedan horas reservables en esta pista.")
            }

            franjasLibres.forEach { franja ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(franja.horaInicio)

                    franja.duracionesDisponibles.forEach { duracion ->
                        Button(
                            onClick = {
                                pistaElegida?.let { pista ->
                                    if (!reservando) {
                                        reservando = true

                                        scope.launch {
                                            error = null
                                            reservaCreada = null

                                            try {
                                                val creada = api.crearReserva(
                                                    token = sesion.token,
                                                    pistaId = pista.id,
                                                    fecha = LocalDate.parse(disponibilidad.fecha),
                                                    horaInicio = franja.horaInicio,
                                                    duracionHoras = duracion
                                                )
                                                reservaCreada = creada
                                                reservasConsultadas = false
                                                dia = null
                                                pistaSeleccionadaId = null
                                            } catch (e: IOException) {
                                                mostrarError(e, "No se pudo crear la reserva.")
                                                dia = null
                                            } catch (e: JSONException) {
                                                error = "La respuesta del servidor no es válida."
                                                dia = null
                                            } finally {
                                                reservando = false
                                            }
                                        }
                                    }
                                }
                            },
                            enabled = !cargando && !reservando && cancelandoId == null
                        ) {
                            Text("$duracion h")
                        }
                    }
                }
            }
        }
    }
}