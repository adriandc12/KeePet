package com.example.keepet

import android.app.Application
import com.example.keepet.data.repository.CloudinaryConfig
import com.example.keepet.util.NotificationHelper
import com.google.firebase.database.FirebaseDatabase

class KeePetApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Modo sin conexion de Realtime Database.
        //
        // Con esto la app guarda una copia local de lo que descarga: puedes abrirla
        // sin internet, ver tus mascotas, e incluso crear o editar. Los cambios se
        // quedan en cola y Firebase los sube solo en cuanto vuelve la conexion.
        // Esto es lo que hace innecesario mantener Room aparte.
        //
        // OJO: tiene que llamarse UNA sola vez y ANTES de tocar la base de datos
        // por primera vez. Por eso vive aqui, en Application, y no en una pantalla.
        // Llamarlo dos veces lanza una excepcion, de ahi el try/catch.
        try {
            FirebaseDatabase.getInstance().setPersistenceEnabled(true)
        } catch (_: Exception) {
            // Ya estaba activado (pasa al recrear el proceso). No es un problema.
        }

        // Cloudinary (donde viven las fotos). Igual que la linea de arriba: una sola
        // vez, al arrancar la app, antes de que ninguna pantalla intente subir nada.
        // Si todavia no has puesto tus datos en local.properties, no hace nada y la
        // app funciona igual; solo avisara al intentar subir una foto.
        CloudinaryConfig.inicializar(this)

        NotificationHelper.createNotificationChannel(this)
    }
}
