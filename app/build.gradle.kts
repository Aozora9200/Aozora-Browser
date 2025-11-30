plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.android.gms.oss-licenses-plugin")
}


android {
    namespace = "com.aozora.aozora"
    compileSdk = 34
    buildToolsVersion = "34.0.0"

    defaultConfig {
        applicationId = "com.aozora.aozora"
        minSdk = 23
        targetSdk = 34
        versionCode = 3
        versionName = "3.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
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
    implementation("androidx.appcompat:appcompat:1.3.1")
    implementation("com.google.android.material:material:1.4.0")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("androidx.recyclerview:recyclerview:1.2.1")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.viewpager:viewpager:1.1.0")
    implementation("com.google.zxing:core:3.3.0")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation("androidx.preference:preference:1.1.1")
    implementation ("androidx.webkit:webkit:1.14.0")
    implementation ("androidx.startup:startup-runtime:1.2.0")
    implementation ("androidx.emoji2:emoji2:1.5.0")
    implementation ("androidx.profileinstaller:profileinstaller:1.4.1")
    implementation ("net.lingala.zip4j:zip4j:2.11.5")
    implementation ("com.airbnb.android:lottie:6.0.0")
    implementation(libs.dagger)
    implementation(libs.dagger.compiler)
    implementation(libs.hilt.core)
    implementation(files("libs\\arity-2.1.2.jar"))


}
