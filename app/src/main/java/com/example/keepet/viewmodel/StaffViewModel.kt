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
import com.example.keepet.data.model.RegistroHistorial
import com.example.keepet.data.model.Rol
import com.example.keepet.data.model.UsuarioConRol
import com.example.keepet.data.repository.StaffRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Filtros de la agenda. */
enum class FiltroAgenda(val etiqueta: String) {
    ACTIVAS("Activas"),
    HOY("Hoy"),
    COMPLETADAS("Completadas"),
    CANCELADAS("Canceladas"),
    TODAS("Todas")
}

/**
 * ViewModel del personal de la clinica (empleado, doctor y admin).
 *
 * Va aparte de PetViewModel porque las pantallas del personal no necesitan nada de
 * "mis mascotas" y si necesitan cosas que un cliente no debe poder ni pedir.
 * Mantenerlos separados hace que sea imposible llamar por descuido a una funcion de
 * administracion desde una pantalla de cliente: alli ese ViewModel no existe.
 */
class StaffViewModel(
    private val repo: StaffRepository,
    val miUid: String,
    private val miCorreo: String,
    val miRol: Rol
) : ViewModel(), GestorDeMascotas {

    // Igual que el resto de flujos de este ViewModel (mira sinTumbarLaApp() mas abajo):
    // sin el catch, perder el permiso al cerrar sesion cierra este Flow con una
    // excepcion que sube por stateIn y mata la app. A este se le habia escapado.
    private val miPerfil = repo.perfilDe(miUid)
        .catch { emit(com.example.keepet.data.model.Usuario()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = com.example.keepet.data.model.Usuario()
        )

    /**
     * Como se llama el empleado que esta usando la app.
     *
     * Se lee de su perfil y, mientras eso llega (o si nunca puso nombre), se usa la
     * parte del correo antes de la arroba. Asi nunca se queda vacio, que es lo que
     * pasaria si la cita se asignara a un doctor "sin nombre".
     */
    val miNombre: String
        get() = miPerfil.value.nombre.ifBlank { miCorreo.substringBefore("@") }

    // ------------------------------------------------------------------
    // Agenda
    // ------------------------------------------------------------------

    private val filtro = MutableStateFlow(FiltroAgenda.ACTIVAS)
    val filtroActual: StateFlow<FiltroAgenda> = filtro

    private val busqueda = MutableStateFlow("")
    val textoBusqueda: StateFlow<String> = busqueda

    fun cambiarFiltro(nuevo: FiltroAgenda) {
        filtro.value = nuevo
    }

    fun buscar(texto: String) {
        busqueda.value = texto
    }

    private val todasLasCitas: StateFlow<List<Cita>> = repo.todasLasCitas
        .sinTumbarLaApp("No se pudo cargar la agenda")
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    /**
     * La agenda ya filtrada y ordenada, lista para pintar.
     *
     * El filtrado se hace aqui y no en la pantalla para que la pantalla solo tenga
     * que dibujar. Ademas asi el filtro sobrevive a que la pantalla se recree.
     */
    val agenda: StateFlow<List<Cita>> =
        combine(todasLasCitas, filtro, busqueda) { citas, f, texto ->
            val filtradas = citas
                .filter { cita ->
                    when (f) {
                        FiltroAgenda.ACTIVAS -> cita.estado in Cita.ESTADOS_ACTIVOS
                        FiltroAgenda.HOY -> esDeHoy(cita)
                        FiltroAgenda.COMPLETADAS -> cita.estado == Cita.ESTADO_COMPLETADA
                        FiltroAgenda.CANCELADAS -> cita.estado == Cita.ESTADO_CANCELADA
                        FiltroAgenda.TODAS -> true
                    }
                }
                .filter { cita ->
                    texto.isBlank() ||
                        cita.nombreMascota.contains(texto, true) ||
                        cita.clienteNombre.contains(texto, true) ||
                        cita.codigo.contains(texto, true)
                }

            // ORDEN. Antes se ordenaba por creadaEn (cuando se pidio la cita) y, de
            // apoyo, por el texto de la fecha. Las dos cosas estaban mal para una agenda:
            //   - a recepcion no le importa cuando se pidio la cita, sino cuando toca;
            //   - ordenar "16 de agosto" como texto es ordenarlo alfabeticamente, asi
            //     que "10 de agosto" salia antes que "2 de mayo".
            // Ahora se usa claveOrden (mira Cita.kt), que junta fecha y hora en un texto
            // que si se puede comparar. Lo que esta abierto se ordena de lo mas proximo
            // a lo mas lejano (la siguiente cita, arriba) y lo ya cerrado al contrario,
            // de lo mas reciente a lo mas viejo, que es como se consulta un archivo.
            when (f) {
                FiltroAgenda.ACTIVAS, FiltroAgenda.HOY -> filtradas.sortedBy { it.claveOrden }
                else -> filtradas.sortedByDescending { it.claveOrden }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    /** Cuantas citas hay en cada estado, para los contadores de arriba. */
    val resumen: StateFlow<Map<String, Int>> = todasLasCitas.map { citas ->
        mapOf(
            Cita.ESTADO_PENDIENTE to citas.count { it.estado == Cita.ESTADO_PENDIENTE },
            Cita.ESTADO_CONFIRMADA to citas.count { it.estado == Cita.ESTADO_CONFIRMADA },
            Cita.ESTADO_COMPLETADA to citas.count { it.estado == Cita.ESTADO_COMPLETADA }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyMap()
    )

    // ------------------------------------------------------------------
    // Pacientes y usuarios
    // ------------------------------------------------------------------

    val pacientes: StateFlow<List<Mascota>> = repo.todosLosPacientes
        .sinTumbarLaApp("No se pudieron cargar los pacientes")
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val usuarios: StateFlow<List<UsuarioConRol>> = repo.usuariosConRol
        .sinTumbarLaApp("No se pudo cargar la lista de usuarios")
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val doctores: StateFlow<List<UsuarioConRol>> = repo.doctores
        .sinTumbarLaApp("No se pudo cargar la lista de doctores")
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    /**
     * Los clientes de la clinica, para elegir a quien pertenece un expediente nuevo.
     *
     * Se deriva de `usuarios`, que ya es un StateFlow, en vez de pedirle otra lista al
     * repositorio. Es a proposito: cada flujo del repositorio abre sus propios
     * listeners contra Firebase, y esta lista es exactamente la misma ya filtrada.
     */
    val clientes: StateFlow<List<UsuarioConRol>> = usuarios.map { lista ->
        lista.filter { it.rol == Rol.CLIENTE }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    /**
     * Convierte un error del flujo en un mensaje en pantalla, en vez de en un cierre
     * de la app.
     *
     * POR QUE EXISTE ESTO: cuando Firebase deniega un permiso, el listener avisa por
     * onCancelled, el repositorio cierra el Flow con esa excepcion, y una excepcion
     * dentro de stateIn sube por viewModelScope hasta el hilo principal y **mata el
     * proceso**. Eso es exactamente lo que pasaba al entrar como doctor: la regla de
     * /roles solo dejaba leer al admin y la app se cerraba de golpe.
     *
     * La regla ya esta corregida, pero esto se queda igualmente: una pantalla que no
     * puede cargar una lista debe quedarse vacia y avisar, nunca cerrarse. Si algun
     * dia una regla se toca mal, se vera un aviso en vez de un cierre inexplicable.
     */
    private fun <T> Flow<List<T>>.sinTumbarLaApp(aviso: String): Flow<List<T>> =
        catch { error ->
            mensaje = "$aviso (${error.localizedMessage})"
            emit(emptyList())
        }

    // ------------------------------------------------------------------
    // Mensajes para la pantalla
    // ------------------------------------------------------------------

    var mensaje by mutableStateOf<String?>(null)
        private set

    /** Cita encontrada al escanear un QR o buscar un codigo. Se enseña en un dialogo. */
    var citaEscaneada by mutableStateOf<Cita?>(null)
        private set

    fun limpiarMensaje() {
        mensaje = null
    }

    fun cerrarCitaEscaneada() {
        citaEscaneada = null
    }

    // ------------------------------------------------------------------
    // Acciones
    // ------------------------------------------------------------------

    fun confirmarLlegada(citaId: String) = enSegundoPlano {
        repo.cambiarEstadoCita(citaId, Cita.ESTADO_CONFIRMADA)
        mensaje = "Llegada registrada"
    }

    fun pasarAAtencion(citaId: String) = enSegundoPlano {
        repo.cambiarEstadoCita(citaId, Cita.ESTADO_EN_ATENCION)
    }

    /**
     * Da la consulta por terminada Y la deja anotada en el expediente del paciente.
     *
     * Antes esto solo cambiaba el estado de la cita: el animal salia de la clinica y en
     * su historial medico no quedaba nada escrito. Lo curioso es que el registro SI se
     * escribia... desde la app del CLIENTE, con un boton "Completar" que tenia el dueño.
     * Es decir: la unica forma de que una visita apareciera en el historial era que el
     * cliente le diera a un boton en su telefono. Ese boton se ha quitado y el trabajo
     * esta ahora aqui, que es donde ocurre la consulta de verdad.
     *
     * Se lee la cita primero por dos motivos: para saber de que cliente y de que mascota
     * es (una cita solo trae el id), y para no anotar dos veces lo mismo si alguien pulsa
     * "Completar" dos veces o si dos empleados lo hacen a la vez.
     *
     * El registro solo lo escribe quien puede escribir en el historial. Si lo completa un
     * empleado de recepcion, la cita se cierra igual pero sin anotacion medica: es mejor
     * que no haya registro que uno firmado por quien no atendio al animal.
     */
    fun completarCita(citaId: String) = enSegundoPlano {
        val cita = repo.buscarCitaPorId(citaId)
        if (cita == null) {
            mensaje = "No se encontró esa cita"
            return@enSegundoPlano
        }
        if (cita.estado == Cita.ESTADO_COMPLETADA) {
            mensaje = "Esa cita ya estaba completada"
            return@enSegundoPlano
        }

        repo.cambiarEstadoCita(citaId, Cita.ESTADO_COMPLETADA)

        val servicio = cita.tipoServicio.ifBlank { cita.motivo }
        if (miRol.puedeEditarHistorial &&
            cita.clienteUid.isNotBlank() &&
            cita.mascotaId.isNotBlank()
        ) {
            repo.agregarRegistroMedico(
                duenoUid = cita.clienteUid,
                mascotaId = cita.mascotaId,
                registro = RegistroHistorial(
                    servicio = servicio.ifBlank { "Consulta" },
                    // La fecha de la VISITA, no la de hoy: una cita se puede cerrar al
                    // dia siguiente y el historial tiene que decir cuando se atendio.
                    fecha = cita.fecha,
                    detalles = listOf(
                        "Cita $servicio completada en la clínica.",
                        if (cita.doctorNombre.isNotBlank()) "Atendió: ${cita.doctorNombre}." else "",
                        if (cita.notasAdicionales.isNotBlank()) "Notas: ${cita.notasAdicionales}" else ""
                    ).filter { it.isNotBlank() }.joinToString(" "),
                    tipoIcono = iconoDeServicio(servicio)
                )
            )
            mensaje = "Cita completada y anotada en el historial"
        } else {
            mensaje = "Cita completada"
        }
    }

    /**
     * Que dibujito le toca a un registro del historial.
     *
     * Estaba escrito en PetViewModel, dentro del boton del cliente que se ha quitado.
     * Se ha traido tal cual para no perder los iconos de baño y de vacuna que ya
     * distinguen los registros antiguos.
     */
    private fun iconoDeServicio(servicio: String): String = when {
        servicio.contains("Baño", true) -> "Bano"
        servicio.contains("Vacuna", true) -> "Vacuna"
        else -> "Consulta"
    }

    fun cancelarCita(citaId: String) = enSegundoPlano {
        repo.cambiarEstadoCita(citaId, Cita.ESTADO_CANCELADA)
        mensaje = "Cita cancelada"
    }

    fun asignarme(citaId: String) = enSegundoPlano {
        repo.asignarDoctor(citaId, miUid, miNombre)
        mensaje = "Cita asignada a ti"
    }

    fun asignarDoctor(citaId: String, doctor: UsuarioConRol) = enSegundoPlano {
        repo.asignarDoctor(citaId, doctor.uid, doctor.nombreVisible)
        mensaje = "Asignada a ${doctor.nombreVisible}"
    }

    /** Procesa lo que devolvio el escaner de QR. */
    fun procesarQr(idCita: String?) = enSegundoPlano {
        if (idCita == null) {
            mensaje = "Ese código QR no es de una cita de KeePet"
            return@enSegundoPlano
        }
        val cita = repo.buscarCitaPorId(idCita)
        if (cita == null) {
            mensaje = "No se encontró ninguna cita con ese código"
        } else {
            citaEscaneada = cita
        }
    }

    /** Busca por el codigo corto de 6 caracteres, para cuando no se puede escanear. */
    fun buscarPorCodigo(codigo: String) = enSegundoPlano {
        val cita = repo.buscarCitaPorCodigo(codigo)
        if (cita == null) {
            mensaje = "No hay ninguna cita con el código ${codigo.uppercase()}"
        } else {
            citaEscaneada = cita
        }
    }

    fun agregarRegistroMedico(
        duenoUid: String,
        mascotaId: String,
        servicio: String,
        detalles: String,
        fecha: String
    ) = enSegundoPlano {
        if (!miRol.puedeEditarHistorial) {
            mensaje = "Solo un doctor puede escribir en el historial"
            return@enSegundoPlano
        }
        repo.agregarRegistroMedico(
            duenoUid = duenoUid,
            mascotaId = mascotaId,
            registro = RegistroHistorial(
                servicio = servicio,
                fecha = fecha,
                detalles = detalles,
                tipoIcono = "Consulta"
            )
        )
        mensaje = "Registro añadido al historial"
    }

    fun cambiarRol(usuario: UsuarioConRol, nuevoRol: Rol) = enSegundoPlano {
        if (!miRol.puedeGestionarUsuarios) {
            mensaje = "Solo un administrador puede cambiar roles"
            return@enSegundoPlano
        }
        if (usuario.uid == miUid) {
            // Si un admin se quitara a si mismo el rol de admin y fuera el unico,
            // nadie podria volver a repartir roles sin entrar a la consola de Firebase.
            mensaje = "No puedes cambiar tu propio rol"
            return@enSegundoPlano
        }
        repo.cambiarRol(usuario.uid, nuevoRol)
        mensaje = "${usuario.nombreVisible} ahora es ${nuevoRol.etiqueta}"
    }

    // ------------------------------------------------------------------
    // Expedientes (implementacion de GestorDeMascotas)
    // ------------------------------------------------------------------
    //
    // Esto es lo que permite que la pestaña Expedientes del personal reutilice las
    // MISMAS pantallas que usa el cliente (AddPetScreen y PetDetailScreen) en vez de
    // tener una copia propia que habria que mantener en paralelo.
    //
    // Respecto al lado del cliente cambia una sola cosa, pero es la importante: alli el
    // dueño de la mascota es siempre quien inicio sesion, asi que no hay nada que
    // averiguar. Aqui el personal trabaja sobre mascotas de otras personas, asi que
    // cada escritura necesita saber PRIMERO en la rama de que cliente va.

    /**
     * Para el personal, "todas las mascotas" son los pacientes de la clinica.
     *
     * Se expone con get() y no con "=" para no crear un segundo flujo: es exactamente
     * la misma lista que ya alimenta la pestaña Pacientes, con un nombre distinto
     * porque es el que espera la pantalla compartida.
     */
    override val allPets: StateFlow<List<Mascota>> get() = pacientes

    /**
     * Doctores y admin escriben en la parte clinica; recepcion no.
     *
     * Es la misma comprobacion que ya decidia si aparece el boton de "Añadir al
     * historial" en la pestaña Pacientes, ahora tambien para el expediente completo.
     * Asi un empleado ve el historial y puede corregir el telefono del dueño, pero no
     * firma diagnosticos.
     */
    override val puedeEditarDatosClinicos: Boolean get() = miRol.puedeEditarHistorial

    /**
     * Cliente sobre cuyo expediente se esta trabajando ahora mismo.
     *
     * Hace falta sobre todo al CREAR: una mascota nueva no existe todavia en ninguna
     * lista, asi que no hay forma de deducir de quien es. La pantalla lo deja aqui
     * antes de abrir el formulario (al crear, el cliente elegido; al editar, el dueño
     * de la mascota que se abre).
     */
    var duenoEnFoco by mutableStateOf("")
        private set

    fun trabajarSobreCliente(duenoUid: String) {
        duenoEnFoco = duenoUid
    }

    /**
     * De quien es esta mascota. Se intenta del dato mas fiable al menos fiable:
     *
     *   1. el duenoUid que ya trae la mascota (lo rellena StaffRepository al leerla);
     *   2. buscarla por id entre los pacientes. Hace falta porque AddPetScreen arma una
     *      Mascota nueva con los campos del formulario, y duenoUid no es un campo del
     *      formulario: llega vacio incluso al editar una mascota existente;
     *   3. el cliente que la pantalla dejo en duenoEnFoco, que es el caso de crear.
     *
     * Si los tres fallan devuelve cadena vacia, y quien llama cancela la operacion con
     * un aviso. Nunca se adivina: escribir en la rama equivocada crearia una mascota
     * duplicada colgada del cliente que no es.
     */
    private fun duenoDe(mascota: Mascota): String =
        mascota.duenoUid
            .ifBlank { duenoDeMascotaConId(mascota.id) }
            .ifBlank { duenoEnFoco }

    private fun duenoDeMascotaConId(mascotaId: String): String =
        pacientes.value.find { it.id == mascotaId }?.duenoUid.orEmpty()

    private fun duenoDePaciente(mascotaId: String): String =
        duenoDeMascotaConId(mascotaId).ifBlank { duenoEnFoco }

    /**
     * Ejecuta una escritura solo si se sabe de que cliente es la mascota.
     *
     * Sin esta comprobacion, un duenoUid vacio construiria la ruta
     * "/usuarios//mascotas", que Firebase rechaza con un error poco claro.
     */
    private fun conDueno(duenoUid: String, bloque: suspend (String) -> Unit) {
        if (duenoUid.isBlank()) {
            mensaje = "No se pudo identificar al dueño de esa mascota"
            return
        }
        enSegundoPlano { bloque(duenoUid) }
    }

    override fun addPet(pet: Mascota) {
        conDueno(duenoDe(pet)) { dueno ->
            repo.crearMascota(dueno, pet)
            mensaje = "Expediente de ${pet.nombre.ifBlank { "la mascota" }} creado"
        }
    }

    override fun updatePet(pet: Mascota) {
        conDueno(duenoDe(pet)) { dueno ->
            repo.actualizarMascota(dueno, pet)
            mensaje = "Expediente actualizado"
        }
    }

    override fun deletePet(pet: Mascota) {
        conDueno(duenoDe(pet)) { dueno ->
            repo.eliminarMascota(dueno, pet.id)
            mensaje = "Expediente eliminado"
        }
    }

    override fun subirFotoMascota(mascotaId: String, archivoLocal: Uri) {
        conDueno(duenoDePaciente(mascotaId)) { dueno ->
            repo.actualizarFotoMascota(dueno, mascotaId, archivoLocal)
        }
    }

    // Los cinco metodos que siguen leen la mascota, la modifican y la vuelven a
    // escribir completa. Es el mismo camino que sigue el cliente en PetRepository:
    // el historial y las alergias son listas guardadas DENTRO de la mascota, no nodos
    // aparte, asi que no se pueden tocar sin reescribir el registro.

    override fun addMedicalRecord(petId: String, record: RegistroHistorial, newAllergy: String) {
        conDueno(duenoDePaciente(petId)) { dueno ->
            val mascota = repo.obtenerMascota(dueno, petId) ?: return@conDueno
            repo.actualizarMascota(
                dueno,
                mascota.copy(
                    historial = mascota.historial + record,
                    alergias = if (newAllergy.isNotBlank()) mascota.alergias + newAllergy
                    else mascota.alergias
                )
            )
        }
    }

    override fun updateMedicalRecord(petId: String, record: RegistroHistorial) {
        conDueno(duenoDePaciente(petId)) { dueno ->
            val mascota = repo.obtenerMascota(dueno, petId) ?: return@conDueno
            repo.actualizarMascota(
                dueno,
                mascota.copy(
                    historial = mascota.historial.map { if (it.id == record.id) record else it }
                )
            )
        }
    }

    override fun deleteMedicalRecord(petId: String, recordId: Int) {
        conDueno(duenoDePaciente(petId)) { dueno ->
            val mascota = repo.obtenerMascota(dueno, petId) ?: return@conDueno
            repo.actualizarMascota(
                dueno,
                mascota.copy(historial = mascota.historial.filter { it.id != recordId })
            )
        }
    }

    override fun updateMedicalNotes(petId: String, notes: String) {
        conDueno(duenoDePaciente(petId)) { dueno ->
            val mascota = repo.obtenerMascota(dueno, petId) ?: return@conDueno
            repo.actualizarMascota(dueno, mascota.copy(notasMedicas = notes))
        }
    }

    override fun deleteAllergy(petId: String, allergy: String) {
        conDueno(duenoDePaciente(petId)) { dueno ->
            val mascota = repo.obtenerMascota(dueno, petId) ?: return@conDueno
            repo.actualizarMascota(
                dueno,
                mascota.copy(alergias = mascota.alergias.filter { it != allergy })
            )
        }
    }

    private fun enSegundoPlano(bloque: suspend () -> Unit) = viewModelScope.launch {
        try {
            bloque()
        } catch (e: Exception) {
            mensaje = e.localizedMessage ?: "No se pudo completar la acción"
        }
    }
}

/**
 * Fecha de hoy escrita como se le enseña a la gente ("12 de agosto").
 *
 * Se usa para RELLENAR fechas nuevas (el registro que escribe el doctor a mano) y para
 * enseñarlas, nunca para comparar: para eso esta esDeHoy(), justo debajo.
 */
fun fechaDeHoyTexto(): String = LocalDate.now()
    .format(DateTimeFormatter.ofPattern("d 'de' MMMM", Locale.forLanguageTag("es-ES")))

/**
 * ¿Es esta cita de hoy?
 *
 * ASI ESTABA ANTES: se comparaba el texto de la fecha ("16 de agosto") con el texto de
 * hoy. Dos problemas, y por eso el filtro "Hoy" del doctor no era de fiar:
 *
 *   1. Ese texto NO LLEVA AÑO. Una cita del 16 de agosto del año pasado contaba como
 *      "hoy". Con una clinica recien montada no se nota; al segundo año, si.
 *   2. Cualquier cambio en como se escribe la fecha (mayusculas, "16 ago" en vez de
 *      "16 de agosto") dejaba el filtro vacio para siempre, sin dar ningun error: solo
 *      parecia que no habia citas.
 *
 * Ahora se compara fechaIso ("2026-08-16"), que si lleva año. La comparacion de texto se
 * queda SOLO como respaldo para las citas creadas antes de que existiera ese campo, para
 * no tener que migrar nada en la base de datos.
 */
fun esDeHoy(cita: Cita): Boolean =
    if (cita.fechaIso.isNotBlank()) cita.fechaIso == LocalDate.now().toString()
    else cita.fecha == fechaDeHoyTexto()

/**
 * Como se le enseña al personal cuando es una cita: "Hoy · 08:00 AM".
 *
 * En la agenda de una clinica lo primero que se quiere saber es si algo es de hoy, de
 * mañana o de la semana que viene. Antes se pintaba solo "16 de agosto · 08:00 AM" y
 * habia que mirar el calendario del movil para saber si eso ya habia pasado.
 *
 * El año se añade solo cuando la cita no es de este año, para no llenar la pantalla de
 * "2026" en las citas de esta semana, que son casi todas.
 */
fun cuandoTexto(cita: Cita): String {
    val hoy = LocalDate.now()
    val dia = runCatching { LocalDate.parse(cita.fechaIso) }.getOrNull()

    val fechaLegible = when {
        dia == null -> cita.fecha.ifBlank { "Sin fecha" }
        dia == hoy -> "Hoy"
        dia == hoy.plusDays(1) -> "Mañana"
        dia == hoy.minusDays(1) -> "Ayer"
        dia.year != hoy.year -> "${cita.fecha} de ${dia.year}"
        else -> cita.fecha
    }

    return if (cita.hora.isBlank()) fechaLegible else "$fechaLegible · ${cita.hora}"
}

class StaffViewModelFactory(
    private val repository: StaffRepository,
    private val uid: String,
    private val correo: String,
    private val rol: Rol
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(StaffViewModel::class.java)) {
            "Clase de ViewModel desconocida: ${modelClass.name}"
        }
        @Suppress("UNCHECKED_CAST")
        return StaffViewModel(repository, uid, correo, rol) as T
    }
}
