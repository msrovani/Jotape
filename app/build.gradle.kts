import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.devtools.ksp")
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

        // Expose as BuildConfig fields using triple quotes to embed the string value correctly
        buildConfigField("String", "SUPABASE_URL", """${supabaseUrl}""")
        buildConfigField("String", "SUPABASE_ANON_KEY", """${supabaseKey}""")
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", """${googleClientId}""")
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
            exclude("META-INF/{AL2.0,LGPL2.1}")
            exclude("META-INF/versions/9/previous-compilation-data.bin")
        }
    }
}

dependencies {

    // Kotlin Standard Library - ESSENTIAL
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:${rootProject.extra["kotlin_version"]}")

    implementation(libs.dagger.hilt.android)
    ksp(libs.dagger.hilt.compiler)

    implementation("androidx.core:core-ktx:1.13.1")
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

    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)
    testImplementation(libs.androidx.room.testing)

    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)

    implementation(libs.androidx.navigation.compose)

    implementation(libs.kotlinx.serialization.json)

    // Ktor (Dependencies moved before Supabase)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)

    // Supabase
    implementation(libs.supabase.gotrue)
    implementation(libs.supabase.postgrest)
    implementation(libs.supabase.storage)
    implementation(libs.supabase.realtime)
    implementation(libs.supabase.compose.auth)

    // Testes
    androidTestImplementation(libs.androidx.junit)

   
}

ksp {
    arg("dagger.hilt.android.internal.disableAndroidSuperclassValidation", "true")
}