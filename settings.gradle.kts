pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        maven("https://jitpack.io")
        maven("https://maven.google.com")
        maven("https://repo1.maven.org/maven2/")
        mavenCentral()
        gradlePluginPortal()
        jcenter() // ← opcional, solo si usás librerías viejas
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        maven("https://jitpack.io")
        maven("https://maven.google.com")
        maven("https://repo1.maven.org/maven2/")
        mavenCentral()
        jcenter() // ← opcional
    }
}

rootProject.name = "TecReciclaje"
include(":app")
