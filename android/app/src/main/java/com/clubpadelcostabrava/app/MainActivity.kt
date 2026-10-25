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
import com.clubpadelcostabrava.app.data.Sesion
import com.clubpadelcostabrava.app.ui.auth.LoginScreen
import com.clubpadelcostabrava.app.ui.reservas.ReservasScreen
import com.clubpadelcostabrava.app.ui.theme.ClubPadelCostaBravaTheme
import kotlinx.coroutines.launch
import org.json.JSONException
import java.io.IOException

class MainActivity : ComponentActivity() {
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
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        if (sesion == null) {
                            LoginScreen(
                                cargando = cargando,
                                error = error,
                                onEntrar = { correo, contrasena ->
                                    scope.launch {
                                        cargando = true
                                        error = null

                                        try {
                                            val nuevaSesion = api.iniciarSesion(correo, contrasena)
                                            if (nuevaSesion.rol == "USER") {
                                                sesion = nuevaSesion
                                            } else {
                                                error = "Esta aplicación solo permite cuentas de socio."
                                            }
                                        } catch (e: IOException) {
                                            error = e.message ?: "No se pudo iniciar sesión."
                                        } catch (e: JSONException) {
                                            error = "La respuesta del servidor no es válida."
                                        } finally {
                                            cargando = false
                                        }
                                    }
                                }
                            )
                        } else {
                            sesion?.let { sesionActual ->
                                ReservasScreen(sesion = sesionActual, api = api)
                            }
                        }
                    }
                }
            }
        }
    }
}