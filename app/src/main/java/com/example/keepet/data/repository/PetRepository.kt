package com.example.keepet.data.repository

import android.net.Uri
import com.example.keepet.data.model.Cita
import com.example.keepet.data.model.Mascota
import com.example.keepet.data.model.RegistroHistorial
import com.example.keepet.data.model.Usuario
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Datos del CLIENTE: sus mascotas, sus citas y su perfil.
 *
 * Como queda el arbol de la base de datos ahora que hay clinica:
 *
 *   roles
 *     └── {uid} : "cliente" | "empleado" | "doctor" | "admin"
 *
 *   usuarios
 *     └── {uid}
 *          ├── perfil   : { nombre, telefono, ... }
 *          └── mascotas
 *                └── {mascotaId} : { nombre, especie, imagenUri, historial: [...] }
 *
 *   citas                              <-- FUERA de usuarios, comun a toda la clinica
 *     └── {citaId} : { clienteUid, mascotaId, fecha, hora, estado, doctorUid, ... }
 *
 * Las citas se sacaron de dentro del cliente a proposito. Recepcion necesita ver la
 * agenda del dia completa; si cada cita viviera colgada de su cliente, habria que
 * recorrer los nodos de todos los clientes para armar una sola pantalla.
 */
class PetRepository(
    private val uid: String,
    private val imagenes: CloudinaryRepository = CloudinaryRepository()
) {

    private val db = FirebaseDatabase.getInstance().reference
    private val miRaiz: DatabaseReference = db.child("usuarios").child(uid)

    private val mascotasRef = miRaiz.child("mascotas")
    private val perfilRef = miRaiz.child("perfil")

    /** Nodo comun de citas de toda la clinica. */
    private val citasRef = db.child("citas")

    // ---------------------------------------------------------------------
    // Lectura en tiempo real
    // ---------------------------------------------------------------------

    val allPets: Flow<List<Mascota>> = mascotasRef.flowDeHijos { snap ->
        snap.getValue(Mascota::class.java)?.copy(id = snap.key.orEmpty(), duenoUid = uid)
    }

    /**
     * Solo MIS citas.
     *
     * orderByChild + equalTo es lo que en SQL seria "WHERE clienteUid = ...".
     * Las reglas de seguridad estan escritas para aceptar exactamente esta consulta
     * y rechazarla si intentas filtrar por el uid de otra persona, asi que un
     * cliente no puede leer la agenda de la clinica ni las citas de nadie mas.
     */
    val allAppointments: Flow<List<Cita>> =
        citasRef.orderByChild("clienteUid").equalTo(uid).flowDeHijos { snap ->
            snap.getValue(Cita::class.java)?.copy(id = snap.key.orEmpty())
        }

    val profile: Flow<Usuario> = perfilRef.flowDeValor { snap ->
        snap.getValue(Usuario::class.java) ?: Usuario()
    }

    // ---------------------------------------------------------------------
    // Mascotas
    // ---------------------------------------------------------------------

    suspend fun addPet(pet: Mascota) {
        mascotasRef.push().setValue(pet.copy(id = "", duenoUid = "")).await()
    }

    suspend fun updatePet(pet: Mascota) {
        if (pet.id.isBlank()) return
        mascotasRef.child(pet.id).setValue(pet.copy(duenoUid = "")).await()
    }

    /**
     * Borra la mascota y sus citas.
     *
     * La foto NO se borra de Cloudinary, y es una limitacion asumida a proposito:
     * borrar un archivo requiere el api_secret de la cuenta, y ese secreto no puede
     * vivir dentro de la app (un APK se descompila en dos minutos). Asi que la imagen
     * se queda huerfana en Cloudinary ocupando espacio. Con 25 GB gratis no es un
     * problema real; si algun dia molesta, se borran a mano desde el panel de
     * Cloudinary o con una funcion en un servidor, que si puede guardar el secreto.
     */
    suspend fun deletePet(pet: Mascota) {
        if (pet.id.isBlank()) return
        citasRef.orderByChild("mascotaId").equalTo(pet.id).get().await()
            .children.forEach { it.ref.removeValue().await() }
        mascotasRef.child(pet.id).removeValue().await()
    }

    suspend fun getPetById(id: String): Mascota? {
        if (id.isBlank()) return null
        val snap = mascotasRef.child(id).get().await()
        return snap.getValue(Mascota::class.java)?.copy(id = snap.key.orEmpty(), duenoUid = uid)
    }

    /**
     * Sube la foto de la mascota a Cloudinary y guarda en la base de datos la URL
     * que devuelve, en el campo de texto `imagenUri`.
     *
     * El orden importa: primero se sube y solo si la subida sale bien se guarda la
     * URL. Al contrario tendrias en la base de datos la direccion de una foto que
     * no existe, y la app enseñaria un hueco roto.
     */
    suspend fun actualizarFotoMascota(mascotaId: String, archivoLocal: Uri) {
        if (mascotaId.isBlank()) return
        val url = imagenes.subirFotoMascota(mascotaId, archivoLocal)
        mascotasRef.child(mascotaId).child("imagenUri").setValue(url).await()
    }

    // ---------------------------------------------------------------------
    // Citas
    // ---------------------------------------------------------------------

    suspend fun addAppointment(cita: Cita) {
        citasRef.push().setValue(
            cita.copy(
                id = "",
                // El repositorio pone el dueño: asi una pantalla no puede crear por
                // error una cita a nombre de otro cliente.
                clienteUid = uid,
                creadaEn = System.currentTimeMillis()
            )
        ).await()
    }

    suspend fun updateAppointment(cita: Cita) {
        if (cita.id.isBlank()) return
        citasRef.child(cita.id).setValue(cita.copy(clienteUid = uid)).await()
    }

    suspend fun deleteAppointment(citaId: String) {
        if (citaId.isBlank()) return
        citasRef.child(citaId).removeValue().await()
    }

    suspend fun getAppointmentById(id: String): Cita? {
        if (id.isBlank()) return null
        val snap = citasRef.child(id).get().await()
        return snap.getValue(Cita::class.java)?.copy(id = snap.key.orEmpty())
    }

    // ---------------------------------------------------------------------
    // Historial medico
    // ---------------------------------------------------------------------

    suspend fun addMedicalRecord(petId: String, record: RegistroHistorial, newAllergy: String) {
        val pet = getPetById(petId) ?: return
        val alergias = if (newAllergy.isNotBlank()) pet.alergias + newAllergy else pet.alergias
        updatePet(pet.copy(historial = pet.historial + record, alergias = alergias))
    }

    suspend fun updateMedicalRecord(petId: String, record: RegistroHistorial) {
        val pet = getPetById(petId) ?: return
        updatePet(pet.copy(historial = pet.historial.map { if (it.id == record.id) record else it }))
    }

    suspend fun deleteMedicalRecord(petId: String, recordId: Int) {
        val pet = getPetById(petId) ?: return
        updatePet(pet.copy(historial = pet.historial.filter { it.id != recordId }))
    }

    suspend fun updateMedicalNotes(petId: String, notes: String) {
        val pet = getPetById(petId) ?: return
        updatePet(pet.copy(notasMedicas = notes))
    }

    suspend fun deleteAllergy(petId: String, allergy: String) {
        val pet = getPetById(petId) ?: return
        updatePet(pet.copy(alergias = pet.alergias.filter { it != allergy }))
    }

    // ---------------------------------------------------------------------
    // Perfil
    // ---------------------------------------------------------------------

    suspend fun updateProfile(usuario: Usuario) {
        perfilRef.setValue(usuario).await()
    }

    /** Sube la foto de perfil a Cloudinary y guarda su URL. */
    suspend fun actualizarFotoPerfil(archivoLocal: Uri) {
        val url = imagenes.subirFotoPerfil(uid, archivoLocal)
        perfilRef.child("imagenUri").setValue(url).await()
    }

    // ---------------------------------------------------------------------
    // Preparacion y migracion
    // ---------------------------------------------------------------------

    /**
     * Crea el perfil la primera vez que entra un usuario.
     *
     * ANTES CREABA TAMBIEN TRES MASCOTAS DE EJEMPLO (Milo, Oliver y Copito) y se han
     * quitado. En una app de escaparate quedaban bonitas, pero en una clinica de verdad
     * cada cliente que se registraba metia tres animales inventados en la lista de
     * pacientes del veterinario, con telefonos falsos y una alergia al polen que nadie
     * habia diagnosticado. Un expediente medico solo debe contener lo que alguien ha
     * escrito a proposito.
     *
     * Si quieres datos para una demostracion, creales el expediente desde la pestaña
     * Expedientes de la clinica: es la forma en la que se van a crear de verdad.
     */
    suspend fun seedIfNewUser(correo: String) {
        migrarCitasAntiguas()

        if (perfilRef.get().await().exists()) return

        perfilRef.setValue(Usuario(nombre = correo.substringBefore("@"), correo = correo)).await()
    }

    /**
     * Mueve las citas de la version anterior de la app al nodo nuevo.
     *
     * Antes vivian en /usuarios/{uid}/citas y ahora van en /citas. Sin esto, las
     * citas que ya tuvieras creadas seguirian en la base de datos pero la app no
     * las encontraria: parecerian borradas.
     *
     * Se ejecuta al entrar y, si no hay nada que mover, no hace nada. Cuando ya no
     * queden usuarios con datos viejos puedes borrar este metodo.
     *
     * Cada cita se copia y se borra de una en una, no todas y luego un borrado
     * final. Es a proposito: si se corta la conexion a mitad, con el borrado al
     * final las citas ya copiadas se volverian a copiar en el siguiente arranque y
     * saldrian duplicadas. Borrando una a una, lo que se haya movido esta movido y
     * lo que quede se reintenta, sin repetir nada.
     */
    private suspend fun migrarCitasAntiguas() {
        try {
            val viejas = miRaiz.child("citas").get().await()
            if (!viejas.exists()) return

            // El nombre del cliente no existia en las citas viejas; se rellena con
            // el del perfil para que el personal no vea la agenda sin nombres.
            val nombre = perfilRef.child("nombre").get().await()
                .getValue(String::class.java).orEmpty()

            viejas.children.forEach { snap ->
                val cita = snap.getValue(Cita::class.java)
                if (cita != null) {
                    citasRef.push().setValue(
                        cita.copy(
                            clienteUid = uid,
                            clienteNombre = cita.clienteNombre.ifBlank { nombre },
                            creadaEn = System.currentTimeMillis()
                        )
                    ).await()
                }
                snap.ref.removeValue().await()
            }
        } catch (_: Exception) {
            // Si falla (sin conexion, por ejemplo) se reintentara al proximo arranque.
        }
    }
}

// -------------------------------------------------------------------------
// Ayudantes: convierten los "listeners" de Firebase en Flow de Kotlin
// -------------------------------------------------------------------------

/**
 * Escucha una LISTA de hijos y avisa en cada cambio.
 *
 * Recibe Query en vez de DatabaseReference para que valga tanto para un nodo
 * entero ("todas mis mascotas") como para una consulta filtrada ("las citas cuyo
 * clienteUid sea el mio"). DatabaseReference ES una Query, asi que sirve para ambos.
 */
private fun <T : Any> Query.flowDeHijos(
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

/** Escucha UN solo valor (ej: el perfil). */
private fun <T : Any> Query.flowDeValor(
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
