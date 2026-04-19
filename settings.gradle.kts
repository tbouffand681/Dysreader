pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // iText7
        maven { url = uri("https://repo.itextsupport.com/android") }
    }
    // Gradle 8+ charge automatiquement gradle/libs.versions.toml comme catalog "libs"
    // Ne pas redéclarer versionCatalogs ici → évite "too_many_import_invocation"
}

rootProject.name = "DyslexiRead"
include(":app")
