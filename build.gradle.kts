import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	id("org.springframework.boot") version "2.6.2"
	id("io.spring.dependency-management") version "1.0.11.RELEASE"
	kotlin("jvm") version "1.6.10"
	kotlin("plugin.spring") version "1.6.10"
	kotlin("plugin.jpa") version "1.6.10"
	kotlin("plugin.allopen") version "1.6.10"
	id("org.flywaydb.flyway") version "8.0.4"
}

group = "no.kartverket.nibas"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_17

repositories {
	mavenLocal()
	mavenCentral()
}

object DependencyVersions {
	const val HIBERNATE_VERSION = "5.4.32.Final"
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-webflux")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springdoc:springdoc-openapi-webmvc-core:1.5.10")
	implementation("org.springdoc:springdoc-openapi-kotlin:1.5.10")
	implementation("org.springdoc:springdoc-openapi-ui:1.5.10")
	implementation("org.locationtech.jts:jts-core:1.18.2")
	implementation("org.locationtech.jts:jts:1.18.2")
	implementation("org.locationtech.jts:jts-modules:1.18.2")
	implementation("org.locationtech.jts:jts-io:1.18.2")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.0")

	implementation("org.locationtech.spatial4j:spatial4j:0.8")
	implementation("org.hibernate:hibernate-spatial:${DependencyVersions.HIBERNATE_VERSION}")
	implementation("org.hibernate:hibernate-envers:${DependencyVersions.HIBERNATE_VERSION}")
	implementation("org.springframework.cloud:spring-cloud-starter")
	implementation("org.springframework.cloud:spring-cloud-starter-vault-config:3.1.0")
	implementation("org.springframework.cloud:spring-cloud-starter-bootstrap:3.1.0")
	implementation("org.locationtech.jts.io:jts-io-common:1.18.2")

	runtimeOnly("org.flywaydb:flyway-core:8.0.4")
	runtimeOnly("org.postgresql:postgresql")
	runtimeOnly("com.google.cloud:spring-cloud-gcp-starter-logging:3.0.0")

	testImplementation("org.jetbrains.kotlin:kotlin-test")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
}

configurations.implementation {
	exclude(group = "junit", module = "junit")
}

allOpen {
	annotation("javax.persistence.Entity")
	annotation("javax.persistence.Embeddable")
	annotation("javax.persistence.MappedSuperclass")
}

tasks.getByName<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
   findProperty("TLSversion")?.let { systemProperty("jdk.tls.client.protocols", it) }
}

tasks.withType<KotlinCompile> {
	kotlinOptions {
		freeCompilerArgs = listOf("-Xjsr305=strict")
		jvmTarget = "17"
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}

tasks.getByName<Jar>("jar") {
	enabled = false
}

flyway {
	schemas = arrayOf("nibas")
}
