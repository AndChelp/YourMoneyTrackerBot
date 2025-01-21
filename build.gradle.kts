plugins {
    kotlin("jvm") version "1.9.25"
    kotlin("plugin.spring") version "1.9.25"
    id("org.springframework.boot") version "3.4.1"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "ru.andchelp"
version = "0.0.1-SNAPSHOT"

java {
    toolchain.languageVersion = JavaLanguageVersion.of(17)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.telegram:telegrambots-springboot-longpolling-starter:8.0.0")
    implementation("org.telegram:telegrambots-client:8.0.0")

    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework:spring-web")
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign:4.2.0")

    runtimeOnly("org.postgresql:postgresql")

    implementation("io.github.oshai:kotlin-logging:7.0.3")

    implementation("org.jetbrains.kotlin:kotlin-reflect")
}
