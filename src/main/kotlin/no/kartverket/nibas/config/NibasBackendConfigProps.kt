package no.kartverket.nibas.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "nibas")
class NibasBackendConfigProps {

    var geonorgeBaseUrl: String = "https://register.geonorge.no/"

    override fun toString(): String {
        return "NibasBackendConfigProps(" +
                "geonorgeBaseUrl='$geonorgeBaseUrl'" +
                ")"
    }

}
