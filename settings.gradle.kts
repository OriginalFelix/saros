plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

/*
 * Only the modules needed for Saros/IntelliJ are built. The eclipse, server, lsp and stf
 * modules depended on the removed buildSrc tooling (goomph / Gradle 6) and are not built anymore.
 */
val prefix = "saros."
listOf("core", "intellij").forEach { dir ->
    val projectName = prefix + dir
    include(projectName)
    project(":$projectName").projectDir = file(dir)
}
