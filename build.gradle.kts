plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "4.8.3"
  kotlin("plugin.spring") version "1.8.10"
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

  // Enable kotlin reflect
  implementation("org.jetbrains.kotlin:kotlin-reflect:1.6.20")

  // Database dependencies
  runtimeOnly("org.flywaydb:flyway-core")
  runtimeOnly("org.postgresql:postgresql:42.5.1")
  implementation("com.vladmihalcea:hibernate-types-52:2.20.0")

  // OpenAPI
  implementation("org.springdoc:springdoc-openapi-ui:1.6.13")
  implementation("org.springdoc:springdoc-openapi-data-rest:1.6.13")
  implementation("org.springdoc:springdoc-openapi-kotlin:1.6.13")

  // Test dependencies
  testImplementation("com.github.tomakehurst:wiremock-standalone:2.27.2")
  testImplementation("org.springframework.security:spring-security-test")
  testImplementation("org.awaitility:awaitility-kotlin:4.2.0")
  testImplementation("io.jsonwebtoken:jjwt:0.9.1")
  testImplementation("net.javacrumbs.json-unit:json-unit-assertj:2.36.0")
  testImplementation("io.swagger.parser.v3:swagger-parser-v2-converter:2.1.8")
  testImplementation("org.mockito:mockito-inline:4.8.0")
  testImplementation("org.junit.jupiter:junit-jupiter:5.9.0")
  testImplementation("com.h2database:h2")
}

java {
  toolchain.languageVersion.set(JavaLanguageVersion.of(18))
}

tasks {
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
      jvmTarget = "18"
    }
  }
}
