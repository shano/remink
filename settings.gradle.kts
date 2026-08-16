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
    }
}

rootProject.name = "Remink"
include(":app")

// MMD built from source — JFrog distribution is suspended, Maven Central not yet published.
// Clone: https://github.com/mudita/MMD alongside this repo at ../MMD
includeBuild("../MMD") {
    dependencySubstitution {
        substitute(module("com.mudita:MMD")).using(project(":mmd-core"))
    }
}
