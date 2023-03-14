package no.kartverket.nibas.config

import no.kartverket.nibas.kodelister.DefaultKodelisteService
import no.kartverket.nibas.kodelister.KodelisteFetcher
import no.kartverket.nibas.kodelister.KodelisteService
import no.kartverket.nibas.repository.LandKodeRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer


@Configuration
class WebConfig @Autowired constructor(val configProps: NibasBackendConfigProps) : WebMvcConfigurer {

    @Bean
    fun kodelisteService(landKodeRepository: LandKodeRepository): KodelisteService {
        return DefaultKodelisteService(
            KodelisteFetcher(geoNorgeWebClient()),
            landKodeRepository)
    }

    /**
     * Overstyrer default buffer-størrelse til 10MB, ellers feiler oppslag for historiske.
     */
    @Bean
    fun geoNorgeWebClient(): WebClient {
        return WebClient.builder()
            .baseUrl(configProps.geonorgeBaseUrl)
            .codecs { it.defaultCodecs().maxInMemorySize(10 * 1024 * 1024) }
            .build()
    }
}
