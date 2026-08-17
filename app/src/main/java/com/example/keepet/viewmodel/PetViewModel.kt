package com.example.keepet.viewmodel

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.keepet.data.model.Cita
import com.example.keepet.data.model.Mascota
import com.example.keepet.data.model.Notificacion
import com.example.keepet.data.model.RegistroHistorial
import com.example.keepet.data.model.Usuario
import com.example.keepet.data.repository.PetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel del CLIENTE: sus mascotas, sus citas y su perfil.
 *
 * Implementa GestorDeMascotas para poder pasarselo a AddPetScreen y PetDetailScreen,
 * que ahora piden ese contrato en vez de esta clase concreta. Eso permite que el
 * personal de la clinica reutilice esas mismas pantallas con su propio ViewModel.
 * Aqui no cambio ningun comportamiento: los metodos son los que ya habia, solo
 * llevan "override" delante.
 */
class PetViewModel(
    private val repository: PetRepository,
    private val correo: String
) : ViewModel(), GestorDeMascotas {

    init {
        // Si es la primera vez que entra este usuario, le creamos el perfil y
        // unas mascotas de ejemplo. Si ya tenia datos, no hace nada.
        enSegundoPlano { repository.seedIfNewUser(correo) }
    }

    /**
     * Ultimo error de guardado, para poder avisar al usuario.
     * Es null cuando todo va bien.
     */
    var errorGuardado by mutableStateOf<String?>(null)
        private set

    fun limpiarError() {
        errorGuardado = null
    }

    /**
     * Lanza una operacion contra Firebase capturando los fallos.
     *
     * Esto es importante: una excepcion sin capturar dentro de viewModelScope.launch
     * NO se queda en silencio, tumba la app. Y escribir en Firebase falla mas a
     * menudo de lo que parece (sin internet, reglas de seguridad, sesion caducada).
     * Con esto la app aguanta y ademas puede contar que ha pasado.
     */
    private fun enSegundoPlano(bloque: suspend () -> Unit) = viewModelScope.launch {
        try {
            bloque()
        } catch (e: Exception) {
            errorGuardado = e.localizedMessage ?: "No se pudo guardar el cambio"
        }
    }

    /**
     * El cliente NO escribe en la parte clinica del expediente.
     *
     * Ve el historial, las alergias y las notas medicas de su mascota (es informacion
     * suya y tiene derecho a leerla), pero quien las escribe es el veterinario. Antes
     * podia añadir y borrar registros medicos de su propia mascota, lo que convertia el
     * historial en algo que no servia como prueba de nada.
     *
     * Sigue pudiendo crear y editar los datos del animal: nombre, raza, edad, peso,
     * telefono, foto. Eso es lo que el dueño sabe mejor que nadie.
     */
    override val puedeEditarDatosClinicos: Boolean = false

    override val allPets: StateFlow<List<Mascota>> = repository.allPets
        .sinTumbarLaApp(emptyList())
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val allAppointments: StateFlow<List<Cita>> = repository.allAppointments
        .sinTumbarLaApp(emptyList())
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val usuario: StateFlow<Usuario> = repository.profile
        .sinTumbarLaApp(Usuario())
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = Usuario()
        )

    /**
     * Si Firebase deniega el permiso a mitad de lectura, se queda con [valorSiFalla]
     * en vez de cerrar el Flow con una excepcion.
     *
     * POR QUE HACE FALTA: al cerrar sesion, el listener de Firebase que sigue
     * escuchando "mis mascotas" / "mis citas" / "mi perfil" pierde el permiso a
     * mitad de lectura y Firebase avisa por onCancelled. PetRepository convierte
     * eso en un Flow que se CIERRA con esa excepcion (mira flowDeHijos/flowDeValor
     * en PetRepository.kt), y una excepcion sin capturar dentro de stateIn sube
     * por viewModelScope hasta el hilo principal y mata el proceso: la app se
     * cerraba de golpe en vez de volver sola a la pantalla de login. Es el mismo
     * fallo que ya se arreglo en StaffViewModel (mira sinTumbarLaApp() alli), pero
     * nunca se habia aplicado en el lado del cliente.
     */
    private fun <T> Flow<T>.sinTumbarLaApp(valorSiFalla: T): Flow<T> =
        catch { emit(valorSiFalla) }

    /** Ids de avisos que el usuario ya cerro con la X. Solo dura mientras la app vive. */
    private val descartadas = MutableStateFlow<Set<String>>(emptySet())

    /**
     * Los avisos NO se guardan en la base de datos: se calculan a partir de las
     * citas reales. Antes eran una lista inventada a fuego dentro de PetRepository,
     * que mostraba avisos de citas inexistentes y no se enteraba si borrabas una.
     * Calculandolos, la pantalla siempre coincide con lo que hay de verdad.
     */
    val notificaciones: StateFlow<List<Notificacion>> =
        combine(allAppointments, allPets, descartadas) { citas, mascotas, ocultas ->
            citas
                .filter { it.id !in ocultas && it.estado == "Pendiente" }
                .map { cita ->
                    val mascota = mascotas.find { it.id == cita.mascotaId }
                    Notificacion(
                        id = cita.id,
                        mascotaId = cita.mascotaId,
                        nombreMascota = cita.nombreMascota,
                        tipoServicio = cita.tipoServicio.ifBlank { cita.motivo },
                        fecha = cita.fecha,
                        hora = cita.hora,
                        aviso = "Cita programada",
                        imagenUri = mascota?.imagenUri,
                        especie = mascota?.especie.orEmpty()
                    )
                }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun descartarNotificacion(id: String) {
        descartadas.value = descartadas.value + id
    }

    // ------------------------------------------------------------------
    // Mascotas
    // ------------------------------------------------------------------

    // Estos metodos llevan cuerpo con llaves en vez de "= enSegundoPlano {...}" porque
    // asi devuelven Unit. Con "=" devolvian el Job de la corrutina, y un override no
    // puede cambiar el tipo de retorno que declara la interfaz. El trabajo que hacen
    // es exactamente el mismo.
    override fun addPet(pet: Mascota) {
        enSegundoPlano { repository.addPet(pet) }
    }

    override fun updatePet(pet: Mascota) {
        enSegundoPlano { repository.updatePet(pet) }
    }

    override fun deletePet(pet: Mascota) {
        enSegundoPlano { repository.deletePet(pet) }
    }

    suspend fun getPetById(id: String): Mascota? = repository.getPetById(id)

    /**
     * Sube la foto de una mascota a Cloudinary.
     *
     * Antes se guardaba la ruta del archivo en el propio telefono, asi que la foto
     * solo existia en ese dispositivo: si el cliente cambiaba de movil o el doctor
     * abria la ficha, no habia foto. Ahora se sube y se guarda la URL, asi que la ve
     * cualquiera con permiso.
     *
     * subiendoFoto sirve para enseñar una ruedecita: subir una foto tarda lo suyo y
     * sin aviso el usuario cree que no funciono y le da al boton otra vez.
     */
    var subiendoFoto by mutableStateOf(false)
        private set

    override fun subirFotoMascota(mascotaId: String, archivoLocal: Uri) {
        enSegundoPlano {
            subiendoFoto = true
            try {
                repository.actualizarFotoMascota(mascotaId, archivoLocal)
            } finally {
                // En finally para que la ruedecita se apague tambien si la subida falla.
                subiendoFoto = false
            }
        }
    }

    fun subirFotoPerfil(archivoLocal: Uri) = enSegundoPlano {
        subiendoFoto = true
        try {
            repository.actualizarFotoPerfil(archivoLocal)
        } finally {
            subiendoFoto = false
        }
    }

    // ------------------------------------------------------------------
    // Citas
    // ------------------------------------------------------------------

    fun addAppointment(cita: Cita) = enSegundoPlano { repository.addAppointment(cita) }

    fun updateAppointment(cita: Cita) = enSegundoPlano { repository.updateAppointment(cita) }

    /**
     * El cliente anula su cita.
     *
     * DOS COSAS CAMBIARON AQUI, Y LAS DOS ERAN INCOHERENCIAS DE VERDAD:
     *
     * 1. Antes la cita se BORRABA de la base de datos. Para el cliente parecia bien,
     *    pero desde la clinica la cita desaparecia de la agenda sin dejar rastro: nadie
     *    se enteraba de que se habia anulado ni de quien falto. Ahora se marca como
     *    "Cancelada", que es el estado que ya existia y que la agenda de la clinica sabe
     *    filtrar. El hueco tambien vuelve a quedar libre para otro cliente.
     *
     * 2. Habia tambien un boton "Completar" en la app del CLIENTE que daba la cita por
     *    atendida, escribia un registro en el historial medico de la mascota y borraba
     *    la cita. Se ha quitado: quien da una consulta por terminada es el veterinario
     *    que la atendio. Ese trabajo ahora lo hace StaffViewModel.completarCita(), que
     *    es donde tiene sentido.
     */
    fun cancelAppointment(cita: Cita) = enSegundoPlano {
        repository.updateAppointment(cita.copy(estado = Cita.ESTADO_CANCELADA))
    }

    // ------------------------------------------------------------------
    // Historial medico
    // ------------------------------------------------------------------

    override fun addMedicalRecord(petId: String, record: RegistroHistorial, newAllergy: String) {
        enSegundoPlano { repository.addMedicalRecord(petId, record, newAllergy) }
    }

    override fun updateMedicalRecord(petId: String, record: RegistroHistorial) {
        enSegundoPlano { repository.updateMedicalRecord(petId, record) }
    }

    override fun deleteMedicalRecord(petId: String, recordId: Int) {
        enSegundoPlano { repository.deleteMedicalRecord(petId, recordId) }
    }

    override fun updateMedicalNotes(petId: String, notes: String) {
        enSegundoPlano { repository.updateMedicalNotes(petId, notes) }
    }

    override fun deleteAllergy(petId: String, allergy: String) {
        enSegundoPlano { repository.deleteAllergy(petId, allergy) }
    }

    // ------------------------------------------------------------------
    // Perfil
    // ------------------------------------------------------------------

    fun updateUsuario(nuevo: Usuario) = enSegundoPlano { repository.updateProfile(nuevo) }
}

/**
 * Un ViewModel no se crea con "PetViewModel(...)" directamente porque Android
 * necesita poder recrearlo al girar la pantalla. Esta fabrica le explica al
 * sistema como construirlo cuando lleva parametros (aqui, el repositorio).
 */
class PetViewModelFactory(
    private val repository: PetRepository,
    private val correo: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(PetViewModel::class.java)) {
            "Clase de ViewModel desconocida: ${modelClass.name}"
        }
        @Suppress("UNCHECKED_CAST")
        return PetViewModel(repository, correo) as T
    }
}
