package com.example.keepet.data.model

/**
 * Quien es cada quien dentro de la clinica.
 *
 * El rol se guarda en la base de datos en /roles/{uid}, en un nodo APARTE del
 * perfil. Esto no es un capricho: las reglas de seguridad de Firebase necesitan
 * leer el rol para decidir si te dejan hacer algo, y si el rol viviera dentro de
 * /usuarios/{uid}/perfil el propio usuario podria editarselo y ascenderse a admin.
 * En /roles solo escriben los admin.
 *
 * Por el mismo motivo, al registrarse NADIE elige su rol: todo el mundo entra como
 * CLIENTE y es un admin quien asciende a alguien a empleado o doctor.
 */
enum class Rol {
    /** Dueño de mascotas. Ve solo lo suyo: sus mascotas, sus citas, su QR. */
    CLIENTE,

    /** Recepcion. Gestiona la agenda de toda la clinica y escanea los QR. */
    EMPLEADO,

    /** Veterinario. Lo de empleado, y ademas escribe en el historial medico. */
    DOCTOR,

    /** Dueño de la clinica. Lo de doctor, y ademas reparte los roles. */
    ADMIN;

    /** true para todo el que trabaja en la clinica (es decir, todos menos el cliente). */
    val esPersonal: Boolean get() = this != CLIENTE

    /** Solo doctores y admin pueden tocar el historial medico de un paciente. */
    val puedeEditarHistorial: Boolean get() = this == DOCTOR || this == ADMIN

    /** Solo el admin reparte roles. */
    val puedeGestionarUsuarios: Boolean get() = this == ADMIN

    /** Nombre bonito para enseñar en pantalla. */
    val etiqueta: String
        get() = when (this) {
            CLIENTE -> "Cliente"
            EMPLEADO -> "Empleado"
            DOCTOR -> "Doctor"
            ADMIN -> "Administrador"
        }

    /** Como se escribe en la base de datos. */
    val clave: String get() = name.lowercase()

    companion object {
        /**
         * Convierte el texto de la base de datos en un Rol.
         * Si el nodo no existe o trae basura, se asume CLIENTE: el permiso mas bajo.
         * Nunca al contrario — ante la duda, menos permisos, no mas.
         */
        fun desde(valor: String?): Rol =
            entries.firstOrNull { it.clave == valor?.trim()?.lowercase() } ?: CLIENTE
    }
}
