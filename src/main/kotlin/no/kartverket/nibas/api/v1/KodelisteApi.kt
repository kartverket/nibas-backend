package no.kartverket.nibas.api.v1

import no.kartverket.nibas.kodelister.KodelisteRespons
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping

const val KODELISTE_BASE_URL = "/v1/kodeliste"
const val KOMMUNENUMMER_PATH = "/kommunenumre"
const val FYLKESNUMMER_PATH = "/fylkesnumre"
const val MAALEMETODE_KODE_PATH = "/maalemetode-koder"
const val TERRENGDETALJKODE_PATH = "/terrengdetaljkoder"
const val LANDKODE_PATH = "/landkoder"
const val NOEYAKTIGHETSKLASSEKODE_PATH = "/noeyaktighetsklasser"
const val GRENSETYPE_PATH = "/grensetyper"
const val FASTSETTINGSTYPE_PATH = "/fastsettingstyper"
const val GRENSESTATUSKODE_PATH = "/grensestatuser"

@RequestMapping(KODELISTE_BASE_URL)
interface KodelisteApi {

    /**
     * Slår opp kommuneumre fra Geonorge, cachet. (Pr des 21 kun de som er gyldige, men kommer inn historiske her innen 2023)
     */
    @GetMapping(KOMMUNENUMMER_PATH, produces = [MediaType.APPLICATION_JSON_VALUE])
    fun fetchKommunenumre(): KodelisteRespons

    /**
     * Slår opp fylkesnumre fra Geonorge, cachet. (Pr des 21 kun de som er gyldige, men kommer inn historiske her innen 2023)
     */
    @GetMapping(FYLKESNUMMER_PATH, produces = [MediaType.APPLICATION_JSON_VALUE])
    fun fetchFylkesnumre(): KodelisteRespons

    /**
     * Slår opp målemetoder fra Geonorge, cachet. Er stridigheter i Geonorge om codevalue bør være tekstlig eller ha
     * nummerert kode, denne metoden benytter tjenesten med nummerert kodeverdi.
     */
    @GetMapping(MAALEMETODE_KODE_PATH, produces = [MediaType.APPLICATION_JSON_VALUE])
    fun fetchMaalemetodeKoder(): KodelisteRespons

    /**
     * Slår opp internt forvaltet landkoder.
     */
    @GetMapping(LANDKODE_PATH, produces = [MediaType.APPLICATION_JSON_VALUE])
    fun fetchLandkoder(): KodelisteRespons

    /**
     * Slår opp internt forvaltet terrengdetaljkoder.
     */
    @GetMapping(TERRENGDETALJKODE_PATH, produces = [MediaType.APPLICATION_JSON_VALUE])
    fun fetchTerrengdetaljkoder(): KodelisteRespons

    /**
     * Slår opp internt forvaltet nøyaktighetsklasser.
     */
    @GetMapping(NOEYAKTIGHETSKLASSEKODE_PATH, produces = [MediaType.APPLICATION_JSON_VALUE])
    fun fetchNoeyaktighetsklasser(): KodelisteRespons

    /**
     * Slår opp internt forvaltet grensetyper.
     */
    @GetMapping(GRENSETYPE_PATH, produces = [MediaType.APPLICATION_JSON_VALUE])
    fun fetchGrensetyper(): KodelisteRespons

    /**
     * Slår opp internt forvaltet fastsettingstyper.
     */
    @GetMapping(FASTSETTINGSTYPE_PATH, produces = [MediaType.APPLICATION_JSON_VALUE])
    fun fetchFastsettingstyper(): KodelisteRespons

    /**
     * Slår opp internt forvaltet grensestatuskoder.
     */
    @GetMapping(GRENSESTATUSKODE_PATH, produces = [MediaType.APPLICATION_JSON_VALUE])
    fun fetchGrensestatuskoder(): KodelisteRespons
}
