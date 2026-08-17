package com.example.keepet.data.repository

import com.example.keepet.data.model.Rol
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Todo lo relacionado con "quien ha iniciado sesion".
 *
 * Esta clase es la UNICA que toca FirebaseAuth. Ni las pantallas ni los ViewModels
 * llaman a Firebase directamente: le piden las cosas a este repositorio. Asi, si
 * manana cambias de sistema de login, solo tocas este archivo.
 */
class AuthRepository {

    private val auth = FirebaseAuth.getInstance()

    /** El usuario de ahora mismo, o null si nadie ha iniciado sesion. */
    val usuarioActual: FirebaseUser?
        get() = auth.currentUser

    /**
     * Un Flow que avisa cada vez que alguien entra o sale.
     *
     * callbackFlow sirve para convertir una API "de avisos" (Firebase te llama a ti)
     * en un Flow (tu escuchas a Firebase). El awaitClose del final es obligatorio:
     * quita el listener cuando ya nadie escucha, y sin el tendrias una fuga de memoria.
     */
    fun estadoSesion(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    /** Inicia sesion. Lanza excepcion si el correo o la contrasena son incorrectos. */
    suspend fun iniciarSesion(correo: String, contrasena: String): FirebaseUser? =
        auth.signInWithEmailAndPassword(correo.trim(), contrasena).await().user

    /** Crea una cuenta nueva y deja la sesion iniciada. */
    suspend fun registrarse(correo: String, contrasena: String): FirebaseUser? =
        auth.createUserWithEmailAndPassword(correo.trim(), contrasena).await().user

    /** Envia un correo para restablecer la contrasena. */
    suspend fun recuperarContrasena(correo: String) {
        auth.sendPasswordResetEmail(correo.trim()).await()
    }

    fun cerrarSesion() = auth.signOut()

    /**
     * Cambia la contraseña de quien tiene la sesion abierta.
     *
     * ANTES EL DIALOGO "Cambiar Contraseña" (ProfileScreen.SecurityDialog) SOLO
     * VALIDABA LOS CAMPOS Y CERRABA: no llamaba a Firebase para nada, asi que la
     * contraseña de verdad nunca cambiaba. Por eso alguien podia "cambiarla", cerrar
     * sesion, y solo la contraseña VIEJA seguia funcionando.
     *
     * Se reautentica con la contraseña ACTUAL antes de cambiarla por dos motivos: (1)
     * Firebase exige un inicio de sesion "reciente" para dejar tocar la contraseña
     * (`updatePassword` sola lanza FirebaseAuthRecentLoginRequiredException si la
     * sesion ya lleva un rato abierta) y (2) de paso comprueba que quien la cambia de
     * verdad sabe la actual, en vez de que baste con tener el telefono desbloqueado.
     */
    suspend fun cambiarContrasena(contrasenaActual: String, contrasenaNueva: String) {
        val usuario = auth.currentUser ?: throw IllegalStateException("No hay sesión iniciada")
        val correo = usuario.email ?: throw IllegalStateException("Esta cuenta no tiene correo")
        val credencial = EmailAuthProvider.getCredential(correo, contrasenaActual)
        usuario.reauthenticate(credencial).await()
        usuario.updatePassword(contrasenaNueva).await()
    }

    // ------------------------------------------------------------------
    // Roles
    // ------------------------------------------------------------------

    private val rolesRef = FirebaseDatabase.getInstance().reference.child("roles")

    /**
     * Escucha el rol de un usuario y avisa si cambia.
     *
     * Que sea un Flow y no una lectura de una vez tiene una consecuencia util: si
     * un admin te asciende a doctor mientras tienes la app abierta, la interfaz
     * cambia al momento, sin necesidad de cerrar sesion.
     */
    fun rolDe(uid: String): Flow<Rol> = callbackFlow {
        val ref = rolesRef.child(uid)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(Rol.desde(snapshot.getValue(String::class.java)))
            }

            override fun onCancelled(error: DatabaseError) {
                // Si no se puede leer el rol, se asume el permiso mas bajo en vez
                // de romper la sesion. Nunca conviene fallar "hacia arriba".
                trySend(Rol.CLIENTE)
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    /**
     * Al registrarse, deja escrito /roles/{uid} = "cliente".
     *
     * Se hace una sola vez y solo si el nodo no existe todavia, para no pisar el
     * rol de alguien a quien un admin ya haya ascendido.
     *
     * Nadie elige su rol al registrarse: si el formulario de registro permitiera
     * marcar "soy administrador", cualquiera con la app tendria control total de
     * la clinica. Ascender es siempre decision de un admin.
     */
    suspend fun crearRolInicialSiNoExiste(uid: String) {
        val ref = rolesRef.child(uid)
        if (!ref.get().await().exists()) {
            ref.setValue(Rol.CLIENTE.clave).await()
        }
    }
}
