package no.kartverket.nibas.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties(prefix = "nibas")
data class NibasBackendConfigProps(
    var geonorgeBaseUrl: String = "https://register.geonorge.no/",
)
