package no.kartverket.nibas.config

import no.kartverket.nibas.kodelister.DefaultKodelisteService
import no.kartverket.nibas.kodelister.KodelisteFetcher
import no.kartverket.nibas.kodelister.KodelisteService
import no.kartverket.nibas.repository.FastsettingstypeRepository
import no.kartverket.nibas.repository.GrensestatusRepository
import no.kartverket.nibas.repository.LandKodeRepository
import no.kartverket.nibas.repository.NoeyaktighetsklassekodeRepository
import no.kartverket.nibas.repository.TerrengdetaljkodeRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.format.FormatterRegistry
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfig @Autowired constructor(val configProps: NibasBackendConfigProps) : WebMvcConfigurer {

    override fun addFormatters(registry: FormatterRegistry) {
        registry.addConverter(StringToEnumConverter())
    }

    @Bean
    fun kodelisteService(landKodeRepository: LandKodeRepository,
                         terrengdetaljkodeRepository: TerrengdetaljkodeRepository,
                         noeyaktighetsklassekodeRepository: NoeyaktighetsklassekodeRepository,
                         fastsettingstypeRepository: FastsettingstypeRepository,
                         grensestatusRepository: GrensestatusRepository): KodelisteService {
        return DefaultKodelisteService(
            KodelisteFetcher(geoNorgeWebClient()),
            landKodeRepository,
            terrengdetaljkodeRepository,
            noeyaktighetsklassekodeRepository,
            fastsettingstypeRepository,
            grensestatusRepository)
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
