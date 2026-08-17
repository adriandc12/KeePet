package com.example.keepet.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import coil.compose.AsyncImage
import com.example.keepet.data.model.Notificacion
import com.example.keepet.ui.components.avatarPorEspecie
import com.example.keepet.ui.theme.*
import com.example.keepet.viewmodel.PetViewModel

/**
 * Los avisos ya no salen de una lista inventada dentro del repositorio: se
 * calculan a partir de las citas reales (mira PetViewModel.notificaciones).
 * Por eso ahora esta pantalla necesita el ViewModel.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(viewModel: PetViewModel, onBack: () -> Unit) {
    val notifications by viewModel.notificaciones.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Notificaciones",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextColor
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás", tint = TextColor)
                    }
                },
                // centerAlignedTopAppBarColors quedo obsoleto; el compilador ya
                // avisaba de ello. topAppBarColors hace exactamente lo mismo.
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundColor
                )
            )
        },
        containerColor = BackgroundColor
    ) { padding ->
        if (notifications.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.NotificationsNone,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = GrayHint.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No tienes notificaciones",
                        color = TextColor.copy(alpha = 0.6f),
                        fontSize = 16.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(notifications, key = { it.id }) { notification ->
                    NotificationCard(
                        notification = notification,
                        onDismiss = { viewModel.descartarNotificacion(notification.id) },
                        onViewAppointment = onBack
                    )
                }
            }
        }
    }
}

@Composable
fun NotificationCard(
    notification: Notificacion,
    onDismiss: () -> Unit,
    onViewAppointment: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(InputBackground)
                ) {
                    if (notification.imagenUri != null) {
                        AsyncImage(
                            model = notification.imagenUri,
                            contentDescription = notification.nombreMascota,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else if (notification.especie.isNotBlank()) {
                        Image(
                            painter = painterResource(id = avatarPorEspecie(notification.especie)),
                            contentDescription = notification.nombreMascota,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            Icons.Default.Pets,
                            contentDescription = null,
                            modifier = Modifier.align(Alignment.Center).size(24.dp),
                            tint = TextColor.copy(alpha = 0.2f)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${notification.nombreMascota} - ${notification.tipoServicio}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = TextColor
                    )
                    Text(
                        text = "${notification.fecha} a las ${notification.hora}",
                        fontSize = 13.sp,
                        color = TextColor.copy(alpha = 0.6f)
                    )
                    Text(
                        text = notification.aviso,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = AccentButton
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Descartar", color = GrayHint)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onViewAppointment,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentButton),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("Ver Cita", fontSize = 13.sp)
                }
            }
        }
    }
}
