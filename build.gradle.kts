plugins {
    id("base")
    id("net.researchgate.release") version "3.1.0"
}

subprojects {
    group = "ch.gatzka"
    version = "1.0.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

tasks {
    clean {
        doLast {
            val gitignore = file(".gitignore")

            gitignore.readLines().filter { it.isNotEmpty() }.filter {
                it != "/.gradle/" && it != "/.idea/"
            }.map { file(it) }.filter { it.exists() }.forEach {
                logger.error("Deleting file: ${it.absolutePath}")
                it.deleteRecursively()
            }
        }
    }
}