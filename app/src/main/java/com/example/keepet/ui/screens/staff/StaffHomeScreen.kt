@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.keepet.ui.screens.staff

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.keepet.data.model.Mascota
import com.example.keepet.data.model.Rol
import com.example.keepet.ui.components.escanearQr
import com.example.keepet.ui.components.idDeCitaDesdeQr
import com.example.keepet.ui.screens.AddPetScreen
import com.example.keepet.ui.screens.PetDetailScreen
import com.example.keepet.ui.theme.*
import com.example.keepet.viewmodel.StaffViewModel
import com.example.keepet.viewmodel.cuandoTexto

/**
 * Pantalla principal del PERSONAL de la clinica.
 *
 * Es la contraparte de HomeScreen, que es la del cliente. Se decidio hacer dos
 * pantallas separadas en vez de una con muchos "if (esPersonal)" porque las dos
 * enseñan cosas completamente distintas: el cliente ve SUS mascotas, el personal ve
 * la agenda de todos. Una sola pantalla con condicionales por todas partes seria
 * mas dificil de leer y mas facil de romper.
 *
 * Las pestañas que se ven dependen del rol:
 *   - Empleado : Agenda y Expedientes
 *   - Doctor   : lo mismo, y puede escribir en el historial de un paciente
 *   - Admin    : lo mismo, mas la pestaña Usuarios para repartir roles
 *
 * "Expedientes" vivia antes en la app del cliente. Se movio aqui porque llevar los
 * expedientes es trabajo de la clinica. Es la misma vista de siempre, no una copia:
 * mira ExpedientesScreen.kt.
 */
@Composable
fun StaffHomeScreen(
    viewModel: StaffViewModel,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    var pestana by remember { mutableStateOf(Pestana.AGENDA) }

    // ------------------------------------------------------------------
    // Navegacion de la pestaña Expedientes
    // ------------------------------------------------------------------
    //
    // Es navegacion "por estados", la misma idea que usa AppKeePet en MainActivity para
    // el cliente: una variable dice que se esta viendo y el resto se deduce. Cuando hay
    // un formulario o una ficha abiertos ocupan toda la pantalla y tapan el Scaffold,
    // igual que le pasa al cliente.
    var creandoExpediente by remember { mutableStateOf(false) }
    var expedienteAEditar by remember { mutableStateOf<Mascota?>(null) }
    var expedienteAbierto by remember { mutableStateOf<String?>(null) }
    var eligiendoCliente by remember { mutableStateOf(false) }

    val clientes by viewModel.clientes.collectAsState()

    /** Deja apuntado de que cliente es la mascota antes de abrir cualquier formulario. */
    fun editar(mascota: Mascota) {
        viewModel.trabajarSobreCliente(mascota.duenoUid)
        expedienteAbierto = null
        expedienteAEditar = mascota
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(viewModel.mensaje) {
        viewModel.mensaje?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.limpiarMensaje()
        }
    }

    // Dialogo con la cita que se acaba de escanear
    viewModel.citaEscaneada?.let { cita ->
        DialogoCitaEscaneada(
            cita = cita,
            rol = viewModel.miRol,
            onCerrar = { viewModel.cerrarCitaEscaneada() },
            onConfirmarLlegada = {
                viewModel.confirmarLlegada(cita.id)
                viewModel.cerrarCitaEscaneada()
            },
            onCompletar = {
                viewModel.completarCita(cita.id)
                viewModel.cerrarCitaEscaneada()
            }
        )
    }

    // Formulario de expediente, para crear uno nuevo o editar uno existente.
    //
    // Es literalmente la misma pantalla de 3 pasos que usaba el cliente. Lo que decide
    // en la mascota de quien se escribe es el StaffViewModel que se le pasa, no la
    // pantalla: mira GestorDeMascotas.kt si quieres ver como encaja.
    if (creandoExpediente || expedienteAEditar != null) {
        AddPetScreen(
            viewModel = viewModel,
            onBack = {
                creandoExpediente = false
                expedienteAEditar = null
            },
            onFinish = {
                creandoExpediente = false
                expedienteAEditar = null
            },
            // null = crear uno nuevo; con mascota = editar esa.
            petToEdit = expedienteAEditar
        )
        return
    }

    // Ficha completa del expediente (foto, alergias, notas medicas, historial).
    // Tambien es la misma pantalla que ve el cliente.
    val petIdAbierto = expedienteAbierto
    if (petIdAbierto != null) {
        PetDetailScreen(
            petId = petIdAbierto,
            viewModel = viewModel,
            onBack = { expedienteAbierto = null },
            onEdit = { id ->
                viewModel.pacientes.value.find { it.id == id }?.let { editar(it) }
            }
            // onSchedule se queda sin poner (null) a proposito: desde la clinica no se
            // agendan citas a nombre de un cliente, la agenda va en su propia pestaña.
            // Antes se le pasaba una funcion vacia y quedaba en la ficha un boton grande
            // de "Agendar cita" que al pulsarlo no hacia nada. Ahora no se dibuja.
        )
        return
    }

    if (eligiendoCliente) {
        DialogoElegirCliente(
            clientes = clientes,
            onCerrar = { eligiendoCliente = false },
            onElegir = { cliente ->
                // Se apunta el cliente ANTES de abrir el formulario: una mascota que
                // todavia no existe no tiene forma de decir de quien es.
                viewModel.trabajarSobreCliente(cliente.uid)
                eligiendoCliente = false
                creandoExpediente = true
            }
        )
    }

    val pestanasVisibles = remember(viewModel.miRol) {
        buildList {
            add(Pestana.AGENDA)
            add(Pestana.EXPEDIENTES)
            if (viewModel.miRol.puedeGestionarUsuarios) add(Pestana.USUARIOS)
        }
    }

    Scaffold(
        containerColor = BackgroundColor,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "KeePet Clínica",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = TextColor
                        )
                        Text(
                            "${viewModel.miNombre} · ${viewModel.miRol.etiqueta}",
                            fontSize = 12.sp,
                            color = TextColor.copy(alpha = 0.6f)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(
                            Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Cerrar sesión",
                            tint = TextColor
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundColor)
            )
        },
        bottomBar = {
            NavigationBar(containerColor = White) {
                pestanasVisibles.forEach { p ->
                    NavigationBarItem(
                        selected = pestana == p,
                        onClick = { pestana = p },
                        icon = { Icon(p.icono, contentDescription = p.etiqueta) },
                        label = { Text(p.etiqueta, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AccentButton,
                            selectedTextColor = AccentButton,
                            indicatorColor = InputBackground,
                            unselectedIconColor = GrayHint,
                            unselectedTextColor = GrayHint
                        )
                    )
                }
            }
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // En Expedientes se añade el boton de crear, encima del de escanear.
                // Es el mismo boton redondo con el "+" que tenia el cliente, con el
                // mismo color y la misma forma; solo cambia de sitio para no taparse
                // con el de escanear, que se queda porque en recepcion se usa
                // constantemente y estaba disponible en toda la app del personal.
                if (pestana == Pestana.EXPEDIENTES) {
                    FloatingActionButton(
                        onClick = { eligiendoCliente = true },
                        containerColor = AccentButton,
                        contentColor = White,
                        shape = CircleShape
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Nuevo Expediente")
                    }
                }

                // El escaneo esta disponible en toda la app del personal, no solo en la
                // agenda: en recepcion lo que mas se hace es escanear.
                ExtendedFloatingActionButton(
                    onClick = {
                        escanearQr(context) { textoDelQr ->
                            viewModel.procesarQr(idDeCitaDesdeQr(textoDelQr))
                        }
                    },
                    containerColor = AccentButton,
                    contentColor = White
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Escanear QR", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(BackgroundColor)
        ) {
            when (pestana) {
                Pestana.AGENDA -> AgendaScreen(viewModel)
                Pestana.EXPEDIENTES -> ExpedientesScreen(
                    viewModel = viewModel,
                    onEditarExpediente = { mascota -> editar(mascota) },
                    onAbrirExpediente = { mascota ->
                        viewModel.trabajarSobreCliente(mascota.duenoUid)
                        expedienteAbierto = mascota.id
                    }
                )
                Pestana.USUARIOS -> UsuariosAdminScreen(viewModel)
            }
        }
    }
}

enum class Pestana(
    val etiqueta: String,
    val icono: androidx.compose.ui.graphics.vector.ImageVector
) {
    AGENDA("Agenda", Icons.Default.CalendarMonth),

    // Mismo icono y misma etiqueta que tenia en el menu del cliente, para que quien ya
    // conocia la app la reconozca en su sitio nuevo.
    //
    // AQUI HABIA TAMBIEN UNA PESTAÑA "PACIENTES", y se ha quitado porque enseñaba LA
    // MISMA LISTA que esta: las mascotas de todos los clientes. Dos pestañas con los
    // mismos animales, una con tarjetas grandes y otra con tarjetas pequeñas, y en cada
    // una se podian hacer cosas distintas; para saber donde ir habia que acordarse de
    // cual era cual. En una clinica hay UNA lista de pacientes.
    //
    // No se pierde nada de lo que hacia: se buscaba igual (nombre, dueño, especie, raza),
    // la ficha completa se abre pulsando el paciente, y el "añadir al historial" que
    // tenia esa pestaña esta dentro de la ficha, en "Añadir Registro Médico", con mas
    // campos (receta, tipo de visita, alergia nueva). El contador de pacientes se ha
    // traido a esta pestaña.
    EXPEDIENTES("Expedientes", Icons.Default.HistoryEdu),
    USUARIOS("Usuarios", Icons.Default.Groups)
}

/**
 * Lo que ve el empleado justo despues de escanear el QR de un cliente.
 *
 * Enseña de quien es la cita y ofrece el paso siguiente segun el estado, en vez de
 * mostrar todos los botones siempre: si la cita ya esta confirmada, no tiene sentido
 * ofrecer "registrar llegada" otra vez.
 */
@Composable
private fun DialogoCitaEscaneada(
    cita: com.example.keepet.data.model.Cita,
    rol: Rol,
    onCerrar: () -> Unit,
    onConfirmarLlegada: () -> Unit,
    onCompletar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Cita ${cita.codigo}", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                FilaDato("Mascota", cita.nombreMascota)
                FilaDato("Cliente", cita.clienteNombre.ifBlank { "—" })
                FilaDato("Servicio", cita.tipoServicio.ifBlank { cita.motivo })
                // Igual que en la agenda: "Hoy · 08:00 AM". Recepcion necesita ver de un
                // golpe si el QR que le acaban de enseñar es de una cita de hoy.
                FilaDato("Cuándo", cuandoTexto(cita))
                FilaDato("Estado", cita.estado)
                if (cita.doctorNombre.isNotBlank()) {
                    FilaDato("Doctor", cita.doctorNombre)
                }
            }
        },
        confirmButton = {
            when (cita.estado) {
                com.example.keepet.data.model.Cita.ESTADO_PENDIENTE -> Button(
                    onClick = onConfirmarLlegada,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentButton)
                ) { Text("Registrar llegada", color = White) }

                com.example.keepet.data.model.Cita.ESTADO_CONFIRMADA,
                com.example.keepet.data.model.Cita.ESTADO_EN_ATENCION ->
                    if (rol.puedeEditarHistorial) {
                        Button(
                            onClick = onCompletar,
                            colors = ButtonDefaults.buttonColors(containerColor = AccentButton)
                        ) { Text("Completar", color = White) }
                    } else {
                        TextButton(onClick = onCerrar) { Text("Cerrar") }
                    }

                else -> TextButton(onClick = onCerrar) { Text("Cerrar") }
            }
        },
        dismissButton = { TextButton(onClick = onCerrar) { Text("Cerrar") } }
    )
}

@Composable
internal fun FilaDato(etiqueta: String, valor: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
    ) {
        Text(
            "$etiqueta: ",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = TextColor.copy(alpha = 0.7f)
        )
        Text(valor, fontSize = 14.sp, color = TextColor)
    }
}
