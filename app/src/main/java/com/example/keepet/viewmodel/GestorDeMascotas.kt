package com.example.keepet.viewmodel

import android.net.Uri
import com.example.keepet.data.model.Mascota
import com.example.keepet.data.model.RegistroHistorial
import kotlinx.coroutines.flow.StateFlow

/**
 * Lo que necesita una pantalla de expedientes para funcionar, sin decir QUIEN lo hace.
 *
 * POR QUE EXISTE ESTO
 * ------------------
 * Las pantallas de expediente (AddPetScreen y PetDetailScreen) estaban escritas contra
 * PetViewModel, que esta atado al uid de quien inicio sesion: todo lo que hace es
 * "MIS mascotas". Eso esta perfecto para el cliente, pero el personal de la clinica
 * necesita las mismas pantallas trabajando sobre la mascota de OTRA persona.
 *
 * Habia dos formas de conseguirlo:
 *
 *   a) Duplicar las pantallas para el personal. Malo: dos copias del mismo formulario
 *      que hay que mantener a la par, y en cuanto se toca una las dos se separan.
 *   b) Que las pantallas pidan este contrato en vez de un ViewModel concreto. Asi la
 *      MISMA pantalla, con el mismo formato y el mismo estilo, sirve para los dos: el
 *      cliente le pasa su PetViewModel y el personal le pasa su StaffViewModel.
 *
 * Se eligio (b). Ni una linea de diseño de esas pantallas cambio: lo unico que cambio
 * fue el tipo del parametro.
 *
 * Los nombres estan en ingles (allPets, addPet...) a proposito: son los que ya usaban
 * las pantallas, y renombrarlos habria obligado a tocar mucho codigo que funciona por
 * un motivo puramente estetico.
 */
interface GestorDeMascotas {

    /**
     * Las mascotas que esta pantalla puede ver.
     *
     * Para el cliente son las suyas; para el personal, las de toda la clinica. La
     * pantalla no necesita saber cual de las dos cosas es.
     */
    val allPets: StateFlow<List<Mascota>>

    /**
     * Si quien esta usando la pantalla puede escribir en la parte CLINICA del
     * expediente: historial, alergias y notas medicas.
     *
     * Es false para el cliente y true para el doctor y el admin. El empleado de
     * recepcion tampoco: puede abrir un expediente y corregir un telefono, pero un
     * diagnostico lo escribe quien lo hace.
     *
     * La division es la que tiene sentido en una clinica: el dueño aporta los datos de
     * su animal (nombre, raza, edad, como localizarle) y la clinica aporta el
     * diagnostico. Un historial medico que puede reescribir el paciente no sirve de
     * prueba de nada.
     *
     * OJO: esto solo decide que BOTONES se ven. Quien impide de verdad la escritura son
     * las reglas de seguridad de Firebase; esconder un boton es comodidad, no seguridad.
     */
    val puedeEditarDatosClinicos: Boolean

    fun addPet(pet: Mascota)

    fun updatePet(pet: Mascota)

    fun deletePet(pet: Mascota)

    fun subirFotoMascota(mascotaId: String, archivoLocal: Uri)

    fun addMedicalRecord(petId: String, record: RegistroHistorial, newAllergy: String)

    fun updateMedicalRecord(petId: String, record: RegistroHistorial)

    fun deleteMedicalRecord(petId: String, recordId: Int)

    fun updateMedicalNotes(petId: String, notes: String)

    fun deleteAllergy(petId: String, allergy: String)
}