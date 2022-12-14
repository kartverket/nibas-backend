package no.kartverket.nibas.auth

import no.kartverket.nibas.api.v1.AuthZApi
import no.kartverket.nibas.api.v1.response.AuthZStatusResponse
import org.springframework.web.bind.annotation.RestController

@RestController
class AuthZController: AuthZApi {
    override fun getStatus(): AuthZStatusResponse = AuthZStatusResponse()

}
