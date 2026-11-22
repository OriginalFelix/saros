import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("org.jetbrains.intellij.platform") version "2.20.1"
}

val versionQualifier = ext.get("versionQualifier") as String
val intellijHome = ext.get("intellijHome") as String
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
        inputs.property("pluginVersion", pluginVersion)
        filesMatching("saros.properties") { filter { it.replace("@pluginVersion@", pluginVersion) } }
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
