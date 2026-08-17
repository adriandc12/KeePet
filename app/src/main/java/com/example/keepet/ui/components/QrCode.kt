package com.example.keepet.ui.components

import android.graphics.Bitmap
import android.graphics.Color as ColorAndroid
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/**
 * Genera y dibuja codigos QR para las citas.
 *
 * COMO FUNCIONA UN QR, en corto: es texto convertido en cuadraditos. Aqui el texto
 * que se guarda dentro es "KEEPET-CITA:{idDeLaCita}". El prefijo sirve para que, al
 * escanear, el personal sepa que ese QR es de esta app y no un QR cualquiera de un
 * producto del supermercado.
 *
 * Todo esto funciona SIN internet: el QR se calcula en el telefono.
 */

/** Marca que llevan todos los QR de la app. */
const val PREFIJO_QR_CITA = "KEEPET-CITA:"

/** Construye el texto que ira dentro del QR de una cita. */
fun contenidoQrDeCita(citaId: String): String = "$PREFIJO_QR_CITA$citaId"

/**
 * Saca el id de cita de lo que devuelve el escaner.
 *
 * Devuelve null si el QR escaneado no es de KeePet, para poder avisar al empleado
 * en vez de ponerse a buscar una cita con un texto que no tiene sentido.
 */
fun idDeCitaDesdeQr(contenido: String?): String? {
    val texto = contenido?.trim().orEmpty()
    return if (texto.startsWith(PREFIJO_QR_CITA)) {
        texto.removePrefix(PREFIJO_QR_CITA).ifBlank { null }
    } else {
        null
    }
}

/**
 * Convierte texto en la imagen de un QR.
 *
 * Devuelve null si algo sale mal (texto vacio, por ejemplo) para que la pantalla
 * pueda enseñar un mensaje en lugar de reventar.
 *
 * ErrorCorrectionLevel.H añade redundancia: el QR sigue leyendose aunque la
 * pantalla este algo sucia, con brillo bajo o el dedo tape una esquina. Cuesta
 * unos cuadraditos mas, y en una recepcion con prisa merece la pena.
 */
fun generarImagenQr(texto: String, tamanoPx: Int = 640): ImageBitmap? {
    if (texto.isBlank()) return null
    return try {
        val matriz = QRCodeWriter().encode(
            texto,
            BarcodeFormat.QR_CODE,
            tamanoPx,
            tamanoPx,
            mapOf(
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.H,
                EncodeHintType.MARGIN to 1
            )
        )

        val bitmap = Bitmap.createBitmap(matriz.width, matriz.height, Bitmap.Config.ARGB_8888)
        // Se recorre cuadradito a cuadradito: negro donde la matriz dice true.
        // setPixels de una fila entera es mas rapido que setPixel uno a uno.
        val fila = IntArray(matriz.width)
        for (y in 0 until matriz.height) {
            for (x in 0 until matriz.width) {
                fila[x] = if (matriz[x, y]) ColorAndroid.BLACK else ColorAndroid.WHITE
            }
            bitmap.setPixels(fila, 0, matriz.width, 0, y, matriz.width, 1)
        }
        bitmap.asImageBitmap()
    } catch (_: Exception) {
        null
    }
}

/**
 * Dibuja el QR de una cita.
 *
 * El remember(texto) es importante: sin el, el QR se recalcularia en cada
 * redibujado de la pantalla, y generar la imagen no es gratis.
 *
 * El fondo blanco con padding tampoco es decorativo: un QR necesita margen claro
 * alrededor para que los lectores lo detecten.
 */
@Composable
fun CodigoQrCita(
    citaId: String,
    modifier: Modifier = Modifier,
    tamano: Int = 220
) {
    val texto = contenidoQrDeCita(citaId)
    val imagen = remember(texto) { generarImagenQr(texto) }

    Box(
        modifier = modifier
            .background(Color.White, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        if (imagen != null) {
            Image(
                bitmap = imagen,
                contentDescription = "Código QR de la cita",
                modifier = Modifier.size(tamano.dp),
                contentScale = ContentScale.Fit
            )
        }
    }
}
