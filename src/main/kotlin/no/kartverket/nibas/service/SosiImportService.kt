package no.kartverket.nibas.service

import no.kartverket.nibas.service.sosiimport.FylkeImporter
import no.kartverket.nibas.service.sosiimport.KommuneImporter
import no.kartverket.nibas.service.sosiimport.NasjonImporter
import no.kartverket.nibas.sosi.common.SosiKoder
import no.kartverket.nibas.sosi.deserializer.NibasSosiDeserializer
import no.kartverket.nibas.sosi.model.SosiObjekt
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.InputStream
import java.util.logging.Logger
import kotlin.system.measureTimeMillis

@Service
class SosiImportService {

    val logger = Logger.getLogger(this::class.java.name)

    @Autowired
    lateinit var kommuneImporter: KommuneImporter

    @Autowired
    lateinit var fylkeImporter: FylkeImporter

    @Autowired
    lateinit var nasjonImporter: NasjonImporter

    fun importStream(stream: InputStream) {
        importerFlater(NibasSosiDeserializer().deserialize(stream))
    }

    private fun importerFlater(root: SosiObjekt) {
        val flater = root.getFlater()
        logger.info("Trying to import ${flater.size} flater...")
        val millis = measureTimeMillis {
            flater.forEachIndexed{index, flate ->
                logger.info("Importing flate ${index + 1} of ${flater.size} (${flate.getObjType()})")
                when (flate.getObjType()) {
                    SosiKoder.FYLKE -> fylkeImporter.importerSosiFylke(flate, root)
                    SosiKoder.KOMMUNE -> kommuneImporter.importerSosiKommune(flate, root)
                    SosiKoder.NASJON -> nasjonImporter.importerSosiNasjon(flate, root)
                    else -> logger.info("Ingen importør funnet for ${flate.getObjType()}")
                }
            }
        }
        logger.info("Took $millis ms to import")
    }
}
