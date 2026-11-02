import com.android.build.api.dsl.LibraryExtension
import com.financemanage.mobile.buildlogic.androidNamespace
import com.financemanage.mobile.buildlogic.configureAndroidCommon
import com.financemanage.mobile.buildlogic.configureKotlinMultiplatform
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/** Kotlin Multiplatform library for Android + iOS (no Compose). */
class KmpLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.multiplatform")
            pluginManager.apply("com.android.library")

            extensions.configure<KotlinMultiplatformExtension> {
                configureKotlinMultiplatform(this)
            }
            extensions.configure<LibraryExtension> {
                namespace = androidNamespace
                configureAndroidCommon(this)
            }
        }
    }
}
