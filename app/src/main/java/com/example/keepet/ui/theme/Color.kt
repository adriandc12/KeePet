package com.example.keepet.ui.theme

import androidx.compose.ui.graphics.Color

// Colores de KeePet.
//
// Aqui habia tambien seis colores morados y rosas (Purple80, Pink40...) que venian en la
// plantilla de Android Studio y que ninguna pantalla usaba. Se han borrado: dejarlos
// invitaba a usarlos por error y a que la app acabara con dos paletas distintas.

val BackgroundColor = Color(0xFFFFF9EB)
val PrimaryBrown = Color(0xFF4E342E)

/**
 * El color de los botones.
 *
 * Antes era el salmon 0xFFD38D82. El problema es que casi todos los botones llevan la
 * letra en BLANCO encima, y blanco sobre ese salmon da un contraste de 2,6:1 cuando el
 * minimo para que un texto se lea es 4,5:1: los botones se "lavaban", sobre todo con el
 * movil al sol. Este terracota es el mismo color de familia, un tono mas profundo, y con
 * letra blanca llega a 4,6:1.
 *
 * Si algun dia lo vuelves a aclarar, acuerdate de cambiar tambien a marron el texto de
 * los botones, o volveran a costar de leer.
 */
val AccentButton = Color(0xFFB05F4F)

val TextColor = Color(0xFF4E342E)
val White = Color(0xFFFFFFFF)
val InputBackground = Color(0xFFE0F7F1) // El verde clarito de los inputs

/**
 * Gris de los textos secundarios: pistas de los campos, "Sin asignar", fechas...
 *
 * Antes era 0xFF9E9E9E. Sobre blanco ese gris tiene un contraste de 2,6:1, cuando el
 * minimo recomendado para texto es 4,5:1, y se usa en letra de 11 y 12 puntos: era de
 * los textos que peor se leian de la app, sobre todo con el movil al sol. Este otro gris
 * llega a 5,1:1 y sigue viendose "apagado" al lado del marron, que es para lo que esta.
 */
val GrayHint = Color(0xFF6D6D6D)

/**
 * Rojo de cancelar y de las alergias.
 *
 * Antes era el MISMO salmon que AccentButton, asi que "Cancelar" y "Eliminar" se veian
 * igual que "Guardar": en una clinica, borrar el expediente de un paciente por
 * confundir dos botones del mismo color es un fallo caro. Ahora es un rojo de verdad,
 * pero apagado para que no chille de mas dentro de la paleta crema.
 */
val CancelRed = Color(0xFFB3261E)
