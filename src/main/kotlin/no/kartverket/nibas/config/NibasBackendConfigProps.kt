package no.kartverket.nibas.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "nibas")
data class NibasBackendConfigProps(
    var geonorgeBaseUrl: String = "https://register.geonorge.no/",
)
