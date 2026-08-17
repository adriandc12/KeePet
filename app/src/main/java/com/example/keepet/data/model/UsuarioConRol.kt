package com.example.keepet.data.model

/**
 * Un usuario junto con su rol, para la pantalla de administracion.
 *
 * No se guarda tal cual en la base de datos: se arma juntando dos ramas distintas,
 * /usuarios/{uid}/perfil y /roles/{uid}. Es un modelo "de pantalla", no de datos.
 */
data class UsuarioConRol(
    val uid: String,
    val perfil: Usuario,
    val rol: Rol
) {
    /** Nombre para mostrar; si el perfil no tiene nombre, se usa el correo. */
    val nombreVisible: String
        get() = perfil.nombre.ifBlank { perfil.correo.substringBefore("@").ifBlank { "Sin nombre" } }
}
