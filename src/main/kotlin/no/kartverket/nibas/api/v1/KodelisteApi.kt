package no.kartverket.nibas.api.v1

import no.kartverket.nibas.kodelister.KodelisteItem
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping

@RequestMapping("/v1/kodeliste")
interface KodelisteApi {

    /**
     * Slår opp kommuneumre fra Geonorge, cachet. (Pr des 21 kun de som er gyldige, men kommer inn historiske her innen 2023)
     */
    @GetMapping("/kommunenumre", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun fetchKommunenumre(): List<KodelisteItem>

    /**
     * Slår opp fylkesnumre fra Geonorge, cachet. (Pr des 21 kun de som er gyldige, men kommer inn historiske her innen 2023)
     */
    @GetMapping("/fylkesnumre", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun fetchFylkesnumre(): List<KodelisteItem>

    /**
     * Slår opp målemetoder fra Geonorge, cachet. Er stridigheter i Geonorge om codevalue bør være tekstlig eller ha
     * nummerert kode, denne metoden benytter tjenesten med nummerert kodeverdi.
     */
    @GetMapping("/malemetode-koder", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun fetchMaalemetodeKoder(): List<KodelisteItem>

}
