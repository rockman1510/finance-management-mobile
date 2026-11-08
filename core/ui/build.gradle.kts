plugins {
    alias(libs.plugins.financemanage.kmp.compose)
}

kotlin {
    sourceSets {
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.biometric)
            implementation(libs.androidx.fragment)
        }
        commonMain.dependencies {
            api(projects.core.model)
            api(projects.core.domain)
            implementation(libs.androidx.lifecycle.runtime.compose)
        }
    }
}

compose.resources {
    publicResClass = true
    packageOfResClass = "com.financemanage.mobile.core.ui.resources"
}
