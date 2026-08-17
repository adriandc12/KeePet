package com.example.keepet.ui.screens.staff

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.keepet.data.model.Mascota
import com.example.keepet.data.model.UsuarioConRol
import com.example.keepet.ui.screens.CustomSearchBar
import com.example.keepet.ui.screens.EmptyState
import com.example.keepet.ui.screens.PetList
import com.example.keepet.ui.theme.AccentButton
import com.example.keepet.ui.theme.GrayHint
import com.example.keepet.ui.theme.TextColor
import com.example.keepet.viewmodel.StaffViewModel

/**
 * EXPEDIENTES de la clinica.
 *
 * Esta pestaña estaba antes en la app del cliente, donde cada dueño gestionaba los
 * expedientes de sus propias mascotas. Se movio aqui porque llevar expedientes es
 * trabajo de la clinica, no del cliente.
 *
 * La vista es la MISMA, no una copia: reutiliza tal cual los composables que ya
 * dibujaban esa pestaña (CustomSearchBar, PetList y EmptyState, todos en HomeScreen.kt).
 * Eso significa que las tarjetas, los botones de editar y borrar, la barra de busqueda y
 * el mensaje de "no se encontraron mascotas" se ven exactamente igual que antes, y que
 * si algun dia se retoca el diseño se retoca en un solo sitio.
 *
 * Lo unico distinto es de donde salen los datos: antes eran "mis mascotas" y ahora son
 * los pacientes de todos los clientes.
 */
@Composable
fun ExpedientesScreen(
    viewModel: StaffViewModel,
    onEditarExpediente: (Mascota) -> Unit,
    onAbrirExpediente: (Mascota) -> Unit
) {
    val pacientes by viewModel.pacientes.collectAsState()
    var busqueda by remember { mutableStateOf("") }

    // Se busca igual que en la pestaña del cliente (nombre y dueño), mas la especie y
    // la raza, que aqui si sirven: el personal maneja cientos de fichas, no tres.
    val filtrados = pacientes.filter { mascota ->
        busqueda.isBlank() ||
            mascota.nombre.contains(busqueda, ignoreCase = true) ||
            mascota.dueno.contains(busqueda, ignoreCase = true) ||
            mascota.especie.contains(busqueda, ignoreCase = true) ||
            mascota.raza.contains(busqueda, ignoreCase = true)
    }

    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        CustomSearchBar(busqueda) { busqueda = it }

        // Cuantos pacientes hay. Venia de la pestaña "Pacientes", que se ha quitado por
        // enseñar esta misma lista otra vez (lo explica el enum Pestana en
        // StaffHomeScreen.kt). Es util para saber de un vistazo si el filtro esta
        // escondiendo la mitad de la clinica.
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (busqueda.isBlank()) {
                "${pacientes.size} paciente${if (pacientes.size == 1) "" else "s"}"
            } else {
                "${filtrados.size} de ${pacientes.size} pacientes"
            },
            fontSize = 12.sp,
            color = TextColor.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(12.dp))

        if (filtrados.isEmpty()) {
            EmptyState(query = busqueda, category = "Todos")
        } else {
            PetList(
                pets = filtrados,
                // PetList entrega solo el id (asi estaba escrita), asi que hay que
                // volver a buscar la mascota en la lista. El objeto completo es
                // imprescindible: dentro viene el duenoUid, que es lo que le dice al
                // ViewModel en la rama de que cliente tiene que escribir.
                onDelete = { petId ->
                    pacientes.find { it.id == petId }?.let { viewModel.deletePet(it) }
                },
                onEdit = { petId ->
                    pacientes.find { it.id == petId }?.let(onEditarExpediente)
                },
                onDetail = { petId ->
                    pacientes.find { it.id == petId }?.let(onAbrirExpediente)
                }
            )
        }
    }
}

/**
 * Pregunta de quien va a ser el expediente nuevo.
 *
 * Este dialogo no existia en la app del cliente y no habia forma de evitarlo: cuando un
 * cliente creaba una mascota, el dueño era el mismo, no habia nada que preguntar. Desde
 * la clinica hay que decirlo, porque el expediente se guarda dentro de la rama de ese
 * cliente y ahi es donde el dueño lo vera despues en su telefono.
 *
 * Solo se ofrecen los usuarios con rol de cliente: crearle un expediente a un compañero
 * de trabajo casi siempre seria un error de dedo.
 */
@Composable
fun DialogoElegirCliente(
    clientes: List<UsuarioConRol>,
    onCerrar: () -> Unit,
    onElegir: (UsuarioConRol) -> Unit
) {
    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("¿De quién es la mascota?", fontWeight = FontWeight.Bold) },
        text = {
            if (clientes.isEmpty()) {
                Text(
                    "Todavía no hay ningún cliente registrado. El dueño tiene que " +
                        "crearse su cuenta en la app para poder abrirle un expediente.",
                    fontSize = 14.sp,
                    color = TextColor
                )
            } else {
                Column {
                    Text(
                        "El expediente se guardará en la cuenta del cliente que elijas, " +
                            "así que él también lo verá desde su teléfono.",
                        fontSize = 12.sp,
                        color = GrayHint
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    clientes.forEach { cliente ->
                        TextButton(
                            onClick = { onElegir(cliente) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                cliente.nombreVisible,
                                color = AccentButton,
                                fontSize = 15.sp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onCerrar) { Text("Cancelar") } }
    )
}
