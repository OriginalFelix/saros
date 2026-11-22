import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    alias(libs.plugins.intellij.platform)
}

/*
 * -PintellijHome=<path> (or INTELLIJ_HOME) builds against a local IntelliJ installation.
 * -PversionQualifier=<qualifier> appends a qualifier to the plugin version.
 */
val intellijHome =
    providers.gradleProperty("intellijHome").orElse(providers.environmentVariable("INTELLIJ_HOME")).getOrElse("")
val versionQualifier = providers.gradleProperty("versionQualifier").map { if (it.isBlank()) "" else ".$it" }.getOrElse("")
val pluginVersion = "0.4.0$versionQualifier"

repositories {
    intellijPlatform {
        defaultRepositories()
    }
}

configurations.named("testImplementation") { extendsFrom(configurations["testConfig"]) }

dependencies {
    implementation(project(":saros.core"))
    testImplementation(project(path = ":saros.core", configuration = "testing"))

    intellijPlatform {
        if (intellijHome.isNotBlank()) local(intellijHome) else intellijIdea("2026.3")
        testFramework(TestFrameworkType.Platform)
    }
}

sourceSets {
    main {
        java.srcDirs("src")
        resources.srcDirs("resources", "src")
        resources.exclude("**/*.java")
    }
    test {
        java.srcDirs("test/junit")
    }
}

intellijPlatform {
    pluginConfiguration {
        version = pluginVersion
        ideaVersion {
            sinceBuild = "263"
            untilBuild = provider { null }
        }
    }
    buildSearchableOptions = false
    pluginVerification {
        ides {
            if (intellijHome.isNotBlank()) local(file(intellijHome)) else recommended()
        }
    }
}

tasks {
    processResources {
        // a local copy, so the action does not capture the script (configuration cache)
        val version = pluginVersion
        inputs.property("pluginVersion", version)
        filesMatching("saros.properties") { filter { it.replace("@pluginVersion@", version) } }
    }

    jar {
        from(rootProject.file("saros_log4j2.xml"))
        from(rootProject.file("log4j2.xml"))
    }

    runIde {
        maxHeapSize = "2048m"
        jvmArgs("-Dsaros.debug=true")
    }
}
