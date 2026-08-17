// Archivo de nivel raiz: aqui SOLO se declaran los plugins, con "apply false".
// "apply false" significa: "descarga este plugin y dejalo disponible, pero no lo
// actives aqui". Quien lo activa de verdad es app/build.gradle.kts.
//
// IMPORTANTE: cada plugin debe aparecer UNA SOLA VEZ en este bloque. Declararlo
// dos veces es lo que provoca el error:
//   "Plugin with id 'com.google.gms.google-services' was already requested at line 2"
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.google.services) apply false
}
