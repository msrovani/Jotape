pluginManagement {
    repositories {
        // Restaurar google() e mavenCentral()
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
    // Add plugin resolution strategy if needed
    // resolutionStrategy {
    //     eachPlugin {
    //         if (requested.id.id == "com.google.devtools.ksp") {
    //             useVersion("...") // Specify version here if catalog doesn't work
    //         }
    //     }
    // }
    plugins {
        // Tentar versão patch mais recente do KSP
        id("com.google.devtools.ksp") version "1.9.24-1.0.20" apply false
        id("com.google.dagger.hilt.android") version "2.50" apply false
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // Restaurar repositórios para dependências também
        google()
        mavenCentral()
    }
}

// Explicitly enable Version Catalogs feature
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "Jotape"
include(":app")
