plugins {
    kotlin("jvm") version "1.9.25"
    kotlin("plugin.spring") version "1.9.25"
    kotlin("plugin.jpa") version "1.9.22"
    id("org.springframework.boot") version "3.5.7"
    id("io.swagger.core.v3.swagger-gradle-plugin") version "2.2.40"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.flywaydb.flyway") version "11.17.0"
    id("org.jlleitschuh.gradle.ktlint") version "12.1.0"
}

group = "com.collederas"
version = "0.0.1-SNAPSHOT"
description = "KRoll is a remote configuration and feature-flag service for games."

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

ktlint {
    version.set("1.2.1")
    enableExperimentalRules.set(true)
    outputToConsole.set(true)
}

repositories {
    mavenCentral()
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
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("com.github.ben-manes.caffeine:caffeine")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0")
    implementation("io.swagger.core.v3:swagger-annotations:2.2.20")

    implementation("org.jetbrains.kotlin:kotlin-reflect")
    runtimeOnly("org.postgresql:postgresql")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.mockk:mockk:1.14.6")
    testImplementation("com.ninja-squad:springmockk:4.0.2")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("com.h2database:h2")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
