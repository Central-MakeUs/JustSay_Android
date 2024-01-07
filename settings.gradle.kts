pluginManagement {
    includeBuild("build-logic")
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

rootProject.name = "justsayit"
include(":app")
include(":authentication")
include(":authentication:authentication-data")
include(":authentication:authentication-domain")
include(":authentication:authentication-presentation")
