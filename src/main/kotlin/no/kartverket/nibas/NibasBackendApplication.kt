package no.kartverket.nibas

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import no.kartverket.nibas.config.NibasBackendConfigProps
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.scheduling.annotation.EnableScheduling
import java.util.*

@EnableScheduling
@EnableConfigurationProperties(NibasBackendConfigProps::class)
@SpringBootApplication
class NibasBackendApplication {

    @Bean
    fun openApiDef(): OpenAPI =
        OpenAPI().info(
            Info().apply {
                title = "NIBAS Server API"
                description = "API for å hente administrative enheter"
                version = "1.0.0"
            }
        )

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            @Suppress("SpreadOperator")
            val configurableAppCtx = runApplication<NibasBackendApplication>(*args)
            asciiArt(configurableAppCtx)
        }
    }
}

/**
 * Kalles fra statisk kontekst. Logger noen relevante konfigurasjonsparametre.
 */
fun asciiArt(appCtx: ConfigurableApplicationContext) {
    val logger = LoggerFactory.getLogger(NibasBackendApplication::class.java)
    val env = appCtx.environment
    val datasourceUrlProp = "spring.datasource.url"

    val asciiArtTemplate = """
----------------------------------------------------------
	
    Application '{}' is running!
	Profiles: 	{}
	TLS-version:   {}
	Configuration: {}
    Datasource url: {}
    
----------------------------------------------------------"""

    logger.info(asciiArtTemplate, *listOf(
        env.getProperty("spring.application.name"),
        env.activeProfiles,
        env.getProperty("jdk.tls.client.protocols", ""),
        appCtx.getBean(NibasBackendConfigProps::class.java).toString(),
        env.getProperty(datasourceUrlProp),
    ).toTypedArray())
}

fun <T> Optional<T>.unwrap(): T? = orElse(null)
