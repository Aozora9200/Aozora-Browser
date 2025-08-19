plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.aozora.aozora"
    compileSdk = 35

    buildToolsVersion ("35.0.0") // Older versions may give compile errors

    defaultConfig {
        applicationId = "com.aozora.aozora"
        minSdk = 19
        targetSdk = 35
        versionCode = 1
        versionName = "2.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {

    implementation ("com.android.support:appcompat-v7:28.0.0")
    implementation ("com.android.support:design:28.0.0")
    implementation ("com.android.support:support-v4:28.0.0")
    implementation ("com.android.support:recyclerview-v7:28.0.0")
    implementation ("com.android.support:cardview-v7:28.0.0")
    implementation ("com.google.zxing:core:3.3.0")
    implementation ("com.journeyapps:zxing-android-embedded:3.3.0")
    implementation ("com.android.support:preference-v7:28.0.0")
    implementation ("frankiesardo:icepick:3.2.0")
    annotationProcessor ("frankiesardo:icepick-processor:3.2.0")

}