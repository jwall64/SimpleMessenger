import buildlogic.Utils;

plugins {
    id("java")
}

repositories {
    mavenCentral()
    maven("https://maven.jwall64.dev/releases")

    if(rootProject.version.toString().endsWith("-SNAPSHOT")) {
        maven("https://maven.jwall64.dev/snapshots")
    }
}

dependencies {

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
    withSourcesJar()
}

tasks.withType<Jar> {
    archiveBaseName.set(Utils.getArchiveName(project))
}

tasks.named<Test>("test") {
    useJUnitPlatform()
    workingDir("run/test")
    run {
        mkdir("run/test")
    }
}
