plugins {
    alias(libs.plugins.financemanage.kmp.library)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(libs.bignum)
            api(libs.kotlinx.datetime)
            api(libs.kotlinx.serialization.json)
        }
    }
}
