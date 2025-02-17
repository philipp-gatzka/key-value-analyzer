@file:Suppress("SpellCheckingInspection")

import com.apollographql.apollo.gradle.internal.ApolloGenerateSourcesTask


plugins {
    id("idea")
    id("java-library")
    id("com.apollographql.apollo") version "4.1.1"
}

dependencies {
    api("com.apollographql.java:client:0.0.2")
}

tasks {
    withType<JavaCompile> {
        options.encoding = "UTF-8"
        dependsOn(generateApolloSources)
    }
    named("generateApolloSources") {
        dependsOn("downloadTarkovApolloSchemaFromIntrospection")
    }
}

val apolloSchemaFile = layout.buildDirectory.file("schema.graphql")

apollo {
    service("tarkov") {
        packageName.set("ch.gatzka")
        srcDir("src/main/resources")
        introspection {
            endpointUrl.set("https://api.tarkov.dev/graphql")
            schemaFile.set(apolloSchemaFile)
        }
        schemaFile.set(apolloSchemaFile)

    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(23))
    }
}