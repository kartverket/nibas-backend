package no.kartverket.nibas.controller

import no.kartverket.nibas.api.v1.ImportApi
import no.kartverket.nibas.service.SosiImportService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

/**
 * Api uten noe autentisering. Test-api for å importere sosi-data.
 * Dette apiet og endepunktet kan fjernes når vi har på plass s3-integrasjon
 */
@RestController
class ImportController : ImportApi {

    @Autowired
    lateinit var importService: SosiImportService

    override fun importer(file: MultipartFile): String {
        importService.importStream(file.inputStream)
        return "OK"
    }
}