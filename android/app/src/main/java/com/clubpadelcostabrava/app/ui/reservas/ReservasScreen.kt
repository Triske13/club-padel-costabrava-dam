package com.clubpadelcostabrava.app.ui.reservas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
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
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

/**
 * Muestra la disponibilidad de la fecha elegida y permite reservar una pista.
 * Presenta solo las franjas cuyo inicio aún no ha pasado, ofrece las duraciones
 * disponibles y actualiza la disponibilidad después de confirmar una reserva.
 *
 * @param sesion sesión del socio que realizará la reserva
 * @param api cliente utilizado para consultar y crear reservas
 * @param onVolverCalendario acción que regresa al calendario
 * @param onSesionInvalida acción que vuelve al login si caduca la sesión
 * @param fechaInicial fecha cuya disponibilidad se consulta al abrir la pantalla
 */
@Composable
fun ReservasScreen(
    sesion: Sesion,
    api: ApiCliente,
    onVolverCalendario: () -> Unit,
    onSesionInvalida: () -> Unit,
    fechaInicial: LocalDate = LocalDate.now(ZoneId.of("Europe/Madrid")).plusDays(1)
) {

    var dia by remember { mutableStateOf<DisponibilidadDia?>(null) }
    var cargando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    var pistaSeleccionadaId by remember { mutableStateOf<Int?>(null) }
    var reservando by remember { mutableStateOf(false) }
    var reservaCreada by remember { mutableStateOf<Reserva?>(null) }

    /**
     * Trata un error de red o de la API producido durante esta pantalla.
     * Si la sesión ha caducado, solicita volver al login; en otro caso,
     * prepara el mensaje que se mostrará al socio.
     *
     * @param e excepción recibida durante la petición
     * @param mensajePorDefecto texto utilizado si la excepción no contiene mensaje
     */
    fun mostrarError(e: IOException, mensajePorDefecto: String) {
        if (e is ApiException && e.estado == 401) {
            onSesionInvalida()
        } else {
            error = e.message ?: mensajePorDefecto
        }
    }

    LaunchedEffect(fechaInicial, sesion.token) {
        cargando = true
        error = null
        dia = null
        pistaSeleccionadaId = null

        try {
            val disponibilidad = api.consultarDisponibilidad(
                sesion.token,
                fechaInicial
            )
            dia = disponibilidad
            pistaSeleccionadaId = disponibilidad.pistas.firstOrNull()?.id
        } catch (e: IOException) {
            mostrarError(e, "No se pudo consultar la disponibilidad.")
        } catch (e: JSONException) {
            error = "La respuesta del servidor no es válida."
        } finally {
            cargando = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val colorFlecha = MaterialTheme.colorScheme.onPrimary

        FilledIconButton(
            onClick = onVolverCalendario,
            modifier = Modifier.size(56.dp)
        ) {
            Canvas(modifier = Modifier.size(28.dp)) {
                val grosor = 3.dp.toPx()
                val punta = Offset(size.width * 0.2f, size.height * 0.5f)

                drawLine(colorFlecha, punta, Offset(size.width * 0.8f, size.height * 0.5f), grosor)
                drawLine(colorFlecha, punta, Offset(size.width * 0.5f, size.height * 0.2f), grosor)
                drawLine(colorFlecha, punta, Offset(size.width * 0.5f, size.height * 0.8f), grosor)
            }
        }

        Text(
            "Reservar pista",
            style = MaterialTheme.typography.headlineSmall
        )

        val idioma = Locale.forLanguageTag("es-ES")
        val diaSemana = fechaInicial.dayOfWeek
            .getDisplayName(TextStyle.FULL, idioma)
            .replaceFirstChar { it.uppercaseChar() }
        val mes = fechaInicial.month
            .getDisplayName(TextStyle.FULL, idioma)
            .replaceFirstChar { it.uppercaseChar() }

        Text(
            text = "$diaSemana ${fechaInicial.dayOfMonth} de $mes",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        if (cargando) {
            Text("Cargando disponibilidad...")
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
            val ahora = LocalDateTime.now(ZoneId.of("Europe/Madrid"))
            val franjas = pistaElegida?.franjas.orEmpty().filter { franja ->
                LocalDateTime.of(
                    LocalDate.parse(disponibilidad.fecha),
                    LocalTime.parse(franja.horaInicio)
                ).isAfter(ahora)
            }

            if (franjas.isEmpty()) {
                Text("No hay turnos para esta pista.")
            }

            franjas.forEach { franja ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(franja.horaInicio)
                    Text(franja.estado)

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
                                                    fecha = LocalDate.parse(
                                                        disponibilidad.fecha
                                                    ),
                                                    horaInicio = franja.horaInicio,
                                                    duracionHoras = duracion
                                                )
                                                reservaCreada = creada

                                                try {
                                                    val actualizada =
                                                        api.consultarDisponibilidad(
                                                            sesion.token,
                                                            LocalDate.parse(
                                                                disponibilidad.fecha
                                                            )
                                                        )
                                                    dia = actualizada
                                                    pistaSeleccionadaId = pista.id
                                                } catch (e: IOException) {
                                                    dia = null
                                                    if (e is ApiException &&
                                                        e.estado == 401
                                                    ) {
                                                        onSesionInvalida()
                                                    } else {
                                                        error =
                                                            "Reserva ${creada.id} confirmada, " +
                                                                    "pero no se pudo actualizar la disponibilidad."
                                                    }
                                                } catch (e: JSONException) {
                                                    dia = null
                                                    error =
                                                        "Reserva ${creada.id} confirmada, " +
                                                                "pero no se pudo actualizar la disponibilidad."
                                                }
                                            } catch (e: IOException) {
                                                mostrarError(
                                                    e,
                                                    "No se pudo crear la reserva."
                                                )
                                                dia = null
                                            } catch (e: JSONException) {
                                                error =
                                                    "La respuesta del servidor no es válida."
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