
plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "10.0.5"
  kotlin("plugin.spring") version "2.3.10"
  kotlin("plugin.jpa") version "2.3.10"
  id("se.patrikerdes.use-latest-versions") version "0.2.19"
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

dependencies {
  annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

  implementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter:2.0.2")

  // Spring boot dependencies
  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.boot:spring-boot-starter-actuator")
  implementation("org.springframework.boot:spring-boot-starter-webclient")
  implementation("org.springframework.boot:spring-boot-starter-flyway")

  implementation("io.hypersistence:hypersistence-utils-hibernate-71:3.15.2")

  // Enable kotlin reflect
  implementation("org.jetbrains.kotlin:kotlin-reflect:2.3.10")

  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:7.0.1")

  implementation("org.springframework:spring-jms:7.0.5")

  // Database dependencies
  runtimeOnly("org.flywaydb:flyway-core")
  runtimeOnly("org.flywaydb:flyway-database-postgresql")
  runtimeOnly("org.postgresql:postgresql")

  // OpenAPI
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.2")

  // Test dependencies
  testImplementation("org.awaitility:awaitility-kotlin")
  testImplementation("org.wiremock:wiremock-standalone:3.13.2")
  testImplementation("org.springframework.security:spring-security-test")
  testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
  testImplementation("org.springframework.boot:spring-boot-starter-security-test")
  testImplementation("org.springframework.boot:spring-boot-webtestclient")
  testImplementation("org.awaitility:awaitility-kotlin:4.3.0")
  testImplementation("io.jsonwebtoken:jjwt:0.13.0")
  testImplementation("net.javacrumbs.json-unit:json-unit-assertj:5.1.1")
  testImplementation("io.swagger.parser.v3:swagger-parser-v2-converter:2.1.39")
  testImplementation("org.mockito:mockito-inline:5.2.0")
  testImplementation("org.junit.jupiter:junit-jupiter:6.0.3")
  testImplementation("org.testcontainers:localstack:1.21.4")
  testImplementation("org.testcontainers:postgresql:1.21.4")
}

kotlin {
  jvmToolchain(25)
}

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(25))
  }
}
