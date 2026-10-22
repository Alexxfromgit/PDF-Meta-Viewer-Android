plugins { alias(libs.plugins.android.application) }
android {
    namespace = "com.example.pdfmetaviewerandroid"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.example.pdfmetaviewerandroid"
        minSdk = 28
        targetSdk = 34
        versionCode = 2
        versionName = "1.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { viewBinding = true }
    packaging { resources.excludes += setOf("META-INF/DEPENDENCIES", "META-INF/LICENSE*", "META-INF/NOTICE*") }
    testOptions { unitTests.isIncludeAndroidResources = true }
}
dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.recyclerview)
    implementation(libs.lifecycle.livedata)
    implementation(libs.lifecycle.viewmodel)
    // Retained for the existing, unused prototype fragments.
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.pdfbox.android)
    implementation(libs.jackson.databind)
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.espresso.core)
}
