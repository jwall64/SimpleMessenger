import buildlogic.Utils

plugins {
    id("build.common")
    id("dev.jwall64.gradle-multi-version")
    id("dev.jwall64.gradle-patch")
}

multiVersion {
    defaultVersion(21)
    additionalVersions(17, 8)
}

patch {
    patchSet("java17", sourceSets["main"], sourceSets["main"].java, multiVersion.getCompileTask(17))
    patchSet("java8", sourceSets["main"], sourceSets["main"].java, multiVersion.getCompileTask(8))
}

multiVersion.getJarTask(8).archiveBaseName.set(Utils.getArchiveName(project, rootProject))
multiVersion.getJarTask(17).archiveBaseName.set(Utils.getArchiveName(project, rootProject))