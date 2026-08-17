@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.keepet.ui.screens.staff

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.keepet.data.model.Cita
import com.example.keepet.data.model.UsuarioConRol
import com.example.keepet.ui.theme.*
import com.example.keepet.viewmodel.FiltroAgenda
import com.example.keepet.viewmodel.StaffViewModel
import com.example.keepet.viewmodel.cuandoTexto

/**
 * La agenda de toda la clinica.
 *
 * Es la pantalla que mas se usa en recepcion, asi que esta pensada para responder de
 * un vistazo a "que tengo ahora": contadores arriba, filtros, y en cada tarjeta solo
 * el boton del PASO SIGUIENTE de esa cita, no todos los botones a la vez.
 */
@Composable
fun AgendaScreen(viewModel: StaffViewModel) {
    val citas by viewModel.agenda.collectAsState()
    val resumen by viewModel.resumen.collectAsState()
    val filtro by viewModel.filtroActual.collectAsState()
    val busqueda by viewModel.textoBusqueda.collectAsState()
    val doctores by viewModel.doctores.collectAsState()

    var citaParaAsignar by remember { mutableStateOf<Cita?>(null) }

    citaParaAsignar?.let { cita ->
        DialogoAsignarDoctor(
            doctores = doctores,
            onCerrar = { citaParaAsignar = null },
            onElegir = { doctor ->
                viewModel.asignarDoctor(cita.id, doctor)
                citaParaAsignar = null
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {

        // ---- Contadores ----
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Contador("Pendientes", resumen[Cita.ESTADO_PENDIENTE] ?: 0, Modifier.weight(1f))
            Contador("En clínica", resumen[Cita.ESTADO_CONFIRMADA] ?: 0, Modifier.weight(1f))
            Contador("Completadas", resumen[Cita.ESTADO_COMPLETADA] ?: 0, Modifier.weight(1f))
        }

        // ---- Buscador ----
        OutlinedTextField(
            value = busqueda,
            onValueChange = viewModel::buscar,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            placeholder = { Text("Buscar por mascota, cliente o código", fontSize = 14.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = White,
                unfocusedContainerColor = White,
                focusedBorderColor = AccentButton,
                unfocusedBorderColor = Color.Transparent
            )
        )

        // ---- Filtros ----
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FiltroAgenda.entries.forEach { f ->
                FilterChip(
                    selected = filtro == f,
                    onClick = { viewModel.cambiarFiltro(f) },
                    label = { Text(f.etiqueta, fontSize = 13.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AccentButton,
                        selectedLabelColor = White,
                        containerColor = White
                    )
                )
            }
        }

        if (citas.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = GrayHint.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "No hay citas con este filtro",
                        color = TextColor.copy(alpha = 0.6f),
                        fontSize = 15.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp, 0.dp, 16.dp, 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(citas, key = { it.id }) { cita ->
                    TarjetaCitaStaff(
                        cita = cita,
                        puedeAtender = viewModel.miRol.puedeEditarHistorial,
                        onConfirmar = { viewModel.confirmarLlegada(cita.id) },
                        onAtender = { viewModel.pasarAAtencion(cita.id) },
                        onCompletar = { viewModel.completarCita(cita.id) },
                        onCancelar = { viewModel.cancelarCita(cita.id) },
                        onAsignar = { citaParaAsignar = cita }
                    )
                }
            }
        }
    }
}

@Composable
private fun Contador(etiqueta: String, valor: Int, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("$valor", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = AccentButton)
            Text(etiqueta, fontSize = 11.sp, color = TextColor.copy(alpha = 0.7f))
        }
    }
}

@Composable
private fun TarjetaCitaStaff(
    cita: Cita,
    puedeAtender: Boolean,
    onConfirmar: () -> Unit,
    onAtender: () -> Unit,
    onCompletar: () -> Unit,
    onCancelar: () -> Unit,
    onAsignar: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        cita.nombreMascota.ifBlank { "Sin mascota" },
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextColor
                    )
                    Text(
                        cita.clienteNombre.ifBlank { "Cliente sin nombre" },
                        fontSize = 13.sp,
                        color = TextColor.copy(alpha = 0.6f)
                    )
                }
                EtiquetaEstado(cita.estado)
            }

            Spacer(modifier = Modifier.height(10.dp))

            FilaDato("Servicio", cita.tipoServicio.ifBlank { cita.motivo.ifBlank { "—" } })
            // "Hoy · 08:00 AM" en vez de "16 de agosto · 08:00 AM": en recepcion lo
            // primero que se necesita saber es si la cita es de hoy. Mira cuandoTexto().
            FilaDato("Cuándo", cuandoTexto(cita))
            FilaDato("Código", cita.codigo)
            FilaDato("Doctor", cita.doctorNombre.ifBlank { "Sin asignar" })

            if (cita.notasAdicionales.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Notas: ${cita.notasAdicionales}",
                    fontSize = 13.sp,
                    color = TextColor.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Solo se ofrece el paso siguiente de esta cita. Enseñar los cuatro
            // botones siempre invita a equivocarse (completar algo que no ha llegado).
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (cita.estado) {
                    Cita.ESTADO_PENDIENTE -> {
                        Button(
                            onClick = onConfirmar,
                            colors = ButtonDefaults.buttonColors(containerColor = AccentButton),
                            shape = RoundedCornerShape(12.dp)
                        ) { Text("Registrar llegada", fontSize = 13.sp, color = White) }
                        TextButton(onClick = onCancelar) {
                            Text("Cancelar", fontSize = 13.sp, color = CancelRed)
                        }
                    }

                    Cita.ESTADO_CONFIRMADA -> {
                        if (puedeAtender) {
                            Button(
                                onClick = onAtender,
                                colors = ButtonDefaults.buttonColors(containerColor = AccentButton),
                                shape = RoundedCornerShape(12.dp)
                            ) { Text("Pasar a consulta", fontSize = 13.sp, color = White) }
                        }
                        TextButton(onClick = onAsignar) {
                            Text("Asignar doctor", fontSize = 13.sp, color = TextColor)
                        }
                    }

                    Cita.ESTADO_EN_ATENCION -> {
                        if (puedeAtender) {
                            Button(
                                onClick = onCompletar,
                                colors = ButtonDefaults.buttonColors(containerColor = AccentButton),
                                shape = RoundedCornerShape(12.dp)
                            ) { Text("Completar", fontSize = 13.sp, color = White) }
                        } else {
                            Text(
                                "En consulta con ${cita.doctorNombre.ifBlank { "el doctor" }}",
                                fontSize = 13.sp,
                                color = TextColor.copy(alpha = 0.6f)
                            )
                        }
                    }

                    else -> Text(
                        "Sin acciones pendientes",
                        fontSize = 13.sp,
                        color = TextColor.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

/** Punto de color con el estado. Se lee mas rapido que un texto suelto. */
@Composable
private fun EtiquetaEstado(estado: String) {
    val color = when (estado) {
        Cita.ESTADO_PENDIENTE -> Color(0xFFE8A33D)
        Cita.ESTADO_CONFIRMADA -> Color(0xFF4A90A4)
        Cita.ESTADO_EN_ATENCION -> Color(0xFF7B68A6)
        Cita.ESTADO_COMPLETADA -> Color(0xFF5B9A6B)
        else -> GrayHint
    }
    Surface(shape = RoundedCornerShape(50.dp), color = color.copy(alpha = 0.15f)) {
        Text(
            estado,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun DialogoAsignarDoctor(
    doctores: List<UsuarioConRol>,
    onCerrar: () -> Unit,
    onElegir: (UsuarioConRol) -> Unit
) {
    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Asignar doctor", fontWeight = FontWeight.Bold) },
        text = {
            if (doctores.isEmpty()) {
                Text(
                    "No hay ningún usuario con rol de Doctor todavía. " +
                        "Un administrador puede asignar ese rol desde la pestaña Usuarios."
                )
            } else {
                Column {
                    doctores.forEach { doctor ->
                        TextButton(
                            onClick = { onElegir(doctor) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                doctor.nombreVisible,
                                color = TextColor,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onCerrar) { Text("Cerrar") } }
    )
}
