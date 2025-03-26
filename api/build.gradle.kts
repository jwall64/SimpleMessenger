plugins {
    id("build.common")
    id("build.library")
    id("build.publish")
    id("build.multiversion")
}

dependencies {
    compileOnlyApi(libs.jetbrains.annotations)
}
