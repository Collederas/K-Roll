import java.security.MessageDigest

group = "com.collederas"
version = "0.1.0"
description = "KRoll is a remote configuration and feature-flag service for games."

plugins {
    kotlin("jvm") version "2.2.20"
    kotlin("plugin.spring") version "2.2.20"
    kotlin("plugin.jpa") version "2.2.20"
    id("org.springframework.boot") version "3.5.7"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.flywaydb.flyway") version "11.17.0"
    id("org.springdoc.openapi-gradle-plugin") version "1.9.0"
    id("org.jlleitschuh.gradle.ktlint") version "14.0.1"
    id("dev.detekt") version ("2.0.0-alpha.1")

    jacoco
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-web")

    implementation("io.jsonwebtoken:jjwt-api:0.13.0")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.13.0")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.13.0")

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.springframework.boot:spring-boot-starter-security")
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("com.github.ben-manes.caffeine:caffeine")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.15")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.17.2")
    implementation("io.swagger.core.v3:swagger-annotations:2.2.20")

    implementation("org.jetbrains.kotlin:kotlin-reflect")
    runtimeOnly("org.postgresql:postgresql")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.mockk:mockk:1.14.6")
    testImplementation("com.ninja-squad:springmockk:4.0.2")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("com.h2database:h2")
}

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

val externalContractHash =
    providers.gradleProperty("contractHash")
        .orElse(
            providers.fileContents(
                layout.buildDirectory.file("contract/openapi.sha256")
            ).asText.map { it.trim() }
        )

springBoot {
    buildInfo {
        properties {
            additional.set(
                externalContractHash.map {
                    mapOf("contract.hash" to it)
                }
            )
        }
    }
    mainClass.set("com.collederas.kroll.KrollApplicationKt")
}

openApi {
    outputDir.set(layout.buildDirectory.dir("contract"))
    outputFileName.set("openapi.json")
    apiDocsUrl.set("http://localhost:8080/v3/api-docs")

    waitTimeInSeconds.set(60)
    customBootRun {
        args.set(listOf("--spring.profiles.active=dev"))
    }
}

ktlint {
    version.set("1.4.1")
    filter {
        exclude { it.file.extension == "kts" }
    }
    outputToConsole.set(true)
}

detekt {
    buildUponDefaultConfig.set(true)
    allRules.set(false)
    config = files("config/detekt/detekt.yml")
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(false)
        csv.required.set(false)
    }
}

tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.jacocoTestReport)

    violationRules {
        rule {
            limit {
                counter = "INSTRUCTION"
                value = "COVEREDRATIO"
                minimum = "0.30".toBigDecimal()  // TODO: after MVP, raise
            }
        }
    }
}

// Ensure openapi JSON is reliable
tasks.register<JavaExec>("normalizeOpenApi") {
    dependsOn("generateOpenApiDocs")

    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.collederas.kroll.api.tools.OpenApiNormalizer")

    args(
        layout.buildDirectory
            .file("contract/openapi.json")
            .get()
            .asFile
            .absolutePath
    )
}

tasks.register("hashContract") {
    group = "contract"
    description = "Generates SHA-256 hash for OpenAPI contract"

    dependsOn("normalizeOpenApi")

    doLast {
        val contractFile = layout.buildDirectory
            .file("contract/openapi.json")
            .get()
            .asFile

        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(contractFile.readBytes())
            .joinToString("") { "%02x".format(it) }

        val hashFile = layout.buildDirectory
            .file("contract/openapi.sha256")
            .get()
            .asFile

        hashFile.writeText(hash)
    }
}

tasks.register("buildContract") {
    group = "contract"
    description = "Generates OpenAPI contract and embeds its hash"

    dependsOn(
        "hashContract",
        "bootBuildInfo"
    )
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    dependsOn("buildContract", "bootBuildInfo")
}

tasks.check {
    dependsOn(tasks.jacocoTestCoverageVerification)
}

tasks.withType<Test> {
    useJUnitPlatform()
    systemProperty("spring.profiles.active", "test")
    finalizedBy(tasks.jacocoTestReport)
}
