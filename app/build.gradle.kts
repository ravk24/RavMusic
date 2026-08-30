import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    // Kotlin itself is provided by AGP 9's built-in Kotlin support; only compiler plugins are applied here.
    alias(libs.plugins.compose.compiler)
    // Required by Navigation 3: NavKey routes must be @Serializable for rememberNavBackStack.
    alias(libs.plugins.kotlin.serialization)
    // Room annotation processing via KSP (kapt is incompatible with built-in Kotlin).
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

/**
 * Release signing (Phase 8, design D1): `keystore.properties` at the repo root (git-ignored), or the
 * same four names as environment variables. Null when the store file is not configured.
 */
data class ReleaseSigning(val storeFile: String, val storePassword: String, val keyAlias: String, val keyPassword: String)

val releaseSigning: ReleaseSigning? = run {
    val props = Properties()
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { props.load(it) }
    fun prop(name: String): String? = props.getProperty(name)?.takeIf { it.isNotBlank() } ?: System.getenv(name)?.takeIf { it.isNotBlank() }
    val store = prop("RAVMUSIC_STORE_FILE") ?: return@run null
    ReleaseSigning(
        storeFile = store,
        storePassword = prop("RAVMUSIC_STORE_PASSWORD") ?: error("RAVMUSIC_STORE_PASSWORD is missing"),
        keyAlias = prop("RAVMUSIC_KEY_ALIAS") ?: error("RAVMUSIC_KEY_ALIAS is missing"),
        keyPassword = prop("RAVMUSIC_KEY_PASSWORD") ?: error("RAVMUSIC_KEY_PASSWORD is missing"),
    )
}

android {
    namespace = "com.ravk24.ravmusic"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.ravk24.ravmusic"
        minSdk = 26
        targetSdk = 37
        versionCode = 4
        versionName = "1.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        // Signed only where the secrets exist (this machine); elsewhere the release APK is unsigned.
        if (releaseSigning != null) {
            create("release") {
                storeFile = file(releaseSigning.storeFile)
                storePassword = releaseSigning.storePassword
                keyAlias = releaseSigning.keyAlias
                keyPassword = releaseSigning.keyPassword
            }
        }
    }

    buildTypes {
        release {
            // R8 code shrinking + resource shrinking; project keep rules live in src/main/keepRules/.
            optimization {
                enable = true
            }
            signingConfig = signingConfigs.findByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

room {
    // Versioned schema JSON is committed so later migrations can be written and tested.
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.room.testing)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
