rootProject.name = "FinanceManage"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

include(":composeApp")
include(":core:model")
include(":core:network")
include(":core:data")
include(":core:ui")
include(":core:testing")
include(":core:navigation")
include(":feature:auth")
include(":feature:onboarding")
include(":feature:settings")
include(":feature:wallet")
include(":feature:category")
include(":feature:transaction")
include(":feature:currency")
include(":feature:budget")
include(":feature:bill")
include(":feature:saving")
