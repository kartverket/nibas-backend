package no.kartverket.nibas.auth

import no.kartverket.nibas.api.v1.klient.AuthZApi
import no.kartverket.nibas.api.v1.klient.response.AuthZStatusResponse
import org.springframework.web.bind.annotation.RestController

@RestController
class AuthZController: AuthZApi {
    override fun getStatus(): AuthZStatusResponse = AuthZStatusResponse()

}
