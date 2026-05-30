import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.variant.impl.VariantOutputImpl
import java.io.ByteArrayOutputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.protobuf)
    alias(libs.plugins.ksp)
    id("org.lsposed.lsparanoid")
}
val appVersionName = "v1.2.2"
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.isFile) {
        file.inputStream().use { load(it) }
    }
}
val keystorePath: String? = localProperties.getProperty("KEYSTORE_PATH")

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

val gitCommitCount = providers.of(GitCommitCount::class.java) {}
val gitShortHash = providers.of(GitShortHash::class.java) {}
val debugVersionNameSuffix = providers.provider {
    getGitHeadRefsSuffix(rootProject, "debug")
}
val releaseVersionNameSuffix = providers.provider {
    getGitHeadRefsSuffix(rootProject, "release")
}

extensions.configure<ApplicationExtension> {
    namespace = "re.limus.timas"
    compileSdk = 36

    defaultConfig {
        applicationId = "re.limus.timas"
        minSdk = 27
        targetSdk = 37
        versionCode = providers.provider { getBuildVersionCode(rootProject) }.get()
        versionName = appVersionName

        ndk {
            abiFilters.add("arm64-v8a")
        }
    }

    signingConfigs {
        create("release") {
            if (!keystorePath.isNullOrBlank()) {
                storeFile = file(keystorePath)
                storePassword = localProperties.getProperty("KEYSTORE_PASSWORD")
                keyAlias = localProperties.getProperty("KEY_ALIAS")
                keyPassword = localProperties.getProperty("KEY_PASSWORD")
                enableV2Signing = true
                enableV3Signing = true
            }
        }

        getByName("debug") {
            if (!keystorePath.isNullOrBlank()) {
                storeFile = file(keystorePath)
                storePassword = localProperties.getProperty("KEYSTORE_PASSWORD")
                keyAlias = localProperties.getProperty("KEY_ALIAS")
                keyPassword = localProperties.getProperty("KEY_PASSWORD")
                enableV2Signing = true
                enableV3Signing = true
            }
        }
    }

    buildTypes {
        debug {
            versionNameSuffix = debugVersionNameSuffix.get()
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            optimization {
                enable = true
            }
            versionNameSuffix = releaseVersionNameSuffix.get()
            signingConfig = signingConfigs.getByName("release")
        }
    }
    packaging {
        resources {
            excludes.addAll(
                arrayOf(
                    "kotlin/**",
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

androidComponents {
    onVariants { variant ->
        variant.outputs.forEach { output ->
            if (output is VariantOutputImpl) {
                val versionNameSuffix = when (variant.buildType) {
                    "debug" -> debugVersionNameSuffix.get()
                    "release" -> releaseVersionNameSuffix.get()
                    else -> ""
                }
                val newApkName = "${rootProject.name}-${appVersionName}$versionNameSuffix.apk"
                output.outputFileName = newApkName
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