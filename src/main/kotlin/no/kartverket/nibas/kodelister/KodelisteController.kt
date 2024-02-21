package no.kartverket.nibas.kodelister

import no.kartverket.nibas.api.v1.KodelisteApi
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RestController

@RestController
class KodelisteController @Autowired constructor(private val kodelisteService: KodelisteService) : KodelisteApi {

    override fun fetchMaalemetodeKoder(): KodelisteRespons {
        return kodelisteService.getMaalemetodeKoder()
    }
}

