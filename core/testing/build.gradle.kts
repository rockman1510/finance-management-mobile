plugins {
    alias(libs.plugins.financemanage.kmp.library)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.model)
            api(projects.core.network)
            api(projects.core.data)
            api(libs.ktor.client.mock)
            api(libs.kotlinx.coroutines.test)
            implementation(libs.androidx.lifecycle.viewmodel)
        }
    }
}
