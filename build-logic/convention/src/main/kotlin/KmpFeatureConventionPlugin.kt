import com.financemanage.mobile.buildlogic.library
import com.financemanage.mobile.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * A feature module (`:feature:xxx`) holding its data / domain / presentation layers.
 * Features depend on the core modules only – never on another feature.
 */
class KmpFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("financemanage.kmp.compose")
            pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")

            extensions.configure<KotlinMultiplatformExtension> {
                sourceSets.getByName("commonMain").dependencies {
                    implementation(project(":core:model"))
                    implementation(project(":core:network"))
                    implementation(project(":core:data"))
                    implementation(project(":core:ui"))
                    implementation(project(":core:navigation"))
                    implementation(libs.library("koin-core"))
                    implementation(libs.library("koin-compose"))
                    implementation(libs.library("koin-compose-viewmodel"))
                    implementation(libs.library("androidx-lifecycle-viewmodel-compose"))
                    implementation(libs.library("androidx-lifecycle-runtime-compose"))
                    implementation(libs.library("androidx-navigation-compose"))
                    implementation(libs.library("kotlinx-serialization-json"))
                    implementation(libs.library("kotlinx-datetime"))
                    implementation(libs.library("ktor-client-core"))
                    implementation(libs.library("bignum"))
                }
                sourceSets.getByName("commonTest").dependencies {
                    implementation(project(":core:testing"))
                    implementation(libs.library("ktor-client-mock"))
                    implementation(libs.library("koin-test"))
                }
            }
        }
    }
}
