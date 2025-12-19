val gitCommitHash = getGitHash()

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.protobuf)
    alias(libs.plugins.ksp)
    id("org.lsposed.lsparanoid")
}

android {
    namespace = "re.limus.timas"
    compileSdk = 36

    defaultConfig {
        applicationId = "re.limus.timas"
        minSdk = 27
        targetSdk = 36
        versionCode = 1
        versionName = "1.1.3.$gitCommitHash"

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

fun getGitHash(): String {
    return try {
        val process = ProcessBuilder("git", "rev-parse", "--short", "HEAD").start()
        process.inputStream.bufferedReader().use { it.readText().trim() }
    } catch (_: Exception) {
        "unknown"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    // Xposed
    compileOnly(libs.xposed.api)
    implementation(libs.xphelper)

    // Json
    implementation(libs.fastjson2)

    // ProtoBuf
    implementation(libs.protobuf.java.lite)

    // Annotations
    implementation(project(":annotations"))

    // KSP
    ksp(project(":processor"))
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.25.8"
    }
    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                create("java") {
                    option("lite")
                }
            }
        }
    }
}
lsparanoid {
    seed = 1209
    classFilter = { true }
    includeDependencies = false
    variantFilter = { true }
}