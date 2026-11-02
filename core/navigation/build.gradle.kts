plugins {
    alias(libs.plugins.financemanage.kmp.library)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(libs.kotlinx.serialization.json)
            api(libs.androidx.navigation.compose)
        }
    }
}
