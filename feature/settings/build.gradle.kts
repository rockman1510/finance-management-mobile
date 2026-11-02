plugins {
    alias(libs.plugins.financemanage.kmp.feature)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.qrose)
        }
    }
}

compose.resources {
    packageOfResClass = "com.financemanage.mobile.feature.settings.resources"
}
