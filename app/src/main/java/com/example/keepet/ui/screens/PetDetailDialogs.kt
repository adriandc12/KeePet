@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.keepet.ui.screens

// Los dialogos de PetDetailScreen (ver detalle/editar un registro medico, editar
// mascota, editar dueño, elegir foto) vivian todos dentro de PetDetailScreen.kt, que
// llego a mas de 1300 lineas. Se movieron aqui, AL MISMO PAQUETE (ui.screens), asi
// que no cambia nada de como se llaman: PetDetailScreen.kt los sigue usando tal cual,
// sin ningun import nuevo. Es solo ordenar el archivo, ninguna logica cambio.

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.keepet.data.model.Mascota
import com.example.keepet.data.model.RegistroHistorial
import com.example.keepet.ui.theme.*
import com.example.keepet.viewmodel.fechaDeHoyTexto
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun MedicalRecordDetailDialog(
    record: RegistroHistorial,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    // Cuando es false, el registro se lee pero no se puede editar ni borrar. Es asi para
    // el cliente: puede consultar lo que le hicieron a su mascota, no reescribirlo.
    puedeEditar: Boolean = true
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.padding(24.dp),
        content = {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = White
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(InputBackground.copy(alpha = 0.5f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                val icon = when(record.tipoIcono) {
                                    "Bano" -> Icons.Default.WaterDrop
                                    "Vacuna" -> Icons.Default.Vaccines
                                    "Consulta" -> Icons.Default.MedicalInformation
                                    else -> Icons.Default.ContentCut
                                }
                                Icon(icon, contentDescription = null, tint = AccentButton, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(record.servicio, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextColor)
                                Text(record.fecha, fontSize = 13.sp, color = TextColor.copy(alpha = 0.6f))
                            }
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Cerrar")
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text("TIPO DE VISITA", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = TextColor.copy(alpha = 0.5f))
                    Text(
                        if (record.tipoIcono == "Bano") "Baño" else record.tipoIcono,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextColor
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    if (record.recetaUri != null) {
                        Text("RECETA MÉDICA", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = TextColor.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(8.dp))
                        AsyncImage(
                            model = record.recetaUri,
                            contentDescription = "Receta Médica",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(InputBackground),
                            contentScale = ContentScale.Fit
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    Text("OBSERVACIONES Y NOTAS", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = TextColor.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(InputBackground.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Text(
                            record.detalles.ifEmpty { "Sin observaciones adicionales." },
                            fontSize = 14.sp,
                            color = TextColor,
                            lineHeight = 20.sp
                        )
                    }

                    if (puedeEditar) {
                        Spacer(modifier = Modifier.height(32.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = onDelete,
                                modifier = Modifier.weight(1f).height(44.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Red.copy(alpha = 0.1f),
                                    contentColor = Color.Red
                                ),
                                shape = RoundedCornerShape(12.dp),
                                elevation = null
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Eliminar", fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = onEdit,
                                modifier = Modifier.weight(1f).height(44.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AccentButton),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Editar", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun AddMedicalRecordDialog(
    record: RegistroHistorial? = null,
    currentNotes: String = "",
    onDismiss: () -> Unit,
    onConfirm: (RegistroHistorial, String, String) -> Unit
) {
    var servicio by remember { mutableStateOf(record?.servicio ?: "") }
    // Antes empezaba vacio y era texto libre: el doctor tenia que escribir la fecha de
    // hoy a mano, sin ningun formato fijo ("17/08", "17 de agosto", "ago 17"...), la
    // unica fecha de toda la app sin selector. Ahora se prellena con hoy (igual que
    // hace fechaDeHoyTexto() en el resto de la app) y se elige con el mismo calendario
    // que usa "Agendar cita", asi el formato queda siempre igual.
    var fecha by remember { mutableStateOf(record?.fecha?.ifBlank { null } ?: fechaDeHoyTexto()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var detalles by remember { mutableStateOf(record?.detalles ?: "") }
    var nuevaAlergia by remember { mutableStateOf("") }
    var notasMedicas by remember { mutableStateOf(currentNotes) }
    var recetaUri by remember { mutableStateOf(record?.recetaUri) }

    var tipoVisita by remember {
        mutableStateOf(
            if (record?.tipoIcono == "Bano") "Baño"
            else record?.tipoIcono ?: "Consulta"
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { recetaUri = it.toString() }
    }

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = TextColor,
        unfocusedTextColor = TextColor,
        focusedBorderColor = AccentButton,
        unfocusedBorderColor = GrayHint.copy(alpha = 0.3f),
        focusedLabelColor = AccentButton,
        unfocusedLabelColor = GrayHint
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.padding(24.dp),
        content = {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = White
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        if (record == null) "Nuevo Registro Médico" else "Editar Registro Médico",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextColor
                    )

                    OutlinedTextField(
                        value = servicio,
                        onValueChange = { servicio = it },
                        label = { Text("Motivo de consulta") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = textFieldColors
                    )
                    // De solo lectura + una capa encima que abre el calendario: es el
                    // mismo truco que usa cualquier selector de fecha en Compose, porque
                    // un OutlinedTextField de solo lectura no reacciona al toque por si
                    // solo.
                    Box {
                        OutlinedTextField(
                            value = fecha,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Fecha") },
                            trailingIcon = {
                                Icon(Icons.Default.CalendarToday, contentDescription = null)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = textFieldColors
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { showDatePicker = true }
                        )
                    }
                    OutlinedTextField(
                        value = detalles,
                        onValueChange = { detalles = it },
                        label = { Text("Observaciones") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = textFieldColors
                    )

                    if (record == null) {
                        OutlinedTextField(
                            value = nuevaAlergia,
                            onValueChange = { nuevaAlergia = it },
                            label = { Text("¿Nueva Alergia?") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = textFieldColors
                        )
                    }

                    OutlinedTextField(
                        value = notasMedicas,
                        onValueChange = { notasMedicas = it },
                        label = { Text("Notas Médicas Relevantes") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = textFieldColors,
                        minLines = 2
                    )

                    Text("Receta Médica:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextColor)
                    if (recetaUri != null) {
                        Box(contentAlignment = Alignment.TopEnd) {
                            AsyncImage(
                                model = recetaUri,
                                contentDescription = "Receta",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop
                            )
                            IconButton(onClick = { recetaUri = null }) {
                                Icon(Icons.Default.Cancel, contentDescription = "Quitar receta", tint = White)
                            }
                        }
                    } else {
                        OutlinedButton(
                            onClick = { launcher.launch("image/*") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Añadir Foto de Receta")
                        }
                    }

                    Text("Tipo de visita:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextColor)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Consulta", "Vacuna", "Baño").forEach { tipo ->
                            val isSelected = tipoVisita == tipo
                            FilterChip(
                                selected = isSelected,
                                onClick = { tipoVisita = tipo },
                                label = { Text(tipo) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AccentButton,
                                    selectedLabelColor = White,
                                    labelColor = TextColor
                                )
                            )
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = onDismiss) { Text("Cancelar", color = GrayHint) }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (servicio.isNotBlank()) {
                                    onConfirm(
                                        RegistroHistorial(
                                            id = record?.id ?: 0,
                                            servicio = servicio,
                                            fecha = fecha,
                                            detalles = detalles,
                                            tipoIcono = if (tipoVisita == "Baño") "Bano" else tipoVisita,
                                            recetaUri = recetaUri
                                        ),
                                        nuevaAlergia,
                                        notasMedicas
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentButton),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Guardar")
                        }
                    }
                }
            }
        }
    )

    if (showDatePicker) {
        // Sin restriccion de dias futuros/pasados a proposito: al contrario que agendar
        // una cita, un registro medico casi siempre es del dia (o se rellena a
        // posteriori), asi que no tiene sentido bloquear fechas pasadas aqui.
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val diaElegido = Instant.ofEpochMilli(millis)
                            .atZone(ZoneOffset.UTC)
                            .toLocalDate()
                        fecha = diaElegido.format(
                            DateTimeFormatter.ofPattern("d 'de' MMMM", Locale.forLanguageTag("es-ES"))
                        )
                    }
                    showDatePicker = false
                }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
fun EditNotesDialog(
    currentNotes: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var notes by remember { mutableStateOf(currentNotes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Notas Médicas", fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 5,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentButton,
                    focusedLabelColor = AccentButton
                )
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(notes) },
                colors = ButtonDefaults.buttonColors(containerColor = AccentButton)
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
fun EditPetDialog(
    pet: Mascota,
    onDismiss: () -> Unit,
    onConfirm: (Mascota) -> Unit
) {
    var nombre by remember { mutableStateOf(pet.nombre) }
    var especie by remember { mutableStateOf(pet.especie) }
    var raza by remember { mutableStateOf(pet.raza) }
    var edad by remember { mutableStateOf(pet.edad) }
    var peso by remember { mutableStateOf(pet.peso) }
    var sexo by remember { mutableStateOf(pet.sexo) }

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = TextColor,
        unfocusedTextColor = TextColor,
        focusedBorderColor = AccentButton,
        unfocusedBorderColor = GrayHint.copy(alpha = 0.3f),
        focusedLabelColor = AccentButton,
        unfocusedLabelColor = GrayHint
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.padding(24.dp),
        content = {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = White
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Editar Mascota", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextColor)

                    OutlinedTextField(
                        value = nombre,
                        onValueChange = { nombre = it },
                        label = { Text("Nombre") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = textFieldColors
                    )

                    Text("Especie", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextColor)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Perro", "Gato", "Conejo").forEach { esp ->
                            FilterChip(
                                selected = especie == esp,
                                onClick = { especie = esp },
                                label = { Text(esp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AccentButton,
                                    selectedLabelColor = White,
                                    labelColor = TextColor
                                )
                            )
                        }
                    }

                    OutlinedTextField(
                        value = raza,
                        onValueChange = { raza = it },
                        label = { Text("Raza") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = textFieldColors
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedTextField(
                            value = edad,
                            onValueChange = { edad = it },
                            label = { Text("Edad") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = textFieldColors
                        )
                        OutlinedTextField(
                            value = peso,
                            onValueChange = { peso = it },
                            label = { Text("Peso (kg)") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = textFieldColors
                        )
                    }

                    Text("Sexo", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextColor)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Macho", "Hembra").forEach { s ->
                            FilterChip(
                                selected = sexo == s,
                                onClick = { sexo = s },
                                label = { Text(s) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AccentButton,
                                    selectedLabelColor = White,
                                    labelColor = TextColor
                                )
                            )
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = onDismiss) { Text("Cancelar", color = GrayHint) }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                onConfirm(pet.copy(
                                    nombre = nombre,
                                    especie = especie,
                                    raza = raza,
                                    edad = edad,
                                    peso = peso,
                                    sexo = sexo
                                ))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentButton),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Guardar")
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun EditOwnerDialog(
    pet: Mascota,
    onDismiss: () -> Unit,
    onConfirm: (Mascota) -> Unit
) {
    var dueno by remember { mutableStateOf(pet.dueno) }
    var telefono by remember { mutableStateOf(pet.telefono) }
    var direccion by remember { mutableStateOf(pet.direccion) }
    var correo by remember { mutableStateOf(pet.correo) }

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = TextColor,
        unfocusedTextColor = TextColor,
        focusedBorderColor = AccentButton,
        unfocusedBorderColor = GrayHint.copy(alpha = 0.3f),
        focusedLabelColor = AccentButton,
        unfocusedLabelColor = GrayHint
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.padding(24.dp),
        content = {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = White
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Editar Datos del Dueño", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextColor)

                    OutlinedTextField(
                        value = dueno,
                        onValueChange = { dueno = it },
                        label = { Text("Nombre completo") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = textFieldColors
                    )

                    OutlinedTextField(
                        value = telefono,
                        onValueChange = { telefono = it },
                        label = { Text("Teléfono") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = textFieldColors
                    )

                    OutlinedTextField(
                        value = direccion,
                        onValueChange = { direccion = it },
                        label = { Text("Dirección") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = textFieldColors
                    )

                    OutlinedTextField(
                        value = correo,
                        onValueChange = { correo = it },
                        label = { Text("Correo electrónico") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = textFieldColors
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = onDismiss) { Text("Cancelar", color = GrayHint) }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                onConfirm(pet.copy(
                                    dueno = dueno,
                                    telefono = telefono,
                                    direccion = direccion,
                                    correo = correo
                                ))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentButton),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Guardar")
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun PhotoSelectionDialog(
    onDismiss: () -> Unit,
    onGalleryClick: () -> Unit,
    onCameraClick: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.padding(24.dp),
        content = {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = White
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Cambiar foto de perfil",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextColor
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        PhotoOptionItem(
                            icon = Icons.Default.PhotoCamera,
                            label = "Cámara",
                            onClick = onCameraClick
                        )
                        PhotoOptionItem(
                            icon = Icons.Default.PhotoLibrary,
                            label = "Galería",
                            onClick = onGalleryClick
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cancelar", color = CancelRed, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    )
}

@Composable
fun PhotoOptionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = InputBackground,
            modifier = Modifier.size(60.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.padding(15.dp),
                tint = TextColor
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextColor)
    }
}
