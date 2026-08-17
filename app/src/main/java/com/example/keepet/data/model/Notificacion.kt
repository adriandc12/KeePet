package com.example.keepet.data.model

/**
 * Un aviso mostrado en la pantalla de Notificaciones.
 *
 * OJO: esta clase NO se guarda en Firebase. Se CALCULA a partir de las citas
 * (mira PetViewModel.notificaciones).
 *
 * Antes existia una lista global con dos notificaciones inventadas a fuego
 * ("Copito", "Milo") dentro de un companion object. Eso hacia que la pantalla
 * mostrara avisos de citas que no existian, y que no desaparecieran al borrar
 * la cita. Derivandolas de las citas reales, la pantalla siempre esta en sintonia
 * con los datos y no hay nada que sincronizar.
 */
data class Notificacion(
    val id: String,
    val mascotaId: String,
    val nombreMascota: String,
    val tipoServicio: String,
    val fecha: String,
    val hora: String,
    val aviso: String,
    val imagenUri: String? = null,
    val especie: String = ""
)
