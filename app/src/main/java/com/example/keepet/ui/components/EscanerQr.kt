package com.example.keepet.ui.components

import android.content.Context
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning

/**
 * Abre el escaner de codigos QR de Google Play Services.
 *
 * POR QUE ESTE ESCANER Y NO UNA LIBRERIA DE CAMARA:
 * lo normal seria montar CameraX, pedir permiso de camara, dibujar la vista previa
 * y analizar cada fotograma. Son varios cientos de lineas y un permiso mas que
 * pedirle al usuario.
 *
 * Este escaner lo pone Google Play Services: abre su PROPIA pantalla de escaneo,
 * hace el trabajo y devuelve el texto leido. No hace falta permiso de camara en el
 * manifest, porque la camara la usa Play Services, no nuestra app.
 *
 * A cambio, requiere que el telefono tenga Google Play Services (cualquier movil
 * Android normal lo tiene) y descarga un modulo pequeño la primera vez.
 *
 * @param onResultado recibe el texto del QR, o null si el usuario cancelo o fallo
 */
fun escanearQr(context: Context, onResultado: (String?) -> Unit) {
    GmsBarcodeScanning.getClient(context)
        .startScan()
        // rawValue es el texto que llevaba dentro el QR. Para nuestras citas sera
        // algo como "KEEPET-CITA:-NkJ8h2pQ...".
        .addOnSuccessListener { codigo -> onResultado(codigo.rawValue) }
        // Si el usuario cierra el escaner sin leer nada, no es un error: se avisa
        // con null y la pantalla simplemente no hace nada.
        .addOnCanceledListener { onResultado(null) }
        .addOnFailureListener { onResultado(null) }
}
