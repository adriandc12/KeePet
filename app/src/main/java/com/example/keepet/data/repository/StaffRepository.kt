package com.example.keepet.data.repository

import android.net.Uri
import com.example.keepet.data.model.Cita
import com.example.keepet.data.model.Mascota
import com.example.keepet.data.model.RegistroHistorial
import com.example.keepet.data.model.Rol
import com.example.keepet.data.model.Usuario
import com.example.keepet.data.model.UsuarioConRol
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

/**
 * Datos que ve el PERSONAL de la clinica: la agenda completa, todos los pacientes
 * y (para el admin) la lista de usuarios con su rol.
 *
 * Esta separado de PetRepository a proposito. PetRepository esta atado a un uid
 * concreto: todo lo que hace es "mis mascotas", "mis citas". El personal necesita
 * lo contrario, mirar por encima de todos los clientes. Mezclar las dos cosas en
 * una clase acabaria en metodos con un parametro "uid" opcional que a veces es el
 * tuyo y a veces el de otro, que es justo como se cuelan los fallos de permisos.
 *
 * Nada de lo que hay aqui funciona si las reglas de seguridad no reconocen tu rol,
 * porque el permiso lo concede el servidor, no la app.
 */
class StaffRepository(
    private val imagenes: CloudinaryRepository = CloudinaryRepository()
) {

    private val db = FirebaseDatabase.getInstance().reference
    private val citasRef = db.child("citas")
    private val usuariosRef = db.child("usuarios")
    private val rolesRef = db.child("roles")

    /** Las mascotas de un cliente concreto: /usuarios/{duenoUid}/mascotas */
    private fun mascotasDe(duenoUid: String) =
        usuariosRef.child(duenoUid).child("mascotas")

    // ---------------------------------------------------------------------
    // Agenda
    // ---------------------------------------------------------------------

    /** TODAS las citas de la clinica, en tiempo real. */
    val todasLasCitas: Flow<List<Cita>> = citasRef.flowDeHijos { snap ->
        snap.getValue(Cita::class.java)?.copy(id = snap.key.orEmpty())
    }

    /** Cambia el estado de una cita (confirmar, atender, completar, cancelar). */
    suspend fun cambiarEstadoCita(citaId: String, nuevoEstado: String) {
        if (citaId.isBlank()) return
        citasRef.child(citaId).child("estado").setValue(nuevoEstado).await()
    }

    /** Asigna el veterinario que atendera la cita. */
    suspend fun asignarDoctor(citaId: String, doctorUid: String, doctorNombre: String) {
        if (citaId.isBlank()) return
        citasRef.child(citaId).updateChildren(
            mapOf(
                "doctorUid" to doctorUid,
                "doctorNombre" to doctorNombre
            )
        ).await()
    }

    /**
     * Busca una cita por su id. Es lo que se usa tras escanear un QR.
     *
     * Devuelve null si no existe, para poder avisar "ese codigo no corresponde a
     * ninguna cita" en vez de dejar la pantalla en blanco.
     */
    suspend fun buscarCitaPorId(citaId: String): Cita? {
        if (citaId.isBlank()) return null
        val snap = citasRef.child(citaId).get().await()
        return snap.getValue(Cita::class.java)?.copy(id = snap.key.orEmpty())
    }

    /**
     * Busca una cita por el codigo corto de 6 caracteres que ve el cliente.
     *
     * El codigo no se guarda en la base de datos (se deduce del id), asi que no se
     * puede consultar con orderByChild: hay que traer las citas y comparar aqui.
     * Para una clinica es perfectamente asumible, y evita guardar un dato duplicado
     * que podria acabar desincronizado.
     */
    suspend fun buscarCitaPorCodigo(codigo: String): Cita? {
        val buscado = codigo.trim().uppercase()
        if (buscado.isBlank()) return null
        return citasRef.get().await().children
            .mapNotNull { snap -> snap.getValue(Cita::class.java)?.copy(id = snap.key.orEmpty()) }
            .firstOrNull { it.codigo == buscado }
    }

    // ---------------------------------------------------------------------
    // Pacientes (mascotas de todos los clientes)
    // ---------------------------------------------------------------------

    /**
     * Todas las mascotas de la clinica, con el uid de su dueño puesto.
     *
     * Se lee /usuarios de una vez y se recorren los hijos. El uid del dueño es la
     * CLAVE del nodo padre, no un campo dentro de la mascota; por eso hace falta
     * recorrer asi y no basta con pedir las mascotas sueltas.
     */
    val todosLosPacientes: Flow<List<Mascota>> = usuariosRef.flowDeValor { raiz ->
        raiz.children.flatMap { usuarioSnap ->
            val duenoUid = usuarioSnap.key.orEmpty()
            usuarioSnap.child("mascotas").children.mapNotNull { mascotaSnap ->
                mascotaSnap.getValue(Mascota::class.java)
                    ?.copy(id = mascotaSnap.key.orEmpty(), duenoUid = duenoUid)
            }
        }
    }

    // ---------------------------------------------------------------------
    // Expedientes: crear, editar y borrar la mascota de un cliente
    // ---------------------------------------------------------------------
    //
    // Todo lo de esta seccion escribe en /usuarios/{duenoUid}/mascotas, es decir, en
    // la rama de OTRA persona. Eso solo funciona porque las reglas de seguridad tienen
    // un permiso explicito para el personal en ese nodo (mira firebase-database-rules
    // .json, bloque "mascotas"). Si esas reglas no estan publicadas, cada una de estas
    // funciones fallara con "Permission denied", y es lo correcto: el permiso lo
    // concede el servidor, no la app.
    //
    // El duenoUid se pasa siempre como parametro aparte y NO se guarda dentro del
    // registro (Mascota.duenoUid lleva @get:Exclude), porque ya esta implicito en la
    // ruta. Guardarlo tambien dentro seria tener el mismo dato en dos sitios, que es
    // como acaban desincronizandose las cosas.

    /**
     * Crea el expediente de una mascota para el cliente indicado.
     *
     * Usa push() igual que el cliente, asi que la clave la genera Firebase y no hay
     * riesgo de que dos personas creando a la vez se pisen el id.
     */
    suspend fun crearMascota(duenoUid: String, mascota: Mascota) {
        if (duenoUid.isBlank()) return
        mascotasDe(duenoUid).push().setValue(mascota.copy(id = "", duenoUid = "")).await()
    }

    /** Sobrescribe el expediente completo de una mascota de un cliente. */
    suspend fun actualizarMascota(duenoUid: String, mascota: Mascota) {
        if (duenoUid.isBlank() || mascota.id.isBlank()) return
        mascotasDe(duenoUid).child(mascota.id).setValue(mascota.copy(duenoUid = "")).await()
    }

    /**
     * Borra la mascota de un cliente y las citas que tuviera.
     *
     * Se borran tambien las citas por el mismo motivo que en el lado del cliente: una
     * cita que apunta a una mascota que ya no existe se queda en la agenda de la
     * clinica sin nombre y sin ficha que abrir.
     *
     * La foto NO se borra de Cloudinary; es la limitacion asumida de las subidas sin
     * firmar, explicada en CloudinaryRepository.
     */
    suspend fun eliminarMascota(duenoUid: String, mascotaId: String) {
        if (duenoUid.isBlank() || mascotaId.isBlank()) return
        citasRef.orderByChild("mascotaId").equalTo(mascotaId).get().await()
            .children.forEach { it.ref.removeValue().await() }
        mascotasDe(duenoUid).child(mascotaId).removeValue().await()
    }

    /** Lee una mascota concreta de un cliente. */
    suspend fun obtenerMascota(duenoUid: String, mascotaId: String): Mascota? {
        if (duenoUid.isBlank() || mascotaId.isBlank()) return null
        val snap = mascotasDe(duenoUid).child(mascotaId).get().await()
        return snap.getValue(Mascota::class.java)
            ?.copy(id = snap.key.orEmpty(), duenoUid = duenoUid)
    }

    /**
     * Sube una foto a Cloudinary y guarda su URL en la mascota del cliente.
     *
     * Primero se sube y solo si la subida sale bien se guarda la URL, igual que en el
     * lado del cliente: al contrario quedaria en la base de datos la direccion de una
     * foto que no existe.
     */
    suspend fun actualizarFotoMascota(duenoUid: String, mascotaId: String, archivoLocal: Uri) {
        if (duenoUid.isBlank() || mascotaId.isBlank()) return
        val url = imagenes.subirFotoMascota(mascotaId, archivoLocal)
        mascotasDe(duenoUid).child(mascotaId).child("imagenUri").setValue(url).await()
    }

    /**
     * Añade un registro al historial de la mascota de OTRO cliente.
     * Solo tiene sentido para doctores; la regla de seguridad lo comprueba aparte.
     */
    suspend fun agregarRegistroMedico(
        duenoUid: String,
        mascotaId: String,
        registro: RegistroHistorial
    ) {
        if (duenoUid.isBlank() || mascotaId.isBlank()) return
        val ref = usuariosRef.child(duenoUid).child("mascotas").child(mascotaId)
        val mascota = ref.get().await().getValue(Mascota::class.java) ?: return
        val siguienteId = (mascota.historial.maxOfOrNull { it.id } ?: 0) + 1
        ref.child("historial")
            .setValue(mascota.historial + registro.copy(id = siguienteId))
            .await()
    }

    /** Actualiza las notas medicas de la mascota de un cliente. */
    suspend fun actualizarNotasMedicas(duenoUid: String, mascotaId: String, notas: String) {
        if (duenoUid.isBlank() || mascotaId.isBlank()) return
        usuariosRef.child(duenoUid).child("mascotas").child(mascotaId)
            .child("notasMedicas").setValue(notas).await()
    }

    // ---------------------------------------------------------------------
    // Usuarios y roles (solo admin)
    // ---------------------------------------------------------------------

    /**
     * Lista de usuarios con su rol.
     *
     * Los perfiles estan en /usuarios y los roles en /roles, en ramas separadas,
     * asi que hay que leer las dos y juntarlas. Estan separadas justamente para que
     * un usuario pueda editar su perfil sin poder tocar su rol.
     */
    val usuariosConRol: Flow<List<UsuarioConRol>> = callbackFlow {
        var perfiles: Map<String, Usuario> = emptyMap()
        var roles: Map<String, Rol> = emptyMap()

        fun emitir() {
            // Se recorren los perfiles, no los roles: interesa la gente que existe.
            // Quien no tenga nodo en /roles se muestra como cliente.
            trySend(
                perfiles.map { (uid, perfil) ->
                    UsuarioConRol(uid = uid, perfil = perfil, rol = roles[uid] ?: Rol.CLIENTE)
                }.sortedBy { it.nombreVisible.lowercase() }
            )
        }

        val escuchaPerfiles = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                perfiles = snapshot.children.associate { usuarioSnap ->
                    usuarioSnap.key.orEmpty() to
                        (usuarioSnap.child("perfil").getValue(Usuario::class.java) ?: Usuario())
                }
                emitir()
            }

            // Con llaves, no con "=". Con "=" el tipo de retorno seria el Boolean que
            // devuelve close(), y la interfaz de Firebase exige Unit.
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        val escuchaRoles = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                roles = snapshot.children.associate { rolSnap ->
                    rolSnap.key.orEmpty() to Rol.desde(rolSnap.getValue(String::class.java))
                }
                emitir()
            }

            // Con llaves, no con "=". Con "=" el tipo de retorno seria el Boolean que
            // devuelve close(), y la interfaz de Firebase exige Unit.
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        usuariosRef.addValueEventListener(escuchaPerfiles)
        rolesRef.addValueEventListener(escuchaRoles)
        awaitClose {
            usuariosRef.removeEventListener(escuchaPerfiles)
            rolesRef.removeEventListener(escuchaRoles)
        }
    }

    /** El perfil de un usuario concreto. Se usa para saber el nombre del propio empleado. */
    fun perfilDe(uid: String): Flow<Usuario> =
        usuariosRef.child(uid).child("perfil").flowDeValor { snap ->
            snap.getValue(Usuario::class.java) ?: Usuario()
        }

    /** Cambia el rol de un usuario. Solo un admin puede; lo impone la regla. */
    suspend fun cambiarRol(uid: String, nuevoRol: Rol) {
        if (uid.isBlank()) return
        rolesRef.child(uid).setValue(nuevoRol.clave).await()
    }

    /**
     * Doctores disponibles, para poder asignarlos a una cita.
     * Se deriva de la lista de usuarios con un simple map, sin abrir otro listener.
     */
    val doctores: Flow<List<UsuarioConRol>> = usuariosConRol.map { lista ->
        lista.filter { it.rol == Rol.DOCTOR || it.rol == Rol.ADMIN }
    }
}

// -------------------------------------------------------------------------
// Ayudantes (los mismos que en PetRepository, pero privados de este archivo)
// -------------------------------------------------------------------------

private fun <T : Any> com.google.firebase.database.Query.flowDeHijos(
    convertir: (DataSnapshot) -> T?
): Flow<List<T>> = callbackFlow {
    val listener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            trySend(snapshot.children.mapNotNull(convertir))
        }

        override fun onCancelled(error: DatabaseError) {
            close(error.toException())
        }
    }
    addValueEventListener(listener)
    awaitClose { removeEventListener(listener) }
}

private fun <T : Any> com.google.firebase.database.Query.flowDeValor(
    convertir: (DataSnapshot) -> T
): Flow<T> = callbackFlow {
    val listener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            trySend(convertir(snapshot))
        }

        override fun onCancelled(error: DatabaseError) {
            close(error.toException())
        }
    }
    addValueEventListener(listener)
    awaitClose { removeEventListener(listener) }
}
