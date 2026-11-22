plugins {
    idea
}

idea {
    module {
        excludeDirs.addAll(mutableListOf(file("docs/.jekyll-cache"), file("docs/_site"), file("docs/node_modules")))
    }
}

// the libs accessor only exists on the root project, so resolve it outside of subprojects {}
val testBundles = listOf(libs.bundles.log4j2, libs.bundles.testing)

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
            if (providers.gradleProperty("skipTestSuites").getOrElse("false").equals("true", true)) {
                exclude("**/*TestSuite*")
            }
            // Don't execute abstract test classes
            exclude("**/Abstract*")
            testLogging {
                events("passed", "skipped", "failed")
            }
        }
    }

    dependencies {
        testBundles.forEach { "testConfig"(it) }
    }
}

tasks.register<Copy>("sarosIntellij") {
    dependsOn(":saros.core:test", ":saros.intellij:test", ":saros.intellij:buildPlugin")
    group = "Build"
    description = "Builds and tests all modules required by Saros for Intellij"
    from(project(":saros.intellij").layout.buildDirectory.dir("distributions"))
    include("*.zip")
    into(layout.buildDirectory.dir("distribution/intellij"))
}
