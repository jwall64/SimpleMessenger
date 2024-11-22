plugins {
    id("build.common")
    id("build.library")
    id("build.multiversion")
}

dependencies {

    compileOnlyApi(libs.jetbrains.annotations)

    api(project(":api"))
    api(project(":base"))

    api(libs.midnight.config)
    api(libs.midnight.lib)

    api(libs.rabbitmq.client)

}
