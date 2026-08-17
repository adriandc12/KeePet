package com.example.keepet.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * Los colores de KeePet, y solo los de KeePet.
 *
 * POR QUE ESTO ESTA ASI (era el motivo de que algunos textos no se leyeran)
 * -----------------------------------------------------------------------
 * Antes este archivo hacia dos cosas que traia la plantilla de Android Studio:
 *
 *   1. dynamicColor = true  -> en Android 12+ los colores de Material se sacaban del
 *      FONDO DE PANTALLA del telefono.
 *   2. darkTheme = isSystemInDarkTheme() -> si el movil estaba en modo oscuro, se usaba
 *      una paleta oscura.
 *
 * El problema es que KeePet pinta sus fondos a mano y siempre claros: el crema
 * (BackgroundColor) y el blanco de las tarjetas. Pero todo lo que NO lleva un color
 * escrito a mano (los AlertDialog, los TextButton, las etiquetas de los campos, el
 * texto que escribes dentro de un TextField...) lo saca de esta paleta. Con la paleta
 * del sistema, en modo oscuro esos textos salian casi blancos... sobre una tarjeta
 * blanca. De ahi que "a veces" no se leyeran: dependia del movil y de si tenia el modo
 * oscuro puesto.
 *
 * La solucion es que la paleta sea siempre esta, clara y con los colores de la app.
 * No es que se haya "desactivado el modo oscuro": es que esta app nunca tuvo un diseño
 * oscuro, y fingir que si lo tenia era lo que rompia la lectura.
 *
 * Si algun dia quieres modo oscuro de verdad, hay que diseñarlo: hacen falta versiones
 * oscuras de BackgroundColor, White e InputBackground y revisar pantalla por pantalla.
 * No se arregla volviendo a encender darkColorScheme.
 */
private val ColoresKeePet = lightColorScheme(
    // primary lo usan los TextButton ("Cancelar", "Cerrar"), los checkbox y los bordes
    // enfocados. Se pone el marron oscuro y NO el salmon (AccentButton) a proposito: el
    // salmon sobre blanco tiene poquisimo contraste y como TEXTO se lee mal. El salmon
    // sigue usandose para rellenar botones, donde va con letra blanca encima y ahi si
    // funciona.
    primary = PrimaryBrown,
    onPrimary = White,

    secondary = PrimaryBrown,
    onSecondary = White,

    tertiary = AccentButton,
    onTertiary = White,

    background = BackgroundColor,
    onBackground = TextColor,

    // surface es el fondo de las tarjetas y de los dialogos; onSurface, el color por
    // defecto de cualquier texto que este encima.
    surface = White,
    onSurface = TextColor,

    surfaceVariant = InputBackground,
    onSurfaceVariant = TextColor,

    error = CancelRed,
    onError = White,

    outline = GrayHint
)

/**
 * Tema de la app. Ya no recibe parametros: antes aceptaba darkTheme y dynamicColor, y
 * poder encenderlos era justo lo que estropeaba la legibilidad.
 */
@Composable
fun KeePetTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ColoresKeePet,
        typography = Typography,
        content = content
    )
}
