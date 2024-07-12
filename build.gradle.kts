plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "6.0.1"
  kotlin("plugin.spring") version "1.9.24"
  kotlin("plugin.jpa") version "2.0.0"
  id("se.patrikerdes.use-latest-versions") version "0.2.18"
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

dependencies {
  annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

  // Spring boot dependencies
  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.boot:spring-boot-starter-actuator")

  implementation("io.hypersistence:hypersistence-utils-hibernate-63:3.8.1")

  // Enable kotlin reflect
  implementation("org.jetbrains.kotlin:kotlin-reflect:2.0.0")

  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:4.0.1")

  implementation("org.springframework:spring-jms:6.1.11")

  // Database dependencies
  runtimeOnly("org.flywaydb:flyway-core")
  runtimeOnly("org.flywaydb:flyway-database-postgresql")
  runtimeOnly("org.postgresql:postgresql")

  // OpenAPI
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0")

  // Test dependencies
  testImplementation("org.awaitility:awaitility-kotlin")
  testImplementation("com.github.tomakehurst:wiremock-jre8-standalone:3.0.1")
  testImplementation("org.springframework.security:spring-security-test")
  testImplementation("org.awaitility:awaitility-kotlin:4.2.1")
  testImplementation("io.jsonwebtoken:jjwt:0.12.6")
  testImplementation("net.javacrumbs.json-unit:json-unit-assertj:3.4.0")
  testImplementation("io.swagger.parser.v3:swagger-parser-v2-converter:2.1.22")
  testImplementation("org.mockito:mockito-inline:5.2.0")
  testImplementation("org.junit.jupiter:junit-jupiter:5.10.3")
  testImplementation("com.h2database:h2")
  testImplementation("org.testcontainers:localstack:1.19.8")
}

kotlin {
  jvmToolchain(21)
}

tasks {
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
      jvmTarget = "21"
    }
  }
}
