import io.gitlab.arturbosch.detekt.Detekt
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "2.7.0"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
    kotlin("jvm") version "1.7.0"
    kotlin("plugin.spring") version "1.7.0"
    kotlin("plugin.jpa") version "1.7.0"
    kotlin("plugin.allopen") version "1.7.0"
    id("org.flywaydb.flyway") version "8.5.12"
    id("io.gitlab.arturbosch.detekt") version "1.20.0"
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
    }
}

val intTestImplementation: Configuration by configurations.getting {
    extendsFrom(configurations.implementation.get())
}

configurations["intTestRuntimeOnly"].extendsFrom(configurations.runtimeOnly.get())

object DependencyVersions {
    const val HIBERNATE_VERSION = "5.6.5.Final"
    const val SPRINGDOC_OPENAPI_VERSION = "1.6.6"
    const val JTS_VERSION = "1.18.2"
}

dependencies {
    implementation("no.kartverket.aut:aut-lib:0.0.12")

    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security") // Web-security
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springdoc:springdoc-openapi-webmvc-core:${DependencyVersions.SPRINGDOC_OPENAPI_VERSION}")
    implementation("org.springdoc:springdoc-openapi-kotlin:${DependencyVersions.SPRINGDOC_OPENAPI_VERSION}")
    implementation("org.springdoc:springdoc-openapi-ui:${DependencyVersions.SPRINGDOC_OPENAPI_VERSION}")
    implementation("org.locationtech.jts:jts-core:${DependencyVersions.JTS_VERSION}")
    implementation("org.locationtech.jts:jts:${DependencyVersions.JTS_VERSION}")
    implementation("org.locationtech.jts:jts-modules:${DependencyVersions.JTS_VERSION}")
    implementation("org.locationtech.jts:jts-io:${DependencyVersions.JTS_VERSION}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.2")

    implementation("org.locationtech.spatial4j:spatial4j:0.8")
    implementation("org.hibernate:hibernate-spatial:${DependencyVersions.HIBERNATE_VERSION}")
    implementation("org.hibernate:hibernate-envers:${DependencyVersions.HIBERNATE_VERSION}")
    implementation("org.springframework.cloud:spring-cloud-starter")
    implementation("org.springframework.cloud:spring-cloud-starter-vault-config:3.1.1")
    implementation("org.locationtech.jts.io:jts-io-common:${DependencyVersions.JTS_VERSION}") {
        exclude(group = "junit", module = "junit")
    }

    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.springframework.boot:spring-boot-starter-test")

    runtimeOnly("org.flywaydb:flyway-core:8.5.12")
    runtimeOnly("org.postgresql:postgresql")

    intTestImplementation("org.jetbrains.kotlin:kotlin-test")
    intTestImplementation("org.springframework.boot:spring-boot-starter-test")
    intTestImplementation("org.testcontainers:postgresql:1.17.2")
    intTestImplementation("org.testcontainers:junit-jupiter:1.17.2")
}

allOpen {
    annotation("javax.persistence.Entity")
    annotation("javax.persistence.MappedSuperclass")
    annotation("javax.persistence.Embeddable")
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

detekt {
    config = files("detekt.yml")
}

tasks.withType<Detekt>().configureEach {
    reports {
        sarif.required.set(true)
    }
}
