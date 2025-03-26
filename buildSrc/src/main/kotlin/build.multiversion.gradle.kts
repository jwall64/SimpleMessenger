import buildlogic.Utils

plugins {
    id("java")
    id("java-library")
    id("dev.jwall64.gradle-multi-version")
    id("dev.jwall64.gradle-patch")
}

multiVersion {
    useSourceDirectorySets()
    defaultVersion(21)
    additionalVersions(17, 11, 8)

    getJarTask(8).archiveBaseName.set(Utils.getArchiveName(project, rootProject))
    getJarTask(11).archiveBaseName.set(Utils.getArchiveName(project, rootProject))
    getJarTask(17).archiveBaseName.set(Utils.getArchiveName(project, rootProject))
}

patch {
    patchSet("java17", sourceSets["main"], sourceSets["main"].java, multiVersion.getCompileTask(17))
    patchSet("java11", sourceSets["main"], sourceSets["main"].java, multiVersion.getCompileTask(11))
    patchSet("java8", sourceSets["main"], sourceSets["main"].java, multiVersion.getCompileTask(8))
}
