package no.kartverket.nibas

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import java.util.*

@SpringBootApplication
class NibasBackendApplication {
    @Bean
    fun openApiDef(): OpenAPI {
        return OpenAPI().info(Info().title("NIBAS Server API").description("API for å hente administrative enheter"));
    }
}

fun main(args: Array<String>) {
    runApplication<NibasBackendApplication>(*args)
}


fun <T> Optional<T>.unwrap(): T? = orElse(null)