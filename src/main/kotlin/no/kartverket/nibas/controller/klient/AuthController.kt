package no.kartverket.nibas.controller.klient

import jakarta.servlet.http.HttpServletRequest
import no.kartverket.nibas.api.v1.klient.AuthApi
import no.kartverket.nibas.api.v1.klient.response.User
import org.springframework.web.bind.annotation.RestController

@RestController("KlientAuthController")
class AuthController(
    private val request: HttpServletRequest
) : AuthApi {

    override fun getUser(): User {
        val rawUsername = request.getHeader("X-Auth-Name")
        val username = String(rawUsername.toByteArray(Charsets.ISO_8859_1), Charsets.UTF_8)
        return User(username = username)
    }
}


