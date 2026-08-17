package com.example.keepet.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.keepet.data.model.Cita
import com.example.keepet.data.model.Mascota
import com.example.keepet.ui.components.avatarPorEspecie
import com.example.keepet.ui.theme.*

import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

import com.example.keepet.viewmodel.PetViewModel

import androidx.compose.ui.platform.LocalContext
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.keepet.workers.AppointmentReminderWorker
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddAppointmentScreen(
    viewModel: PetViewModel,
    onBack: () -> Unit,
    onFinish: () -> Unit,
    onAddPet: () -> Unit
) {
    val context = LocalContext.current
    var step by remember { mutableIntStateOf(1) }
    
    // Appointment State
    var selectedPet by remember { mutableStateOf<Mascota?>(null) }
    var selectedService by remember { mutableStateOf<Service?>(null) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var selectedTime by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    // El perfil del cliente, para guardar su nombre dentro de la cita.
    val perfil by viewModel.usuario.collectAsState()

    val localeEs = Locale.forLanguageTag("es-ES")
    val formattedDate = selectedDate?.format(DateTimeFormatter.ofPattern("d 'de' MMMM", localeEs)) ?: ""

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
    ) {
        if (step < 3) {
            // Header for steps 1 and 2
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { if (step > 1) step-- else onBack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = TextColor)
                }
                Text(
                    text = if (step == 1) "Agendar cita" else "Resumen cita",
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextColor
                )
                Box(modifier = Modifier.size(48.dp))
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            when (step) {
                1 -> StepOneSelect(
                    viewModel,
                    selectedPet,
                    // Al cambiar de mascota se olvida el servicio elegido: un baño de
                    // perro ("Baño estético"...) no tiene sentido si el cliente cambia
                    // a un gato, y viceversa con el baño simple.
                    { selectedPet = it; selectedService = null },
                    selectedService, { selectedService = it },
                    selectedDate, { selectedDate = it; selectedTime = "" }, // Reset time if date changes
                    selectedTime, { selectedTime = it },
                    onAddPet = onAddPet,
                    onConfirm = { step = 2 }
                )
                2 -> StepTwoSummary(
                    selectedPet, selectedService, formattedDate, selectedTime,
                    notes, { notes = it },
                    onConfirm = { 
                        val cita = Cita(
                            // Vacio: Firebase le asigna la clave al guardar con push().
                            id = "",
                            // El nombre del cliente se guarda DENTRO de la cita a
                            // proposito. El personal necesita saber a quien llamar sin
                            // tener que ir a buscar el perfil del dueño en otra rama de
                            // la base de datos, que en NoSQL es una consulta extra por
                            // cada linea de la agenda.
                            clienteNombre = perfil.nombre.ifBlank {
                                perfil.correo.substringBefore("@")
                            },
                            mascotaId = selectedPet?.id.orEmpty(),
                            nombreMascota = selectedPet?.nombre ?: "",
                            fecha = formattedDate,
                            // La misma fecha en "2026-08-16". Es la que usa la agenda de
                            // la clinica para ordenar y para saber que es de hoy; el
                            // texto de arriba solo sirve para enseñarlo. Mira Cita.kt.
                            fechaIso = selectedDate?.toString().orEmpty(),
                            hora = selectedTime,
                            tipoServicio = selectedService?.name ?: "",
                            precio = selectedService?.price ?: "",
                            motivo = selectedService?.name ?: "",
                            notasAdicionales = notes
                        )
                        viewModel.addAppointment(cita)
                        
                        // Schedule Notification
                        selectedDate?.let { date ->
                            try {
                                val appointmentDateTimeString = "${date.format(DateTimeFormatter.ISO_LOCAL_DATE)} $selectedTime"
                                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:00 a", Locale.US)
                                val appointmentDateTime = java.time.LocalDateTime.parse(appointmentDateTimeString, formatter)
                                
                                val delay = java.time.Duration.between(
                                    java.time.LocalDateTime.now(),
                                    appointmentDateTime.minusHours(1)
                                ).toMillis()

                                if (delay > 0) {
                                    val data = Data.Builder()
                                        .putString("petName", selectedPet?.nombre)
                                        .putString("serviceType", selectedService?.name)
                                        .putString("appointmentTime", selectedTime)
                                        .putInt("notificationId", (System.currentTimeMillis() % Int.MAX_VALUE).toInt())
                                        .build()

                                    val reminderRequest = OneTimeWorkRequestBuilder<AppointmentReminderWorker>()
                                        .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                                        .setInputData(data)
                                        .build()

                                    WorkManager.getInstance(context).enqueue(reminderRequest)
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }

                        step = 3 
                    }
                )
                3 -> StepThreeSuccess(
                    formattedDate, selectedTime,
                    onBackToHome = onFinish
                )
            }
        }
    }
}

data class Service(val name: String, val description: String, val price: String, val icon: ImageVector)

/**
 * Servicios que se ofrecen sea cual sea la especie: el enunciado solo pide ramificar el
 * baño para los perros, "para las demas especies y otros servicios, las opciones de
 * cita se mantienen simples".
 *
 * El nombre se guarda como TEXTO en la cita (Cita.tipoServicio), asi que cambiar esta
 * lista no estropea las citas que ya estuvieran creadas: siguen mostrando el nombre con
 * el que se guardaron.
 *
 * Los precios son de ejemplo y estan en dolares. Cambialos por los de la clinica; si
 * algun dia tienen que poder cambiarse sin recompilar la app, tendrian que vivir en la
 * base de datos y no aqui.
 */
val serviciosGenerales = listOf(
    Service("Consulta general", "Revisión y diagnóstico", "$20.00", Icons.Default.MedicalServices),
    Service("Vacunación", "Aplicación de vacunas y refuerzos", "$15.00", Icons.Default.Vaccines),
    Service("Desparasitación", "Tratamiento interno y externo", "$12.00", Icons.Default.BugReport),
    Service("Control y seguimiento", "Revisión de un tratamiento en curso", "$10.00", Icons.Default.CalendarMonth),
    Service("Valoración quirúrgica", "Revisión previa a una operación", "$30.00", Icons.Default.LocalHospital),
    Service("Urgencia", "Atención el mismo día", "$35.00", Icons.Default.Warning)
)

/** El baño para gatos, conejos, o cuando todavia no se ha elegido mascota: una sola
 *  opción simple, sin sub-tipos. */
val banoSimple = Service("Baño y estética", "Lavado, secado y corte", "$19.99", Icons.Default.WaterDrop)

/**
 * Las cuatro opciones de baño para PERROS, tal cual las pide el enunciado: nombre y
 * descripcion son exactamente los suyos, no se han tocado. Los precios si son de
 * ejemplo (el enunciado no los fija) y hay que cambiarlos por los reales de la clinica.
 */
val banoPerro = listOf(
    Service("Baño básico", "Un lavado y secado simple.", "$15.00", Icons.Default.WaterDrop),
    Service(
        "Baño con recorte de uñas",
        "El baño básico más un recorte de uñas.",
        "$18.00",
        Icons.Default.ContentCut
    ),
    Service("Baño estético", "Un baño con corte de pelo y estilo.", "$22.00", Icons.Default.Brush),
    Service(
        "Baño medicado",
        "Un baño con un champú especial para tratar problemas de piel.",
        "$25.00",
        Icons.Default.Healing
    )
)

/** Que servicios se enseñan, segun la especie de la mascota elegida. */
fun serviciosPara(especie: String): List<Service> =
    serviciosGenerales + if (especie == "Perro") banoPerro else listOf(banoSimple)

/**
 * Lista "plana" que sigue usando EditAppointmentDialog (en HomeScreen.kt), que no sabe
 * la especie de la mascota de la cita que esta editando. Si esa cita es de uno de los
 * cuatro baños de perro, su nombre no se pierde: EditAppointmentDialog ya sabe conservar
 * el servicio de una cita aunque no aparezca en esta lista (mira el comentario de
 * "opciones" ahi), solo no deja CAMBIAR a otro tipo de baño de perro desde el dialogo de
 * edicion. Cambiar eso es tocar HomeScreen.kt, fuera de lo que se pidio aqui.
 */
val services = serviciosGenerales + banoSimple

@Composable
fun StepOneSelect(
    viewModel: PetViewModel,
    selectedPet: Mascota?, onPetSelect: (Mascota) -> Unit,
    selectedService: Service?, onServiceSelect: (Service) -> Unit,
    selectedDate: LocalDate?, onDateSelect: (LocalDate) -> Unit,
    selectedTime: String, onTimeSelect: (String) -> Unit,
    onAddPet: () -> Unit,
    onConfirm: () -> Unit
) {
    val scrollState = rememberScrollState()
    val pets by viewModel.allPets.collectAsState()

    // Generar horas de 8:00 AM a 4:00 PM (cada 1 hora)
    val timeFormatter = DateTimeFormatter.ofPattern("hh:00 a", Locale.US)
    val allTimes = (8..16).map { hour ->
        LocalTime.of(hour % 24, 0).format(timeFormatter).uppercase()
    }

    // Horas que ya tiene pedidas este cliente para el dia elegido, para no ofrecerselas
    // otra vez.
    //
    // DOS AVISOS SOBRE ESTO:
    //
    //  - Una cita CANCELADA ya no ocupa el hueco. Antes si: si cancelabas la de las
    //    10:00 no podias volver a pedir las 10:00 nunca mas.
    //  - Esta lista son solo LAS CITAS DE ESTE CLIENTE, porque un cliente no tiene
    //    permiso para leer la agenda de la clinica (y esta bien que no lo tenga: ahi
    //    estan los datos de los demas). Es decir, esto evita que TU pidas dos veces la
    //    misma hora, pero no evita que dos clientes distintos pidan las 10:00 del mismo
    //    dia. Quien resuelve eso es recepcion desde la agenda. Para arreglarlo de verdad
    //    habria que guardar los huecos ocupados en una rama aparte que todos puedan leer
    //    sin ver de quien son; es un cambio de base de datos, no de pantalla.
    val localeEs = Locale.forLanguageTag("es-ES")
    val formattedSelectedDate = selectedDate?.format(DateTimeFormatter.ofPattern("d 'de' MMMM", localeEs))
    val isoSelectedDate = selectedDate?.toString()
    val appointments by viewModel.allAppointments.collectAsState()
    val bookedTimes = appointments
        .filter { it.estado != Cita.ESTADO_CANCELADA }
        .filter {
            // Se compara por fechaIso, que lleva año; el texto es el respaldo para las
            // citas creadas antes de que ese campo existiera.
            if (it.fechaIso.isNotBlank()) it.fechaIso == isoSelectedDate
            else it.fecha == formattedSelectedDate
        }
        .map { it.hora.uppercase() }

    val now = LocalTime.now()
    val availableTimes = allTimes.filter { time ->
        val isBooked = time in bookedTimes
        if (isBooked) return@filter false

        if (selectedDate == LocalDate.now()) {
            try {
                val timeObj = LocalTime.parse(time, timeFormatter)
                timeObj.isAfter(now)
            } catch (e: Exception) {
                true
            }
        } else {
            true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(scrollState)
    ) {
        // Pet Selection
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(pets) { pet ->
                PetAvatar(pet, isSelected = selectedPet == pet) { onPetSelect(pet) }
            }
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .clip(CircleShape)
                            .background(InputBackground)
                            .clickable { onAddPet() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Añadir", tint = TextColor)
                    }
                    Text("Añadir", fontSize = 12.sp, color = TextColor, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        // Decia "Módulo de baños" encima de la lista de servicios. Se quedo ahi de cuando
        // los cuatro servicios eran cuatro tipos de baño; ahora debajo hay consultas,
        // vacunas y urgencias.
        Text("¿Qué necesita?", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextColor)
        Spacer(modifier = Modifier.height(16.dp))

        // Services Grid
        // Perro -> las 4 opciones de baño del enunciado. Cualquier otra especie (o
        // ninguna mascota elegida todavia) -> el baño simple, sin sub-tipos.
        val serviciosDisponibles = serviciosPara(selectedPet?.especie ?: "")
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val rows = serviciosDisponibles.chunked(2)
            rows.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { service ->
                        ServiceCard(
                            service = service,
                            isSelected = selectedService == service,
                            modifier = Modifier.weight(1f)
                        ) { onServiceSelect(service) }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("Selecciona la fecha", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextColor)
        Spacer(modifier = Modifier.height(8.dp))
        
        SimpleCalendar(selectedDate) { onDateSelect(it) }

        Spacer(modifier = Modifier.height(24.dp))
        Text("Horario disponible", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextColor)
        Spacer(modifier = Modifier.height(16.dp))

        // Time Slots
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            availableTimes.forEach { time ->
                TimeSlot(time, isSelected = selectedTime == time) { onTimeSelect(time) }
            }
            if (selectedDate != null && availableTimes.isEmpty()) {
                Text("No hay horarios disponibles para este día", fontSize = 12.sp, color = Color.Red.copy(alpha = 0.6f))
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onConfirm,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AccentButton,
                disabledContainerColor = AccentButton.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(50.dp),
            enabled = selectedPet != null && selectedService != null && selectedDate != null && selectedTime.isNotEmpty()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Confirmar cita", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (selectedPet != null && selectedService != null && selectedDate != null && selectedTime.isNotEmpty()) White else TextColor.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.CheckCircleOutline, contentDescription = null, modifier = Modifier.size(20.dp), tint = if (selectedPet != null && selectedService != null && selectedDate != null && selectedTime.isNotEmpty()) White else TextColor.copy(alpha = 0.3f))
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun PetAvatar(pet: Mascota, isSelected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(70.dp)
                .clip(CircleShape)
                .border(
                    width = if (isSelected) 3.dp else 0.dp,
                    color = if (isSelected) AccentButton else Color.Transparent,
                    shape = CircleShape
                )
        ) {
            if (pet.especie.isNotBlank()) {
                Image(
                    painter = painterResource(id = avatarPorEspecie(pet.especie)),
                    contentDescription = pet.nombre,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(modifier = Modifier.fillMaxSize().background(InputBackground), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Pets, contentDescription = null, tint = TextColor.copy(alpha = 0.3f))
                }
            }
        }
        Text(
            text = pet.nombre,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) AccentButton else TextColor,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
fun ServiceCard(service: Service, isSelected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(100.dp),
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) AccentButton else White,
        border = BorderStroke(1.dp, if (isSelected) AccentButton else Color.LightGray.copy(alpha = 0.5f)),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    service.icon,
                    contentDescription = null,
                    tint = if (isSelected) White else AccentButton,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    service.name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) White else TextColor
                )
            }
            Text(
                service.description,
                fontSize = 10.sp,
                color = if (isSelected) White.copy(alpha = 0.8f) else GrayHint,
                lineHeight = 12.sp
            )
            Text(
                "Precio: ${service.price}",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) White else TextColor
            )
        }
    }
}

@Composable
fun SimpleCalendar(selectedDate: LocalDate?, onDateSelect: (LocalDate) -> Unit) {
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    val daysInMonth = currentMonth.lengthOfMonth()
    val firstDayOfMonth = currentMonth.atDay(1).dayOfWeek.value % 7 // 0 for Sunday
    
    val localeEs = Locale.forLanguageTag("es-ES")
    val monthName = currentMonth.month.getDisplayName(TextStyle.FULL, localeEs)
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(localeEs) else it.toString() }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFEBC8B2).copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Month and Year Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Mes anterior", tint = TextColor)
                }
                Text(
                    text = "$monthName ${currentMonth.year}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextColor
                )
                IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Mes siguiente", tint = TextColor)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Days of Week Header
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                // Estaban en ingles ("Su, Mo, Tu...") en una app que por dentro y por
                // fuera esta en español. El alpha sube de 0.5 a 0.7 porque a 12sp y al
                // 50% el marron sobre blanco se lee mal.
                listOf("Do", "Lu", "Ma", "Mi", "Ju", "Vi", "Sá").forEach {
                    Text(it, fontSize = 12.sp, color = TextColor.copy(alpha = 0.7f), fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Grid of days
            val totalCells = daysInMonth + firstDayOfMonth
            val rows = (totalCells + 6) / 7
            
            Column {
                for (row in 0 until rows) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        for (col in 0 until 7) {
                            val cellIndex = row * 7 + col
                            val day = cellIndex - firstDayOfMonth + 1
                            
                            if (day in 1..daysInMonth) {
                                val date = currentMonth.atDay(day)
                                val isSelected = selectedDate == date
                                val isToday = date == LocalDate.now()

                                val isPast = date.isBefore(LocalDate.now())
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                isSelected -> AccentButton
                                                isToday -> Color.LightGray.copy(alpha = 0.3f)
                                                else -> Color.Transparent
                                            }
                                        )
                                        .clickable(enabled = !isPast) { onDateSelect(date) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        day.toString(),
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = when {
                                            isSelected -> White
                                            isPast -> TextColor.copy(alpha = 0.3f)
                                            else -> TextColor
                                        }
                                    )
                                }
                            } else {
                                Box(modifier = Modifier.size(36.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TimeSlot(time: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) AccentButton else Color(0xFFEBC8B2).copy(alpha = 0.5f),
        modifier = Modifier.width(100.dp)
    ) {
        Text(
            text = time,
            modifier = Modifier.padding(vertical = 10.dp),
            textAlign = TextAlign.Center,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) White else TextColor
        )
    }
}

@Composable
fun StepTwoSummary(
    pet: Mascota?, service: Service?, date: String, time: String,
    notes: String, onNotesChange: (String) -> Unit,
    onConfirm: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Pet Summary Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = White)
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(60.dp).clip(RoundedCornerShape(12.dp))) {
                    if (pet != null) {
                        Image(
                            painterResource(avatarPorEspecie(pet.especie)),
                            null,
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(Modifier.fillMaxSize().background(InputBackground))
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Mascota", fontSize = 12.sp, color = AccentButton, fontWeight = FontWeight.Bold)
                    Text(pet?.nombre ?: "", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextColor)
                    Text(pet?.raza ?: "", fontSize = 12.sp, color = GrayHint)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Service Details
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = White)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Detalles del servicio", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextColor)
                Spacer(modifier = Modifier.height(16.dp))
                
                DetailItem(Icons.Default.ContentCut, "Servicio", service?.name ?: "")
                DetailItem(Icons.Default.CalendarToday, "Fecha", date)
                DetailItem(Icons.Default.AccessTime, "Hora", time)
                
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFE0F2F1))
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccountBalanceWallet, null, modifier = Modifier.size(16.dp), tint = Color(0xFF00796B))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Precio", fontSize = 12.sp, color = Color(0xFF00796B), fontWeight = FontWeight.Bold)
                    }
                    Text(service?.price ?: "", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00796B))
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        // Notes
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.MenuBook, null, modifier = Modifier.size(16.dp), tint = TextColor)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Notas adicionales", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextColor)
            }
            Spacer(modifier = Modifier.height(8.dp))
            TextField(
                value = notes,
                onValueChange = onNotesChange,
                modifier = Modifier.fillMaxWidth().height(100.dp),
                placeholder = { Text("Sin notas", color = GrayHint) },
                colors = TextFieldDefaults.colors(
                    focusedTextColor = TextColor,
                    unfocusedTextColor = TextColor,
                    focusedContainerColor = White,
                    unfocusedContainerColor = White,
                    focusedIndicatorColor = AccentButton,
                    unfocusedIndicatorColor = Color.LightGray.copy(alpha = 0.5f)
                ),
                textStyle = LocalTextStyle.current.copy(color = TextColor),
                shape = RoundedCornerShape(16.dp)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onConfirm,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentButton),
            shape = RoundedCornerShape(50.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Confirmar", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
            }
        }
    }
}

@Composable
fun DetailItem(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, modifier = Modifier.size(18.dp), tint = AccentButton)
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, fontSize = 12.sp, color = GrayHint, modifier = Modifier.width(80.dp))
        Text(value, fontSize = 12.sp, color = TextColor, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun StepThreeSuccess(date: String, time: String, onBackToHome: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Pets, contentDescription = null, tint = TextColor, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("KeePet", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextColor)
        }
        
        Spacer(modifier = Modifier.height(40.dp))
        
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE0F2F1).copy(alpha = 0.5f))
            )
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .clip(CircleShape)
                    .background(White)
                    .border(2.dp, Color(0xFFE0E0E0), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Pets, contentDescription = null, modifier = Modifier.size(70.dp), tint = TextColor)
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .align(Alignment.BottomEnd)
                        .offset(x = (-5).dp, y = (-5).dp)
                        .clip(CircleShape)
                        .background(White)
                        .border(1.dp, TextColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Check, null, modifier = Modifier.size(24.dp), tint = Color(0xFF4CAF50))
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        Text("¡Todo listo!", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = TextColor)
        Text("Tu cita ha sido programada", fontSize = 16.sp, color = TextColor.copy(alpha = 0.7f))

        Spacer(modifier = Modifier.height(40.dp))
        
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = White),
            elevation = CardDefaults.cardElevation(2.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CalendarToday, null, tint = Color(0xFF00796B))
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("HORARIO CONFIRMADO", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00796B))
                    Text("$date, $time", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextColor)
                }
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onBackToHome,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentButton),
            shape = RoundedCornerShape(50.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Email, null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Volver al inicio", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Preview(showBackground = true)
@Composable
fun AddAppointmentScreenPreview() {
    KeePetTheme {
        // Se usa un marcador de posición porque AddAppointmentScreen requiere un ViewModel con dependencias
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Vista previa de Agendar Cita (Requiere ViewModel)")
        }
    }
}
