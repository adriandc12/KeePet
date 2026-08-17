package com.example.keepet.data.model

import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties

/**
 * Una cita de la clinica.
 *
 * CAMBIO IMPORTANTE: antes las citas vivian dentro de cada cliente, en
 * /usuarios/{uid}/citas. Eso funcionaba mientras la app era solo para el dueño de
 * la mascota, pero con personal de clinica se rompe: para que recepcion vea la
 * agenda del dia tendria que recorrer los nodos de TODOS los clientes uno a uno.
 *
 * Ahora todas las citas viven juntas en /citas y cada una lleva dentro a quien
 * pertenece (clienteUid). Asi:
 *   - recepcion lee /citas y tiene la agenda completa de un tiro;
 *   - el cliente pide /citas filtrando por su propio clienteUid, y las reglas de
 *     seguridad comprueban que solo pueda filtrar por el suyo.
 */
@IgnoreExtraProperties
data class Cita(
    @get:Exclude var id: String = "",

    // --- de quien es ---
    val clienteUid: String = "",
    val clienteNombre: String = "",
    val mascotaId: String = "",
    val nombreMascota: String = "",

    // --- cuando y de que ---

    /**
     * La fecha tal y como se le ENSEÑA a la gente: "16 de agosto".
     *
     * Sirve para pintarla en pantalla y para nada mas. No se puede ordenar (por orden
     * alfabetico "10 de agosto" va antes que "2 de mayo") y no dice el año.
     */
    val fecha: String = "",

    /**
     * La MISMA fecha, pero para la maquina: "2026-08-16" (año-mes-dia).
     *
     * POR QUE HACEN FALTA LAS DOS. La agenda de la clinica tiene que hacer dos cosas
     * que con el texto de arriba son imposibles:
     *   - ordenar las citas por cuando toca (con texto salen en orden alfabetico);
     *   - saber si una cita es de HOY (sin año, una cita del 16 de agosto del año
     *     pasado parecia de hoy).
     * Escrita asi, año-mes-dia, ordenar alfabeticamente y ordenar por fecha son lo
     * mismo, que es justo la gracia de este formato.
     *
     * Las citas creadas antes de este cambio lo tienen vacio. Todo lo que lo usa lleva
     * un respaldo para ese caso, asi que no hay que migrar nada.
     */
    val fechaIso: String = "",

    /** La hora como se enseña y se guarda: "08:00 AM". */
    val hora: String = "",
    val motivo: String = "",
    val tipoServicio: String = "",
    val precio: String = "",
    val notasAdicionales: String = "",

    // --- quien la atiende (lo asigna el personal, empieza vacio) ---
    val doctorUid: String = "",
    val doctorNombre: String = "",

    /**
     * Pendiente -> Confirmada -> EnAtencion -> Completada, o Cancelada.
     * "Confirmada" la pone el personal al escanear el QR cuando el cliente llega.
     */
    val estado: String = ESTADO_PENDIENTE,

    /** Milisegundos de cuando se creo. Sirve para ordenar la agenda. */
    val creadaEn: Long = 0L
) {
    /**
     * Codigo corto que se le enseña al cliente y que va dentro del QR.
     *
     * No se guarda en la base de datos: se calcula a partir del id, que Firebase
     * ya garantiza unico. Guardar un dato que puedes deducir es pedir que algun
     * dia las dos copias no coincidan.
     */
    @get:Exclude
    val codigo: String
        get() = if (id.length >= 6) id.takeLast(6).uppercase() else id.uppercase()

    /**
     * La hora convertida a minutos desde medianoche: "02:00 PM" -> 840.
     *
     * Es lo que permite ordenar dos citas del mismo dia. Ordenar el texto no vale:
     * "08:00 AM" y "10:00 AM" salen bien por casualidad, pero "02:00 PM" se colocaria
     * antes de "08:00 AM" porque el 0 va antes que el 8.
     *
     * Si la hora viene con un formato que no se entiende devuelve -1 en vez de fallar:
     * una cita mal escrita debe salir descolocada, no tumbar la agenda.
     */
    @get:Exclude
    val minutoDelDia: Int
        get() {
            val partes = Regex("""(\d{1,2}):(\d{2})\s*([AaPp])""").find(hora) ?: return -1
            val hora12 = partes.groupValues[1].toIntOrNull() ?: return -1
            val minutos = partes.groupValues[2].toIntOrNull() ?: return -1
            val esTarde = partes.groupValues[3].uppercase() == "P"
            return ((hora12 % 12) + if (esTarde) 12 else 0) * 60 + minutos
        }

    /**
     * Texto con el que se ordenan las citas cronologicamente: "2026-08-16 0480".
     *
     * Junta fecha y hora en una sola cadena que se puede comparar de golpe.
     * Las citas viejas, que no tienen fechaIso, se ponen a "9999-12-31" para que
     * queden AL FINAL y no aparezcan disfrazadas de "proxima cita" en la agenda.
     */
    @get:Exclude
    val claveOrden: String
        get() = fechaIso.ifBlank { "9999-12-31" } +
            " " + minutoDelDia.coerceAtLeast(0).toString().padStart(4, '0')

    companion object {
        const val ESTADO_PENDIENTE = "Pendiente"
        const val ESTADO_CONFIRMADA = "Confirmada"
        const val ESTADO_EN_ATENCION = "En atención"
        const val ESTADO_COMPLETADA = "Completada"
        const val ESTADO_CANCELADA = "Cancelada"

        val ESTADOS_ACTIVOS = listOf(ESTADO_PENDIENTE, ESTADO_CONFIRMADA, ESTADO_EN_ATENCION)
    }
}
