package no.kartverket.nibas.inndelinger

import no.kartverket.nibas.api.v1.common.geojson.Feature
import no.kartverket.nibas.domain.AdministrativEnhet
import no.kartverket.nibas.domain.Fylke
import no.kartverket.nibas.domain.Kommune
import no.kartverket.nibas.domain.Nasjon
import no.kartverket.nibas.repository.FylkeRepository
import no.kartverket.nibas.repository.KommuneRepository
import no.kartverket.nibas.repository.NasjonRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.logging.Logger

@Service
class FlateService @Autowired constructor(
    private val kommuneRepository: KommuneRepository,
    private val fylkeRepository: FylkeRepository,
    private val nasjonRepository: NasjonRepository
) {

    val logger = Logger.getLogger(this::class.qualifiedName)

    fun beregnFlaterFor(features: List<Feature>) {
        val enheterSomSkalFlatedannes: Set<AdministrativEnhet> = features.flatMap {
            when (GrenseType.valueOf(it.properties.type)) {
                GrenseType.Kommunegrense -> finnEnheterMedKommunegrense(it.id!!)
                GrenseType.Fylkesgrense -> finnEnheterMedFylkesgrense(it.id!!)
                GrenseType.Riksgrense -> finnEnheterMedRiksgrense(it.id!!)
                GrenseType.AvtaltAvgrensningslinje -> finnEnheterMedAvtaltAvgrensningslinje(it.id!!)
                GrenseType.Territorialgrense -> finnEnheterMedTerritorialgrense(it.id!!)
            }
        }.toSet()

        enheterSomSkalFlatedannes.forEach { enhet ->
            logger.info(enhet.administrativenhetnavn.map { it.navn }.toString())
            enhet.oppdaterFlate()
            when(enhet) {
                is Kommune -> kommuneRepository.save(enhet)
                is Fylke -> fylkeRepository.save(enhet)
                is Nasjon -> nasjonRepository.save(enhet)
            }
        }
    }

    private fun finnEnheterMedKommunegrense(kommunegrenseId: String): Set<AdministrativEnhet> =
        HashSet(kommuneRepository.findByKommunegrenser_Kommunegrense_Id(kommunegrenseId))

    private fun finnEnheterMedFylkesgrense(fylkesgrenseId: String): Set<AdministrativEnhet> = HashSet(
        kommuneRepository.findByFylkesgrenser_Fylkesgrense_Id(fylkesgrenseId) +
                fylkeRepository.findByFylkesgrenser_Fylkesgrense_Id(fylkesgrenseId))

    private fun finnEnheterMedTerritorialgrense(id: String): Set<AdministrativEnhet> = HashSet(
        kommuneRepository.findByTerritorialgrenser_Territorialgrense_Id(id) +
                fylkeRepository.findByTerritorialgrenser_Territorialgrense_Id(id) +
                nasjonRepository.findByTerritorialgrenser_Territorialgrense_Id(id))


    private fun finnEnheterMedAvtaltAvgrensningslinje(id: String): Set<AdministrativEnhet> = HashSet(
        kommuneRepository.findByAvtaltavgrensningslinjer_Avtaltavgrensningslinje_Id(id) +
                fylkeRepository.findByAvtaltavgrensningslinjer_Avtaltavgrensningslinje_Id(id) +
                nasjonRepository.findByAvtaltavgrensningslinjer_Avtaltavgrensningslinje_Id(id))

    private fun finnEnheterMedRiksgrense(id: String): Set<AdministrativEnhet> = HashSet(
        kommuneRepository.findByRiksgrenser_Riksgrense_Id(id) +
                fylkeRepository.findByRiksgrenser_Riksgrense_Id(id) +
                nasjonRepository.findByRiksgrenser_Riksgrense_Id(id))


}