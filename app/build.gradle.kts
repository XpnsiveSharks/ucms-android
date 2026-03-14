import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { load(it) }
    }
}

android {
    namespace = "com.example.ucms_android"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.ucms_android"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "SUPABASE_ANON_KEY", "\"${localProperties.getProperty("SUPABASE_ANON_KEY", "")}\"")
        buildConfigField("String", "BACKEND_BASE_URL", "\"${localProperties.getProperty("BACKEND_BASE_URL", "http://10.0.2.2:8080/")}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true
    }
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.security.crypto)
    implementation("com.facebook.shimmer:shimmer:0.5.0")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
}

gradle.taskGraph.whenReady {
    if (allTasks.any { it.name.contains("Release") }) {
        val supabaseKey = localProperties.getProperty("SUPABASE_ANON_KEY", "")
        check(supabaseKey.isNotBlank()) {
            "SUPABASE_ANON_KEY must be set in local.properties for release builds"
        }
    }
}
