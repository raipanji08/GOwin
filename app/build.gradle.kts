import groovy.json.JsonSlurper
import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
}

val googleWebClientId = file("google-services.json")
    .takeIf { it.exists() }
    ?.let { JsonSlurper().parse(it) as? Map<*, *> }
    ?.get("client")
    ?.let { it as? List<*> }
    ?.asSequence()
    ?.mapNotNull { it as? Map<*, *> }
    ?.flatMap { client ->
        (client["oauth_client"] as? List<*>)
            .orEmpty()
            .asSequence()
            .mapNotNull { it as? Map<*, *> }
    }
    ?.firstOrNull { (it["client_type"] as? Number)?.toInt() == 3 }
    ?.get("client_id")
    ?.toString()
    .orEmpty()

val localProperties = Properties().apply {
    val propertiesFile = rootProject.file("local.properties")
    if (propertiesFile.exists()) {
        propertiesFile.inputStream().use(::load)
    }
}
val midtransBackendUrl = localProperties
    .getProperty("MIDTRANS_BACKEND_URL")
    .orEmpty()
    .trim()
    .trimEnd('/')
    .ifBlank {
        "https://gowin-midtrans.gowin-ahmadhaqinn.workers.dev"
    }
    .replace("\\", "\\\\")
    .replace("\"", "\\\"")

android {
    namespace = "com.panjirai0110.gowin"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.panjirai0110.gowin"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        resValue("string", "google_web_client_id", googleWebClientId)
        buildConfigField(
            "String",
            "MIDTRANS_BACKEND_URL",
            "\"$midtransBackendUrl\""
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
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.zxing.core)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
