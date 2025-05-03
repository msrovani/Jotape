import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.dagger.hilt)
    alias(libs.plugins.kotlin.serialization)
}

// Load properties from local.properties
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}

android {
    namespace = "com.jotape"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.jotape"
        minSdk = 30
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Read values from local.properties
        val supabaseUrl: String = localProperties.getProperty("supabase.url", "YOUR_DEFAULT_URL_IF_NOT_SET")
        val supabaseKey: String = localProperties.getProperty("supabase.key", "YOUR_DEFAULT_KEY_IF_NOT_SET")
        val googleClientId: String = localProperties.getProperty("google.web.client.id", "YOUR_DEFAULT_GOOGLE_CLIENT_ID_IF_NOT_SET")
        // Ler a chave da API do Gemini
        val geminiApiKey: String = localProperties.getProperty("gemini.api.key", "YOUR_DEFAULT_GEMINI_KEY_IF_NOT_SET")

        // Expose as BuildConfig fields using triple quotes to embed the string value correctly
        // Forma correta escapando aspas para Java: "\"valor\""
        buildConfigField("String", "SUPABASE_URL",         "\"${supabaseUrl.replace("\"", "\\\"")}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY",    "\"${supabaseKey.replace("\"", "\\\"")}\"")
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"${googleClientId.replace("\"", "\\\"")}\"")
        buildConfigField("String", "GEMINI_API_KEY",       "\"${geminiApiKey.replace("\"", "\\\"")}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            // Load properties from local.properties
            val localProps = Properties()
            val localPropsFile = rootProject.file("local.properties")
            if (localPropsFile.exists()) {
                localPropsFile.inputStream().use { localProps.load(it) }
            } else {
                println("Warning: local.properties not found. Default values will be used.")
            }

            // Helper function to safely get properties or return a default value
            fun getLocalProperty(key: String, defaultValue: String): String {
                return localProps.getProperty(key, defaultValue)
            }

            // BuildConfig fields - READ THESE CAREFULLY
            // Supabase URL - CRITICAL for connection
            val supabaseUrl = getLocalProperty("supabase.url", "YOUR_DEFAULT_URL")
            buildConfigField("String", "SUPABASE_URL", "\"${supabaseUrl.replace("\"", "\\\"")}\"")

            // Supabase Anon Key - CRITICAL for connection
            val supabaseKey = getLocalProperty("supabase.key", "YOUR_DEFAULT_KEY")
            buildConfigField("String", "SUPABASE_ANON_KEY", "\"${supabaseKey.replace("\"", "\\\"")}\"")

            // Google Web Client ID - Needed for Google Sign In
            val googleWebClientId = getLocalProperty("google.web.client.id", "YOUR_DEFAULT_GOOGLE_ID")
            buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"${googleWebClientId.replace("\"", "\\\"")}\"")

            // Gemini API Key - Read from local.properties
            val geminiApiKey = getLocalProperty("gemini.api.key", "NO_GEMINI_KEY_IN_PROPERTIES") // Provide a default if not found
            buildConfigField("String", "GEMINI_API_KEY", "\"${geminiApiKey.replace("\"", "\\\"")}\"")

            // Gemini API URL - REMOVED, not needed in client if backend handles it
            // val geminiApiUrl = getLocalProperty("Gemini.api.url", "")
            // buildConfigField("String", "GEMINI_API_URL", ""\"${geminiApiUrl.replace(""", "\"")}\"")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
    packaging {
        resources {
            excludes.add("META-INF/{AL2.0,LGPL2.1}")
            excludes.add("META-INF/versions/9/previous-compilation-data.bin")
        }
    }
}

dependencies {
    implementation(libs.kotlin.stdlib)
    implementation(libs.androidx.core.ktx)

    implementation(libs.dagger.hilt.android)
    ksp(libs.dagger.hilt.compiler)

    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.0")
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation(libs.androidx.hilt.navigation.compose)

    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)

    implementation(libs.androidx.navigation.compose)

    implementation(libs.kotlinx.serialization.json)

    implementation(libs.retrofit2.retrofit)
    implementation(libs.retrofit2.converter.kotlinx.serialization)

    implementation(libs.okhttp3.logging.interceptor)

    implementation(libs.supabase.gotrue)
    implementation(libs.supabase.postgrest)
    implementation(libs.supabase.storage)
    implementation(libs.supabase.realtime)
    implementation(libs.supabase.compose.auth)

    androidTestImplementation(libs.androidx.junit)
}

ksp {
    arg("dagger.hilt.android.internal.disableAndroidSuperclassValidation", "true")
}