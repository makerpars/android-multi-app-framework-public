import java.util.Properties

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

val localProperties =
    Properties().apply {
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localPropertiesFile.inputStream().use { load(it) }
        }
    }

android {
    namespace = "com.parsfilo.contentapp.core.auth"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
        resValue("string", "web_client_id", localProperties.getProperty("WEB_CLIENT_ID", ""))
    }

    buildFeatures {
        resValues = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(libs.timber)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(project(":core:firebase"))

    api(platform(libs.firebase.bom))
    api(libs.firebase.auth) {
        exclude(group = "androidx.credentials", module = "credentials")
        exclude(group = "androidx.credentials", module = "credentials-play-services-auth")
        exclude(group = "com.google.android.gms", module = "play-services-identity-credentials")
    }

    api(libs.androidx.credentials)
    api(libs.androidx.credentials.play.services.auth) {
        exclude(group = "com.google.android.gms", module = "play-services-identity-credentials")
    }
    api(libs.googleid)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}
