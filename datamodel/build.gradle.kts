import org.flywaydb.gradle.task.FlywayMigrateTask
import org.jooq.meta.jaxb.MatcherTransformType
import org.testcontainers.containers.PostgreSQLContainer

plugins {
    id("java-library")
    id("idea")
    id("org.jooq.jooq-codegen-gradle") version "3.19.18"
    id("org.flywaydb.flyway") version "11.2.0"
}

buildscript {
    dependencies {
        classpath(platform("org.springframework.boot:spring-boot-dependencies:3.3.4"))
        classpath("org.postgresql:postgresql")
        classpath("org.testcontainers:postgresql")
        classpath("org.flywaydb:flyway-database-postgresql:11.2.0")
    }
}

dependencies {
    implementation(platform("org.springframework.boot:spring-boot-dependencies:3.3.4"))
    implementation("org.springframework.boot:spring-boot-starter-jooq")

    jooqCodegen("org.jooq:jooq-meta-extensions:3.19.18")
}

val databaseContainer = PostgreSQLContainer<Nothing>("postgres:15").apply {
    withDatabaseName("postgres")
    withUsername("postgres")
    withPassword("postgres")
    start()
}

jooq {
    configuration {
        jdbc {
            driver = "org.postgresql.Driver"
            url = databaseContainer.jdbcUrl
            user = databaseContainer.username
            password = databaseContainer.password
        }
        generator {
            database {
                name = "org.jooq.meta.postgres.PostgresDatabase"
                excludes = "flyway_schema_history"
                inputSchema = "public"
                isIncludeSequences = true
                isIncludeSystemSequences = true
            }
            target {
                packageName = "ch.gatzka"
            }
            generate {
                isFluentSetters = true
            }
            strategy {
                matchers {
                    tables {
                        table {
                            tableClass {
                                transform = MatcherTransformType.PASCAL
                                expression = "$0_Table"
                            }
                            recordClass {
                                transform = MatcherTransformType.PASCAL
                                expression = "$0_Record"
                            }
                        }
                    }
                }
            }
        }
    }
}

val dbConnectionUrl: String? = findProperty("dbConnectionUrl") as String?
val dbConnectionUser: String? = findProperty("dbConnectionUser") as String?
val dbConnectionPassword: String? = findProperty("dbConnectionPassword") as String?

tasks {
    withType<JavaCompile> {
        options.encoding = "UTF-8"
        dependsOn("jooqCodegen")
    }
    register<FlywayMigrateTask>("migrateDEV") {
        doFirst {
            url = databaseContainer.jdbcUrl
            user = databaseContainer.username
            password = databaseContainer.password
            schemas = arrayOf("public")
        }
    }
    register<FlywayMigrateTask>("migrate") {
        doFirst {
            checkNotNull(dbConnectionUrl) { "dbConnectionUrl is not set" }
            checkNotNull(dbConnectionUser) { "dbConnectionUser is not set" }
            checkNotNull(dbConnectionPassword) { "dbConnectionPassword is not set" }
        }
        url = dbConnectionUrl
        user = dbConnectionUser
        password = dbConnectionPassword
        schemas = arrayOf("public")
    }
    jooqCodegen {
        dependsOn(named("migrateDEV"))
    }
}