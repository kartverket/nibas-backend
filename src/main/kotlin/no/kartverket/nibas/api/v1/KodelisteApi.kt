package no.kartverket.nibas.api.v1

import no.kartverket.nibas.kodelister.KodelisteRespons
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping

const val KODELISTE_BASE_URL = "/v1/kodeliste"
const val MAALEMETODE_KODE_PATH = "/maalemetode-koder"

@RequestMapping(KODELISTE_BASE_URL)
interface KodelisteApi {

    /**
     * Slår opp målemetoder fra Geonorge, cachet. Er stridigheter i Geonorge om codevalue bør være tekstlig eller ha
     * nummerert kode, denne metoden benytter tjenesten med nummerert kodeverdi.
     */
    @GetMapping(MAALEMETODE_KODE_PATH, produces = [MediaType.APPLICATION_JSON_VALUE])
    fun fetchMaalemetodeKoder(): KodelisteRespons

}
