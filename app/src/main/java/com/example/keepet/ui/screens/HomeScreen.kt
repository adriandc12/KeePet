package com.example.keepet.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.keepet.R
import com.example.keepet.data.model.Mascota
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.keepet.data.model.Cita
import com.example.keepet.viewmodel.PetViewModel
import com.example.keepet.ui.components.CodigoQrCita
import com.example.keepet.ui.components.avatarPorEspecie
import com.example.keepet.ui.theme.*

@Composable
fun HomeScreen(
    viewModel: PetViewModel,
    onAddPetClick: () -> Unit,
    onAddAppointmentClick: () -> Unit,
    onEditPetClick: (String) -> Unit,
    onPetDetailClick: (String) -> Unit,
    onNotificationsClick: () -> Unit,
    onLogout: () -> Unit
) {
    var selectedTab by remember { mutableStateOf("Inicio") }
    val petList by viewModel.allPets.collectAsState()
    
    // Appointment Edit State
    var appointmentToEdit by remember { mutableStateOf<Cita?>(null) }

    // Cita cuyo QR se esta enseñando. El cliente lo muestra en recepcion y el
    // empleado lo escanea para registrar la llegada.
    var citaParaQr by remember { mutableStateOf<Cita?>(null) }
    
    // Search and Filter State
    var searchQuery by remember { mutableStateOf("") }
    
    val appointments by viewModel.allAppointments.collectAsState()
    var selectedFilter by remember { mutableStateOf("Todos") }

    // Si al guardar en Firebase algo falla (sin internet, reglas, sesion caducada)
    // el usuario ve un mensaje abajo en vez de que el cambio desaparezca en silencio.
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(viewModel.errorGuardado) {
        viewModel.errorGuardado?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.limpiarError()
        }
    }

    // Filtering logic
    val filteredPets = petList.filter { pet ->
        val matchesSearch = pet.nombre.contains(searchQuery, ignoreCase = true) || 
                            pet.dueno.contains(searchQuery, ignoreCase = true)
        val matchesFilter = when (selectedFilter) {
            "Todos" -> true
            "Perros" -> pet.especie == "Perro"
            "Gatos" -> pet.especie == "Gato"
            "Conejos" -> pet.especie == "Conejo"
            else -> pet.especie == selectedFilter
        }
        matchesSearch && matchesFilter
    }

    Scaffold(
        bottomBar = { 
            KeePetBottomNavigation(
                selectedTab = selectedTab,
                onTabSelected = { 
                    selectedTab = it
                }
            ) 
        },
        containerColor = BackgroundColor,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            // Sirve para APUNTAR UNA MASCOTA, no para abrir un expediente clinico.
            //
            // Este boton estaba en la pestaña "Expedientes", que se movio a la clinica, y
            // se quedo aqui con el nombre "Nuevo Expediente". Eso era lo que chocaba: el
            // cliente no lleva expedientes. Lo que si hace es apuntar a su mascota para
            // poder pedirle cita, y el formulario que abre ya no le pide antecedentes
            // medicos (mira AddPetScreen: para el cliente son 2 pasos, no 3).
            if (selectedTab == "Inicio") {
                FloatingActionButton(
                    onClick = onAddPetClick,
                    containerColor = AccentButton,
                    contentColor = White,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Añadir mascota")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (selectedTab != "Agendar") {
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    HeaderSection(selectedTab, onNotificationsClick)
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
            
            when (selectedTab) {
                "Inicio" -> {
                    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                        CustomSearchBar(searchQuery) { searchQuery = it }
                        Spacer(modifier = Modifier.height(20.dp))
                        FilterSection(selectedFilter) { selectedFilter = it }
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        if (filteredPets.isEmpty()) {
                            EmptyState(query = searchQuery, category = selectedFilter)
                        } else {
                        PetList(
                                pets = filteredPets,
                                onDelete = { petId ->
                                    petList.find { it.id == petId }?.let { viewModel.deletePet(it) }
                                },
                                onEdit = onEditPetClick,
                                onDetail = onPetDetailClick,
                                // El cliente puede quitar una mascota que apunto por
                                // error, pero NO una que ya tiene historial medico: eso
                                // es el archivo de la clinica y ahi decide la clinica.
                                // Si se equivoco con una que ya tiene visitas, lo pide en
                                // recepcion, que si puede borrarla.
                                puedeEliminar = { mascota -> mascota.historial.isEmpty() }
                            )
                        }
                    }
                }
                // Aqui habia una rama "Expedientes" con esta misma lista. Se movio a
                // ui/screens/staff/ExpedientesScreen.kt, que reutiliza los mismos
                // componentes (CustomSearchBar, PetList y EmptyState), asi que la vista
                // es la de siempre; lo que cambio es quien la usa.
                "Citas" -> {
                    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                        AppointmentsSection(
                            viewModel = viewModel,
                            onEditAppointment = { appointmentToEdit = it },
                            onMostrarQr = { citaParaQr = it }
                        )
                    }
                }
                "Agendar" -> {
                    AddAppointmentScreen(
                        viewModel = viewModel,
                        onBack = { selectedTab = "Inicio" },
                        onFinish = { selectedTab = "Inicio" },
                        onAddPet = onAddPetClick
                    )
                }
                "Perfil" -> {
                    // Antes el onLogout estaba vacio con un comentario "Navegar a
                    // login o cerrar": el boton de cerrar sesion no hacia nada.
                    ProfileScreen(viewModel = viewModel, onLogout = onLogout)
                }
                // Ya no hay rama "else": antes enseñaba "Sección en construcción", que era
                // texto de una app a medias. Las cuatro pestañas del menu de abajo estan
                // todas cubiertas, asi que ese caso no puede darse.
            }
        }

        if (appointmentToEdit != null) {
            EditAppointmentDialog(
                appointment = appointmentToEdit!!,
                onDismiss = { appointmentToEdit = null },
                onSave = { updated ->
                    viewModel.updateAppointment(updated)
                    appointmentToEdit = null
                }
            )
        }

        citaParaQr?.let { cita ->
            DialogoQrCita(cita = cita, onCerrar = { citaParaQr = null })
        }
    }
}

@Composable
fun HeaderSection(title: String, onNotificationsClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Pets,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = TextColor
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "KeePet",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextColor
            )
        }
        IconButton(onClick = onNotificationsClick) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = "Notificaciones",
                tint = TextColor,
                modifier = Modifier.size(28.dp)
            )
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = if (title == "Inicio") "¡Hola! 👋" else title,
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        color = TextColor
    )
    if (title == "Inicio") {
        Text(
            text = "¿Qué mascota atenderemos hoy?",
            fontSize = 14.sp,
            color = TextColor.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun CustomSearchBar(query: String, onQueryChange: (String) -> Unit) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(50.dp)),
        placeholder = { Text("Buscar mascota por nombre...", color = GrayHint) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = GrayHint) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Close, contentDescription = "Limpiar", tint = GrayHint)
                }
            }
        },
        colors = TextFieldDefaults.colors(
            focusedTextColor = TextColor,
            unfocusedTextColor = TextColor,
            cursorColor = TextColor,
            focusedContainerColor = White,
            unfocusedContainerColor = White,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        ),
        textStyle = LocalTextStyle.current.copy(
            color = TextColor,
            fontSize = 16.sp
        ),
        singleLine = true
    )
}

@Composable
fun FilterSection(selectedFilter: String, onFilterSelected: (String) -> Unit) {
    val filters = listOf("Todos", "Perros", "Gatos", "Conejos")

    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(filters) { filter ->
            val isSelected = selectedFilter == filter
            Surface(
                modifier = Modifier.clickable { onFilterSelected(filter) },
                shape = RoundedCornerShape(20.dp),
                color = if (isSelected) AccentButton else InputBackground.copy(alpha = 0.5f)
            ) {
                Text(
                    text = filter,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    color = if (isSelected) White else TextColor,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun EmptyState(query: String, category: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 50.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.SearchOff,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = GrayHint.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No se encontraron mascotas",
                fontWeight = FontWeight.Bold,
                color = TextColor
            )
            Text(
                text = if (query.isNotEmpty()) "No hay resultados para \"$query\"" 
                       else "No hay mascotas en la categoría $category",
                color = TextColor.copy(alpha = 0.6f),
                fontSize = 14.sp
            )
        }
    }
}

/**
 * La lista de mascotas. La usan el cliente (sus mascotas) y la clinica (sus pacientes).
 *
 * puedeEliminar decide, mascota a mascota, si se dibuja la papelera. La clinica no lo
 * pasa, asi que puede borrar cualquiera; el cliente solo las que aun no tienen historial
 * medico (mira HomeScreen, donde se le pasa).
 */
@Composable
fun PetList(
    pets: List<Mascota>,
    onDelete: (String) -> Unit,
    onEdit: (String) -> Unit,
    onDetail: (String) -> Unit,
    puedeEliminar: (Mascota) -> Boolean = { true }
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 20.dp)
    ) {
        items(pets) { pet ->
            PetCard(pet, onDelete, onEdit, onDetail, puedeEliminar(pet))
        }
    }
}

@Composable
fun PetCard(
    pet: Mascota,
    onDelete: (String) -> Unit,
    onEdit: (String) -> Unit,
    onDetail: (String) -> Unit,
    puedeEliminar: Boolean = true
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            // Antes solo decia "no se puede deshacer". Se dice QUE se borra: borrar una
            // mascota borra tambien su historial medico y sus citas, y eso no lo adivina
            // nadie leyendo "eliminar el expediente".
            title = { Text("¿Eliminar a ${pet.nombre}?") },
            text = {
                Text(
                    "Se borrará su ficha, su historial médico y las citas que tenga " +
                        "pedidas. No se puede deshacer."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(pet.id)
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Pet Image
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(InputBackground)
            ) {
                if (pet.imagenUri != null) {
                    AsyncImage(
                        model = pet.imagenUri,
                        contentDescription = pet.nombre,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else if (pet.especie.isNotBlank()) {
                    Image(
                        painter = painterResource(id = avatarPorEspecie(pet.especie)),
                        contentDescription = pet.nombre,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Pets,
                        contentDescription = null,
                        modifier = Modifier.align(Alignment.Center).size(40.dp),
                        tint = TextColor.copy(alpha = 0.2f)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = pet.nombre, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextColor)
                Text(text = "Raza: ${pet.raza}", fontSize = 13.sp, color = TextColor.copy(alpha = 0.8f))
                Text(text = "Edad: ${pet.edad}", fontSize = 13.sp, color = TextColor.copy(alpha = 0.8f))
                Text(text = "Dueño: ${pet.dueno}", fontSize = 13.sp, color = TextColor.copy(alpha = 0.8f))
            }

            Column(
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End,
                modifier = Modifier.height(100.dp)
            ) {
                Row {
                    IconButton(
                        onClick = { onEdit(pet.id) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar", tint = AccentButton)
                    }
                    if (puedeEliminar) {
                        IconButton(
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color.Red.copy(alpha = 0.6f))
                        }
                    }
                }
                
                // Clipboard Action Button
                IconButton(
                    onClick = { onDetail(pet.id) },
                    modifier = Modifier
                        .size(36.dp)
                        .background(InputBackground, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Assignment,
                        contentDescription = "Expediente",
                        tint = TextColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AppointmentsSection(
    viewModel: PetViewModel,
    onEditAppointment: (Cita) -> Unit,
    onMostrarQr: (Cita) -> Unit
) {
    val appointments by viewModel.allAppointments.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    val filteredAppointments = appointments.filter {
        it.nombreMascota.contains(searchQuery, ignoreCase = true)
    }

    // ORDEN DE LA LISTA. Antes salian en el orden en que Firebase las devolvia, que es
    // por su clave interna: en la practica, por orden de creacion. Con dos citas no se
    // nota; en cuanto hay unas cuantas, la cita de mañana puede quedar debajo de una de
    // hace tres meses.
    //
    // Ahora: primero lo que sigue vivo (pendiente, confirmada, en atencion), de lo mas
    // proximo a lo mas lejano, que es lo que el cliente abre la app para mirar; debajo el
    // pasado, de lo mas reciente hacia atras. claveOrden esta explicado en Cita.kt.
    val (citasAbiertas, citasCerradas) =
        filteredAppointments.partition { it.estado in Cita.ESTADOS_ACTIVOS }
    val citasOrdenadas =
        citasAbiertas.sortedBy { it.claveOrden } + citasCerradas.sortedByDescending { it.claveOrden }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Mis citas",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = TextColor
        )
        Text(
            text = "Gestiona y revisa todas las citas programadas",
            fontSize = 12.sp,
            color = TextColor.copy(alpha = 0.6f)
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        // Appointment Search Bar
        TextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(50.dp)),
            placeholder = { Text("Buscar por nombre de mascota...", color = GrayHint, fontSize = 14.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = GrayHint, modifier = Modifier.size(20.dp)) },
            colors = TextFieldDefaults.colors(
                focusedTextColor = TextColor,
                unfocusedTextColor = TextColor,
                cursorColor = TextColor,
                focusedContainerColor = White,
                unfocusedContainerColor = White,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            textStyle = LocalTextStyle.current.copy(
                color = TextColor,
                fontSize = 16.sp
            ),
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        if (citasOrdenadas.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    if (searchQuery.isEmpty()) "No tienes citas programadas" else "No se encontraron citas para \"$searchQuery\"",
                    color = GrayHint
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                items(citasOrdenadas, key = { it.id }) { appointment ->
                    AppointmentCard(
                        appointment = appointment,
                        viewModel = viewModel,
                        // Anular no borra la cita: la deja en "Cancelada" para que la
                        // clinica se entere. Lo explica PetViewModel.cancelAppointment().
                        onCancel = { viewModel.cancelAppointment(appointment) },
                        onEdit = onEditAppointment,
                        onMostrarQr = { onMostrarQr(appointment) }
                    )
                }
            }
        }
    }
}

/**
 * El QR que el cliente enseña en recepcion.
 *
 * Dentro del QR no van los datos de la cita, solo su identificador
 * ("KEEPET-CITA:{id}"). Es a proposito: si el QR llevara dentro el nombre del dueño
 * y su telefono, cualquiera que le hiciera una foto tendria esos datos. Con el id
 * solo, quien escanea necesita ademas permiso en la base de datos para ver algo.
 *
 * Debajo se enseña el codigo de 6 caracteres por si el escaner falla o el movil del
 * cliente se ha quedado sin batería: recepcion puede teclearlo a mano.
 */
@Composable
fun DialogoQrCita(cita: Cita, onCerrar: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCerrar,
        title = {
            Text("Tu cita", fontWeight = FontWeight.Bold, color = TextColor)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "${cita.nombreMascota} · ${cita.tipoServicio.ifBlank { cita.motivo }}",
                    fontSize = 14.sp,
                    color = TextColor,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${cita.fecha} a las ${cita.hora}",
                    fontSize = 13.sp,
                    color = TextColor.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(16.dp))
                CodigoQrCita(citaId = cita.id)
                Spacer(modifier = Modifier.height(16.dp))

                Text("Código", fontSize = 11.sp, color = GrayHint)
                Text(
                    cita.codigo,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentButton
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Muéstralo en recepción al llegar a la clínica.",
                    fontSize = 12.sp,
                    color = TextColor.copy(alpha = 0.6f)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onCerrar) { Text("Cerrar", color = TextColor) }
        }
    )
}

/**
 * Una cita en la lista del cliente.
 *
 * QUE SE PUEDE HACER DEPENDE DEL ESTADO, y antes no: se enseñaban siempre los mismos
 * botones, asi que se podia "editar" o "cancelar" una cita que ya estaba atendida.
 *
 *   - Pendiente : se puede cambiar y se puede anular. Todavia no ha pasado nada.
 *   - Confirmada / En atencion : el animal ya esta en la clinica. Aqui los cambios se
 *     hablan en recepcion, no se hacen desde el movil.
 *   - Completada / Cancelada : es historia, solo se lee.
 *
 * Tambien desaparecio el boton "Completar" que tenia el cliente: dar una consulta por
 * terminada (y escribirla en el historial medico) es del veterinario. Esta explicado en
 * PetViewModel.cancelAppointment().
 */
@Composable
fun AppointmentCard(
    appointment: Cita,
    viewModel: PetViewModel,
    onCancel: () -> Unit,
    onEdit: (Cita) -> Unit,
    onMostrarQr: () -> Unit
) {
    var pet by remember { mutableStateOf<Mascota?>(null) }

    LaunchedEffect(appointment.mascotaId) {
        pet = viewModel.getPetById(appointment.mascotaId)
    }

    val currentPet = pet
    val sePuedeCambiar = appointment.estado == Cita.ESTADO_PENDIENTE
    val estaEnCurso = appointment.estado in Cita.ESTADOS_ACTIVOS

    // Anular una cita es una de esas cosas que no se pueden deshacer, asi que se
    // pregunta. Antes se anulaba al primer toque, sin avisar.
    var confirmandoAnular by remember { mutableStateOf(false) }
    if (confirmandoAnular) {
        AlertDialog(
            onDismissRequest = { confirmandoAnular = false },
            title = { Text("¿Anular la cita?", fontWeight = FontWeight.Bold, color = TextColor) },
            text = {
                Text(
                    "Se avisará a la clínica de que ya no vas a llevar a " +
                        "${appointment.nombreMascota} el ${appointment.fecha} " +
                        "a las ${appointment.hora}.",
                    color = TextColor
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onCancel()
                        confirmandoAnular = false
                    }
                ) {
                    Text("Sí, anular", color = CancelRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmandoAnular = false }) {
                    Text("No, mantenerla", color = TextColor)
                }
            },
            containerColor = White
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Imagen de la mascota (grande a la izquierda)
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(InputBackground)
                ) {
                    if (currentPet?.imagenUri != null) {
                        AsyncImage(
                            model = currentPet.imagenUri,
                            contentDescription = currentPet.nombre,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else if (currentPet != null) {
                        Image(
                            painter = painterResource(id = avatarPorEspecie(currentPet.especie)),
                            contentDescription = currentPet.nombre,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            Icons.Default.Pets, 
                            contentDescription = null, 
                            modifier = Modifier.align(Alignment.Center).size(40.dp),
                            tint = TextColor.copy(alpha = 0.1f)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // Detalles de la cita
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = appointment.nombreMascota,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = TextColor
                        )
                        // Solo mientras la cita siga pendiente. Con la mascota ya en la
                        // clinica, el lapiz no tiene sentido.
                        if (sePuedeCambiar) {
                            IconButton(onClick = { onEdit(appointment) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Edit, contentDescription = "Editar", tint = AccentButton, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                    
                    Text(
                        text = appointment.tipoServicio.ifEmpty { appointment.motivo },
                        fontSize = 14.sp,
                        color = TextColor.copy(alpha = 0.7f)
                    )

                    if (appointment.notasAdicionales.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            color = AccentButton.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Nota: ${appointment.notasAdicionales}",
                                fontSize = 11.sp,
                                color = AccentButton,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                maxLines = 1
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Fecha: ${appointment.fecha}",
                        fontSize = 13.sp,
                        color = TextColor.copy(alpha = 0.8f)
                    )
                    Text(
                        text = "Hora: ${appointment.hora}",
                        fontSize = 13.sp,
                        color = TextColor.copy(alpha = 0.8f)
                    )
                    Text(
                        text = "Código: ${appointment.codigo}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentButton
                    )
                    if (appointment.estado != Cita.ESTADO_PENDIENTE) {
                        Text(
                            text = "Estado: ${appointment.estado}",
                            fontSize = 13.sp,
                            color = TextColor.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // El QR solo mientras la cita este viva: es para enseñarlo al llegar a la
            // clinica. En una cita ya completada o anulada no sirve para nada.
            if (estaEnCurso) {
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onMostrarQr,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        Icons.Default.QrCode2,
                        contentDescription = null,
                        tint = TextColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Mostrar QR de la cita",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TextColor
                    )
                }
            }

            if (sePuedeCambiar) {
                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { confirmandoAnular = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GrayHint.copy(alpha = 0.2f),
                        contentColor = TextColor
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = "Anular cita", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            } else if (estaEnCurso) {
                // Ni "cancelar" ni "editar": la mascota ya esta en la clinica. Se dice
                // que hacer en vez de dejar la tarjeta sin ninguna accion, que parece que
                // la app se ha quedado a medias.
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Tu mascota ya está en la clínica. Para cambiar o anular esta cita, " +
                        "habla con recepción.",
                    fontSize = 12.sp,
                    color = TextColor.copy(alpha = 0.6f)
                )
            }
        }
    }
}

/**
 * Cambiar el servicio o las notas de una cita que todavia esta pendiente.
 *
 * ESTE DIALOGO NO GUARDABA NADA. Tenia la fecha y la hora como texto libre y, antes de
 * guardar, comprobaba que la fecha fuera "16/08/2026" y la hora "08:00 AM". Pero las
 * citas se guardan con la fecha escrita "16 de agosto", asi que esa comprobacion nunca se
 * cumplia: se pulsaba "Guardar cambios" y no pasaba absolutamente nada, sin ningun aviso.
 * Ni siquiera se podian cambiar solo las notas, porque la fecha tumbaba la comprobacion.
 *
 * Que se hizo:
 *   - La FECHA y la HORA se enseñan pero no se tocan. Cambiarlas de verdad significa
 *     comprobar que el hueco nuevo este libre, y eso ya lo hace bien la pantalla de
 *     agendar; repetir esa logica aqui a mano es como se acaba con dos citas a la misma
 *     hora. Para moverla: se anula y se pide otra.
 *   - El SERVICIO se elige de la lista real de la clinica, no se escribe a mano. Asi el
 *     precio se queda en el que le toca; escribiendolo a mano quedaba el precio del
 *     servicio anterior pegado a un servicio distinto.
 *   - Las NOTAS se editan libremente, que es lo que de verdad se cambia ("va con la
 *     pata vendada", "llegaremos 10 minutos tarde").
 */
@Composable
fun EditAppointmentDialog(
    appointment: Cita,
    onDismiss: () -> Unit,
    onSave: (Cita) -> Unit
) {
    var service by remember { mutableStateOf(appointment.tipoServicio.ifBlank { appointment.motivo }) }
    var notes by remember { mutableStateOf(appointment.notasAdicionales) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cambiar la cita", fontWeight = FontWeight.Bold, color = TextColor) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Cuando es la cita: informativo. Se enseña para que quien esta cambiando
                // el servicio sepa de que cita se trata.
                Surface(
                    color = InputBackground.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "${appointment.nombreMascota} · ${appointment.fecha} a las ${appointment.hora}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextColor
                        )
                        Text(
                            "Para cambiar el día o la hora, anula esta cita y agenda otra: " +
                                "así la clínica ve qué horas quedan libres.",
                            fontSize = 12.sp,
                            color = TextColor.copy(alpha = 0.7f)
                        )
                    }
                }

                Text("Servicio", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextColor)

                // La misma lista que en "Agendar" (services, en AddAppointmentScreen.kt).
                // Si la cita tiene un servicio de una version anterior que ya no esta en la
                // lista, se le añade arriba para no perderlo al guardar.
                val opciones = (listOf(service) + services.map { it.name })
                    .filter { it.isNotBlank() }
                    .distinct()

                opciones.forEach { nombre ->
                    val elegido = nombre == service
                    Surface(
                        onClick = { service = nombre },
                        shape = RoundedCornerShape(12.dp),
                        color = if (elegido) AccentButton else White,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (elegido) AccentButton else GrayHint.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                nombre,
                                fontSize = 14.sp,
                                fontWeight = if (elegido) FontWeight.Bold else FontWeight.Normal,
                                color = if (elegido) White else TextColor,
                                modifier = Modifier.weight(1f)
                            )
                            services.find { it.name == nombre }?.let { s ->
                                Text(
                                    s.price,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (elegido) White else TextColor.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notas para la clínica") },
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextColor,
                        unfocusedTextColor = TextColor,
                        focusedBorderColor = AccentButton,
                        unfocusedBorderColor = GrayHint.copy(alpha = 0.5f),
                        focusedLabelColor = AccentButton,
                        unfocusedLabelColor = GrayHint
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        appointment.copy(
                            tipoServicio = service,
                            motivo = service,
                            // El precio va con el servicio. Si el servicio elegido no
                            // esta en la lista (uno viejo), se deja el que ya tenia.
                            precio = services.find { it.name == service }?.price
                                ?: appointment.precio,
                            notasAdicionales = notes.trim()
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentButton),
                enabled = service.isNotBlank()
            ) {
                Text("Guardar cambios", color = White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = TextColor)
            }
        },
        containerColor = White,
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun KeePetBottomNavigation(selectedTab: String, onTabSelected: (String) -> Unit) {
    NavigationBar(
        containerColor = White,
        tonalElevation = 8.dp
    ) {
        // "Expedientes" ya no esta aqui: se movio a la app del personal de la clinica
        // (mira ui/screens/staff/ExpedientesScreen.kt). Llevar los expedientes es
        // trabajo de la clinica, no del dueño de la mascota.
        //
        // El cliente NO se queda sin nada: en "Inicio" sigue viendo sus mascotas, con
        // las mismas tarjetas y los mismos botones que antes.
        val items = listOf(
            Triple("Inicio", Icons.Default.Home, "Inicio"),
            Triple("Citas", Icons.Default.CalendarMonth, "Citas"),
            Triple("Agendar", Icons.Default.AddCircle, "Agendar"),
            Triple("Perfil", Icons.Default.Person, "Perfil")
        )

        items.forEach { (label, icon, tabId) ->
            NavigationBarItem(
                selected = selectedTab == tabId,
                onClick = { onTabSelected(tabId) },
                icon = { Icon(icon, contentDescription = label) },
                label = { Text(label, fontSize = 10.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = AccentButton,
                    selectedTextColor = AccentButton,
                    unselectedIconColor = GrayHint,
                    unselectedTextColor = GrayHint,
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    KeePetTheme {
        // En preview solemos usar un ViewModel "mock" o simplemente no mostrarlo si requiere dependencias complejas
        // Por ahora, para que compile, necesitaría una instancia o cambiar el diseño para aceptar datos directamente
        Text("Preview no disponible temporalmente por dependencias de ViewModel")
    }
}
