package com.clubpadelcostabrava.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.clubpadelcostabrava.app.ui.theme.ClubPadelCostaBravaTheme
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ClubPadelCostaBravaTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    DiagnosticoScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
private fun DiagnosticoScreen(modifier: Modifier = Modifier) {
    var mensaje by remember { mutableStateOf("Consultando servidor...") }

    LaunchedEffect(Unit) {
        mensaje = try {
            withContext(Dispatchers.IO) {
                val conexion = URL(
                    "${ApiConfig.BASE_URL}/api/diagnostico"
                ).openConnection() as HttpURLConnection

                conexion.connectTimeout = 5000
                conexion.readTimeout = 5000

                try {
                    if (conexion.responseCode == HttpURLConnection.HTTP_OK) {
                        conexion.inputStream.bufferedReader().use {
                            it.readText()
                        }
                    } else {
                        "Error HTTP ${conexion.responseCode}"
                    }
                } finally {
                    conexion.disconnect()
                }
            }
        } catch (e: IOException) {
            "Error de conexión: ${e.message ?: "desconocido"}"
        }
    }

    Text(text = mensaje, modifier = modifier)
}