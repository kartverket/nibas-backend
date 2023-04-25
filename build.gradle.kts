import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    val kotlinPluginVersion = "1.8.20"

    id("org.springframework.boot") version "3.0.5"
    id("io.spring.dependency-management") version "1.1.0"
    kotlin("jvm") version kotlinPluginVersion
    kotlin("plugin.spring") version kotlinPluginVersion
    kotlin("plugin.jpa") version kotlinPluginVersion
    kotlin("plugin.allopen") version kotlinPluginVersion
    id("org.flywaydb.flyway") version "9.16.3"
}

group = "no.kartverket.nibas"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_17

repositories {
    mavenLocal()
    mavenCentral()
    // Aut-lib package registry
    maven {
        name = "Aut-lib GitHubPackages"
        url = uri("https://maven.pkg.github.com/kartverket/aut-lib")
        credentials {
            username = project.findProperty("gh_username") as String? ?: System.getenv("GH_USERNAME")
            password = project.findProperty("pat") as String? ?: System.getenv("PAT")
        }
    }
}

sourceSets {
    create("intTest") {
        compileClasspath += sourceSets.main.get().output
        runtimeClasspath += sourceSets.main.get().output
        // Legger til fra sourceSet test også for å få tilgang på buildere osv.
        compileClasspath += sourceSets.test.get().output
        runtimeClasspath += sourceSets.test.get().output
    }
}

val intTestImplementation: Configuration by configurations.getting {
    extendsFrom(configurations.implementation.get())
}

configurations["intTestRuntimeOnly"].extendsFrom(configurations.runtimeOnly.get())

// Definerer versjoner for avhengigheter som ikke dekkes av BOM
val hibernateVersion = "6.1.7.Final"
val springdocOpenapiVersion2 = "2.1.0"
val jtsVersion = "1.19.0"
val testContainerVersion = "1.17.6"
val autLibVersion = "2.0.0"
val flywayVersion = "9.16.1"
val hypersistenceUtilsHibernate60Version = "3.3.2"
val springCloudStarterVaultConfigVersion = "4.0.1"
val spatial4jVersion = "0.8"
val kotlinxCoroutinesCoreVersion = "1.6.4"
val logstashVersion = "7.3"
val springmockkVersion = "4.0.2"
val kotlinMockitoVersion = "4.1.0"

ext["snakeyaml.version"] = "1.31"

dependencies {
    implementation("no.kartverket.aut:aut-lib:$autLibVersion")

    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security") // Web-security
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springdoc:springdoc-openapi-starter-common:$springdocOpenapiVersion2")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-api:$springdocOpenapiVersion2")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:$springdocOpenapiVersion2")
    implementation("org.locationtech.jts:jts-core:$jtsVersion")
    implementation("org.locationtech.jts:jts:$jtsVersion")
    implementation("org.locationtech.jts:jts-modules:$jtsVersion")
    implementation("org.locationtech.jts:jts-io:$jtsVersion")
    implementation("org.locationtech.jts.io:jts-io-common:$jtsVersion") {
        exclude(group = "junit", module = "junit")
    }
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinxCoroutinesCoreVersion")
    implementation("org.locationtech.spatial4j:spatial4j:$spatial4jVersion")
    implementation("org.hibernate.orm:hibernate-spatial:$hibernateVersion")
    implementation("net.logstash.logback:logstash-logback-encoder:$logstashVersion")
    implementation("org.springframework.cloud:spring-cloud-starter")
    implementation("org.springframework.cloud:spring-cloud-starter-vault-config:$springCloudStarterVaultConfigVersion")

    implementation("org.springframework.integration:spring-integration-core")

    // This library provides Hibernate 5.5+ Json column support, which is not natively supported by Hibernate
    implementation("io.hypersistence:hypersistence-utils-hibernate-60:$hypersistenceUtilsHibernate60Version")

    runtimeOnly("org.flywaydb:flyway-core:$flywayVersion")
    runtimeOnly("org.postgresql:postgresql")

    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.batch:spring-batch-test")
    testImplementation("com.ninja-squad:springmockk:$springmockkVersion")
    testImplementation("org.mockito.kotlin:mockito-kotlin:$kotlinMockitoVersion")

    testRuntimeOnly("com.h2database:h2")

    intTestImplementation("org.jetbrains.kotlin:kotlin-test")
    intTestImplementation("org.springframework.boot:spring-boot-starter-test")
    intTestImplementation("org.testcontainers:postgresql:$testContainerVersion")
    intTestImplementation("org.testcontainers:junit-jupiter:$testContainerVersion")
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

tasks.named<BootJar>("bootJar") {
    mainClass.set("no.kartverket.nibas.NibasBackendApplication")
}

tasks.getByName<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
    findProperty("TLSversion")?.let { systemProperty("jdk.tls.client.protocols", it) }
    when (findProperty("Import")) {
        null -> mainClass.set("no.kartverket.nibas.NibasBackendApplication")
        else -> mainClass.set("no.kartverket.nibas.NibasImportApplication")
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "17"
    }
}

tasks.getByName<Jar>("jar") {
    enabled = false
}

val integrationTest = task<Test>("integrationTest") {
    description = "Runs integration tests."
    group = "verification"

    testClassesDirs = sourceSets["intTest"].output.classesDirs
    classpath = sourceSets["intTest"].runtimeClasspath
    shouldRunAfter("test")
}

tasks.check { dependsOn(integrationTest) }

tasks.withType<Test> {
    useJUnitPlatform()
    exclude("**/**IntegrationTest.class")
}

flyway {
    schemas = arrayOf("nibas")
}
