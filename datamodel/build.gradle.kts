import org.jooq.meta.jaxb.MatcherTransformType

plugins {
    id("java-library")
    id("idea")
    id("org.jooq.jooq-codegen-gradle") version "3.19.18"
}

dependencies {
    jooqCodegen("org.postgresql:postgresql:42.7.5")
}

val databaseUrl = project.properties["database.url"] as String?
val databaseUser = project.properties["database.user"] as String?
val databasePassword = project.properties["database.password"] as String?

jooq {
    configuration {
        jdbc {
            driver = "org.postgresql.Driver"
            url = databaseUrl
            user = "postgres"
            password = "postgres"
        }

        generator {
            database {
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

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    dependsOn("jooqCodegen")
}

dependencies {
    implementation(platform("org.springframework.boot:spring-boot-dependencies:3.3.4"))
    implementation("org.springframework.boot:spring-boot-starter-jooq")
}