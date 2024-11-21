plugins {
    id("build.common")
    id("build.library")
}

dependencies {

    api(project(":api"))

    api(libs.midnight.config)
    api(libs.midnight.lib)

    api(libs.rabbitmq.client)

}
