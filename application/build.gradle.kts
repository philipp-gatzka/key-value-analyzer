@file:Suppress("SpellCheckingInspection")

import org.apache.tools.ant.filters.ReplaceTokens


plugins {
    id("java")
    id("idea")
    id("org.springframework.boot") version "3.3.4"
    id("io.freefair.lombok") version "8.12"
    id("com.vaadin") version "24.6.5"
}

dependencies {
    implementation(platform("org.springframework.boot:spring-boot-dependencies:3.3.4"))
    implementation(platform("com.vaadin:vaadin-bom:24.6.5"))

    implementation(project(":tarkov-api"))
    implementation(project(":datamodel"))
    implementation("com.vaadin:vaadin-spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-jooq")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
    implementation("org.parttio:line-awesome:2.1.0")

    developmentOnly("org.springframework.boot:spring-boot-devtools:3.3.4")

    runtimeOnly("org.postgresql:postgresql:42.7.5")
    runtimeOnly("org.springframework.boot:spring-boot-starter-actuator")
}

tasks {
    withType<JavaCompile> {
        options.encoding = "UTF-8"
    }
    processResources {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        from("src/main/resources") {
            include("application-development.properties")

            project.properties.forEach { (key, value) ->
                if (key != null) {
                    filter<ReplaceTokens>("tokens" to mapOf(key to value.toString()))
                    filter<ReplaceTokens>("tokens" to mapOf("project.$key" to value.toString()))
                }
            }
        }
    }

}

vaadin {
    optimizeBundle = false
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(23))
    }
}