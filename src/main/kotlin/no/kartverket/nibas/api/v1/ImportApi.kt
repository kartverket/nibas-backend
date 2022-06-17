package no.kartverket.nibas.api.v1

import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.multipart.MultipartFile

/**
 * Api uten noe autentisering. Test-api for å importere sosi-data.
 * Dette apiet og endepunktet kan fjernes når vi har på plass s3-integrasjon
 */
@RequestMapping("/v1/import")
interface ImportApi {

    // Er motstander av nullable collections, men fant ingen omvei for ikke-påkrevd parameter. Må kanskje lage wrapperobjekt.
    @PostMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    fun importer(@RequestParam file: MultipartFile,
                 @RequestParam(required = false) fylker: List<String>? = emptyList()): String
}
