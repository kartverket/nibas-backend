package no.kartverket.nibas.inndelinger

import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.api.v1.common.geojson.Feature
import no.kartverket.nibas.domain.AdministrativEnhet
import no.kartverket.nibas.domain.Flate
import no.kartverket.nibas.domain.Fylke
import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.domain.Grunnkrets
import no.kartverket.nibas.domain.Kommune
import no.kartverket.nibas.domain.Nasjon
import no.kartverket.nibas.domain.Stemmekrets
import no.kartverket.nibas.repository.FylkeRepository
import no.kartverket.nibas.repository.GrenseRepository
import no.kartverket.nibas.repository.GrunnkretsRepository
import no.kartverket.nibas.repository.KommuneRepository
import no.kartverket.nibas.repository.NasjonRepository
import no.kartverket.nibas.repository.StemmekretsRepository
import no.kartverket.nibas.unwrap
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.logging.Logger

@Service
class FlateService @Autowired constructor(
    private val kommuneRepository: KommuneRepository,
    private val fylkeRepository: FylkeRepository,
    private val nasjonRepository: NasjonRepository,
    private val grunnkretsRepository: GrunnkretsRepository,
    private val stemmekretsRepository: StemmekretsRepository,
    private val grenseRepository: GrenseRepository
) {

    val logger: Logger = Logger.getLogger(this::class.qualifiedName)

    fun beregnFlaterFor(features: List<Feature>) {
        val enheterSomSkalFlatedannes: Set<Flate> = features.flatMap {
            val grense = getGrense(it.id!!)
            when (Grensetype.fromValue(it.properties.type)) {
                Grensetype.Kommunegrense -> grense.finnFlaterOppTilKommune()
                Grensetype.Fylkesgrense -> grense.finnFlaterOppTilFylke()
                Grensetype.Riksgrense -> grense.finnFlaterOppTilNasjon()
                Grensetype.AvtaltAvgrensningslinje -> grense.finnFlaterOppTilNasjon()
                Grensetype.Territorialgrense -> grense.finnFlaterOppTilNasjon()
                Grensetype.Grunnkretsgrense -> grense.finnFlaterOppTilGrunnkrets()
                Grensetype.Delomraadegrense -> grense.finnFlaterOppTilGrunnkrets()
                Grensetype.Stemmekretsgrense -> grense.finnFlaterOppTilStemmekrets()
                else -> throw RuntimeException("Not yet implemented for grensetype: ${it.properties.type}")
            }
        }.toSet()

        enheterSomSkalFlatedannes.forEach { enhet ->
            logger.fine(finnNavn(enhet))
            enhet.oppdaterOmraade()
            when (enhet) {
                is Kommune -> kommuneRepository.save(enhet)
                is Fylke -> fylkeRepository.save(enhet)
                is Nasjon -> nasjonRepository.save(enhet)
                is Grunnkrets -> grunnkretsRepository.save(enhet)
                is Stemmekrets -> stemmekretsRepository.save(enhet)
            }
        }
    }

    private fun finnNavn(enhet: Flate): String? = when (enhet) {
        is AdministrativEnhet -> enhet.administrativenhetnavn.map { it.navn }.toString()
        is Grunnkrets -> enhet.grunnkretsnavn
        else -> "Ukjent enhet"
    }

    private fun getGrense(id: String): Grense = grenseRepository.findById(id).unwrap()!!
}
