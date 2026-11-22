plugins {
    `java-library`
}

configurations.named("testImplementation") { extendsFrom(configurations["testConfig"]) }

dependencies {
    api(libs.commons.codec)
    api(libs.commons.io)
    api(libs.commons.lang3)

    api(libs.xpp3)
    api(libs.xstream)
    api(libs.libidn)

    api(libs.bundles.log4j2)

    // TODO: use real release. This version is a customized SNAPSHOT
    api(files("libs/weupnp.jar"))
    // Workaround until we updated to a newer smack version
    api(files("libs/smack-3.4.1.jar"))
    api(files("libs/smackx-3.4.1.jar"))
    api(rootProject.files("libs/picocontainer-2.11.2-patched_relocated.jar"))
}

sourceSets {
    main {
        java.srcDirs("src", "patches")
        resources.srcDirs("resources")
    }
    test {
        java.srcDirs("test/junit")
    }
}

val testJar = tasks.register<Jar>("testJar") {
    archiveClassifier.set("tests")
    from(sourceSets["test"].output)
}
artifacts {
    add("testing", testJar)
}
