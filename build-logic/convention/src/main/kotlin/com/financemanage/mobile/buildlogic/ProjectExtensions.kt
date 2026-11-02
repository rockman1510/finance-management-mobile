package com.financemanage.mobile.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun VersionCatalog.library(alias: String): Provider<MinimalExternalModuleDependency> =
    findLibrary(alias).orElseThrow { IllegalArgumentException("Unknown library alias '$alias' in libs.versions.toml") }

internal fun VersionCatalog.versionInt(alias: String): Int = findVersion(alias).get().requiredVersion.toInt()

/** `:core:model` -> `com.financemanage.mobile.core.model` */
internal val Project.androidNamespace: String
    get() = "com.financemanage.mobile" + path.replace(':', '.').replace('-', '_')

/** Android + iOS targets shared by every module. */
internal fun Project.configureKotlinMultiplatform(extension: KotlinMultiplatformExtension) {
    extension.apply {
        androidTarget {
            compilerOptions {
                jvmTarget.set(JvmTarget.JVM_17)
            }
        }
        iosX64()
        iosArm64()
        iosSimulatorArm64()

        compilerOptions {
            freeCompilerArgs.add("-Xexpect-actual-classes")
            optIn.add("kotlin.uuid.ExperimentalUuidApi")
        }

        sourceSets.getByName("commonTest").dependencies {
            implementation(libs.library("kotlin-test"))
            implementation(libs.library("kotlinx-coroutines-test"))
        }
    }
}

internal fun Project.configureAndroidCommon(extension: CommonExtension<*, *, *, *, *, *>) {
    extension.apply {
        compileSdk = libs.versionInt("android-compileSdk")
        defaultConfig {
            minSdk = libs.versionInt("android-minSdk")
            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }
        testOptions {
            unitTests.isReturnDefaultValues = true
        }
    }
}
