plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
}

android {
    namespace = "io.live.timas"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.live.timas"
        minSdk = 27
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        ndk {
            abiFilters.add("arm64-v8a")
        }
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
    packaging {
        resources {
            excludes.addAll(
                arrayOf(
                    "kotlin/**",
                    "META-INF/**",
                    "schema/**",
                    "**.bin",
                    "kotlin-tooling-metadata.json"
                )
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        viewBinding = true
    }
    androidResources {
        additionalParameters += arrayOf(
            "--allow-reserved-package-id",
            "--package-id", "0xf2"
        )
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    // Xposed
    compileOnly(libs.xposed.api)
    implementation(libs.xphelper)
    
    // Annotations
    implementation(project(":annotations"))

    // KSP
    ksp(project(":processor"))
}