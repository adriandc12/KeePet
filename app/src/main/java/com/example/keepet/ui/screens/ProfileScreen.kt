@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.keepet.ui.screens

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.HelpCenter
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.keepet.R
import com.example.keepet.data.model.Usuario
import com.example.keepet.ui.components.NOMBRES_AVATAR
import com.example.keepet.ui.components.avatarPorNombre
import com.example.keepet.ui.components.createImageUri
import com.example.keepet.ui.theme.*
import com.example.keepet.viewmodel.PetViewModel

/**
 * Antes leia el usuario de "PetRepository.usuario", una variable global dentro de
 * un companion object. Eso significaba que los datos del perfil vivian en memoria:
 * se perdian al cerrar la app y no habia forma de que llegaran a la nube.
 * Ahora vienen del ViewModel, que los lee y los escribe en Firebase.
 */
@Composable
fun ProfileScreen(viewModel: PetViewModel, onLogout: () -> Unit) {
    val usuario by viewModel.usuario.collectAsState()
    val context = LocalContext.current
    
    var showEditDataDialog by remember { mutableStateOf(false) }
    var showSecurityDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showLogoutConfirm by remember { mutableStateOf(false) }
    var showPhotoPicker by remember { mutableStateOf(false) }

    // Logic for Image Selection (Reuse from PetDetailScreen logic style)
    var capturedImageUri by remember { mutableStateOf<Uri>(Uri.EMPTY) }
    
    // La foto de perfil tambien va a Cloudinary, no a una ruta del telefono.
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.subirFotoPerfil(it) }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            viewModel.subirFotoPerfil(capturedImageUri)
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
        }
    }

    // Dialogs
    if (showEditDataDialog) {
        EditProfileDialog(
            usuario = usuario,
            onDismiss = { showEditDataDialog = false },
            onConfirm = { nuevoUsuario ->
                viewModel.updateUsuario(nuevoUsuario)
                showEditDataDialog = false
            }
        )
    }

    if (showSecurityDialog) {
        SecurityDialog(onDismiss = { showSecurityDialog = false })
    }

    if (showHelpDialog) {
        HelpCenterDialog(onDismiss = { showHelpDialog = false })
    }

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text("Cerrar sesión", fontWeight = FontWeight.Bold) },
            text = { Text("¿Estás seguro de que deseas cerrar sesión?") },
            confirmButton = {
                Button(
                    onClick = { 
                        showLogoutConfirm = false
                        onLogout() 
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CancelRed)
                ) { Text("Cerrar Sesión", color = White) }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) { Text("Cancelar") }
            }
        )
    }

    if (showPhotoPicker) {
        PhotoPickerDialog(
            onDismiss = { showPhotoPicker = false },
            onSelectAvatar = { nombreAvatar ->
                viewModel.updateUsuario(usuario.copy(avatar = nombreAvatar, imagenUri = null))
                showPhotoPicker = false
            },
            onCamera = {
                permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA))
                showPhotoPicker = false
            },
            onGallery = {
                galleryLauncher.launch("image/*")
                showPhotoPicker = false
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
            .verticalScroll(rememberScrollState())
    ) {
        // Header con foto
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp, bottom = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier.clickable { showPhotoPicker = true },
                    contentAlignment = Alignment.BottomEnd
                ) {
                    if (usuario.imagenUri != null) {
                        AsyncImage(
                            model = usuario.imagenUri,
                            contentDescription = "Perfil",
                            modifier = Modifier
                                .size(150.dp)
                                .clip(CircleShape)
                                .background(White),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Image(
                            painter = painterResource(id = avatarPorNombre(usuario.avatar)),
                            contentDescription = "Perfil",
                            modifier = Modifier
                                .size(150.dp)
                                .clip(CircleShape)
                                .background(White),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Surface(
                        shape = CircleShape,
                        color = AccentButton,
                        shadowElevation = 4.dp,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = null,
                            modifier = Modifier.padding(8.dp),
                            tint = White
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = usuario.nombre,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextColor
                )
                // Decia "Médico Veterinario" fijo, un resto de cuando esta pantalla no
                // distinguia clientes de personal. ProfileScreen la usa solo el cliente
                // (recibe PetViewModel, no GestorDeMascotas), asi que ese texto era
                // incorrecto para cualquiera que lo viera.
                Text(
                    text = "Cliente de la clínica",
                    fontSize = 14.sp,
                    color = TextColor.copy(alpha = 0.6f)
                )
            }
        }

        // Secciones
        Column(
            modifier = Modifier.padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Card de Mis Datos
            ProfileCard(
                title = "Mis datos personales",
                icon = Icons.Default.ContactPage,
                onEditClick = { showEditDataDialog = true }
            ) {
                ProfileDataItem(Icons.Default.Person, "Nombre completo", usuario.nombre)
                ProfileDataItem(Icons.Default.Phone, "Teléfono", usuario.telefono)
                ProfileDataItem(Icons.Default.LocationOn, "Dirección", usuario.direccion)
                ProfileDataItem(Icons.Default.Email, "Correo electrónico", usuario.correo)
            }

            // Card de Configuración y Seguridad
            ProfileCard(
                title = "Configuración y seguridad",
                icon = Icons.Default.Settings
            ) {
                ProfileOptionItem(Icons.Default.ManageAccounts, "Configuración de la cuenta") {
                    showEditDataDialog = true
                }
                ProfileOptionItem(Icons.Default.Shield, "Seguridad y Contraseña") {
                    showSecurityDialog = true
                }
                ProfileOptionItem(Icons.AutoMirrored.Filled.HelpCenter, "Centro de ayuda") {
                    showHelpDialog = true
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = GrayHint.copy(alpha = 0.2f))
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showLogoutConfirm = true }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Logout, 
                        contentDescription = null, 
                        tint = CancelRed, 
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Cerrar sesión", color = CancelRed, fontWeight = FontWeight.Bold)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun ProfileCard(
    title: String,
    icon: ImageVector,
    onEditClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, contentDescription = null, tint = AccentButton, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = TextColor)
                }
                if (onEditClick != null) {
                    IconButton(onClick = onEditClick, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar", tint = GrayHint, modifier = Modifier.size(18.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
fun ProfileDataItem(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.padding(bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(InputBackground.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = TextColor, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(label, fontSize = 11.sp, color = TextColor.copy(alpha = 0.5f))
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextColor)
        }
    }
}

@Composable
fun ProfileOptionItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = TextColor.copy(alpha = 0.7f), modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text(label, color = TextColor, fontSize = 15.sp)
        }
        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = GrayHint, modifier = Modifier.size(18.dp))
    }
}

@Composable
fun EditProfileDialog(usuario: Usuario, onDismiss: () -> Unit, onConfirm: (Usuario) -> Unit) {
    var nombre by remember { mutableStateOf(usuario.nombre) }
    var telefono by remember { mutableStateOf(usuario.telefono) }
    var direccion by remember { mutableStateOf(usuario.direccion) }
    var correo by remember { mutableStateOf(usuario.correo) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Mis Datos", fontWeight = FontWeight.Bold, color = TextColor) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = nombre, 
                    onValueChange = { nombre = it }, 
                    label = { Text("Nombre completo") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextColor,
                        unfocusedTextColor = TextColor,
                        focusedLabelColor = TextColor,
                        unfocusedLabelColor = GrayHint,
                        focusedBorderColor = AccentButton,
                        unfocusedBorderColor = GrayHint.copy(alpha = 0.5f)
                    )
                )
                OutlinedTextField(
                    value = telefono,
                    // Solo numeros (y los signos que se usan al escribir un telefono).
                    // Este es el numero por el que la clinica llama al dueño si le pasa
                    // algo a su mascota: con letras dentro no sirve para nada.
                    onValueChange = { nuevo -> telefono = nuevo.filter { it.isDigit() || it in "+- " } },
                    label = { Text("Teléfono") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextColor,
                        unfocusedTextColor = TextColor,
                        focusedLabelColor = TextColor,
                        unfocusedLabelColor = GrayHint,
                        focusedBorderColor = AccentButton,
                        unfocusedBorderColor = GrayHint.copy(alpha = 0.5f)
                    )
                )
                OutlinedTextField(
                    value = direccion, 
                    onValueChange = { direccion = it }, 
                    label = { Text("Dirección") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextColor,
                        unfocusedTextColor = TextColor,
                        focusedLabelColor = TextColor,
                        unfocusedLabelColor = GrayHint,
                        focusedBorderColor = AccentButton,
                        unfocusedBorderColor = GrayHint.copy(alpha = 0.5f)
                    )
                )
                OutlinedTextField(
                    value = correo, 
                    onValueChange = { correo = it }, 
                    label = { Text("Correo electrónico") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextColor,
                        unfocusedTextColor = TextColor,
                        focusedLabelColor = TextColor,
                        unfocusedLabelColor = GrayHint,
                        focusedBorderColor = AccentButton,
                        unfocusedBorderColor = GrayHint.copy(alpha = 0.5f)
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        usuario.copy(
                            nombre = nombre.trim(),
                            telefono = telefono.trim(),
                            direccion = direccion.trim(),
                            correo = correo.trim()
                        )
                    )
                },
                // Sin nombre, en la agenda de la clinica la cita aparece sin dueño y en la
                // lista de usuarios sale como "Sin nombre". Es el unico dato que se exige.
                enabled = nombre.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = AccentButton),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Guardar Cambios", color = White) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = GrayHint) }
        },
        containerColor = White,
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun SecurityDialog(onDismiss: () -> Unit) {
    var currentPass by remember { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }
    var confirmPass by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cambiar Contraseña", fontWeight = FontWeight.Bold, color = TextColor) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Asegúrate de usar una contraseña segura.", fontSize = 13.sp, color = GrayHint)
                OutlinedTextField(
                    value = currentPass, 
                    onValueChange = { currentPass = it }, 
                    label = { Text("Contraseña actual") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextColor,
                        unfocusedTextColor = TextColor,
                        focusedLabelColor = TextColor,
                        unfocusedLabelColor = GrayHint,
                        focusedBorderColor = AccentButton,
                        unfocusedBorderColor = GrayHint.copy(alpha = 0.5f)
                    )
                )
                OutlinedTextField(
                    value = newPass, 
                    onValueChange = { newPass = it }, 
                    label = { Text("Nueva contraseña") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextColor,
                        unfocusedTextColor = TextColor,
                        focusedLabelColor = TextColor,
                        unfocusedLabelColor = GrayHint,
                        focusedBorderColor = AccentButton,
                        unfocusedBorderColor = GrayHint.copy(alpha = 0.5f)
                    )
                )
                OutlinedTextField(
                    value = confirmPass, 
                    onValueChange = { confirmPass = it }, 
                    label = { Text("Confirmar nueva contraseña") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextColor,
                        unfocusedTextColor = TextColor,
                        focusedLabelColor = TextColor,
                        unfocusedLabelColor = GrayHint,
                        focusedBorderColor = AccentButton,
                        unfocusedBorderColor = GrayHint.copy(alpha = 0.5f)
                    )
                )
                if (error.isNotEmpty()) {
                    Text(error, color = CancelRed, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    if (newPass != confirmPass) {
                        error = "Las contraseñas no coinciden"
                    } else if (newPass.length < 6) {
                        error = "La contraseña debe tener al menos 6 caracteres"
                    } else {
                        onDismiss() 
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentButton),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Actualizar", color = White) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = GrayHint) }
        },
        containerColor = White,
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun HelpCenterDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Centro de Ayuda", fontWeight = FontWeight.Bold, color = TextColor) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                HelpItem("¿Cómo agendar una cita?", "Ve a la sección 'Agendar' en la barra inferior, selecciona la mascota, el servicio y la fecha.")
                // Antes decia "En 'Inicio' o 'Expedientes'". La pestaña Expedientes ya no
                // esta en la app del cliente (la gestiona la clinica), asi que se quita
                // de la ayuda para no mandar al usuario a un sitio que no existe.
                HelpItem("¿Cómo editar un expediente?", "En 'Inicio', presiona el ícono de lápiz en la tarjeta de la mascota.")
                HelpItem("Soporte Técnico", "Si tienes problemas, contáctanos a: soporte@keepet.com o al +506 8888-8888.")
                Spacer(modifier = Modifier.height(8.dp))
                Text("Versión de la app: 1.0.2 (Beta)", fontSize = 11.sp, color = GrayHint)
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = AccentButton)) {
                Text("Entendido", color = White)
            }
        },
        containerColor = White,
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun HelpItem(q: String, a: String) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(q, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextColor)
        Text(a, fontSize = 13.sp, color = TextColor.copy(alpha = 0.7f))
    }
}

@Composable
fun PhotoPickerDialog(
    onDismiss: () -> Unit, 
    onSelectAvatar: (String) -> Unit,
    onCamera: () -> Unit,
    onGallery: () -> Unit
) {
    // Se recorren NOMBRES ("perfil", "perro"...), no numeros de R.drawable, porque
    // lo que se guarda en la nube es el nombre. Mira ui/components/Avatares.kt.
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Foto de Perfil", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    IconButton(onClick = onCamera) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = "Cámara")
                            Text("Cámara", fontSize = 10.sp)
                        }
                    }
                    IconButton(onClick = onGallery) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = "Galería")
                            Text("Galería", fontSize = 10.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("O elige un avatar:", fontSize = 14.sp, color = GrayHint)
                Spacer(modifier = Modifier.height(12.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.height(80.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(NOMBRES_AVATAR) { nombreAvatar ->
                        Image(
                            painter = painterResource(id = avatarPorNombre(nombreAvatar)),
                            contentDescription = nombreAvatar,
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .clickable { onSelectAvatar(nombreAvatar) },
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        }
    )
}
