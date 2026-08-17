import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    // Lee google-services.json y genera las claves de Firebase. Debe ir DESPUES
    // del plugin de Android. No añadas aqui "org.jetbrains.kotlin.android":
    // AGP 9 ya trae Kotlin incorporado, y añadirlo provoca el error
    // "Cannot add extension with name 'kotlin'".
    alias(libs.plugins.google.services)
}

/**
 * Lee local.properties, donde estan los datos de la cuenta de Cloudinary.
 *
 * POR QUE AHI Y NO EN EL CODIGO: local.properties no se sube nunca a GitHub
 * (esta en .gitignore de serie y el propio archivo lo advierte en su cabecera).
 * Asi tus datos de cuenta no acaban publicados, y cada persona que trabaje en el
 * proyecto puede poner los suyos sin tocar el codigo.
 *
 * Si el archivo o la clave no existen se devuelve texto vacio en vez de fallar:
 * el proyecto tiene que poder compilar aunque todavia no hayas puesto tus datos.
 * La app avisara "Falta configurar Cloudinary" al intentar subir una foto.
 */
val propiedadesLocales = Properties().apply {
    val archivo = rootProject.file("local.properties")
    if (archivo.exists()) archivo.inputStream().use { load(it) }
}

fun propiedadLocal(clave: String): String = propiedadesLocales.getProperty(clave, "")

android {
    namespace = "com.example.keepet"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.example.keepet"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Estos dos valores viajan de local.properties al codigo Kotlin, donde se
        // leen como BuildConfig.CLOUDINARY_CLOUD_NAME y BuildConfig.CLOUDINARY_UPLOAD_PRESET.
        //
        // Fijate en las comillas escapadas (\"): buildConfigField escribe una linea
        // de codigo Java tal cual, asi que el valor tiene que llevar sus propias
        // comillas o no compilaria.
        //
        // El upload preset NO es una contraseña: solo dice "se admiten subidas sin
        // firmar con estas reglas". La clave secreta (api_secret) no aparece por
        // ningun lado, y eso es justo lo que hace seguro este metodo en una app
        // movil: cualquiera puede descompilar un APK y leer lo que lleve dentro.
        buildConfigField(
            "String",
            "CLOUDINARY_CLOUD_NAME",
            "\"${propiedadLocal("cloudinary.cloudName")}\""
        )
        buildConfigField(
            "String",
            "CLOUDINARY_UPLOAD_PRESET",
            "\"${propiedadLocal("cloudinary.uploadPreset")}\""
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true
    }
    buildFeatures {
        compose = true
        // Necesario para que existan los BuildConfig.CLOUDINARY_* de arriba.
        // Sin esta linea, AGP 9 no genera la clase BuildConfig y el codigo no compila.
        buildConfig = true
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.coil.compose)

    // WorkManager (recordatorios de citas)
    implementation(libs.androidx.work.runtime.ktx)

    // --- Firebase ---
    // La BOM (Bill of Materials) decide que version usa cada libreria de Firebase,
    // por eso las de abajo no llevan numero de version. Asi nunca se pelean entre si.
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.database)
    implementation(libs.firebase.auth)
    // Permite usar .await() sobre las Task de Firebase dentro de corrutinas
    implementation(libs.kotlinx.coroutines.play.services)

    // --- Fotos: Cloudinary ---
    // Realtime Database guarda texto y numeros; no sirve para imagenes. Antes se
    // usaba Firebase Storage, pero pasa a requerir el plan de pago Blaze. Cloudinary
    // hace lo mismo con plan gratis y sin tarjeta: la foto vive alli y en la base de
    // datos solo se guarda su URL, que es una linea de texto.
    implementation(libs.cloudinary.android)

    // --- QR ---
    // zxing genera el codigo QR (solo la parte matematica, sin interfaz).
    implementation(libs.zxing.core)
    // El escaner de Google Play Services. Se eligio este y no una libreria de
    // camara porque no requiere pedir permiso de camara ni montar CameraX:
    // abre su propia pantalla de escaneo y devuelve el texto leido.
    implementation(libs.play.services.code.scanner)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
