package com.clubpadelcostabrava.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.clubpadelcostabrava.app.data.ApiCliente
import com.clubpadelcostabrava.app.data.ApiException
import com.clubpadelcostabrava.app.data.Sesion
import com.clubpadelcostabrava.app.ui.auth.CodigoScreen
import com.clubpadelcostabrava.app.ui.auth.LoginScreen
import com.clubpadelcostabrava.app.ui.calendario.CalendarioScreen
import com.clubpadelcostabrava.app.ui.cuenta.CuentaScreen
import com.clubpadelcostabrava.app.ui.reservas.MisReservasScreen
import com.clubpadelcostabrava.app.ui.reservas.ReservasScreen
import com.clubpadelcostabrava.app.ui.theme.ClubPadelCostaBravaTheme
import kotlinx.coroutines.launch
import org.json.JSONException
import java.io.IOException
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/**
 * Actividad principal de la aplicación Android del socio.
 * Mantiene el estado de la sesión y decide qué pantalla de Compose se muestra:
 * acceso, recuperación, calendario, reserva, mis reservas o cuenta.
 */
class MainActivity : ComponentActivity() {

    /**
     * Inicializa la interfaz Compose y conecta las acciones de las pantallas
     * con la API. Gestiona la navegación, los mensajes de error y el regreso
     * al login cuando termina o deja de ser válida la sesión.
     *
     * @param savedInstanceState estado anterior proporcionado por Android,
     * o null si la actividad se crea por primera vez
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ClubPadelCostaBravaTheme {
                val api = remember { ApiCliente() }
                val scope = rememberCoroutineScope()

                var sesion by remember { mutableStateOf<Sesion?>(null) }
                var cargando by remember { mutableStateOf(false) }
                var error by remember { mutableStateOf<String?>(null) }
                var mensajeCodigo by remember { mutableStateOf<String?>(null) }
                var mensajeAcceso by remember { mutableStateOf<String?>(null) }
                var mostrandoCodigo by remember { mutableStateOf(false) }
                var mostrandoCuenta by remember { mutableStateOf(false) }
                var mostrandoMisReservas by remember { mutableStateOf(false) }
                var fechaSeleccionada by remember { mutableStateOf<LocalDate?>(null) }
                var mesCalendario by remember {
                    mutableStateOf(YearMonth.now(ZoneId.of("Europe/Madrid")))
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        if (sesion == null) {
                            if (mostrandoCodigo) {
                                CodigoScreen(
                                    cargando = cargando,
                                    error = error,
                                    mensaje = mensajeCodigo,
                                    onSolicitarCodigo = { correo ->
                                        scope.launch {
                                            cargando = true
                                            error = null
                                            mensajeCodigo = null

                                            try {
                                                api.solicitarCodigo(correo)
                                                mensajeCodigo =
                                                    "Si el correo corresponde a una cuenta habilitada, recibirás un código."
                                            } catch (e: IOException) {
                                                error = e.message
                                                    ?: "No se pudo solicitar el código."
                                            } finally {
                                                cargando = false
                                            }
                                        }
                                    },
                                    onEstablecerContrasena = { correo, codigo, nuevaContrasena ->
                                        scope.launch {
                                            cargando = true
                                            error = null
                                            mensajeCodigo = null

                                            try {
                                                api.establecerContrasena(
                                                    correo,
                                                    codigo,
                                                    nuevaContrasena
                                                )
                                                mensajeAcceso =
                                                    "Contraseña guardada. Ya puedes iniciar sesión."
                                                mostrandoCodigo = false
                                            } catch (e: IOException) {
                                                error = e.message
                                                    ?: "No se pudo establecer la contraseña."
                                            } finally {
                                                cargando = false
                                            }
                                        }
                                    },
                                    onVolver = {
                                        mostrandoCodigo = false
                                        error = null
                                        mensajeCodigo = null
                                    }
                                )
                            } else {
                                LoginScreen(
                                    cargando = cargando,
                                    error = error,
                                    mensaje = mensajeAcceso,
                                    onAbrirCodigo = {
                                        mostrandoCodigo = true
                                        error = null
                                        mensajeCodigo = null
                                        mensajeAcceso = null
                                    },
                                    onEntrar = { correo, contrasena ->
                                        scope.launch {
                                            cargando = true
                                            error = null
                                            mensajeAcceso = null

                                            try {
                                                val nuevaSesion =
                                                    api.iniciarSesion(correo, contrasena)

                                                if (nuevaSesion.rol == "USER") {
                                                    sesion = nuevaSesion
                                                    fechaSeleccionada = null
                                                    mesCalendario = YearMonth.now(
                                                        ZoneId.of("Europe/Madrid")
                                                    )
                                                } else {
                                                    error =
                                                        "Esta aplicación solo permite cuentas de socio."
                                                }
                                            } catch (e: IOException) {
                                                error = e.message
                                                    ?: "No se pudo iniciar sesión."
                                            } catch (e: JSONException) {
                                                error =
                                                    "La respuesta del servidor no es válida."
                                            } finally {
                                                cargando = false
                                            }
                                        }
                                    }
                                )
                            }
                        } else {
                            sesion?.let { sesionActual ->
                                if (mostrandoCuenta) {
                                    CuentaScreen(
                                        cargando = cargando,
                                        error = error,
                                        onCambiarContrasena = { actual, nueva ->
                                            scope.launch {
                                                cargando = true
                                                error = null

                                                try {
                                                    api.cambiarContrasena(
                                                        sesionActual.token,
                                                        actual,
                                                        nueva
                                                    )
                                                    sesion = null
                                                    mostrandoCuenta = false
                                                    fechaSeleccionada = null
                                                    mensajeAcceso =
                                                        "Contraseña actualizada. Inicia sesión de nuevo."
                                                } catch (e: IOException) {
                                                    if (e is ApiException && e.estado == 401) {
                                                        sesion = null
                                                        mostrandoCuenta = false
                                                        fechaSeleccionada = null
                                                        error = null
                                                        mensajeAcceso =
                                                            "Tu sesión ya no es válida. Inicia sesión de nuevo."
                                                    } else {
                                                        error = e.message
                                                            ?: "No se pudo cambiar la contraseña."
                                                    }
                                                } finally {
                                                    cargando = false
                                                }
                                            }
                                        },
                                        onCerrarSesion = {
                                            scope.launch {
                                                cargando = true
                                                error = null

                                                try {
                                                    api.cerrarSesion(sesionActual.token)
                                                    mensajeAcceso = "Sesión cerrada."
                                                } catch (e: IOException) {
                                                    mensajeAcceso =
                                                        "Has salido de la aplicación, pero no se pudo confirmar el cierre en el servidor."
                                                } finally {
                                                    sesion = null
                                                    mostrandoCuenta = false
                                                    fechaSeleccionada = null
                                                    cargando = false
                                                }
                                            }
                                        },
                                        onVolver = {
                                            mostrandoCuenta = false
                                            error = null
                                        }
                                    )
                                } else if (mostrandoMisReservas) {
                                    MisReservasScreen(
                                        sesion = sesionActual,
                                        api = api,
                                        onVolver = {
                                            mostrandoMisReservas = false
                                        },
                                        onSesionInvalida = {
                                            sesion = null
                                            mostrandoMisReservas = false
                                            mostrandoCuenta = false
                                            fechaSeleccionada = null
                                            error = null
                                            mensajeAcceso =
                                                "Tu sesión ya no es válida. Inicia sesión de nuevo."
                                        }
                                    )
                                } else {
                                    val fecha = fechaSeleccionada

                                    if (fecha == null) {
                                        CalendarioScreen(
                                            sesion = sesionActual,
                                            api = api,
                                            mesVisible = mesCalendario,
                                            onCambiarMes = { nuevoMes ->
                                                mesCalendario = nuevoMes
                                            },
                                            onSeleccionarFecha = { fechaElegida ->
                                                fechaSeleccionada = fechaElegida
                                            },
                                            onAbrirCuenta = {
                                                mostrandoCuenta = true
                                                error = null
                                            },
                                            onAbrirMisReservas = {
                                                mostrandoMisReservas = true
                                            },
                                            onSesionInvalida = {
                                                sesion = null
                                                mostrandoCuenta = false
                                                fechaSeleccionada = null
                                                error = null
                                                mensajeAcceso =
                                                    "Tu sesión ya no es válida. Inicia sesión de nuevo."
                                            }
                                        )
                                    } else {
                                        ReservasScreen(
                                            sesion = sesionActual,
                                            api = api,
                                            fechaInicial = fecha,
                                            onVolverCalendario = {
                                                fechaSeleccionada = null
                                            },
                                            onSesionInvalida = {
                                                sesion = null
                                                mostrandoCuenta = false
                                                fechaSeleccionada = null
                                                error = null
                                                mensajeAcceso =
                                                    "Tu sesión ya no es válida. Inicia sesión de nuevo."
                                            }
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
}