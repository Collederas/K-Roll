import java.util.Properties
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

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
    implementation("io.github.erdtman:java-json-canonicalization:1.1")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.15")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("io.swagger.core.v3:swagger-annotations:2.2.20")

    implementation("org.jetbrains.kotlin:kotlin-reflect")
    runtimeOnly("org.postgresql:postgresql")
    // RuntimeOnly ensures H2 is available for 'bootRun' and 'generateOpenApiDocs',
    // but also for tests (which inherit runtime scope).
    runtimeOnly("com.h2database:h2")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.mockk:mockk:1.14.6")
    testImplementation("com.ninja-squad:springmockk:4.0.2")
    testImplementation("org.springframework.security:spring-security-test")
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

springBoot {
    buildInfo()
    mainClass.set("com.collederas.kroll.KrollApplicationKt")
}

openApi {
    outputDir.set(layout.buildDirectory.dir("contract"))
    outputFileName.set("openapi.json")
    apiDocsUrl.set("http://localhost:8080/v3/api-docs")

    waitTimeInSeconds.set(60)
    customBootRun {
        // Use the lightweight profile that needs no Docker
        args.set(listOf("--spring.profiles.active=contractgen"))
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
                minimum = "0.30".toBigDecimal()
            }
        }
    }
}

// Generate Canonical Contract and Hash
tasks.register<JavaExec>("generateCanonicalContract") {
    dependsOn("generateOpenApiDocs")
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.collederas.kroll.api.tools.OpenApiCanonicalContractGenerator")
    args(layout.buildDirectory.file("contract/openapi.json").get().asFile.absolutePath)
    outputs.file(layout.buildDirectory.file("contract/openapi.sha256"))
    outputs.file(layout.buildDirectory.file("contract/openapi.json"))
}

// Generate a Properties File containing the Hash
val generateContractProps = tasks.register("generateContractProperties") {
    group = "contract"
    description = "Creates a contract.properties file from the generated hash"

    dependsOn("generateCanonicalContract")

    val hashFile = layout.buildDirectory.file("contract/openapi.sha256")
    val propsFile = layout.buildDirectory.file("generated/contract/contract.properties")

    inputs.file(hashFile)
    outputs.file(propsFile)

    doLast {
        val hash = hashFile.get().asFile.readText().trim()
        val props = Properties()
        props.setProperty("kroll.contract.hash", hash)

        propsFile.get().asFile.parentFile.mkdirs()
        propsFile.get().asFile.outputStream().use { props.store(it, "Generated Contract Hash") }
    }
}

// Inject the generated properties file into the JAR's classpath
tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    // This injects the file into 'BOOT-INF/classes/contract.properties'
    // where Spring can easily load it via @Value("${kroll.contract.hash}")
    from(generateContractProps) {
        into("BOOT-INF/classes")
    }
}

// Convenience task for debugging contract building
tasks.register("buildContract") {
    group = "contract"
    description = "Generates OpenAPI contract and properties"
    dependsOn(generateContractProps)
}

tasks.check {
    dependsOn(tasks.jacocoTestCoverageVerification)
}

tasks.withType<Test> {
    useJUnitPlatform()
    systemProperty("spring.profiles.active", "test")
    finalizedBy(tasks.jacocoTestReport)
}
val compileKotlin: KotlinCompile by tasks
compileKotlin.compilerOptions {
    freeCompilerArgs.set(listOf("-Xannotation-default-target=param-property"))
}
