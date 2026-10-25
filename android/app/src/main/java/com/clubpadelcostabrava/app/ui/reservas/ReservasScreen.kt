package com.clubpadelcostabrava.app.ui.reservas

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.clubpadelcostabrava.app.data.ApiCliente
import com.clubpadelcostabrava.app.data.DisponibilidadDia
import com.clubpadelcostabrava.app.data.Sesion
import java.time.LocalDate
import java.time.ZoneId
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.rememberCoroutineScope
import java.io.IOException
import java.time.format.DateTimeParseException
import kotlinx.coroutines.launch
import org.json.JSONException
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.FilterChip
import com.clubpadelcostabrava.app.data.Reserva
import androidx.compose.ui.Alignment
import androidx.compose.material3.Card

@Composable
fun ReservasScreen(sesion: Sesion, api: ApiCliente) {
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Reservar pista", style = MaterialTheme.typography.headlineSmall)

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
                        error = e.message ?: "No se pudo consultar la disponibilidad."
                    } catch (e: JSONException) {
                        error = "La respuesta del servidor no es válida."
                    } finally {
                        cargando = false
                    }
                }
            },
            enabled = !cargando && !reservando && fechaTexto.isNotBlank()
        ) {
            Text(if (cargando) "Consultando..." else "Ver disponibilidad")
        }

        error?.let { mensaje ->
            Text(mensaje, color = MaterialTheme.colorScheme.error)
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
                        error = e.message ?: "No se pudieron cargar las reservas."
                    } catch (e: JSONException) {
                        error = "La respuesta del servidor no es válida."
                    } finally {
                        cargandoReservas = false
                    }
                }
            },
            enabled = !cargandoReservas && !reservando
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
                    }
                }
            }
        }

        dia?.let { disponibilidad ->
            Text(
                "Horario: ${disponibilidad.horaApertura}–${disponibilidad.horaCierre}"
            )
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
                                                error = e.message ?: "No se pudo crear la reserva."
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
                            enabled = !cargando && !reservando
                        ) {
                            Text("$duracion h")
                        }
                    }
                }
            }
        }
    }
}