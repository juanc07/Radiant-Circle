plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// Firebase Phase 2: keep local development friendly.
// The Google services plugin is applied only when the developer has placed
// app/google-services.json from Firebase Console into the app module.
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

android {
    namespace = "com.thinkblox.radiantrush"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.thinkblox.radiantrush"
        minSdk = 26
        targetSdk = 36
        versionCode = 3
        versionName = "0.3.0-phase3"
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/INDEX.LIST"
            excludes += "/META-INF/io.netty.versions.properties"
            excludes += "/META-INF/versions/9/previous-compilation-data.bin"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)

    // Phase 3 MWA connect only.
    // Exclude test/mock dependencies that older MWA KTX metadata can expose transitively.
    // Those libraries are not needed by the production app and can trigger noisy/failing
    // Android manifest/resource merges in newer Android Studio/AGP combinations.
    implementation(libs.solana.mwa.client) {
        exclude(group = "androidx.test")
        exclude(group = "junit")
        exclude(group = "org.mockito")
        exclude(group = "org.mockito.kotlin")
    }
    implementation(libs.solana.web3)
    implementation(libs.solana.rpc.core)
    implementation(libs.multimult)

    debugImplementation(libs.androidx.compose.ui.tooling)
}
