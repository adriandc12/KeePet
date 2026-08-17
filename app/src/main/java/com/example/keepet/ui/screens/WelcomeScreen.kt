package com.example.keepet.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.keepet.ui.theme.AccentButton
import com.example.keepet.ui.theme.BackgroundColor
import com.example.keepet.ui.theme.KeePetTheme
import com.example.keepet.ui.theme.TextColor
import com.example.keepet.ui.theme.White

import com.example.keepet.R

@Composable
fun WelcomeScreen(onNextClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly // Mejor distribución
    ) {
        // Logo Section
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Pets,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = TextColor
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "KeePet",
                fontSize = 40.sp, // Aumentado de 32.sp a 40.sp
                fontWeight = FontWeight.Bold,
                color = TextColor
            )
        }

        // Main Image
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(320.dp)
                .background(Color.Transparent) // Cambiado a transparente
        ) {
            Image(
                painter = painterResource(id = R.drawable.pet_group),
                contentDescription = "Mascotas KeePet",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(15.dp)), // Recorte aplicado directamente a la imagen
                contentScale = ContentScale.Crop // Asegura que llene el espacio y se recorte
            )
        }

        // Slogan
        Text(
            text = "Cuídamos a los que\nmás amas",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = TextColor,
            textAlign = TextAlign.Center,
            lineHeight = 34.sp
        )

        // Button
        Button(
            onClick = onNextClick,
            colors = ButtonDefaults.buttonColors(containerColor = AccentButton),
            shape = RoundedCornerShape(50.dp),
            modifier = Modifier
                .height(56.dp)
                .width(200.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Blanco sobre el terracota del boton, igual que en el resto de la app.
                Text(
                    text = "Siguiente",
                    fontSize = 18.sp,
                    color = White,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = White
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun WelcomeScreenPreview() {
    KeePetTheme {
        WelcomeScreen(onNextClick = {})
    }
}
