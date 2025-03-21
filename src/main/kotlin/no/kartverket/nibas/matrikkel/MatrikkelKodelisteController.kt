package no.kartverket.nibas.matrikkel

import no.kartverket.nibas.api.v1.MatrikkelKodelisteApi
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RestController

@RestController
class MatrikkelKodelisteController @Autowired constructor(private val kodelisteService: MatrikkelKodelisteService) : MatrikkelKodelisteApi {

    override fun fetchMatrikkelKodelister(): MatrikkelKodelisterRespons {
        return kodelisteService.fetchMatrikkelKodelister()
    }
}

