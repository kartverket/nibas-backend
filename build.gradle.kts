import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.springframework.boot.gradle.tasks.bundling.BootJar
import org.springframework.boot.gradle.tasks.run.BootRun
import java.util.*

// Versjonering settes i gradle/libs.versions.toml - vær obs. på at
// intellij ikke plukker opp på endringer der, før gradle er reloadet.
// Mer info: https://docs.gradle.org/current/userguide/platforms.html

plugins {
    alias(libs.plugins.spring.framework)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.kotlin.jpa)
    alias(libs.plugins.kotlin.allopen)
    alias(libs.plugins.flyway)
}

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { load(it) }
    }
}

fun localProperty(key: String): String? =
    localProperties.getProperty(key) ?: System.getenv(key)

group = "no.kartverket.nibas"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenLocal()
    mavenCentral {
        content {
            excludeGroup("no.kartverket.matrikkelapi-v1")
        }
    }
    maven {
        name = "GitHubPackages"
        url = uri("https://maven.pkg.github.com/kartverket/matrikkelapi-v1-wsclient")
        content {
            includeGroup("no.kartverket.matrikkelapi-v1")
        }
        val matrikkelRepoToken = localProperty("GH_PACKAGES_PAT")
        if (!matrikkelRepoToken.isNullOrBlank()) {
            credentials(HttpHeaderCredentials::class) {
                name = "Authorization"
                value = "Bearer $matrikkelRepoToken"
            }
            authentication {
                create("header", HttpHeaderAuthentication::class)
            }
        } else {
            logger.warn("GH_PACKAGES_PAT er ikke satt: Kan ikke hente avhengigheter fra GitHub Packages")
        }
    }
}


val intTest: SourceSet? by sourceSets.creating {
    compileClasspath += sourceSets.main.get().output
    runtimeClasspath += sourceSets.main.get().output
    // Legger til fra sourceSet test også for å få tilgang på buildere osv.
    compileClasspath += sourceSets.test.get().output
    runtimeClasspath += sourceSets.test.get().output
}

val intTestImplementation: Configuration by configurations.getting {
    extendsFrom(configurations.implementation.get())
}

configurations["intTestRuntimeOnly"].extendsFrom(configurations.runtimeOnly.get())

buildscript {
    dependencies {
        classpath(libs.flyway.postgres)
    }
}

dependencies {
    implementation(libs.spring.starter)
    implementation(libs.spring.starter.webmvc)
    implementation(libs.spring.starter.jpa)
    implementation(libs.spring.starter.jackson)
    implementation(libs.spring.starter.security) // Web-security
    implementation(libs.spring.starter.webflux)
    implementation(libs.jackson.module.kotlin)
    implementation(libs.kotlin.reflect)
    implementation(libs.spring.starter.actuator)
    implementation(libs.spring.starter.validator)
    implementation(libs.springdoc.openapi.common)
    implementation(libs.springdoc.openapi.webmvc.api)
    implementation(libs.springdoc.openapi.webmvc.ui)
    implementation(libs.locationtech.jts.core)
    implementation(libs.locationtech.jts)
    implementation(libs.locationtech.jts.modules)
    implementation(libs.locationtech.jts.io)
    implementation(libs.locationtech.jts.io.common) {
        exclude(group = "junit", module = "junit")
    }
    implementation(libs.kotlinx.coroutines)
    implementation(libs.locationtech.spatial4j)
    implementation(libs.locationtech.proj4j)
    implementation(libs.locationtech.proj4j.epsg)
    implementation(libs.hibernate.spatial)
    implementation(libs.logstash.logback.encoder)
    implementation(libs.micrometer.registry.prometheus)
    implementation(libs.spring.integration.core)
    implementation(libs.nimbus.jose.jwt)
    implementation(platform(libs.opentelemetry.bom))
    implementation(libs.opentelemetry.api)

    //WS
    implementation(libs.matrikkelapi.wsclient)
    implementation(libs.org.apache.cxf.frontend)
    implementation(libs.org.apache.cxf.transports)

    runtimeOnly(libs.spring.starter.flyway)
    runtimeOnly(libs.flyway.postgres)
    runtimeOnly(libs.postgres)

    testImplementation(libs.kotlin.test)
    testImplementation(libs.spring.starter.jackson.test)
    testImplementation(libs.spring.starter.webmvc.test)
    testImplementation(libs.spring.starter.test)
    testImplementation(libs.spring.batch.test)
    testImplementation(libs.spring.mockk)
    testImplementation(libs.kotlin.mockito)
    testImplementation(libs.spring.starter.thymeleaf.test)
    testImplementation(libs.spring.starter.jackson.test)
    testImplementation(libs.spring.starter.security.test)

    testRuntimeOnly(libs.h2.db)

    intTestImplementation(libs.kotlin.test)
    intTestImplementation(libs.spring.starter.jackson.test)
    intTestImplementation(libs.spring.starter.test)
    intTestImplementation(libs.testcontainers.postgres)
    intTestImplementation(libs.testcontainers.junit)
    intTestImplementation(libs.spring.mockk)
    intTestImplementation(libs.spring.starter.flyway.test)
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

tasks.named<BootJar>("bootJar") {
    mainClass.set("no.kartverket.nibas.NibasBackendApplication")
}

tasks.getByName<BootRun>("bootRun") {
    findProperty("TLSversion")?.let { systemProperty("jdk.tls.client.protocols", it) }
    when (findProperty("Import")) {
        null -> mainClass.set("no.kartverket.nibas.NibasBackendApplication")
        else -> mainClass.set("no.kartverket.nibas.NibasImportApplication")
    }
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        freeCompilerArgs.set(listOf("-Xjsr305=strict", "-Xannotation-default-target=param-property"))
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

tasks.getByName<Jar>("jar") {
    enabled = false
}

val integrationTest = tasks.register<Test>("integrationTest") {
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
    jvmArgs = listOf("--add-opens=java.base/java.time=ALL-UNNAMED")
}

flyway {
    schemas = arrayOf("nibas")
}

if (hasProperty("buildScan")) {
    extensions.findByName("buildScan")?.withGroovyBuilder {
        setProperty("termsOfServiceUrl", "https://gradle.com/terms-of-service")
        setProperty("termsOfServiceAgree", "no")
    }
}
