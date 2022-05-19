package no.kartverket.nibas.inndelinger

import no.kartverket.nibas.api.v1.common.geojson.Feature
import no.kartverket.nibas.domain.AdministrativEnhet
import no.kartverket.nibas.domain.Flate
import no.kartverket.nibas.domain.Fylke
import no.kartverket.nibas.domain.Grunnkrets
import no.kartverket.nibas.domain.Kommune
import no.kartverket.nibas.domain.Nasjon
import no.kartverket.nibas.repository.FylkeRepository
import no.kartverket.nibas.repository.GrunnkretsRepository
import no.kartverket.nibas.repository.KommuneRepository
import no.kartverket.nibas.repository.NasjonRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.logging.Logger

@Service
class FlateService @Autowired constructor(
    private val kommuneRepository: KommuneRepository,
    private val fylkeRepository: FylkeRepository,
    private val nasjonRepository: NasjonRepository,
    private val grunnkretsRepository: GrunnkretsRepository,
) {

    val logger = Logger.getLogger(this::class.qualifiedName)

    fun beregnFlaterFor(features: List<Feature>) {
        val enheterSomSkalFlatedannes: Set<Flate> = features.flatMap {
            val type = it.properties.type
            when (GrenseType.fromValue(type)) {
                GrenseType.Kommunegrense -> finnEnheterMedKommunegrense(it.id!!)
                GrenseType.Fylkesgrense -> finnEnheterMedFylkesgrense(it.id!!)
                GrenseType.Riksgrense -> finnEnheterMedRiksgrense(it.id!!)
                GrenseType.AvtaltAvgrensningslinje -> finnEnheterMedAvtaltAvgrensningslinje(it.id!!)
                GrenseType.Territorialgrense -> finnEnheterMedTerritorialgrense(it.id!!)
                GrenseType.Grunnkretsgrense -> finnEnheterMedGrunnkretsgrense(it.id!!)
                GrenseType.Delomraadegrense -> finnEnheterMedDelomraadegrense(it.id!!)
                else -> throw RuntimeException("Not yet implemented for grensetype: $type")
            }
        }.toSet()

        enheterSomSkalFlatedannes.forEach { enhet ->
            logger.fine(finnNavn(enhet))
            enhet.oppdaterFlate()
            when(enhet) {
                is Kommune -> kommuneRepository.save(enhet)
                is Fylke -> fylkeRepository.save(enhet)
                is Nasjon -> nasjonRepository.save(enhet)
                is Grunnkrets -> grunnkretsRepository.save(enhet)
            }
        }
    }

    private fun finnNavn(enhet: Flate): String? = when(enhet) {
        is AdministrativEnhet -> enhet.administrativenhetnavn.map { it.navn }.toString()
        is Grunnkrets -> enhet.grunnkretsnavn
        else -> "Ukjent enhet"
    }

    private fun finnEnheterMedKommunegrense(kommunegrenseId: String): Set<Flate> = HashSet(
        grunnkretsRepository.findByKommunegrenser_Kommunegrense_Id(kommunegrenseId) +
                kommuneRepository.findByKommunegrenser_Kommunegrense_Id(kommunegrenseId))

    private fun finnEnheterMedFylkesgrense(fylkesgrenseId: String): Set<Flate> = HashSet(
        grunnkretsRepository.findByFylkesgrenser_Fylkesgrense_Id(fylkesgrenseId) +
                kommuneRepository.findByFylkesgrenser_Fylkesgrense_Id(fylkesgrenseId) +
                fylkeRepository.findByFylkesgrenser_Fylkesgrense_Id(fylkesgrenseId))

    private fun finnEnheterMedTerritorialgrense(id: String): Set<Flate> = HashSet(
        grunnkretsRepository.findByTerritorialgrenser_Territorialgrense_Id(id) +
                kommuneRepository.findByTerritorialgrenser_Territorialgrense_Id(id) +
                fylkeRepository.findByTerritorialgrenser_Territorialgrense_Id(id) +
                nasjonRepository.findByTerritorialgrenser_Territorialgrense_Id(id))


    private fun finnEnheterMedAvtaltAvgrensningslinje(id: String): Set<Flate> = HashSet(
        grunnkretsRepository.findByAvtaltavgrensningslinjer_Avtaltavgrensningslinje_Id(id) +
                kommuneRepository.findByAvtaltavgrensningslinjer_Avtaltavgrensningslinje_Id(id) +
                fylkeRepository.findByAvtaltavgrensningslinjer_Avtaltavgrensningslinje_Id(id) +
                nasjonRepository.findByAvtaltavgrensningslinjer_Avtaltavgrensningslinje_Id(id))

    private fun finnEnheterMedRiksgrense(id: String): Set<Flate> = HashSet(
        grunnkretsRepository.findByRiksgrenser_Riksgrense_Id(id) +
                kommuneRepository.findByRiksgrenser_Riksgrense_Id(id) +
                fylkeRepository.findByRiksgrenser_Riksgrense_Id(id) +
                nasjonRepository.findByRiksgrenser_Riksgrense_Id(id))

    private fun finnEnheterMedGrunnkretsgrense(id: String): Set<Flate> = HashSet(
        grunnkretsRepository.findByGrunnkretsgrenser_Grunnkretsgrense_Id(id))

    private fun finnEnheterMedDelomraadegrense(id: String): Set<Flate> = HashSet(
        grunnkretsRepository.findByDelomraadegrenser_Delomraadegrense_Id(id))

}
