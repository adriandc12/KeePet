@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.example.keepet.ui.screens

import android.Manifest
import android.content.Context
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.keepet.data.model.Mascota
import com.example.keepet.viewmodel.GestorDeMascotas
import com.example.keepet.data.model.RegistroHistorial
import com.example.keepet.ui.components.avatarPorEspecie
import com.example.keepet.ui.components.createImageUri
import com.example.keepet.ui.theme.*
import com.example.keepet.viewmodel.fechaDeHoyTexto
import java.io.File
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * Ficha completa de una mascota: foto, datos, alergias, notas medicas e historial.
 *
 * Recibe GestorDeMascotas y no PetViewModel por el mismo motivo que AddPetScreen: asi
 * el personal de la clinica abre esta misma ficha sobre la mascota de un cliente.
 *
 * LA MISMA PANTALLA SIRVE PARA LEER Y PARA ESCRIBIR. Los botones de la parte medica
 * (añadir registro, borrar una alergia, editar las notas) solo aparecen si quien la abre
 * puede escribir ahi: mira GestorDeMascotas.puedeEditarDatosClinicos. El cliente ve
 * exactamente la misma ficha de su mascota, con la misma informacion, pero de lectura.
 *
 * onSchedule es NULO cuando la ficha se abre desde la clinica. Ese boton llevaba a
 * "agendar cita" del cliente, que desde el lado del personal no existe: antes se le
 * pasaba una funcion vacia y el boton estaba ahi sin hacer absolutamente nada al
 * pulsarlo. Ahora, si no hay a donde ir, el boton no se dibuja.
 */
@Composable
fun PetDetailScreen(
    petId: String,
    viewModel: GestorDeMascotas,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    onSchedule: ((String) -> Unit)? = null
) {
    val puedeEditarClinica = viewModel.puedeEditarDatosClinicos

    // Antes la mascota se leia UNA sola vez (LaunchedEffect + getPetById) y se
    // guardaba en una variable local. El problema: al editar las notas o añadir un
    // registro al historial, el dato cambiaba en la base de datos pero esta
    // pantalla seguia mostrando la copia vieja hasta salir y volver a entrar.
    //
    // Ahora se toma de la lista en tiempo real, asi que cualquier cambio se
    // refleja aqui al instante y no hay dos copias del mismo dato conviviendo.
    val mascotas by viewModel.allPets.collectAsState()
    val currentPet = mascotas.find { it.id == petId } ?: return

    val context = LocalContext.current
    var showAddRecordDialog by remember { mutableStateOf(false) }
    var recordToEdit by remember { mutableStateOf<RegistroHistorial?>(null) }
    var recordToShowDetails by remember { mutableStateOf<RegistroHistorial?>(null) }
    var showPhotoPicker by remember { mutableStateOf(false) }
    var showEditPetDialog by remember { mutableStateOf(false) }
    var showEditOwnerDialog by remember { mutableStateOf(false) }
    var showEditNotesDialog by remember { mutableStateOf(false) }

    // Logic for Image Selection
    var capturedImageUri by remember { mutableStateOf<Uri>(Uri.EMPTY) }
    
    // Antes estas dos guardaban la ruta local (file:///...) directamente en la
    // mascota. Ahora la foto se SUBE a Cloudinary y lo que se guarda en Realtime
    // Database es la URL que devuelve, para que se vea desde cualquier telefono y
    // desde la ficha que abre el doctor en la clinica.
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.subirFotoMascota(currentPet.id, it) }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            viewModel.subirFotoMascota(currentPet.id, capturedImageUri)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
        if (cameraGranted) {
            val uri = createImageUri(context)
            capturedImageUri = uri
            cameraLauncher.launch(uri)
        } else {
            Toast.makeText(context, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
        }
    }

    if (showPhotoPicker) {
        PhotoSelectionDialog(
            onDismiss = { showPhotoPicker = false },
            onGalleryClick = {
                showPhotoPicker = false
                galleryLauncher.launch("image/*")
            },
            onCameraClick = {
                showPhotoPicker = false
                val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    arrayOf(Manifest.permission.CAMERA, Manifest.permission.READ_MEDIA_IMAGES)
                } else {
                    arrayOf(Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE)
                }
                permissionLauncher.launch(permissions)
            }
        )
    }

    if (showEditPetDialog) {
        EditPetDialog(
            pet = currentPet,
            onDismiss = { showEditPetDialog = false },
            onConfirm = { updatedPet ->
                viewModel.updatePet(updatedPet)
                showEditPetDialog = false
            }
        )
    }

    if (showEditOwnerDialog) {
        EditOwnerDialog(
            pet = currentPet,
            onDismiss = { showEditOwnerDialog = false },
            onConfirm = { updatedPet ->
                viewModel.updatePet(updatedPet)
                showEditOwnerDialog = false
            }
        )
    }

    if (showAddRecordDialog || recordToEdit != null) {
        AddMedicalRecordDialog(
            record = recordToEdit,
            currentNotes = currentPet.notasMedicas,
            onDismiss = { 
                showAddRecordDialog = false
                recordToEdit = null
            },
            onConfirm = { record, newAllergy, updatedNotes ->
                if (recordToEdit != null) {
                    viewModel.updateMedicalRecord(petId, record)
                } else {
                    val nextId = (currentPet.historial.maxOfOrNull { it.id } ?: 0) + 1
                    viewModel.addMedicalRecord(petId, record.copy(id = nextId), newAllergy)
                }
                if (updatedNotes != currentPet.notasMedicas) {
                    viewModel.updateMedicalNotes(petId, updatedNotes)
                }
                showAddRecordDialog = false
                recordToEdit = null
            }
        )
    }

    if (showEditNotesDialog) {
        EditNotesDialog(
            currentNotes = currentPet.notasMedicas,
            onDismiss = { showEditNotesDialog = false },
            onConfirm = { updatedNotes ->
                viewModel.updateMedicalNotes(petId, updatedNotes)
                showEditNotesDialog = false
            }
        )
    }

    if (recordToShowDetails != null) {
        MedicalRecordDetailDialog(
            record = recordToShowDetails!!,
            puedeEditar = puedeEditarClinica,
            onDismiss = { recordToShowDetails = null },
            onEdit = { 
                recordToEdit = recordToShowDetails
                recordToShowDetails = null
            },
            onDelete = {
                viewModel.deleteMedicalRecord(petId, recordToShowDetails!!.id)
                recordToShowDetails = null
            }
        )
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás", tint = TextColor)
                }
                Text(
                    text = "Expediente",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextColor
                )
                IconButton(onClick = { onEdit(currentPet.id) }) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar todo", tint = TextColor)
                }
            }
        },
        containerColor = BackgroundColor
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Perfil Principal
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        Surface(
                            shape = CircleShape,
                            border = androidx.compose.foundation.BorderStroke(4.dp, White),
                            shadowElevation = 4.dp
                        ) {
                            if (currentPet.imagenUri != null) {
                                AsyncImage(
                                    model = currentPet.imagenUri,
                                    contentDescription = currentPet.nombre,
                                    modifier = Modifier
                                        .size(140.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Image(
                                    painter = painterResource(id = avatarPorEspecie(currentPet.especie)),
                                    contentDescription = currentPet.nombre,
                                    modifier = Modifier
                                        .size(140.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                        // Botón Editar Foto (Estilo imagen)
                        Surface(
                            shape = CircleShape,
                            color = AccentButton,
                            modifier = Modifier
                                .size(40.dp)
                                .offset(x = (-5).dp, y = (-5).dp)
                                .clickable { showPhotoPicker = true },
                            shadowElevation = 6.dp
                        ) {
                            Icon(
                                Icons.Default.AddPhotoAlternate,
                                contentDescription = "Cambiar foto",
                                modifier = Modifier.padding(8.dp),
                                tint = White
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(currentPet.nombre, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextColor)
                    Text("${currentPet.raza} | ${currentPet.edad}", fontSize = 16.sp, color = TextColor.copy(alpha = 0.6f))
                    
                    // Solo cuando hay a donde ir (app del cliente). Y en el color de la
                    // app, no en rojo: "Agendar cita" no es una accion peligrosa. Antes
                    // los dos colores eran el mismo salmon y no se notaba; ahora que
                    // CancelRed es un rojo de verdad, este boton parecia un "Eliminar".
                    if (onSchedule != null) {
                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = { onSchedule(currentPet.id) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentButton),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Text(
                                "Agendar cita",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = White
                            )
                        }
                    }
                }
            }

            // Datos de la mascota
            item {
                InfoCard(
                    title = "Datos de la mascota", 
                    icon = Icons.Default.Info,
                    onEditClick = { showEditPetDialog = true }
                ) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        InfoItem("Nombre", currentPet.nombre, Modifier.weight(1f))
                        InfoItem("Raza", currentPet.raza, Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        InfoItem("Edad", currentPet.edad, Modifier.weight(1f))
                        InfoItem("Peso", currentPet.peso.ifEmpty { "N/A" }, Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        InfoItem("Especie", currentPet.especie, Modifier.weight(1f))
                        InfoItem("Sexo", currentPet.sexo.ifEmpty { "N/A" }, Modifier.weight(1f))
                    }
                }
            }

            // Datos del dueño
            item {
                InfoCard(
                    title = "Datos del dueño", 
                    icon = Icons.Default.Person,
                    onEditClick = { showEditOwnerDialog = true }
                ) {
                    InfoRow(Icons.Default.PersonOutline, "Nombre", currentPet.dueno)
                    Spacer(modifier = Modifier.height(12.dp))
                    InfoRow(Icons.Default.Phone, "Teléfono", currentPet.telefono)
                    Spacer(modifier = Modifier.height(12.dp))
                    InfoRow(Icons.Default.LocationOn, "Dirección", currentPet.direccion)
                    Spacer(modifier = Modifier.height(12.dp))
                    InfoRow(Icons.Default.Email, "Correo", currentPet.correo.ifEmpty { "N/A" })
                }
            }

            // Información Médica
            item {
                InfoCard(title = "Información Médica", icon = Icons.Default.MedicalServices) {
                    Text("ALERGIAS", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = TextColor)
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (currentPet.alergias.isEmpty()) {
                            Text("Ninguna registrada", fontSize = 13.sp, color = GrayHint)
                        } else {
                            currentPet.alergias.forEach { alergia ->
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = Color(0xFFEBC8B2).copy(alpha = 0.4f)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        // Sin la X de borrar, el hueco de la derecha se
                                        // iguala al de la izquierda para que la etiqueta
                                        // no quede descentrada.
                                        modifier = Modifier.padding(
                                            start = 12.dp,
                                            end = if (puedeEditarClinica) 6.dp else 12.dp,
                                            top = 4.dp,
                                            bottom = 4.dp
                                        )
                                    ) {
                                        Text(
                                            alergia, 
                                            fontSize = 12.sp, 
                                            color = TextColor,
                                            fontWeight = FontWeight.Medium
                                        )
                                        // La X de borrar solo para quien lleva el
                                        // historial. Una alergia la quita el veterinario
                                        // que la descarto, no el dueño.
                                        if (puedeEditarClinica) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Eliminar alergia",
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .clickable { viewModel.deleteAllergy(petId, alergia) },
                                                tint = TextColor.copy(alpha = 0.6f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("NOTAS MÉDICAS", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = TextColor)
                        if (puedeEditarClinica) {
                            IconButton(onClick = { showEditNotesDialog = true }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Edit, contentDescription = "Editar notas", tint = TextColor.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFE0F2F1), RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Text(
                            currentPet.notasMedicas.ifEmpty { "Sin notas adicionales." }, 
                            fontSize = 13.sp, 
                            color = Color(0xFF00695C),
                            lineHeight = 18.sp
                        )
                    }
                    
                    // Escribir en el historial es cosa del veterinario. En el color de la
                    // app y no en rojo, por lo mismo que el boton de agendar: añadir un
                    // registro no destruye nada.
                    if (puedeEditarClinica) {
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = { showAddRecordDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentButton),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                "Añadir Registro Médico",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = White
                            )
                        }
                    }
                }
            }

            // Historial
            item {
                InfoCard(
                    title = "Historial",
                    icon = Icons.Default.History,
                    // El lapiz de esta tarjeta sirve para añadir un registro, asi que
                    // desaparece para quien no puede escribir en el historial. InfoCard ya
                    // sabe no dibujarlo cuando le llega null.
                    onEditClick = if (puedeEditarClinica) {
                        { showAddRecordDialog = true }
                    } else null
                ) {
                    val sortedHistorial = currentPet.historial.sortedByDescending { it.id }
                    
                    if (sortedHistorial.isEmpty()) {
                        Text("No hay registros previos.", fontSize = 13.sp, color = GrayHint, modifier = Modifier.padding(vertical = 8.dp))
                    } else {
                        sortedHistorial.forEachIndexed { index, registro ->
                            HistorialItem(
                                registro = registro,
                                onClick = { recordToShowDetails = registro }
                            )
                            if (index < sortedHistorial.size - 1) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = GrayHint.copy(alpha = 0.1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InfoCard(
    title: String, 
    icon: androidx.compose.ui.graphics.vector.ImageVector, 
    onEditClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(InputBackground.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, contentDescription = null, tint = TextColor, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = TextColor)
                }
                if (onEditClick != null) {
                    IconButton(onClick = onEditClick, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar sección", tint = TextColor.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            content()
        }
    }
}

@Composable
fun InfoItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, fontSize = 12.sp, color = TextColor.copy(alpha = 0.5f), fontWeight = FontWeight.Medium)
        Text(value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextColor)
    }
}

@Composable
fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = TextColor.copy(alpha = 0.4f), modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(label, fontSize = 11.sp, color = TextColor.copy(alpha = 0.5f))
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextColor)
        }
    }
}

@Composable
fun HistorialItem(
    registro: com.example.keepet.data.model.RegistroHistorial,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(Color(0xFFF5F5F5), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            val icon = when(registro.tipoIcono) {
                "Bano" -> Icons.Default.WaterDrop
                "Vacuna" -> Icons.Default.Vaccines
                "Consulta" -> Icons.Default.MedicalInformation
                else -> Icons.Default.ContentCut
            }
            Icon(icon, contentDescription = null, tint = AccentButton, modifier = Modifier.size(22.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(registro.servicio, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextColor)
            Text(
                registro.fecha, 
                fontSize = 12.sp, 
                color = TextColor.copy(alpha = 0.6f)
            )
        }
        Icon(
            Icons.Default.ChevronRight, 
            contentDescription = "Ver detalles", 
            tint = GrayHint, 
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun MedicalRecordDetailDialog(
    record: com.example.keepet.data.model.RegistroHistorial,
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
    record: com.example.keepet.data.model.RegistroHistorial? = null,
    currentNotes: String = "",
    onDismiss: () -> Unit,
    onConfirm: (com.example.keepet.data.model.RegistroHistorial, String, String) -> Unit
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
                                        com.example.keepet.data.model.RegistroHistorial(
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
    pet: com.example.keepet.data.model.Mascota,
    onDismiss: () -> Unit,
    onConfirm: (com.example.keepet.data.model.Mascota) -> Unit
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
    pet: com.example.keepet.data.model.Mascota,
    onDismiss: () -> Unit,
    onConfirm: (com.example.keepet.data.model.Mascota) -> Unit
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

// createImageUri estaba definida aqui y ProfileScreen la usaba desde esta pantalla,
// lo que ataba una pantalla a otra sin motivo. Ahora vive en ui/components/Avatares.kt.
