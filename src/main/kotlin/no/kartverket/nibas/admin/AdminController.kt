package no.kartverket.nibas.admin

import no.kartverket.nibas.kodelister.KodelisteService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RestController

// TODO: Denne controller bør på sikt ha andre rolletilganger enn øvrige
@RestController
class AdminController @Autowired constructor(private val kodelisteService: KodelisteService) : AdminApi {

    override fun invalidateKodelisteCache() {
        kodelisteService.clearCachedeKodelister()
    }

}
