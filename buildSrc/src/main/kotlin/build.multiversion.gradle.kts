import buildlogic.Utils

plugins {
    id("java")
    id("java-library")
    id("dev.jwall64.gradle-multi-version")
    id("dev.jwall64.gradle-patch")
}

multiVersion {
    useSourceDirectorySets()
    defaultVersion(25)
    additionalVersions(21, 17)

    getJarTask(17).archiveBaseName.set(Utils.getArchiveName(project))
    getJarTask(21).archiveBaseName.set(Utils.getArchiveName(project))
}

patch {
    patchSet("java21", sourceSets["main"], sourceSets["main"].java, multiVersion.getCompileTask(21))
    patchSet("java17", sourceSets["main"], sourceSets["main"].java, multiVersion.getCompileTask(17))
}
