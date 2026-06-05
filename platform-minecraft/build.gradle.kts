plugins {
    id("build.library")
    id("build.fabric")
    id("build.publish")
}


dependencies {

    minecraft("com.mojang:minecraft:26.1.2")
    implementation("net.fabricmc:fabric-loader:0.19.2")

    api(project(":api"))
    implementation(project(":base"))
    implementation(project(":impl-amqp"))

    include(project(":api")) { isTransitive = false }
    include(project(":base")) { isTransitive = false }
    include(project(":impl-amqp")) { isTransitive = false }

    include(libs.rabbitmq.client) { isTransitive = false }
    include(libs.midnight.lib) { isTransitive = false }

    // Fabric API
    val apiModules = listOf(
        "fabric-api-base",
        "fabric-lifecycle-events-v1"
    )
    for(mod in apiModules) {
        api(fabricApi.module(mod, "0.150.0+26.1.2"))
    }

    implementation(libs.midnight.config.mc)


    compileOnly(libs.jetbrains.annotations)
    implementation(libs.slf4j.api)

    testRuntimeOnly(libs.slf4j.simple)

    // Gametest API modules
    val testApiModules = listOf(
        "fabric-gametest-api-v1",
        "fabric-registry-sync-v0"
    )
    for(mod in testApiModules) {
        gametestImplementation(fabricApi.module(mod, "0.150.0+26.1.2"))
    }
}
