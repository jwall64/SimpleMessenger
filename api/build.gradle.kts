plugins {
    id("build.common")
    id("build.library")
    id("build.multiversion")
}

dependencies {

    api(libs.midnight.config)
    api(libs.midnight.lib)

}
