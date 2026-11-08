plugins {
    alias(libs.plugins.financemanage.kmp.library)
    alias(libs.plugins.kotlin.serialization)
}

// Domain layer shared by every feature: models, repository interfaces and use cases.
// Pure Kotlin: no Ktor, Room, DataStore or Compose. `core:data` implements the interfaces.
kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.model)
            api(libs.kotlinx.coroutines.core)
        }
    }
}
