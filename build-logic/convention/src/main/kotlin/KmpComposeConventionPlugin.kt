import com.financemanage.mobile.buildlogic.library
import com.financemanage.mobile.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/** KMP library that also uses Compose Multiplatform (UI modules). */
class KmpComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("financemanage.kmp.library")
            pluginManager.apply("org.jetbrains.compose")
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

            extensions.configure<KotlinMultiplatformExtension> {
                sourceSets.getByName("commonMain").dependencies {
                    implementation(libs.library("compose-runtime"))
                    implementation(libs.library("compose-foundation"))
                    implementation(libs.library("compose-material3"))
                    implementation(libs.library("compose-material-icons-extended"))
                    implementation(libs.library("compose-ui"))
                    implementation(libs.library("compose-ui-tooling-preview"))
                    implementation(libs.library("compose-components-resources"))
                }
            }
        }
    }
}
