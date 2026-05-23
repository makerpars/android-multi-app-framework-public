pluginManagement {
    repositories {
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
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "ContentApp"
include(":app")
include(":core:common")
include(":core:model")
include(":core:designsystem")
include(":core:datastore")
include(":core:database")
include(":core:firebase")
include(":core:auth")
include(":feature:content")
include(":feature:audio")
include(":feature:ads")
include(":feature:billing")
include(":feature:auth")
include(":feature:notifications")
include(":feature:messages")
include(":feature:settings")
include(":feature:otherapps")
include(":feature:prayertimes")
include(":feature:qibla")
include(":feature:counter")
include(":feature:quran")
