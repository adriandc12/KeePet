package com.example.keepet.data.model

import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties

/**
 * Una mascota, tal y como se guarda en Realtime Database.
 *
 * Tres reglas de oro para que Firebase pueda leer y escribir esta clase:
 *
 * 1. TODAS las propiedades necesitan un valor por defecto. Firebase crea el objeto
 *    vacio y luego lo va rellenando, asi que necesita poder construirlo sin argumentos.
 *    Si a una propiedad le quitas el "= ..." la app crashea al leer de la nube.
 *
 * 2. El "id" lleva @get:Exclude porque NO se guarda dentro del registro: el id ES la
 *    clave del nodo en la base de datos. Guardarlo tambien dentro seria duplicarlo.
 *    Al leer, lo rellenamos a mano con snapshot.key (mira PetRepository).
 *
 * 3. El id es String, no Int. Firebase genera claves de texto tipo "-NkJ8h2p...".
 *    Son unicas aunque dos telefonos creen una mascota a la vez, cosa que un
 *    contador de enteros no garantiza.
 */
@IgnoreExtraProperties
data class Mascota(
    @get:Exclude var id: String = "",

    /**
     * uid del cliente dueño de esta mascota.
     *
     * Lleva @get:Exclude igual que el id: no se guarda dentro del registro porque
     * ya esta implicito en la ruta (/usuarios/{uid}/mascotas/{id}). Se rellena al
     * leer. Lo necesita el personal de la clinica, que ve mascotas de muchos
     * clientes a la vez y tiene que saber a quien llamar por telefono.
     */
    @get:Exclude var duenoUid: String = "",

    val nombre: String = "",
    val especie: String = "", // Perro, Gato, Conejo
    val raza: String = "",
    val edad: String = "",
    val dueno: String = "",
    val telefono: String = "",
    /**
     * Foto de la mascota.
     *
     * Es solo TEXTO: la URL https que devuelve Cloudinary al subir la imagen. La
     * imagen en si no se guarda nunca en la base de datos.
     *
     * Antes aqui habia una ruta del propio telefono (file:///...), asi que la foto
     * solo se veia en el dispositivo donde se hizo. Con una URL se ve desde cualquier
     * telefono y sobrevive a reinstalar la app.
     *
     * El campo se sigue llamando imagenUri, no fotoUrl, a proposito: cambiarle el
     * nombre dejaria huerfanas las fotos ya guardadas en la base de datos (Firebase
     * busca por nombre de campo, y el viejo dejaria de leerse).
     *
     * Coil (AsyncImage) carga URLs y rutas locales igual de bien, asi que las fotos
     * antiguas que quedaran guardadas siguen viendose en su propio dispositivo.
     */
    val imagenUri: String? = null,
    val direccion: String = "",
    val correo: String = "",
    val peso: String = "",
    val sexo: String = "", // Macho, Hembra
    val alergias: List<String> = emptyList(),
    val notasMedicas: String = "",
    val vacunas: String = "",

    // Aqui habia dos campos mas, incluyeBano y tipoBano. Se rellenaban en el formulario
    // del expediente y no se leian en ninguna pantalla. Ademas un baño es un servicio de
    // una cita, no un dato clinico del animal, y como servicio sigue estando en
    // AddAppointmentScreen. Quitarlos de aqui es seguro incluso para las mascotas que ya
    // los tengan guardados: @IgnoreExtraProperties (arriba) le dice a Firebase que ignore
    // los campos que sobran en vez de fallar al leer.

    val historial: List<RegistroHistorial> = emptyList()
)

/**
 * Una entrada del historial medico. Va anidada dentro de la mascota, no en su
 * propio nodo, porque siempre se lee junto con ella.
 *
 * Aqui el id si es Int: estos registros se numeran dentro de una sola mascota
 * (1, 2, 3...), no compiten con los de otros usuarios.
 */
data class RegistroHistorial(
    val id: Int = 0,
    val servicio: String = "",
    val fecha: String = "",
    val detalles: String = "",
    val tipoIcono: String = "", // "Bano", "Vacuna", "Consulta", "Corte"
    val recetaUri: String? = null
)
