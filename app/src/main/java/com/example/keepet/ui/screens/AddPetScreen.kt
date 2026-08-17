package com.example.keepet.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.keepet.R
import com.example.keepet.data.model.Mascota
import com.example.keepet.viewmodel.GestorDeMascotas
import com.example.keepet.ui.theme.*

/**
 * Formulario para crear o editar la ficha de una mascota.
 *
 * Recibe GestorDeMascotas y no PetViewModel para que la MISMA pantalla, con el mismo
 * diseño, valga para el cliente (crea sus mascotas) y para el personal de la clinica
 * (crea y edita las de cualquier cliente). Quien decide sobre que mascota se escribe
 * es el gestor que te pasen, no esta pantalla.
 *
 * TIENE 3 PASOS PARA LA CLINICA Y 2 PARA EL CLIENTE. El tercero son los antecedentes
 * medicos (alergias, condiciones, vacunas) y eso lo escribe el veterinario, no el dueño:
 * lo decide viewModel.puedeEditarDatosClinicos.
 */
@Composable
fun AddPetScreen(
    viewModel: GestorDeMascotas,
    onBack: () -> Unit,
    onFinish: () -> Unit,
    petToEdit: Mascota? = null,
    initialStep: Int = 1
) {
    var step by remember { mutableIntStateOf(initialStep) }

    // Mismo aviso que en PetDetailScreen: si guardar falla (sin internet, permiso
    // denegado...) el usuario lo ve aqui, en vez de que el formulario se cierre sin
    // decir nada y parezca que el cambio se perdio.
    val context = LocalContext.current
    LaunchedEffect(viewModel.mensaje) {
        viewModel.mensaje?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.limpiarMensaje()
        }
    }

    // Quien rellena esto: la clinica o el dueño. Cambia cuantos pasos hay y como se
    // llaman las cosas ("expediente" cuando lo abre la clinica, "mascota" cuando la
    // apunta su dueño: para el cliente un "expediente" no significa nada).
    val esClinica = viewModel.puedeEditarDatosClinicos
    val totalPasos = if (esClinica) 3 else 2

    // Form State
    var selectedSpecies by remember { mutableStateOf(petToEdit?.especie ?: "") }
    var petName by remember { mutableStateOf(petToEdit?.nombre ?: "") }
    var petAge by remember { mutableStateOf(petToEdit?.edad ?: "") }
    var petBreed by remember { mutableStateOf(petToEdit?.raza ?: "") }
    var ownerName by remember { mutableStateOf(petToEdit?.dueno ?: "") }
    var ownerPhone by remember { mutableStateOf(petToEdit?.telefono ?: "") }
    var ownerAddress by remember { mutableStateOf(petToEdit?.direccion ?: "") }
    var petWeight by remember { mutableStateOf(petToEdit?.peso ?: "") }
    var allergies by remember { mutableStateOf(petToEdit?.alergias?.joinToString(", ") ?: "") }
    var medicalConditions by remember { mutableStateOf(petToEdit?.notasMedicas ?: "") }
    var vaccines by remember { mutableStateOf(petToEdit?.vacunas ?: "") }

    val scrollState = rememberScrollState()

    /**
     * Que falta por rellenar en el paso que se esta viendo. null = se puede continuar.
     *
     * Antes no habia ninguna validacion: se podia pulsar "Siguiente" sin elegir especie y
     * "Finalizar" sin escribir el nombre, y quedaba en la base de datos de la clinica un
     * expediente en blanco, imposible de buscar y sin saber de que animal es ni a quien
     * llamar. En una ficha clinica el nombre del paciente y el del dueño son el minimo.
     *
     * El telefono NO se exige: hay clientes que no lo dan en la primera visita y bloquear
     * el expediente por eso seria peor que guardarlo sin telefono. Lo que si se hace es
     * impedir que se escriban letras en ese campo.
     */
    val loQueFalta: String? = when (step) {
        1 -> if (selectedSpecies.isBlank()) {
            if (esClinica) "Elige la especie del paciente para continuar"
            else "Elige la especie de tu mascota para continuar"
        } else null
        2 -> when {
            petName.isBlank() ->
                if (esClinica) "El paciente necesita un nombre"
                else "Tu mascota necesita un nombre"
            ownerName.isBlank() ->
                if (esClinica) "Falta el nombre del dueño" else "Falta tu nombre"
            else -> null
        }
        else -> null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
            .padding(24.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Pets, contentDescription = null, tint = TextColor, modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("KeePet", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextColor)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = when {
                    esClinica && petToEdit == null -> "Crear expediente"
                    esClinica -> "Editar expediente"
                    petToEdit == null -> "Añadir mascota"
                    else -> "Editar mascota"
                },
                fontSize = 16.sp,
                color = TextColor,
                fontWeight = FontWeight.Medium
            )
            Text("$step/$totalPasos", fontSize = 16.sp, color = TextColor, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Progress Bar
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(totalPasos) { index ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(if (index + 1 <= step) AccentButton else Color.LightGray.copy(alpha = 0.3f))
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        when (step) {
            1 -> StepOne(selectedSpecies, esClinica) { selectedSpecies = it }
            2 -> StepTwo(
                esClinica,
                petName, { petName = it },
                petAge, { petAge = it },
                petBreed, { petBreed = it },
                petWeight, { petWeight = it },
                ownerName, { ownerName = it },
                // Se filtra aqui, mientras se escribe, en vez de avisar despues: un
                // telefono con letras no sirve para llamar a nadie. Se dejan el +, el
                // guion y el espacio porque se usan de verdad al escribir un numero.
                ownerPhone, { nuevo -> ownerPhone = nuevo.filter { it.isDigit() || it in "+- " } },
                ownerAddress, { ownerAddress = it }
            )
            3 -> StepThree(
                allergies, { allergies = it },
                medicalConditions, { medicalConditions = it },
                vaccines, { vaccines = it }
            )
        }

        Spacer(modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.height(32.dp))

        // Se dice QUE falta antes de que el usuario se pelee con un boton apagado. Un
        // boton deshabilitado sin explicacion parece que la app esta rota.
        loQueFalta?.let { aviso ->
            Text(
                text = aviso,
                color = CancelRed,
                fontSize = 13.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            )
        }

        // Navigation Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = { 
                    if (step > 1) step-- else onBack()
                },
                modifier = Modifier.weight(0.4f).height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7BA3B2)), // Color azulado del diseño
                shape = RoundedCornerShape(50.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = White)
            }

            Button(
                onClick = {
                    if (step < totalPasos) {
                        step++
                    } else {
                        val newPet = Mascota(
                            // Al crear va vacio y Firebase asigna la clave con push().
                            // Al editar se conserva la clave para sobrescribir la misma
                            // mascota en vez de crear una copia nueva.
                            id = petToEdit?.id.orEmpty(),
                            // trim() en los textos que se buscan y se muestran: un nombre
                            // guardado como "Milo " no encuentra nada al buscar "Milo".
                            nombre = petName.trim(),
                            especie = selectedSpecies,
                            raza = petBreed.trim(),
                            edad = petAge.trim(),
                            dueno = ownerName.trim(),
                            telefono = ownerPhone.trim(),
                            direccion = ownerAddress,
                            // OJO, ESTO ES IMPORTANTE. Guardar una mascota reescribe el
                            // registro COMPLETO, asi que los datos clinicos hay que
                            // ponerlos siempre: si aqui llegara vacio, se borrarian de la
                            // base de datos. Cuando lo rellena la clinica salen del paso
                            // 3; cuando lo rellena el cliente, que no ve ese paso, se
                            // copian tal cual estaban. Sin este "if", el dueño editando
                            // el peso de su perro le borraba las alergias.
                            alergias = if (esClinica) {
                                allergies.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                            } else {
                                petToEdit?.alergias ?: emptyList()
                            },
                            notasMedicas = if (esClinica) medicalConditions.trim()
                            else petToEdit?.notasMedicas.orEmpty(),
                            vacunas = if (esClinica) vaccines.trim()
                            else petToEdit?.vacunas.orEmpty(),
                            peso = petWeight.trim(),
                            historial = petToEdit?.historial ?: emptyList(),
                            // Estos tres campos no se editan en este formulario. Antes no
                            // se copiaban, asi que editar una mascota le borraba la foto,
                            // el sexo y el correo. Ahora se conservan.
                            imagenUri = petToEdit?.imagenUri,
                            sexo = petToEdit?.sexo.orEmpty(),
                            correo = petToEdit?.correo.orEmpty()
                            // Ya no se guarda imagenRes: la imagen por defecto se deduce
                            // de la especie en la UI (ui/components/Avatares.kt).
                        )
                        if (petToEdit == null) {
                            viewModel.addPet(newPet)
                        } else {
                            viewModel.updatePet(newPet)
                        }
                        onFinish()
                    }
                },
                // Sin esto se podia guardar un expediente sin especie y sin nombre.
                enabled = loQueFalta == null,
                modifier = Modifier.weight(0.6f).height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentButton),
                shape = RoundedCornerShape(50.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (step < totalPasos) "Siguiente" else "Finalizar", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                }
            }
        }
    }
}

@Composable
fun StepOne(selected: String, esClinica: Boolean, onSelect: (String) -> Unit) {
    Column(horizontalAlignment = Alignment.Start, modifier = Modifier.fillMaxWidth()) {
        Text("¿Qué especie es?", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextColor)
        Text(
            if (esClinica) "Seleccione la especie para comenzar\nel expediente clínico."
            else "Elige la especie de tu mascota\npara empezar.",
            fontSize = 14.sp,
            color = TextColor.copy(alpha = 0.7f),
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
        )

        SpeciesCard("Perro", "Canino", R.drawable.perro, selected == "Perro") { onSelect("Perro") }
        Spacer(modifier = Modifier.height(16.dp))
        SpeciesCard("Gato", "Felino", R.drawable.gato, selected == "Gato") { onSelect("Gato") }
        Spacer(modifier = Modifier.height(16.dp))
        SpeciesCard("Conejo", "Lagomorfo", R.drawable.conejo, selected == "Conejo") { onSelect("Conejo") }
    }
}

@Composable
fun SpeciesCard(name: String, scientific: String, imageRes: Int, isSelected: Boolean, onSelect: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) AccentButton else Color.Transparent,
                shape = RoundedCornerShape(24.dp)
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = imageRes),
                contentDescription = name,
                modifier = Modifier.size(80.dp).clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextColor)
                Text(scientific, fontSize = 14.sp, color = TextColor.copy(alpha = 0.6f))
            }
        }
    }
}

@Composable
fun StepTwo(
    esClinica: Boolean,
    name: String, onNameChange: (String) -> Unit,
    age: String, onAgeChange: (String) -> Unit,
    breed: String, onBreedChange: (String) -> Unit,
    weight: String, onWeightChange: (String) -> Unit,
    owner: String, onOwnerChange: (String) -> Unit,
    phone: String, onPhoneChange: (String) -> Unit,
    address: String, onAddressChange: (String) -> Unit
) {
    Column(horizontalAlignment = Alignment.Start, modifier = Modifier.fillMaxWidth()) {
        // Antes decia "¿Como se llama tu compañero?" y "para personalizar su
        // experiencia". Este formulario ya no lo usa solo el dueño: es el que rellena la
        // clinica al abrir un expediente, asi que el texto habla de paciente y de dueño.
        Text(
            if (esClinica) "Datos del paciente" else "Datos de tu mascota",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = TextColor
        )
        Text(
            if (esClinica) "Nombre y datos básicos del animal,\ny cómo localizar a su dueño."
            else "Los datos de tu mascota y un teléfono\ndonde la clínica pueda avisarte.",
            fontSize = 14.sp,
            color = TextColor.copy(alpha = 0.7f),
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
        )

        CustomInputField("Nombre", name, onNameChange, Icons.Default.Label)
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(modifier = Modifier.weight(1f)) {
                CustomInputField("Edad", age, onAgeChange, Icons.Default.Cake)
            }
            Box(modifier = Modifier.weight(1f)) {
                CustomInputField("Peso", weight, onWeightChange, Icons.Default.MonitorWeight)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        CustomInputField("Raza", breed, onBreedChange, Icons.Default.Pets)

        Spacer(modifier = Modifier.height(32.dp))
        Text(
            if (esClinica) "Datos del dueño" else "Tus datos",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextColor
        )
        Spacer(modifier = Modifier.height(16.dp))

        CustomInputField("Nombre", owner, onOwnerChange, Icons.Default.Label)
        Spacer(modifier = Modifier.height(16.dp))
        CustomInputField(
            "Número de teléfono", phone, onPhoneChange, Icons.Default.Phone,
            keyboardType = KeyboardType.Phone
        )
        Spacer(modifier = Modifier.height(16.dp))
        CustomInputField("Dirección", address, onAddressChange, Icons.Default.LocationOn)
    }
}

/**
 * Paso 3: antecedentes medicos.
 *
 * Aqui habia tambien un apartado "Servicios adicionales" con una casilla de "Incluir
 * servicio de baño" y tres tipos de baño, que solo aparecia si la especie era Perro. Se
 * ha quitado por dos motivos: esos dos datos se guardaban en la base de datos y **no se
 * mostraban en ninguna pantalla** (ni en la ficha del cliente ni en la del veterinario),
 * y un baño no es un dato del expediente clinico del animal, es un servicio que se pide
 * al agendar una cita, donde sigue estando disponible.
 */
@Composable
fun StepThree(
    allergies: String, onAllergiesChange: (String) -> Unit,
    conditions: String, onConditionsChange: (String) -> Unit,
    vaccines: String, onVaccinesChange: (String) -> Unit
) {
    Column(horizontalAlignment = Alignment.Start, modifier = Modifier.fillMaxWidth()) {
        Text("Antecedentes médicos", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextColor)
        Text(
            "Alergias, condiciones y vacunas.\nSe pueden completar más tarde.",
            fontSize = 14.sp,
            color = TextColor.copy(alpha = 0.7f),
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
        )

        CustomInputField("Alergias conocidas", allergies, onAllergiesChange, Icons.Default.MedicalServices)
        Spacer(modifier = Modifier.height(16.dp))
        CustomInputField("Condiciones médicas", conditions, onConditionsChange, Icons.Default.LocalHospital)
        Spacer(modifier = Modifier.height(16.dp))
        CustomInputField("Vacunas registradas", vaccines, onVaccinesChange, Icons.Default.Vaccines)
    }
}

@Composable
fun CustomInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    icon: ImageVector,
    // Para el telefono: asi sale el teclado de numeros y no el de letras. Es una ayuda,
    // no una validacion; quien filtra de verdad lo que entra es quien llama a esta
    // funcion, porque un teclado se puede cambiar desde el propio movil.
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, fontSize = 14.sp, color = TextColor, modifier = Modifier.padding(bottom = 8.dp))
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            leadingIcon = { Icon(icon, contentDescription = null, tint = TextColor) },
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            shape = RoundedCornerShape(12.dp),
            colors = TextFieldDefaults.colors(
                focusedTextColor = TextColor,
                unfocusedTextColor = TextColor,
                cursorColor = TextColor,
                focusedContainerColor = InputBackground,
                unfocusedContainerColor = InputBackground,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            textStyle = LocalTextStyle.current.copy(
                color = TextColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AddPetScreenPreview() {
    KeePetTheme {
        // Se usa un marcador de posición porque AddPetScreen requiere un ViewModel con dependencias
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Vista previa de Agregar Mascota (Requiere ViewModel)")
        }
    }
}
