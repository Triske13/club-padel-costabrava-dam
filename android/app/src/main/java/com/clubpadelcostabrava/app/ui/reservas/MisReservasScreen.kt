package com.clubpadelcostabrava.app.ui.reservas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.clubpadelcostabrava.app.data.ApiCliente
import com.clubpadelcostabrava.app.data.ApiException
import com.clubpadelcostabrava.app.data.Reserva
import com.clubpadelcostabrava.app.data.Sesion
import kotlinx.coroutines.launch
import org.json.JSONException
import java.io.IOException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Muestra las reservas futuras confirmadas del socio y las canceladas
 * por administración. Permite solicitar la cancelación de una reserva
 * cuando la API indica que puede cancelarse y pide confirmación antes
 * de enviarla al servidor.
 *
 * @param sesion sesión del socio titular de las reservas
 * @param api cliente utilizado para consultar y cancelar reservas
 * @param onVolver acción que regresa a la pantalla anterior
 * @param onSesionInvalida acción que vuelve al login si caduca la sesión
 */
@Composable
fun MisReservasScreen(
    sesion: Sesion,
    api: ApiCliente,
    onVolver: () -> Unit,
    onSesionInvalida: () -> Unit
) {
    var reservas by remember { mutableStateOf<List<Reserva>>(emptyList()) }
    var reservaPendiente by remember { mutableStateOf<Reserva?>(null) }
    var cancelandoId by remember { mutableStateOf<Int?>(null) }
    var errorCancelacion by remember { mutableStateOf<String?>(null) }
    var avisoCancelacion by remember { mutableStateOf<String?>(null) }
    var cargando by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(sesion.token) {
        cargando = true
        error = null
        reservas = emptyList()

        try {
            reservas = api.listarMisReservas(sesion.token)
        } catch (e: IOException) {
            when {
                e is ApiException && e.estado == 401 ->
                    onSesionInvalida()

                e is ApiException ->
                    error = e.message ?: "No se pudieron cargar tus reservas."

                else ->
                    error = "No se pudo conectar con el servidor. Inténtalo de nuevo."
            }
        } catch (e: JSONException) {
            error = "La respuesta del servidor no es válida."
        } finally {
            cargando = false
        }
    }
    val ahora = LocalDateTime.now(ZoneId.of("Europe/Madrid"))
    val reservasVisibles = reservas.filter { reserva ->
        when {
            reserva.estado == "CANCELADA" &&
                    reserva.canceladaPor == "ADMIN" -> true

            reserva.estado == "CONFIRMADA" ->
                LocalDateTime.of(
                    LocalDate.parse(reserva.fecha),
                    LocalTime.parse(reserva.horaInicio)
                ).isAfter(ahora)

            else -> false
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
            onClick = onVolver,
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
            "Mis reservas",
            style = MaterialTheme.typography.headlineSmall
        )

        if (cargando) {
            Text("Cargando reservas...")
        }

        error?.let { mensaje ->
            Text(mensaje, color = MaterialTheme.colorScheme.error)
        }
        errorCancelacion?.let { mensaje ->
            Text(mensaje, color = MaterialTheme.colorScheme.error)
        }

        avisoCancelacion?.let { mensaje ->
            Text(mensaje, color = MaterialTheme.colorScheme.primary)
        }

        if (!cargando && error == null) {
            if (reservasVisibles.isEmpty()) {
                Text("No hay reservas para mostrar.")
            }

            reservasVisibles.forEach { reserva ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "${reserva.pistaNombre} · ${reserva.fecha}",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "${reserva.horaInicio}–${reserva.horaFin} " +
                                    "(${reserva.duracionHoras} h)"
                        )

                        if (reserva.estado == "CANCELADA") {
                            Text(
                                "Cancelada por administración",
                                color = MaterialTheme.colorScheme.error
                            )
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
                TextButton(
                    onClick = {
                        reservaPendiente = null
                        cancelandoId = pendiente.id
                        errorCancelacion = null
                        avisoCancelacion = null

                        scope.launch {
                            try {
                                val cancelada = api.cancelarMiReserva(
                                    sesion.token,
                                    pendiente.id
                                )

                                reservas = reservas.map { actual ->
                                    if (actual.id == cancelada.id) cancelada else actual
                                }
                                avisoCancelacion =
                                    "Reserva ${cancelada.id} cancelada."
                            } catch (e: IOException) {
                                when {
                                    e is ApiException && e.estado == 401 ->
                                        onSesionInvalida()

                                    e is ApiException ->
                                        errorCancelacion =
                                            e.message ?: "No se pudo cancelar la reserva."

                                    else ->
                                        errorCancelacion =
                                            "No se pudo confirmar la cancelación. " +
                                                    "Vuelve a consultar tus reservas antes de repetirla."
                                }
                            } catch (e: JSONException) {
                                errorCancelacion =
                                    "No se pudo leer la respuesta. Comprueba el estado de la reserva."
                            } finally {
                                cancelandoId = null
                            }
                        }
                    }
                ) {
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
}