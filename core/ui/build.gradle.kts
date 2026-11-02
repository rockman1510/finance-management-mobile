plugins {
    alias(libs.plugins.financemanage.kmp.compose)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.model)
        }
    }
}

compose.resources {
    publicResClass = true
    packageOfResClass = "com.financemanage.mobile.core.ui.resources"
}
