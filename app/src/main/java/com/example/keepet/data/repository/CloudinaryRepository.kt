package com.example.keepet.data.repository

import android.content.Context
import android.net.Uri
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.keepet.BuildConfig
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Datos de la cuenta de Cloudinary.
 *
 * Los valores NO estan escritos aqui: vienen de local.properties, pasan por
 * build.gradle.kts y llegan como BuildConfig.CLOUDINARY_*. Ver local.properties para
 * saber de donde se saca cada uno.
 */
object CloudinaryConfig {

    val cloudName: String = BuildConfig.CLOUDINARY_CLOUD_NAME
    val uploadPreset: String = BuildConfig.CLOUDINARY_UPLOAD_PRESET

    /**
     * Si el usuario ya puso sus datos de verdad.
     *
     * Se comprueba tambien "REEMPLAZAME" porque es el texto de relleno que trae
     * local.properties. Sin esto, la app intentaria subir a una cuenta que no existe
     * y el error de Cloudinary seria dificil de entender.
     */
    val estaConfigurado: Boolean
        get() = cloudName.isNotBlank() && uploadPreset.isNotBlank() &&
            cloudName != "REEMPLAZAME" && uploadPreset != "REEMPLAZAME"

    private var iniciado = false

    /**
     * Arranca el SDK. Se llama una sola vez, desde KeePetApplication.
     *
     * MediaManager.init lanza excepcion si se llama dos veces, de ahi la bandera y el
     * try/catch: al recrear el proceso Android puede volver a pasar por aqui.
     */
    fun inicializar(context: Context) {
        if (iniciado || !estaConfigurado) return
        try {
            MediaManager.init(
                context,
                // "secure" = true fuerza URLs https. Importante: Android bloquea el
                // trafico http sin cifrar por defecto, asi que una URL http no se
                // veria y parecerian fotos roras sin motivo aparente.
                hashMapOf("cloud_name" to cloudName, "secure" to "true")
            )
            iniciado = true
        } catch (_: Exception) {
            // Ya estaba iniciado. No es un problema.
            iniciado = true
        }
    }
}

/**
 * Sube imagenes a Cloudinary y devuelve su URL publica.
 *
 * POR QUE CLOUDINARY Y NO FIREBASE STORAGE:
 * Storage empezo a exigir el plan de pago Blaze (con tarjeta) incluso para el uso
 * dentro del limite gratuito. Cloudinary hace lo mismo con plan gratis y sin tarjeta.
 * Firebase se sigue usando para TODO lo demas (base de datos y login): lo unico que
 * cambia es donde se guardan los archivos de imagen.
 *
 * El reparto de tareas es el mismo de antes:
 *   - la FOTO se guarda en Cloudinary;
 *   - en Realtime Database solo se guarda su URL (una linea de texto).
 *
 * SUBIDA SIN FIRMAR (unsigned), y por que es la forma correcta aqui:
 * Cloudinary admite dos formas de subir. La "firmada" necesita el api_secret de la
 * cuenta; la "sin firmar" solo necesita el nombre de un upload preset. En una app
 * movil hay que usar la segunda **obligatoriamente**: un APK se puede descompilar en
 * dos minutos, asi que cualquier secreto que metas dentro deja de ser secreto. Con el
 * preset, lo peor que puede hacer alguien que lo saque del APK es subir imagenes a tu
 * cuenta; no puede borrar ni leer nada.
 */
class CloudinaryRepository {

    /** Sube la foto de una mascota y devuelve su URL. */
    suspend fun subirFotoMascota(mascotaId: String, archivoLocal: Uri): String =
        subir(archivoLocal, carpeta = "keepet/mascotas")

    /** Sube la foto de perfil de un usuario y devuelve su URL. */
    suspend fun subirFotoPerfil(uid: String, archivoLocal: Uri): String =
        subir(archivoLocal, carpeta = "keepet/perfiles")

    /**
     * El trabajo real.
     *
     * El SDK de Cloudinary avisa del resultado por callbacks (onSuccess / onError),
     * que es el estilo antiguo de Java. suspendCancellableCoroutine es el puente que
     * convierte eso en una funcion suspend normal, para poder escribir
     * "val url = subir(foto)" y que el resto del codigo no se entere de nada.
     *
     * NO se le pasa un public_id fijo a proposito. Seria bonito que la foto de cada
     * mascota sobrescribiera la anterior, pero las subidas sin firmar **no pueden
     * sobrescribir** un archivo existente: Cloudinary devolveria la foto vieja y
     * cambiar de foto no haria nada visible. Asi que cada subida crea un archivo
     * nuevo con nombre aleatorio y en la base de datos se guarda la URL nueva.
     * La consecuencia es que las fotos antiguas se quedan ocupando espacio en
     * Cloudinary; con 25 GB gratis no es un problema para una clinica, y borrarlas
     * exigiria el api_secret, que es justo lo que no queremos meter en la app.
     */
    private suspend fun subir(archivoLocal: Uri, carpeta: String): String {
        check(CloudinaryConfig.estaConfigurado) {
            "Falta configurar Cloudinary. Pon cloudinary.cloudName y " +
                "cloudinary.uploadPreset en local.properties y vuelve a compilar."
        }

        return suspendCancellableCoroutine { continuacion ->
            val idPeticion = MediaManager.get()
                .upload(archivoLocal)
                .unsigned(CloudinaryConfig.uploadPreset)
                .option("folder", carpeta)
                .callback(object : UploadCallback {

                    override fun onStart(requestId: String?) = Unit

                    override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) = Unit

                    override fun onSuccess(requestId: String?, resultData: MutableMap<Any?, Any?>?) {
                        // secure_url es la direccion https. Existe tambien "url", en
                        // http, que Android bloquearia: hay que coger esta.
                        val url = resultData?.get("secure_url") as? String
                        if (url.isNullOrBlank()) {
                            continuacion.resumeWithException(
                                IllegalStateException("Cloudinary no devolvió la URL de la foto")
                            )
                        } else {
                            continuacion.resume(url)
                        }
                    }

                    override fun onError(requestId: String?, error: ErrorInfo?) {
                        continuacion.resumeWithException(
                            IllegalStateException(mensajeDeError(error))
                        )
                    }

                    /**
                     * Cloudinary llama aqui cuando decide reintentar mas tarde (por
                     * ejemplo sin conexion). Para la pantalla eso es un fallo: la foto
                     * no esta subida y no hay URL que guardar, asi que se corta aqui
                     * en vez de dejar la ruedecita girando para siempre.
                     */
                    override fun onReschedule(requestId: String?, error: ErrorInfo?) {
                        continuacion.resumeWithException(
                            IllegalStateException(
                                "No se pudo subir la foto ahora: ${mensajeDeError(error)}"
                            )
                        )
                    }
                })
                .dispatch()

            // Si el usuario sale de la pantalla a mitad de la subida, la corrutina se
            // cancela; sin esto la subida seguiria gastando datos para nada.
            continuacion.invokeOnCancellation {
                runCatching { MediaManager.get().cancelRequest(idPeticion) }
            }
        }
    }

    private fun mensajeDeError(error: ErrorInfo?): String =
        error?.description ?: "error desconocido al subir la imagen"
}
