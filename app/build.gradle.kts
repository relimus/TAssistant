import java.io.ByteArrayOutputStream
import javax.inject.Inject
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.gradle.process.ExecOperations
import org.gradle.api.Project
import java.io.File

abstract class GitCommitCount : ValueSource<Int, ValueSourceParameters.None> {
    @get:Inject abstract val execOperations: ExecOperations

    override fun obtain(): Int {
        val output = ByteArrayOutputStream()
        execOperations.exec {
            commandLine("git", "rev-list", "--count", "HEAD")
            standardOutput = output
        }
        return output.toString().trim().toInt()
    }
}

abstract class GitShortHash : ValueSource<String, ValueSourceParameters.None> {
    @get:Inject abstract val execOperations: ExecOperations

    override fun obtain(): String {
        val output = ByteArrayOutputStream()
        execOperations.exec {
            commandLine("git", "rev-parse", "--short=7", "HEAD")
            standardOutput = output
        }
        return output.toString().trim()
    }
}

val gitCommitCount = providers.of(GitCommitCount::class.java) {}!!
val gitShortHash = providers.of(GitShortHash::class.java) {}!!

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
        versionCode = providers.provider { getBuildVersionCode(rootProject) }.get()
        versionName = "v1.2.1"

        ndk {
            abiFilters.add("arm64-v8a")
        }
    }

    buildTypes {
        debug {
            versionNameSuffix = providers.provider {
                getGitHeadRefsSuffix(rootProject, "debug")
            }.get()
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            versionNameSuffix = providers.provider {
                getGitHeadRefsSuffix(rootProject, "release")
            }.get()
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

    applicationVariants.all {
        outputs.all {
            val output = this as com.android.build.gradle.internal.api.BaseVariantOutputImpl
            output.outputFileName?.let { fileName ->
                if (fileName.endsWith(".apk")) {
                    val projectName = rootProject.name
                    val currentVersionName = versionName
                    output.outputFileName = "${projectName}-${currentVersionName}.apk"
                }
            }
        }
    }
}

fun getGitHeadRefsSuffix(project: Project, buildType: String): String {
    val rootProject = project.rootProject
    val projectDir = rootProject.projectDir
    val headFile = File(projectDir, ".git" + File.separator + "HEAD")
    return if (headFile.exists()) {
        try {
            val commitCount = gitCommitCount.get()
            val hash = gitShortHash.get()
            val prefix = if (buildType == "debug") ".d" else ".r"
            "$prefix$commitCount.$hash"
        } catch (e: Exception) {
            println("Failed to get git info: ${e.message}")
            ".standalone"
        }
    } else {
        println("Git HEAD file not found")
        ".standalone"
    }
}

fun getBuildVersionCode(project: Project): Int {
    val rootProject = project.rootProject
    val projectDir = rootProject.projectDir
    val headFile = File(projectDir, ".git" + File.separator + "HEAD")
    return if (headFile.exists()) {
        try {
            gitCommitCount.get()
        } catch (e: Exception) {
            println("Failed to get git commit count: ${e.message}")
            1
        }
    } else {
        println("Git HEAD file not found")
        1
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    // okhttp3
    implementation(libs.okhttp3)

    // Xposed
    compileOnly(libs.xposed.api)
    implementation(libs.xphelper)

    // ByteBuddy
    implementation(libs.byte.buddy.android)

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
        artifact = "com.google.protobuf:protoc:4.33.5"
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
    variantFilter = { variant ->
        if (variant.buildType == "release") {
            seed = 1209
            classFilter = { true }
            includeDependencies = false
            true
        } else {
            false
        }
    }
}
