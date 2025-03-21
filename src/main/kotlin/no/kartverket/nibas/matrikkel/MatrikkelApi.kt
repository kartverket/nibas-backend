package no.kartverket.nibas.matrikkel

import jakarta.xml.ws.BindingProvider
import jakarta.xml.ws.handler.MessageContext
import no.statkart.matrikkel.matrikkelapi.wsapi.v1.domain.MatrikkelContext
import no.statkart.matrikkel.matrikkelapi.wsapi.v1.domain.geometri.koder.KoordinatsystemKodeId
import no.statkart.matrikkel.matrikkelapi.wsapi.v1.service.kodeliste.KodelisteService
import no.statkart.matrikkel.matrikkelapi.wsapi.v1.service.kodeliste.KodelisteServiceWS
import no.statkart.matrikkel.matrikkelapi.wsapi.v1.service.store.StoreService
import no.statkart.matrikkel.matrikkelapi.wsapi.v1.service.store.StoreServiceWS
import org.slf4j.MDC
import java.net.URI

data class MatrikkelApiConfig(
    val useStub: Boolean = false,
    val baseUrl: String,
    val username: String,
    val password: String,
)

class MatrikkelApi(private val baseUrl: URI) {
    private val storeServiceFactory = StoreServiceWS()
    private val kodelisteServiceFactory = KodelisteServiceWS()

    val matrikkelContext = MatrikkelContext().apply {
        systemVersion = "4.18"
        locale = "no_NO"
        klientIdentifikasjon = "nibas"
        koordinatsystemKodeId = KoordinatsystemKodeId().apply { value = 11 } //Euref 33
        isBrukOriginaleKoordinater = false
    }

    fun withAuth(username: String, password: String): WithAuth {
        return WithAuthImpl(BasicAuthAuthenticator(username, password))
    }

    fun withAuth(bearerToken: String): WithAuth {
        return WithAuthImpl(BearerTokenAuthenticator(bearerToken))
    }

    interface WithAuth {
        fun storeService(): StoreService
        fun kodelisteService(): KodelisteService
        val matrikkelContext: MatrikkelContext
    }

    private inner class WithAuthImpl(private val authenticator: Authenticator) : WithAuth {
        //Kopiert fra matrikkel-bygning-egenregistrering
        private fun BindingProvider.configure(endpointPath: String) {
            requestContext[BindingProvider.ENDPOINT_ADDRESS_PROPERTY] = baseUrl.resolve(endpointPath).toString()
            MDC.get("request_id")?.let {
                (requestContext.computeIfAbsent(MessageContext.HTTP_REQUEST_HEADERS) { HashMap<String, MutableList<String>>() } as HashMap<String, MutableList<String>>)
                    .computeIfAbsent("X-Request-ID") { ArrayList() }
                    .add(it)
            }
            authenticator(requestContext)
        }

        override fun storeService(): StoreService {
            @Suppress("UsePropertyAccessSyntax") // getStoreServicePort er ikke en getter for en property
            val storeService = storeServiceFactory.getStoreServicePort()
            (storeService as BindingProvider).configure("/matrikkelapi/wsapi/v1/StoreServiceWS")
            return storeService
        }

        override fun kodelisteService(): KodelisteService {
            @Suppress("UsePropertyAccessSyntax") // getKodelisteServicePort er ikke en getter for en property
            val kodelisteService = kodelisteServiceFactory.getKodelisteServicePort()
            (kodelisteService as BindingProvider).configure("/matrikkelapi/wsapi/v1/KodelisteServiceWS")
            return kodelisteService
        }

        override val matrikkelContext: MatrikkelContext
            get() = this@MatrikkelApi.matrikkelContext
    }

    private interface Authenticator {
        operator fun invoke(requestContext: MutableMap<String, Any?>)
    }

    private class BasicAuthAuthenticator(private val username: String, private val password: String) :
        Authenticator {
        override fun invoke(requestContext: MutableMap<String, Any?>) {
            requestContext[BindingProvider.USERNAME_PROPERTY] = username
            requestContext[BindingProvider.PASSWORD_PROPERTY] = password
        }
    }

    private class BearerTokenAuthenticator(private val bearerToken: String) : Authenticator {
        override fun invoke(requestContext: MutableMap<String, Any?>) {
            requestContext[MessageContext.HTTP_REQUEST_HEADERS] = mapOf(
                "Authorization" to listOf("Bearer $bearerToken"),
            )
        }
    }
}
