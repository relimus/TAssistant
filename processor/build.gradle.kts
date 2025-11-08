plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(project(":annotations"))
    implementation(libs.ksp.api)
}




