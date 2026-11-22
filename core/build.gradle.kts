plugins {
    `java-library`
}

val log4j2ApiVersion = ext.get("log4j2ApiVersion") as String
val log4j2CoreVersion = ext.get("log4j2CoreVersion") as String
val log4j2BridgeVersion = ext.get("log4j2BridgeVersion") as String

configurations.named("testImplementation") { extendsFrom(configurations["testConfig"]) }

dependencies {
    api("commons-codec:commons-codec:1.23.0")
    api("commons-io:commons-io:2.22.0")
    api("org.apache.commons:commons-lang3:3.22.0")

    api("xpp3:xpp3:1.1.4c")
    api("com.thoughtworks.xstream:xstream:1.4.21")
    api("org.gnu.inet:libidn:1.15")

    api(log4j2ApiVersion)
    api(log4j2CoreVersion)
    api(log4j2BridgeVersion)

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

tasks {
    val testJar by registering(Jar::class) {
        archiveClassifier.set("tests")
        from(sourceSets["test"].output)
    }
    artifacts {
        add("testing", testJar)
    }
}
