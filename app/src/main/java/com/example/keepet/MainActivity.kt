package com.example.keepet

import android.Manifest
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.keepet.data.repository.PetRepository
import com.example.keepet.data.repository.StaffRepository
import com.example.keepet.ui.screens.staff.StaffHomeScreen
import com.example.keepet.ui.screens.AddAppointmentScreen
import com.example.keepet.ui.screens.AddPetScreen
import com.example.keepet.ui.screens.HomeScreen
import com.example.keepet.ui.screens.LoginScreen
import com.example.keepet.ui.screens.NotificationsScreen
import com.example.keepet.ui.screens.PetDetailScreen
import com.example.keepet.ui.screens.WelcomeScreen
import com.example.keepet.ui.theme.KeePetTheme
import com.example.keepet.viewmodel.AuthViewModel
import com.example.keepet.viewmodel.PetViewModel
import com.example.keepet.viewmodel.PetViewModelFactory
import com.example.keepet.viewmodel.StaffViewModel
import com.example.keepet.viewmodel.StaffViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // SystemBarStyle.light quiere decir "el fondo que hay detras de la barra es
        // claro", asi que Android dibuja la hora, la bateria y la señal en OSCURO.
        //
        // Hace falta decirlo a mano porque con enableEdgeToEdge() a secas Android decide
        // el color de esos iconos segun el modo del TELEFONO, no segun el de la app: con
        // el movil en modo oscuro los pintaba en blanco sobre nuestro fondo crema y no se
        // veian. KeePet es siempre clara (mira ui/theme/Theme.kt), asi que se fija aqui.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        )
        setContent {
            KeePetTheme {
                PedirPermisoDeNotificaciones()

                val authViewModel: AuthViewModel = viewModel()
                val usuarioFirebase by authViewModel.usuario.collectAsState()

                // Esta es toda la logica de "quien puede entrar":
                // si no hay sesion se ve la bienvenida y el login; si la hay, la app.
                // Como el estado viene de un Flow de Firebase, al cerrar sesion la
                // app vuelve sola al login sin que haya que navegar a mano.
                if (usuarioFirebase == null) {
                    var pantalla by remember { mutableStateOf("welcome") }
                    when (pantalla) {
                        "welcome" -> WelcomeScreen(onNextClick = { pantalla = "login" })
                        else -> LoginScreen(authViewModel = authViewModel)
                    }
                } else {
                    val uid = usuarioFirebase!!.uid
                    val correo = usuarioFirebase!!.email.orEmpty()
                    val rol by authViewModel.rol.collectAsState()

                    // AQUI SE DIVIDE LA APP SEGUN QUIEN ENTRA.
                    //
                    // El rol se lee de /roles/{uid} en la base de datos, no de nada
                    // que el telefono decida. Y ojo: esto solo cambia lo que se VE.
                    // Lo que de verdad impide que un cliente lea la agenda de la
                    // clinica son las reglas de seguridad del servidor. Si toda la
                    // proteccion estuviera aqui, bastaria con modificar la app.
                    if (rol.esPersonal) {
                        val staffViewModel: StaffViewModel = viewModel(
                            key = "staff-$uid",
                            factory = StaffViewModelFactory(StaffRepository(), uid, correo, rol)
                        )
                        StaffHomeScreen(
                            viewModel = staffViewModel,
                            onLogout = { authViewModel.cerrarSesion() }
                        )
                    } else {
                        // key = uid: si entra otra persona, Compose tira este ViewModel
                        // y crea uno nuevo apuntando a SUS datos. Sin el key, el usuario
                        // nuevo veria las mascotas del anterior.
                        val petViewModel: PetViewModel = viewModel(
                            key = uid,
                            factory = PetViewModelFactory(PetRepository(uid), correo)
                        )

                        AppKeePet(
                            viewModel = petViewModel,
                            onLogout = { authViewModel.cerrarSesion() }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Navegacion de la app ya con sesion iniciada.
 *
 * Sigue siendo navegacion "por estados" (una variable que dice que pantalla toca),
 * igual que la tenias. Es sencilla de entender y funciona; si algun dia la app
 * crece, el paso natural seria Navigation Compose.
 */
@Composable
private fun AppKeePet(viewModel: PetViewModel, onLogout: () -> Unit) {
    var pantalla by remember { mutableStateOf("home") }
    var mascotaSeleccionada by remember { mutableStateOf<String?>(null) }
    var pasoInicial by remember { mutableIntStateOf(1) }

    val mascotas by viewModel.allPets.collectAsState()

    when (pantalla) {
        "home" -> HomeScreen(
            viewModel = viewModel,
            onAddPetClick = {
                mascotaSeleccionada = null
                pasoInicial = 1
                pantalla = "add_pet"
            },
            onAddAppointmentClick = { pantalla = "add_appointment" },
            onEditPetClick = { id ->
                mascotaSeleccionada = id
                pasoInicial = 1
                pantalla = "add_pet"
            },
            onPetDetailClick = { id ->
                mascotaSeleccionada = id
                pantalla = "pet_detail"
            },
            onNotificationsClick = { pantalla = "notifications" },
            onLogout = onLogout
        )

        "notifications" -> NotificationsScreen(
            viewModel = viewModel,
            onBack = { pantalla = "home" }
        )

        "add_pet" -> AddPetScreen(
            viewModel = viewModel,
            onBack = { pantalla = "home" },
            onFinish = { pantalla = "home" },
            petToEdit = mascotaSeleccionada?.let { id -> mascotas.find { it.id == id } },
            initialStep = pasoInicial
        )

        "pet_detail" -> PetDetailScreen(
            petId = mascotaSeleccionada.orEmpty(),
            viewModel = viewModel,
            onBack = { pantalla = "home" },
            onEdit = { id ->
                mascotaSeleccionada = id
                pasoInicial = 1
                pantalla = "add_pet"
            },
            onSchedule = { pantalla = "add_appointment" }
        )

        "add_appointment" -> AddAppointmentScreen(
            viewModel = viewModel,
            onBack = { pantalla = "home" },
            onFinish = { pantalla = "home" },
            onAddPet = {
                mascotaSeleccionada = null
                pantalla = "add_pet"
            }
        )
    }
}

/**
 * Desde Android 13 hay que pedir permiso para mostrar notificaciones.
 *
 * Faltaba declararlo en AndroidManifest.xml, asi que el sistema descartaba la
 * peticion sin enseñar el dialogo y los recordatorios de citas nunca aparecian.
 * Ya esta añadido en el manifest.
 */
@Composable
private fun PedirPermisoDeNotificaciones() {
    val lanzador = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { }
    )
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            lanzador.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
