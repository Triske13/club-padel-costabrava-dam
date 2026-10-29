package com.clubpadelcostabrava.app.ui.calendario

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.clubpadelcostabrava.app.data.ApiCliente
import com.clubpadelcostabrava.app.data.ApiException
import com.clubpadelcostabrava.app.data.CalendarioMes
import com.clubpadelcostabrava.app.data.Sesion
import org.json.JSONException
import java.io.IOException
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

@Composable
fun CalendarioScreen(
    sesion: Sesion,
    api: ApiCliente,
    onAbrirCuenta: () -> Unit,
    onSeleccionarFecha: (LocalDate) -> Unit,
    onSesionInvalida: () -> Unit,
    mesVisible: YearMonth,
    onCambiarMes: (YearMonth) -> Unit
) {
    val hoy = LocalDate.now(ZoneId.of("Europe/Madrid"))
    val primerMes = YearMonth.from(hoy)
    val ultimoMes = YearMonth.from(hoy.plusDays(13))
    val meses = listOf(primerMes, ultimoMes).distinct()

    val pagerState = rememberPagerState(
        initialPage = meses.indexOf(mesVisible).coerceAtLeast(0),
        pageCount = { meses.size }
    )

    var calendarios by remember {
        mutableStateOf<Map<YearMonth, CalendarioMes>>(emptyMap())
    }
    var cargando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var fechasConReserva by remember {
        mutableStateOf<Set<LocalDate>>(emptySet())
    }
    var errorReservas by remember { mutableStateOf<String?>(null) }

    val formatoMes = DateTimeFormatter.ofPattern(
        "LLLL yyyy",
        Locale.forLanguageTag("es-ES")
    )

    LaunchedEffect(sesion.token, primerMes, ultimoMes) {
        cargando = true
        error = null
        calendarios = emptyMap()

        try {
            for (mes in meses) {
                val datos = api.consultarCalendario(sesion.token, mes)
                calendarios = calendarios + (mes to datos)
            }
        } catch (e: IOException) {
            if (e is ApiException && e.estado == 401) {
                onSesionInvalida()
            } else {
                error = e.message ?: "No se pudo cargar el calendario."
            }
        } catch (e: JSONException) {
            error = "La respuesta del servidor no es válida."
        } catch (e: DateTimeParseException) {
            error = "El servidor devolvió una fecha no válida."
        } finally {
            cargando = false
        }
    }

    LaunchedEffect(sesion.token) {
        fechasConReserva = emptySet()
        errorReservas = null

        try {
            fechasConReserva = api.listarMisReservas(sesion.token)
                .filter { it.estado == "CONFIRMADA" }
                .map { LocalDate.parse(it.fecha) }
                .toSet()
        } catch (e: IOException) {
            if (e is ApiException && e.estado == 401) {
                onSesionInvalida()
            } else {
                errorReservas = "No se pudieron consultar tus reservas."
            }
        } catch (e: JSONException) {
            errorReservas = "La lista de reservas no es válida."
        } catch (e: DateTimeParseException) {
            errorReservas = "Una reserva tiene una fecha no válida."
        }
    }

    LaunchedEffect(pagerState.settledPage) {
        onCambiarMes(meses[pagerState.settledPage])
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Calendario",
            style = MaterialTheme.typography.headlineSmall
        )

        TextButton(onClick = onAbrirCuenta) {
            Text("Mi cuenta")
        }

        Text(
            text = mesVisible.format(formatoMes),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        if (cargando) {
            Text("Cargando calendario...")
        }

        error?.let { mensaje ->
            Text(mensaje, color = MaterialTheme.colorScheme.error)
        }

        errorReservas?.let { mensaje ->
            Text(mensaje, color = MaterialTheme.colorScheme.error)
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            beyondViewportPageCount = 1
        ) { pagina ->
            val mesPagina = meses[pagina]
            val datos = calendarios[mesPagina]

            if (datos == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Cargando mes...")
                }
            } else {
                val huecosIniciales =
                    mesPagina.atDay(1).dayOfWeek.value - 1

                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        listOf("L", "M", "X", "J", "V", "S", "D")
                            .forEach { nombreDia ->
                                Text(
                                    text = nombreDia,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center
                                )
                            }
                    }

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(7),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        items(huecosIniciales) {
                            Spacer(modifier = Modifier.aspectRatio(1f))
                        }

                        items(datos.dias) { dia ->
                            val colorFondo = when {
                                dia.fecha.isBefore(hoy) || !dia.consultable ->
                                    Color(0xFFE0E0E0)
                                dia.sinHorasFuturas ->
                                    Color(0xFFF5B5B5)
                                dia.completo ->
                                    Color(0xFFFFD8A8)
                                else ->
                                    Color(0xFFBFE8C3)
                            }

                            Box(
                                modifier = Modifier
                                    .padding(2.dp)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(colorFondo)
                                    .clickable(enabled = dia.consultable) {
                                        onSeleccionarFecha(dia.fecha)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = dia.fecha.dayOfMonth.toString(),
                                    color = Color(0xFF1B1B1B)
                                )

                                if (dia.fecha in fechasConReserva) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(5.dp)
                                            .size(8.dp)
                                            .background(
                                                Color(0xFF1A237E),
                                                CircleShape
                                            )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}