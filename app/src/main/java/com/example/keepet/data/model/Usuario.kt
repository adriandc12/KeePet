package com.example.keepet.data.model

import com.google.firebase.database.IgnoreExtraProperties

/**
 * Datos del perfil. Se guarda en /usuarios/{uid}/perfil.
 *
 * Antes esta clase tenia "imagenRes: Int" con un R.drawable dentro. Eso era un
 * error silencioso: los numeros de R.drawable los genera el compilador y CAMBIAN
 * en cada compilacion, asi que guardarlos en la nube significa que manana ese
 * numero puede apuntar a otra imagen distinta (o a ninguna).
 *
 * Ahora se guarda "avatar", el NOMBRE del avatar ("perro", "gato"...), que es
 * estable. La conversion de nombre a imagen se hace en la UI (ui/components/Avatares.kt).
 */
@IgnoreExtraProperties
data class Usuario(
    val nombre: String = "",
    val telefono: String = "",
    val direccion: String = "",
    val correo: String = "",
    val imagenUri: String? = null,
    val avatar: String = "perfil"
)
