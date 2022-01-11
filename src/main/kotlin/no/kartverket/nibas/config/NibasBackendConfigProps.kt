package no.kartverket.nibas.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties(prefix = "nibas")
data class NibasBackendConfigProps(
    var geonorgeBaseUrl: String = "https://register.geonorge.no/",
    var httpProxy: HttpProxyConfig = HttpProxyConfig(enabled = false)
)

data class HttpProxyConfig(
    val enabled: Boolean = false,
    val host: String = "",
    val port: Int = 0
)