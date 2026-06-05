plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "smi"

include("api")
include("base")
include("impl-amqp")
include("platform-minecraft")
