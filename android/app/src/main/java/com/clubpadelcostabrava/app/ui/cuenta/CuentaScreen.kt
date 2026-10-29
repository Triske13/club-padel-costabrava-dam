package com.clubpadelcostabrava.app.ui.cuenta

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun CuentaScreen(
    cargando: Boolean,
    error: String?,
    onCambiarContrasena: (String, String) -> Unit,
    onVolver: () -> Unit,
    onCerrarSesion: () -> Unit
) {
    var contrasenaActual by remember { mutableStateOf("") }
    var nuevaContrasena by remember { mutableStateOf("") }

    val longitudNueva =
        nuevaContrasena.codePointCount(0, nuevaContrasena.length)

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Mi cuenta", style = MaterialTheme.typography.headlineSmall)

        OutlinedTextField(
            value = contrasenaActual,
            onValueChange = { contrasenaActual = it },
            label = { Text("Contraseña actual") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = nuevaContrasena,
            onValueChange = { nuevaContrasena = it },
            label = { Text("Nueva contraseña") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        if (!error.isNullOrBlank()) {
            Text(error, color = MaterialTheme.colorScheme.error)
        }

        Button(
            onClick = {
                onCambiarContrasena(contrasenaActual, nuevaContrasena)
            },
            enabled = !cargando &&
                    contrasenaActual.isNotEmpty() &&
                    longitudNueva in 10..64,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (cargando) "Guardando..." else "Cambiar contraseña")
        }

        TextButton(onClick = onCerrarSesion, enabled = !cargando) {
            Text("Cerrar sesión")
        }

        TextButton(onClick = onVolver, enabled = !cargando) {
            Text("Volver")
        }
    }
}