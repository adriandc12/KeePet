package com.example.keepet.ui.components

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.annotation.DrawableRes
import androidx.core.content.FileProvider
import com.example.keepet.R
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Traduce nombres de texto a imagenes del proyecto.
 *
 * POR QUE EXISTE ESTE ARCHIVO: antes se guardaba el numero de R.drawable
 * (ej: 2131165234) dentro de la mascota y del usuario. Ese numero lo inventa el
 * compilador en cada compilacion y CAMBIA. Mientras todo vivia en el telefono
 * casi no se notaba, pero al subirlo a la nube el fallo se vuelve real: guardas
 * "2131165234" hoy y manana ese numero apunta a otra imagen distinta.
 *
 * La solucion es guardar el NOMBRE ("perro", "gato") y convertirlo a imagen aqui,
 * en la capa de UI, que es la unica que sabe de imagenes.
 */

/** Imagen por defecto de una mascota segun su especie. */
@DrawableRes
fun avatarPorEspecie(especie: String): Int = when (especie.lowercase(Locale.ROOT)) {
    "perro" -> R.drawable.perro
    "gato" -> R.drawable.gato
    "conejo" -> R.drawable.conejo
    else -> R.drawable.pet_group
}

/** Avatares que el usuario puede elegir para su perfil. */
val NOMBRES_AVATAR = listOf("perfil", "perro", "gato", "conejo")

/** Imagen de un avatar de perfil a partir de su nombre. */
@DrawableRes
fun avatarPorNombre(nombre: String): Int = when (nombre.lowercase(Locale.ROOT)) {
    "perro" -> R.drawable.perro
    "gato" -> R.drawable.gato
    "conejo" -> R.drawable.conejo
    else -> R.drawable.perfil
}

/**
 * Crea un archivo vacio y devuelve su Uri para que la camara escriba la foto ahi.
 *
 * Estaba definida dentro de PetDetailScreen y ProfileScreen la usaba desde alli,
 * lo cual obligaba a que una pantalla dependiera de otra sin motivo. Vive aqui
 * porque las dos la necesitan por igual.
 */
fun createImageUri(context: Context): Uri {
    val marcaDeTiempo = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val carpeta = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    val archivo = File.createTempFile("JPEG_${marcaDeTiempo}_", ".jpg", carpeta)
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", archivo)
}
