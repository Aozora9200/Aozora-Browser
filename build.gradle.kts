plugins {
    alias(libs.plugins.android.application) apply false
}

buildscript {
    repositories {
        maven {
            url = uri("https://plugins.gradle.org/m2/")
        }
    }

    dependencies {
        classpath("gradle.plugin.com.cookpad.android.plugin:plugin:1.2.8")
    }
}

apply(plugin = "com.cookpad.android.plugin.license-tools")