plugins {
    alias(libs.plugins.financemanage.kmp.compose)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.model)
            api(projects.core.data)
            implementation(libs.androidx.lifecycle.runtime.compose)
        }
    }
}

compose.resources {
    publicResClass = true
    packageOfResClass = "com.financemanage.mobile.core.ui.resources"
}
