package com.clubpadelcostabrava.app.ui.auth

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
fun CodigoScreen(
    cargando: Boolean,
    error: String?,
    mensaje: String?,
    onSolicitarCodigo: (String) -> Unit,
    onEstablecerContrasena: (String, String, String) -> Unit,
    onVolver: () -> Unit
) {
    var correo by remember { mutableStateOf("") }
    var codigo by remember { mutableStateOf("") }
    var nuevaContrasena by remember { mutableStateOf("") }

    val longitudContrasena =
        nuevaContrasena.codePointCount(0, nuevaContrasena.length)

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Activar o recuperar cuenta",
            style = MaterialTheme.typography.headlineSmall
        )

        OutlinedTextField(
            value = correo,
            onValueChange = { correo = it },
            label = { Text("Correo") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = { onSolicitarCodigo(correo.trim()) },
            enabled = !cargando && correo.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (cargando) "Enviando..." else "Enviar código")
        }

        if (!mensaje.isNullOrBlank()) {
            Text(mensaje, color = MaterialTheme.colorScheme.primary)
        }

        OutlinedTextField(
            value = codigo,
            onValueChange = { codigo = it },
            label = { Text("Código de 6 cifras") },
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
                onEstablecerContrasena(
                    correo.trim(),
                    codigo.trim(),
                    nuevaContrasena
                )
            },
            enabled = !cargando &&
                    correo.isNotBlank() &&
                    codigo.matches(Regex("[0-9]{6}")) &&
                    longitudContrasena in 10..64,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Establecer contraseña")
        }

        TextButton(onClick = onVolver, enabled = !cargando) {
            Text("Volver")
        }
    }
}