// MyPlay (plan N19, N20): the offline speed limit file. Its format, the builder the CI job runs on
// an OpenStreetMap extract, and the reader and matcher the car side uses. Plain JVM, no Android.
plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
kotlin {
    compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
}

application {
    mainClass.set("net.prototux.aare.speedlimit.BuildKt")
    applicationName = "myplay-speedlimit"
    applicationDefaultJvmArgs = listOf("-Xmx3g")
}

dependencies {
    testImplementation(libs.junit)
}
