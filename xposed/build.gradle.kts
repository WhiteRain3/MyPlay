// A standalone libxposed module: a separate APK, installed and enabled in an Xposed manager
// (LSPosed/Vector) on a rooted phone. It is not part of the AARE app and ships nothing itself —
// see xposed/README.md and docs/xposed.md. Its hooks are compatibility shims for AARE only.
//
// MyPlay: this is "MyPlay host shims". The MyPlay release workflow builds it, signs it with the
// same key as the app and attaches it to the release as MyPlay-hostshims-<version>.apk.
plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "net.prototux.aare.xposed"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "io.github.whiterain3.myplay.hostshims"
        minSdk = 26
        targetSdk = 37
        // The same version as the MyPlay release it ships with (the release workflow passes it).
        versionCode = providers.gradleProperty("versionCode").orNull?.toInt() ?: 1
        versionName = providers.gradleProperty("versionName").orNull ?: "0.0.0"
    }

    signingConfigs {
        // The release key, from the same environment variables as the app (app/build.gradle.kts).
        // Without them the release APK is unsigned; the release workflow refuses to publish that.
        val keystore = providers.environmentVariable("AARE_KEYSTORE").orNull
        if (keystore != null) {
            create("release") {
                storeFile = file(keystore)
                storePassword = providers.environmentVariable("AARE_KEYSTORE_PASSWORD").orNull
                keyAlias = providers.environmentVariable("AARE_KEY_ALIAS").orNull
                keyPassword = providers.environmentVariable("AARE_KEY_PASSWORD").orNull
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release")
        }
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    // The framework provides the API at runtime; compile against it, never bundle it.
    // MyPlay: version 101, not the catalog's 102. The owner's framework is known to load a module
    // built against 101 (their own, in WhiteRain3/AA-Experiments); whether it loads 102 is not known.
    compileOnly("io.github.libxposed:api:101.0.1")

    // For the test that the class and method the hook names exist in the car library the app is built with.
    testImplementation(libs.junit)
    testImplementation("io.github.libxposed:api:101.0.1")
    testImplementation(libs.androidx.car.app)
}
