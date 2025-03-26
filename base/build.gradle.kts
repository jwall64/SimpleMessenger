plugins {
    id("build.common")
    id("build.library")
    id("build.multiversion")
}

dependencies {

    compileOnlyApi(libs.jetbrains.annotations)

    api(project(":api"))
    api(libs.midnight.config)

}
