import buildlogic.Utils

plugins {
    id("build.library")
    id("build.fabric")
    id("build.publish")
}

Utils.setupResources(project, rootProject, "fabric.mod.json")

dependencies {

    minecraft("com.mojang:minecraft:1.21.10")
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:0.18.1")

    api(project(":api"))
    implementation(project(":base"))
    implementation(project(":impl-amqp"))

    shadow(project(":api")) { isTransitive = false }
    shadow(project(":base")) { isTransitive = false }
    shadow(project(":impl-amqp")) { isTransitive = false }

    include(libs.rabbitmq.client) { isTransitive = false }
    include(libs.midnight.lib) { isTransitive = false }

    // Fabric API
    val apiModules = listOf(
        "fabric-api-base",
        "fabric-lifecycle-events-v1"
    )
    for(mod in apiModules) {
        modApi(fabricApi.module(mod, "0.138.3+1.21.10"))
    }

    modImplementation(libs.midnight.config.mc)


    compileOnly(libs.jetbrains.annotations)
    implementation(libs.slf4j.api)

    testRuntimeOnly(libs.slf4j.simple)
}
