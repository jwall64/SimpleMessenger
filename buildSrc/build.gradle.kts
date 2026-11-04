plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    maven("https://maven.jwall64.dev/plugins")
    maven {
        name = "Fabric"
        url = uri("https://maven.fabricmc.net/")
    }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.3.21")
    implementation("dev.jwall64:gradle-multi-version:0.4.0")
    implementation("dev.jwall64:gradle-patch:0.3.0")
    implementation("net.fabricmc:fabric-loom:1.19-SNAPSHOT")
}
