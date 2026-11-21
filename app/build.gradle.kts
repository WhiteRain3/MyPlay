plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// Car screen mockups for the README (tools/mockups.sh): Paparazzi draws them on the JVM. Only with
// -Pmockups, so ordinary builds and CI don't carry it; it doesn't support the configuration cache here.
val mockups = providers.gradleProperty("mockups").isPresent
// MyPlay (I5): the plugin is on the classpath only with -Pmockups, see the root build.gradle.kts.
if (mockups) apply(plugin = "app.cash.paparazzi")

android {
    namespace = "net.prototux.aare"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "io.github.whiterain3.myplay"
        // Android 8. Newer features need newer Android: car audio by screen capture and the
        // modern Wi-Fi join (10), Shizuku capture (12); older phones fall back or go without.
        minSdk = 26
        targetSdk = 37
        // The release workflow passes the tag's version (-PversionName=1.2.3 -PversionCode=10203).
        versionCode = providers.gradleProperty("versionCode").orNull?.toInt() ?: 10000
        versionName = providers.gradleProperty("versionName").orNull ?: "1.0.0"
    }

    signingConfigs {
        // Release key from the environment (the release workflow's secrets). Without it the
        // release APK is unsigned: use the debug one locally.
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
            signingConfig = signingConfigs.findByName("release")
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
        aidl = true
    }
    if (mockups) sourceSets["test"].kotlin.srcDir("src/mockups/java")
    testOptions {
        unitTests.isReturnDefaultValues = true
        // MyPlay (R4): Compose focus tests run on Robolectric, which needs the app's resources.
        unitTests.isIncludeAndroidResources = true
    }
    packaging {
        resources.excludes += setOf("META-INF/*.kotlin_module", "META-INF/versions/**")
    }
    lint {
        // a sideloaded app: no Play Store policy checks
        disable += setOf("ProtectedPermissions", "QueryAllPackagesPermission")
        disable += setOf("GradleDependency", "NewerVersionAvailable", "AndroidGradlePluginVersion")
    }
}

// AARE's built-in phone certificate, offered to head units after any imported one and before the
// runtime-generated one. Release builds get it from the AARE_PHONE_CERT / AARE_PHONE_KEY secrets
// (PEM certificate chain, PEM PKCS#8 key); other builds simply go without. It ships in the APK, so
// it isn't secret from users: it's a stable identity a head-unit vendor or CA could sign one day.
abstract class PhoneIdentityAssets : DefaultTask() {
    @get:Input @get:Optional abstract val cert: Property<String>
    @get:Input @get:Optional abstract val key: Property<String>
    @get:OutputDirectory abstract val outputDir: DirectoryProperty

    @TaskAction
    fun write() {
        val dir = outputDir.get().asFile.resolve("phone_identity")
        dir.deleteRecursively()
        val c = cert.orNull?.takeIf { it.isNotBlank() }
        val k = key.orNull?.takeIf { it.isNotBlank() }
        if (c == null || k == null) {
            if (c != null || k != null) logger.warn("AARE_PHONE_CERT and AARE_PHONE_KEY go together: no built-in certificate")
            return
        }
        dir.mkdirs()
        dir.resolve("cert.pem").writeText(c)
        dir.resolve("key.pem").writeText(k)
    }
}

val phoneIdentityAssets = tasks.register<PhoneIdentityAssets>("phoneIdentityAssets") {
    cert.set(providers.environmentVariable("AARE_PHONE_CERT"))
    key.set(providers.environmentVariable("AARE_PHONE_KEY"))
}

// MyPlay: the release notes (docs/releases/<version>.md) go into the app as assets
// (release_notes/<version>.md), so the phone can show "What's new" after an update without the network.
abstract class ReleaseNotesAssets : DefaultTask() {
    @get:InputFiles @get:PathSensitive(PathSensitivity.RELATIVE) abstract val notes: ConfigurableFileCollection
    @get:OutputDirectory abstract val outputDir: DirectoryProperty

    @TaskAction
    fun copy() {
        val dir = outputDir.get().asFile.resolve("release_notes")
        dir.deleteRecursively()
        dir.mkdirs()
        notes.files.filter { it.isFile && it.extension == "md" }.forEach { it.copyTo(dir.resolve(it.name)) }
    }
}

val releaseNotesAssets = tasks.register<ReleaseNotesAssets>("releaseNotesAssets") {
    notes.from(rootProject.fileTree("docs/releases") { include("*.md") })
}

androidComponents {
    onVariants { variant ->
        variant.sources.assets?.addGeneratedSourceDirectory(phoneIdentityAssets, PhoneIdentityAssets::outputDir)
        variant.sources.assets?.addGeneratedSourceDirectory(releaseNotesAssets, ReleaseNotesAssets::outputDir)
    }
}

dependencies {
    implementation(project(":aap"))
    implementation(project(":speedlimit")) // MyPlay (N20): the speed limit file and its matcher
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.car.app)
    implementation(libs.androidx.car.app.projected)
    implementation(libs.media3.session)
    implementation(libs.kotlinx.coroutines.android)
    // Car audio without the screen-capture consent: through Shizuku (adb rights), or Android's
    // audio-policy interface when installed as a privileged app (tools/magisk).
    implementation(libs.shizuku.api)
    implementation(libs.shizuku.provider)
    implementation(libs.hiddenapibypass)
    testImplementation(libs.junit)
    // vehicles.json is parsed with the platform's org.json, which unit tests only see as a stub.
    testImplementation(libs.org.json)
    // MyPlay (R4): D-pad and knob focus walks tested off-device (Compose on Robolectric).
    testImplementation(libs.robolectric)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
