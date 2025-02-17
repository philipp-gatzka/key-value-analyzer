@file:Suppress("SpellCheckingInspection")

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

    runtimeOnly("org.postgresql:postgresql:42.7.5")
    runtimeOnly("org.springframework.boot:spring-boot-starter-actuator")
}

tasks {
    withType<JavaCompile> {
        options.encoding = "UTF-8"
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