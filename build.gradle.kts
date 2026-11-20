// MyPlay (I5): the Paparazzi plugin only draws the README's car screen mockups (tools/mockups.sh,
// with -Pmockups). Declared with the other plugins, every build had to download it, and a release
// run once failed because Maven did not deliver it. Now it is put on the build's classpath only
// when -Pmockups is given on the command line; app/build.gradle.kts applies it in the same case.
buildscript {
    if (gradle.startParameter.projectProperties.containsKey("mockups")) {
        repositories {
            mavenCentral()
            gradlePluginPortal()
        }
        dependencies {
            classpath("app.cash.paparazzi:paparazzi-gradle-plugin:${libs.versions.paparazzi.get()}")
        }
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.protobuf) apply false
}
