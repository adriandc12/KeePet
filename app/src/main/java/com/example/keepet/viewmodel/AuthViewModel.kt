package com.example.keepet.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.keepet.data.model.Rol
import com.example.keepet.data.repository.AuthRepository
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Lo que la pantalla de login necesita saber para dibujarse. */
data class AuthUiState(
    val cargando: Boolean = false,
    val error: String? = null,
    val aviso: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModel(
    private val repo: AuthRepository = AuthRepository()
) : ViewModel() {

    /**
     * Quien tiene la sesion abierta. null = nadie.
     * MainActivity lo observa para decidir si enseña el login o la app.
     * Al ser un Flow de Firebase, si la sesion caduca la app vuelve sola al login.
     */
    val usuario: StateFlow<FirebaseUser?> = repo.estadoSesion().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = repo.usuarioActual
    )

    /**
     * El rol del usuario que tiene la sesion abierta.
     *
     * flatMapLatest quiere decir: "cada vez que cambie el usuario, deja de escuchar
     * el rol del anterior y ponte a escuchar el del nuevo". Sin ese "deja de
     * escuchar el anterior", al cambiar de cuenta seguirias recibiendo el rol de
     * quien estaba antes y podrias acabar viendo pantallas que no te tocan.
     *
     * Mientras no se sabe el rol se asume CLIENTE, que es el permiso mas bajo.
     */
    val rol: StateFlow<Rol> = usuario
        .flatMapLatest { u -> if (u == null) flowOf(Rol.CLIENTE) else repo.rolDe(u.uid) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = Rol.CLIENTE
        )

    var uiState by mutableStateOf(AuthUiState())
        private set

    fun limpiarMensajes() {
        uiState = uiState.copy(error = null, aviso = null)
    }

    fun iniciarSesion(correo: String, contrasena: String) {
        val problema = validar(correo, contrasena)
        if (problema != null) {
            uiState = uiState.copy(error = problema)
            return
        }
        ejecutar { repo.iniciarSesion(correo, contrasena) }
    }

    fun registrarse(correo: String, contrasena: String) {
        val problema = validar(correo, contrasena)
        if (problema != null) {
            uiState = uiState.copy(error = problema)
            return
        }
        ejecutar { repo.registrarse(correo, contrasena) }
    }

    fun recuperarContrasena(correo: String) {
        if (correo.isBlank()) {
            uiState = uiState.copy(error = "Escribe tu correo primero")
            return
        }
        viewModelScope.launch {
            uiState = uiState.copy(cargando = true, error = null, aviso = null)
            uiState = try {
                repo.recuperarContrasena(correo)
                uiState.copy(cargando = false, aviso = "Te enviamos un correo para cambiar la contraseña")
            } catch (e: Exception) {
                uiState.copy(cargando = false, error = mensajeDe(e))
            }
        }
    }

    fun cerrarSesion() {
        repo.cerrarSesion()
        uiState = AuthUiState()
    }

    // -----------------------------------------------------------------

    /**
     * Envuelve login y registro: los dos hacen exactamente lo mismo salvo la
     * llamada final, asi que el manejo de "cargando" y de errores se escribe
     * una sola vez en lugar de duplicarlo.
     */
    private fun ejecutar(accion: suspend () -> FirebaseUser?) {
        viewModelScope.launch {
            uiState = uiState.copy(cargando = true, error = null, aviso = null)
            uiState = try {
                val creado = accion()
                // Deja escrito /roles/{uid} = "cliente" si es la primera vez.
                // Sin esto un usuario nuevo no tendria rol y, aunque el codigo
                // asume cliente por defecto, el admin no lo veria en su lista.
                creado?.let { repo.crearRolInicialSiNoExiste(it.uid) }
                // No ponemos cargando=false al triunfar: MainActivity ya esta
                // cambiando de pantalla y apagarlo aqui haria un parpadeo feo.
                uiState.copy(error = null)
            } catch (e: Exception) {
                uiState.copy(cargando = false, error = mensajeDe(e))
            }
        }
    }

    private fun validar(correo: String, contrasena: String): String? = when {
        correo.isBlank() -> "Escribe tu correo"
        !correo.contains("@") || !correo.contains(".") -> "Ese correo no parece válido"
        contrasena.isBlank() -> "Escribe tu contraseña"
        contrasena.length < 6 -> "La contraseña debe tener al menos 6 caracteres"
        else -> null
    }

    /** Traduce las excepciones de Firebase (en ingles y tecnicas) a algo legible. */
    private fun mensajeDe(e: Exception): String = when (e) {
        is FirebaseAuthWeakPasswordException -> "La contraseña es muy débil, usa al menos 6 caracteres"
        is FirebaseAuthUserCollisionException -> "Ese correo ya tiene una cuenta. Inicia sesión."
        is FirebaseAuthInvalidUserException -> "No existe ninguna cuenta con ese correo"
        is FirebaseAuthInvalidCredentialsException -> "Correo o contraseña incorrectos"
        is FirebaseNetworkException -> "Sin conexión a internet"
        else -> e.localizedMessage ?: "Algo salió mal, inténtalo de nuevo"
    }
}
