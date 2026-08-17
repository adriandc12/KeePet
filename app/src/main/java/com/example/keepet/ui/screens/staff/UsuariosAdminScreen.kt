@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.keepet.ui.screens.staff

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.keepet.data.model.Rol
import com.example.keepet.data.model.UsuarioConRol
import com.example.keepet.ui.theme.*
import com.example.keepet.viewmodel.StaffViewModel

/**
 * Gestion de usuarios y roles. Solo la ve un ADMIN.
 *
 * Aqui es donde se convierte a alguien en empleado o doctor. El flujo pensado es:
 *   1. la persona se registra normalmente en la app (entra como cliente);
 *   2. el admin la busca en esta lista y le cambia el rol.
 *
 * No hay forma de elegir "quiero ser doctor" al registrarse, y es a proposito: si el
 * formulario de registro dejara escoger el rol, cualquiera podria darse permisos de
 * administrador de la clinica.
 */
@Composable
fun UsuariosAdminScreen(viewModel: StaffViewModel) {
    val usuarios by viewModel.usuarios.collectAsState()
    var usuarioAEditar by remember { mutableStateOf<UsuarioConRol?>(null) }

    usuarioAEditar?.let { usuario ->
        DialogoCambiarRol(
            usuario = usuario,
            esYoMismo = usuario.uid == viewModel.miUid,
            onCerrar = { usuarioAEditar = null },
            onElegir = { nuevoRol ->
                viewModel.cambiarRol(usuario, nuevoRol)
                usuarioAEditar = null
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = InputBackground)
        ) {
            Text(
                "Toca un usuario para cambiar su rol. Los clientes ven solo sus " +
                    "mascotas; empleados y doctores ven la clínica completa.",
                modifier = Modifier.padding(14.dp),
                fontSize = 13.sp,
                color = TextColor
            )
        }

        if (usuarios.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Cargando usuarios…",
                    color = TextColor.copy(alpha = 0.6f),
                    fontSize = 15.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp, 0.dp, 16.dp, 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(usuarios, key = { it.uid }) { usuario ->
                    TarjetaUsuario(
                        usuario = usuario,
                        esYoMismo = usuario.uid == viewModel.miUid,
                        onClick = { usuarioAEditar = usuario }
                    )
                }
            }
        }
    }
}

@Composable
private fun TarjetaUsuario(
    usuario: UsuarioConRol,
    esYoMismo: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(InputBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, contentDescription = null, tint = TextColor)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    usuario.nombreVisible + if (esYoMismo) " (tú)" else "",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextColor
                )
                Text(
                    usuario.perfil.correo.ifBlank { "Sin correo" },
                    fontSize = 12.sp,
                    color = TextColor.copy(alpha = 0.6f)
                )
            }

            InsigniaRol(usuario.rol)
        }
    }
}

@Composable
private fun InsigniaRol(rol: Rol) {
    val color = when (rol) {
        Rol.CLIENTE -> GrayHint
        Rol.EMPLEADO -> Color(0xFF4A90A4)
        Rol.DOCTOR -> Color(0xFF5B9A6B)
        Rol.ADMIN -> Color(0xFFB5651D)
    }
    Surface(shape = RoundedCornerShape(50.dp), color = color.copy(alpha = 0.15f)) {
        Text(
            rol.etiqueta,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun DialogoCambiarRol(
    usuario: UsuarioConRol,
    esYoMismo: Boolean,
    onCerrar: () -> Unit,
    onElegir: (Rol) -> Unit
) {
    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text(usuario.nombreVisible, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                if (esYoMismo) {
                    // Si un admin se degradara a si mismo y fuera el unico, ya nadie
                    // podria repartir roles sin entrar a mano en la consola de Firebase.
                    Text(
                        "No puedes cambiar tu propio rol. Pídeselo a otro " +
                            "administrador, o cámbialo en la consola de Firebase.",
                        fontSize = 13.sp,
                        color = CancelRed
                    )
                } else {
                    Text("Elige el nuevo rol:", fontSize = 13.sp, color = GrayHint)
                    Spacer(modifier = Modifier.height(8.dp))
                    Rol.entries.forEach { rol ->
                        val esActual = rol == usuario.rol
                        TextButton(
                            onClick = { onElegir(rol) },
                            enabled = !esActual,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    rol.etiqueta + if (esActual) "  (actual)" else "",
                                    color = if (esActual) GrayHint else TextColor,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        Text(
                            descripcionDe(rol),
                            fontSize = 11.sp,
                            color = GrayHint,
                            modifier = Modifier.padding(start = 12.dp, bottom = 6.dp)
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onCerrar) { Text("Cerrar") } }
    )
}

private fun descripcionDe(rol: Rol): String = when (rol) {
    Rol.CLIENTE -> "Ve solo sus mascotas y sus citas."
    Rol.EMPLEADO -> "Ve la agenda completa y escanea los QR."
    Rol.DOCTOR -> "Lo de empleado y escribe en el historial médico."
    Rol.ADMIN -> "Todo lo anterior y reparte los roles."
}
