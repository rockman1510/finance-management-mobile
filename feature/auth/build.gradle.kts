plugins {
    alias(libs.plugins.financemanage.kmp.feature)
}

kotlin {
    sourceSets {
        androidMain.dependencies {
            implementation(libs.androidx.credentials)
            implementation(libs.androidx.credentials.play.services)
            implementation(libs.googleid)
        }
    }
}

compose.resources {
    packageOfResClass = "com.financemanage.mobile.feature.auth.resources"
}
