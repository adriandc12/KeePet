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

    // Si guardar un cambio falla (sin internet, permiso denegado...) el usuario lo ve
    // aqui mismo. Antes este aviso solo estaba montado en HomeScreen/StaffHomeScreen,
    // asi que un fallo al editar desde ESTA pantalla (el expediente) no se notaba: no
    // se guardaba el cambio y tampoco aparecia ningun error.
    LaunchedEffect(viewModel.mensaje) {
        viewModel.mensaje?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.limpiarMensaje()
        }
    }

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

