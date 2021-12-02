package no.kartverket.nibas.api.v1

import no.kartverket.nibas.kodelister.KodelisteItem
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping

@RequestMapping("/v1/kodeliste")
interface KodelisteApi {

    @GetMapping("/kommunenumre", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun fetchGyldigeKommunenumre(): List<KodelisteItem>

    @GetMapping("/alle-kommunenumre", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun fetchAlleKommunenumre(): List<KodelisteItem>

    @GetMapping("/fylkesnumre", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun fetchGyldigeFylkesnumre(): List<KodelisteItem>

    @GetMapping("/alle-fylkesnumre", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun fetchAlleFylkesnumre(): List<KodelisteItem>

    @GetMapping("/malemetode-koder", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun fetchMalemetodeKoder(): List<KodelisteItem>

    @GetMapping("/malemetoder", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun fetchMalemetoder(): List<KodelisteItem>

}
