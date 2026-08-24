import java.text.SimpleDateFormat
import java.util.Date
import java.util.Properties
import kotlin.apply

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val appVersionCode = 6
val appVersionName = "1.0.5"

val betaTags = providers.provider {
    providers.exec {
        commandLine("git", "tag", "-l", "v${appVersionName}-beta.*")
    }.standardOutput.asText.get().trim()
}
val betaNumber = betaTags.map { tags ->
    tags.lineSequence()
        .mapNotNull { it.substringAfter("-beta.").toIntOrNull() }
        .maxOrNull()
        ?.plus(1)
        ?: 1
}

android {
    namespace = "com.kangrio.byd.assistant"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.kangrio.byd.assistant"
        minSdk = 24
        targetSdk = 37
        versionCode = appVersionCode
        versionName = appVersionName
        ndk {
            abiFilters += listOf("arm64-v8a")
        }

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (file("signing.properties").exists()) {
            create("release") {
                val properties = Properties().apply {
                    file("signing.properties").inputStream().use { load(it) }
                }

                keyAlias = properties["KEY_ALIAS"] as String
                keyPassword = properties["KEY_PASSWORD"] as String
                storeFile = file(properties["STORE_FILE"] as String)
                storePassword = properties["KEY_PASSWORD"] as String
            }
        }
    }

    buildTypes {
        release {
            optimization {
                enable = true
                isMinifyEnabled = true
                isShrinkResources = true
            }
            signingConfig = if (file("signing.properties").exists()) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }

        register("beta") {
            initWith(getByName("release"))
            matchingFallbacks += listOf("release")
            versionNameSuffix = "-beta.${betaNumber.get()}"
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

androidComponents {
    onVariants { variant ->
        variant.outputs.forEach { output ->
            val date = SimpleDateFormat("ddMMyyyyHHmmss").format(Date())
            output.outputFileName.set(
                "${rootProject.name}-${output.versionName.get()}(${output.versionCode.get()})-${variant.name}-$date.apk"
            )
        }
    }
}

dependencies {
    implementation(libs.dadb)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.okhttp)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.accompanist.drawablepainter)
    implementation(project(":androidwakeword"))

    compileOnly(libs.onnxruntime.android)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.adaptive.navigation.suite)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}