package com.example.keepet.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.keepet.ui.theme.*
import com.example.keepet.viewmodel.AuthUiState
import com.example.keepet.viewmodel.AuthViewModel

/**
 * Login y registro reales contra Firebase Authentication.
 *
 * Antes esta pantalla solo llamaba a onLoginClick() y te dejaba pasar escribieras
 * lo que escribieras: no habia usuarios, asi que tampoco habia forma de separar
 * las mascotas de cada persona. Ahora el correo y la contrasena se validan de
 * verdad, y el uid que devuelve Firebase es lo que decide de que rama de la base
 * de datos se leen los datos.
 *
 * Fijate en que esta funcion NO recibe onLoginClick. Cuando el login funciona,
 * AuthViewModel.usuario cambia y MainActivity, que lo esta observando, cambia de
 * pantalla solo. La pantalla no tiene que avisar a nadie.
 */
@Composable
fun LoginScreen(authViewModel: AuthViewModel) {
    var correo by remember { mutableStateOf("") }
    var contrasena by remember { mutableStateOf("") }
    var modoRegistro by remember { mutableStateOf(false) }
    var verContrasena by remember { mutableStateOf(false) }

    LoginContenido(
        correo = correo,
        onCorreoChange = { correo = it; authViewModel.limpiarMensajes() },
        contrasena = contrasena,
        onContrasenaChange = { contrasena = it; authViewModel.limpiarMensajes() },
        verContrasena = verContrasena,
        onToggleVerContrasena = { verContrasena = !verContrasena },
        modoRegistro = modoRegistro,
        onToggleModo = { modoRegistro = !modoRegistro; authViewModel.limpiarMensajes() },
        estado = authViewModel.uiState,
        onEnviar = {
            if (modoRegistro) authViewModel.registrarse(correo, contrasena)
            else authViewModel.iniciarSesion(correo, contrasena)
        },
        onOlvideContrasena = { authViewModel.recuperarContrasena(correo) }
    )
}

/**
 * La parte visual, separada del ViewModel.
 *
 * Se hace asi para que el @Preview de abajo funcione: un Preview no puede crear
 * un AuthViewModel de verdad porque necesitaria Firebase en marcha.
 */
@Composable
private fun LoginContenido(
    correo: String,
    onCorreoChange: (String) -> Unit,
    contrasena: String,
    onContrasenaChange: (String) -> Unit,
    verContrasena: Boolean,
    onToggleVerContrasena: () -> Unit,
    modoRegistro: Boolean,
    onToggleModo: () -> Unit,
    estado: AuthUiState,
    onEnviar: () -> Unit,
    onOlvideContrasena: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 48.dp, bottom = 32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Pets,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = TextColor
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("KeePet", fontSize = 40.sp, fontWeight = FontWeight.Bold, color = TextColor)
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (modoRegistro) "Crea tu cuenta 🐾" else "¡Hola de nuevo! 👋",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextColor
                )
                Text(
                    text = if (modoRegistro) "Regístrate para guardar tus mascotas en la nube"
                    else "Inicia sesión para cuidar a tus amigos",
                    fontSize = 14.sp,
                    color = GrayHint,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
                )

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Correo electrónico",
                        fontSize = 14.sp,
                        color = TextColor,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    TextField(
                        value = correo,
                        onValueChange = onCorreoChange,
                        singleLine = true,
                        enabled = !estado.cargando,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        placeholder = { Text("ejemplo@correo.com", color = GrayHint) },
                        leadingIcon = {
                            Icon(Icons.Default.Email, contentDescription = null, tint = TextColor)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        shape = RoundedCornerShape(12.dp),
                        colors = coloresCampo()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Contraseña",
                        fontSize = 14.sp,
                        color = TextColor,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    TextField(
                        value = contrasena,
                        onValueChange = onContrasenaChange,
                        singleLine = true,
                        enabled = !estado.cargando,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        placeholder = { Text("Mínimo 6 caracteres", color = GrayHint) },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = TextColor)
                        },
                        trailingIcon = {
                            // Antes este icono era decorativo: no hacia nada al pulsarlo.
                            IconButton(onClick = onToggleVerContrasena) {
                                Icon(
                                    imageVector = if (verContrasena) Icons.Default.Visibility
                                    else Icons.Default.VisibilityOff,
                                    contentDescription = if (verContrasena) "Ocultar contraseña"
                                    else "Mostrar contraseña",
                                    tint = GrayHint
                                )
                            }
                        },
                        visualTransformation = if (verContrasena) VisualTransformation.None
                        else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        shape = RoundedCornerShape(12.dp),
                        colors = coloresCampo()
                    )
                }

                if (!modoRegistro) {
                    TextButton(
                        onClick = onOlvideContrasena,
                        enabled = !estado.cargando,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("¿Olvidaste tu contraseña?", fontSize = 12.sp, color = TextColor)
                    }
                }

                // Mensajes de error y de aviso, para que el usuario sepa que pasa
                // en vez de quedarse mirando un boton que no reacciona.
                estado.error?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(it, color = CancelRed, fontSize = 13.sp, textAlign = TextAlign.Center)
                }
                estado.aviso?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(it, color = TextColor, fontSize = 13.sp, textAlign = TextAlign.Center)
                }
            }
        }

        Button(
            onClick = onEnviar,
            enabled = !estado.cargando,
            colors = ButtonDefaults.buttonColors(containerColor = AccentButton),
            shape = RoundedCornerShape(50.dp),
            modifier = Modifier
                .height(56.dp)
                .width(200.dp)
        ) {
            // Blanco, no marron. El fondo del boton es el terracota de la app: cuando era
            // un salmon mas claro el marron se leia mejor que el blanco, pero al oscurecer
            // el boton (mira AccentButton en ui/theme/Color.kt) es al contrario. Es el
            // unico boton de la app que llevaba la letra marron.
            if (estado.cargando) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = White,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = if (modoRegistro) "Registrarme" else "Entrar",
                    fontSize = 18.sp,
                    color = White,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        TextButton(
            onClick = onToggleModo,
            enabled = !estado.cargando,
            modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
        ) {
            Text(
                text = if (modoRegistro) "Ya tengo cuenta, iniciar sesión"
                else "No tengo cuenta, registrarme",
                fontSize = 14.sp,
                color = TextColor
            )
        }
    }
}

/**
 * Colores de los campos de correo y contraseña.
 *
 * Los tres colores de TEXTO no estaban puestos, asi que lo que escribias heredaba el
 * color del tema del sistema: con el movil en modo oscuro salia casi blanco sobre el
 * verde clarito del campo y no se leia lo que estabas tecleando. Ahora es siempre el
 * marron de la app.
 */
@Composable
private fun coloresCampo() = TextFieldDefaults.colors(
    focusedTextColor = TextColor,
    unfocusedTextColor = TextColor,
    disabledTextColor = TextColor,
    cursorColor = TextColor,
    focusedContainerColor = InputBackground,
    unfocusedContainerColor = InputBackground,
    disabledContainerColor = InputBackground,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent
)

@Preview(showBackground = true)
@Composable
private fun LoginScreenPreview() {
    KeePetTheme {
        LoginContenido(
            correo = "tati@correo.com",
            onCorreoChange = {},
            contrasena = "123456",
            onContrasenaChange = {},
            verContrasena = false,
            onToggleVerContrasena = {},
            modoRegistro = false,
            onToggleModo = {},
            estado = AuthUiState(),
            onEnviar = {},
            onOlvideContrasena = {}
        )
    }
}
