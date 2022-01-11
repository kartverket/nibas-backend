package no.kartverket.nibas.config

import no.kartverket.nibas.kodelister.DefaultKodelisteService
import no.kartverket.nibas.kodelister.KodelisteFetcher
import no.kartverket.nibas.kodelister.KodelisteService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.format.FormatterRegistry
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import reactor.netty.http.client.HttpClient
import reactor.netty.transport.ProxyProvider
import reactor.netty.transport.ProxyProvider.TypeSpec

@Configuration
class WebConfig @Autowired constructor(val configProps: NibasBackendConfigProps) : WebMvcConfigurer {
    override fun addFormatters(registry: FormatterRegistry) {
        registry.addConverter(StringToEnumConverter())
    }

    @Bean
    fun kodelisteService(): KodelisteService {
        return DefaultKodelisteService(KodelisteFetcher(geoNorgeWebClient()))
    }

    /**
     * Overstyrer default buffer-størrelse til 10MB, ellers feiler oppslag for historiske.
     */
    @Bean
    fun geoNorgeWebClient(): WebClient {
        val webClientBuilder = WebClient.builder()
            .baseUrl(configProps.geonorgeBaseUrl)
            .codecs { it.defaultCodecs().maxInMemorySize(10 * 1024 * 1024) }

        return when (configProps.httpProxy.enabled) {
            true -> {
                val httpClient = HttpClient.create()
                    .proxy { proxy: TypeSpec ->
                        proxy.type(ProxyProvider.Proxy.HTTP)
                            .host(configProps.httpProxy.host)
                            .port(configProps.httpProxy.port)
                    }
                webClientBuilder.clientConnector(ReactorClientHttpConnector(httpClient)).build()
            }
            false -> webClientBuilder.build()
        }
    }
}
