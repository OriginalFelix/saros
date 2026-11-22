plugins {
    idea
}

idea {
    module {
        excludeDirs.addAll(mutableListOf(file("docs/.jekyll-cache"), file("docs/_site"), file("docs/node_modules")))
    }
}

subprojects {
    apply(plugin = "idea")
    apply(plugin = "java")

    idea {
        module {
            excludeDirs.addAll(mutableListOf(file("bin"), file("lib"), file("libs")))
        }
    }

    repositories {
        mavenCentral()
    }

    // IntelliJ 2026.x runs on Java 25 and accepts Java 21 bytecode
    extensions.configure<JavaPluginExtension> {
        toolchain.languageVersion.set(JavaLanguageVersion.of(25))
    }
    tasks.withType<JavaCompile> {
        options.release.set(21)
        options.encoding = "UTF-8"
    }

    configurations {
        create("testing") // used to reference the testJar
        create("testConfig") // contains test dependencies that are used by all java subprojects
    }

    tasks {
        withType<Javadoc> {
            isFailOnError = false
        }

        withType<Test> {
            // Disable jmx in the test environment to avoid issues with easymock
            systemProperty("log4j2.disableJmx", "true")
            // PowerMock and XStream reflect into JDK internals; IntelliJ itself starts with these opens
            jvmArgs(listOf("java.lang", "java.util", "java.util.concurrent", "java.util.concurrent.locks", "java.util.stream", "java.io", "java.lang.reflect", "java.net", "java.text").flatMap { listOf("--add-opens", "java.base/$it=ALL-UNNAMED") })
            val skipTestSuites: String? by project
            if (skipTestSuites != null && skipTestSuites.equals("true", true)) {
                exclude("**/*TestSuite*")
            }
            // Don't execute abstract test classes
            exclude("**/Abstract*")
            testLogging {
                events("passed", "skipped", "failed")
            }
        }
    }

    val log4j2VersionNr = "2.26.1"
    val log4j2Api = "org.apache.logging.log4j:log4j-api:$log4j2VersionNr"
    val log4j2Core = "org.apache.logging.log4j:log4j-core:$log4j2VersionNr"
    // Bridge that routes log4j calls to log4j2
    val log4j2Bridge = "org.apache.logging.log4j:log4j-1.2-api:$log4j2VersionNr"

    extra["log4j2ApiVersion"] = log4j2Api
    extra["log4j2CoreVersion"] = log4j2Core
    extra["log4j2BridgeVersion"] = log4j2Bridge

    dependencies {
        val testConfig by configurations
        testConfig(log4j2Api)
        testConfig(log4j2Core)
        testConfig(log4j2Bridge)
        testConfig("junit:junit:4.13.2")
        testConfig("org.easymock:easymock:5.4.0")
        testConfig("org.powermock:powermock-core:2.0.9")
        testConfig("org.powermock:powermock-module-junit4:2.0.9")
        testConfig("org.powermock:powermock-api-easymock:2.0.9")
        // Java 25 bytecode support for EasyMock/PowerMock proxies
        testConfig("net.bytebuddy:byte-buddy:1.17.7")
        testConfig("net.bytebuddy:byte-buddy-agent:1.17.7")
    }

    /*
     * -PintellijHome=<path> (or INTELLIJ_HOME) builds against a local IntelliJ installation.
     * -PversionQualifier=<qualifier> appends a qualifier to the plugin version.
     */
    val intellijHome: String? by project
    val versionQualifier: String? by project
    extra["intellijHome"] = intellijHome ?: System.getenv("INTELLIJ_HOME") ?: ""
    extra["versionQualifier"] = if (versionQualifier.isNullOrBlank()) "" else ".$versionQualifier"
}

tasks.register("sarosIntellij", Copy::class) {
    dependsOn(":saros.core:test", ":saros.intellij:test", ":saros.intellij:buildPlugin")
    group = "Build"
    description = "Builds and tests all modules required by Saros for Intellij"
    from(project(":saros.intellij").layout.buildDirectory.dir("distributions"))
    include("*.zip")
    into(layout.buildDirectory.dir("distribution/intellij"))
}
