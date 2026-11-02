plugins {
    alias(libs.plugins.financemanage.kmp.feature)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.filekit.compose)
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
        }
    }
}

compose.resources {
    packageOfResClass = "com.financemanage.mobile.feature.transaction.resources"
}
