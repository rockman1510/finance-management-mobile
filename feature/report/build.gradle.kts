plugins {
    alias(libs.plugins.financemanage.kmp.feature)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.filekit.compose)
        }
    }
}

compose.resources {
    packageOfResClass = "com.financemanage.mobile.feature.report.resources"
}
