package no.kartverket.nibas.sosi.sosiimport

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import no.kartverket.nibas.domain.Avtaltavgrensningslinje
import no.kartverket.nibas.domain.Fylkesgrense
import no.kartverket.nibas.domain.GrenseKoblingAttributter
import no.kartverket.nibas.domain.Kommunegrense
import no.kartverket.nibas.domain.Riksgrense
import no.kartverket.nibas.domain.Stemmekrets
import no.kartverket.nibas.domain.StemmekretsAvtaltavgrensningslinje
import no.kartverket.nibas.domain.StemmekretsFylkesgrense
import no.kartverket.nibas.domain.StemmekretsKommunegrense
import no.kartverket.nibas.domain.StemmekretsRiksgrense
import no.kartverket.nibas.domain.StemmekretsStemmekretsgrense
import no.kartverket.nibas.domain.StemmekretsTerritorialgrense
import no.kartverket.nibas.domain.Stemmekretsgrense
import no.kartverket.nibas.domain.Territorialgrense
import no.kartverket.nibas.kodelister.KodelisteService
import no.kartverket.nibas.repository.StemmekretsRepository
import no.kartverket.nibas.sosi.model.SosiFlate
import no.kartverket.nibas.sosi.model.SosiObjekt
import no.kartverket.nibas.sosi.model.SosiReferanse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.logging.Logger

@Component
class StemmekretsImporter @Autowired constructor(private val kommunalKretsImporter: KommunalKretsImporter, private val grenseImporter: GrenseImporter, private val kommunegrenseImporter: KommunegrenseImporter, private val stemmekretsRepository: StemmekretsRepository, private val kodelisteService: KodelisteService) {

    val logger = Logger.getLogger(this::class.java.name)

    @Transactional
    fun importerSosiStemmekrets(flater: List<SosiFlate>, root: SosiObjekt) {
        // Henter egenskaper fra første flate. Antar at all metadata er lik
        val forsteFlate = flater.first()

        val lokalid = forsteFlate.getIdent()?.getLokalId()!!

        val stemmekrets = kommunalKretsImporter.importer(forsteFlate) as Stemmekrets
        stemmekrets.stemmekretsnummer = forsteFlate.getStemmekretsId()?.getStemmekretsnummer()
        stemmekrets.stemmekretsnavn = forsteFlate.getStemmekretsId()?.getStemmekretsnavn()
        stemmekrets.tellekretsnummer = forsteFlate.getStemmekretsId()?.getTellekretsnummer()
        stemmekrets.tellekretsnavn = forsteFlate.getStemmekretsId()?.getTellekretsnavn()
        stemmekrets.valgdistriktsnummer = forsteFlate.getStemmekretsId()?.getValgdistriktsnummer()
        stemmekrets.fylkesnummer = forsteFlate.getFylkesnummer()
        stemmekrets.kommunenummer = kodelisteService.getKommunenummerIdByCodeValue(forsteFlate.getKommunenummer()
            ?: throw RuntimeException("Kommunenummer ikke satt i modellen for kommune med flate-id $lokalid"))

        runBlocking {
            withContext(Dispatchers.Default) {
                flater.forEachIndexed { skall_index, flate ->
                    val skall: List<Job> = flate.getSkallReferanser().mapIndexed { rekkefoelge, ref ->
                        launch {
                            importerStemmekretsRef(stemmekrets, ref, root, skall_index, rekkefoelge, null)
                        }
                    }

                    val hull: List<List<Job>> = flate.getHullReferanser().mapIndexed { hullIndex, liste ->
                        liste.mapIndexed { rekkefoelge, ref ->
                            launch {
                                importerStemmekretsRef(stemmekrets, ref, root, skall_index, rekkefoelge, hullIndex)
                            }
                        }
                    }

                    skall.joinAll()
                    hull.forEach { it.joinAll() }
                }
            }
        }

        stemmekrets.oppdaterFlate()
        stemmekretsRepository.save(stemmekrets)
    }

    fun importerStemmekretsRef(stemmekrets: Stemmekrets, ref: SosiReferanse, root: SosiObjekt, flateIndex: Int, rekkefoelge: Int, hullIndex: Int? = null) {
        val referanse = ref.getVerdiUtenRetningOgParentes()

        when (val grenseRef = root.getObjektMedId(referanse)) {
            null -> throw RuntimeException("Finner ikke grense som refereres til. Feil i datagrunnlaget. Ref: $referanse")
            else -> {
                val importertGrense = grenseImporter.importerGrense(grenseRef)
                val attributter = GrenseKoblingAttributter(ref.erRetningMedKlokken(), rekkefoelge, flateIndex, hullIndex)
                when (importertGrense) {
                    is Stemmekretsgrense -> stemmekrets.stemmekretsgrenser.add(StemmekretsStemmekretsgrense(stemmekrets, importertGrense, attributter.retningMedKlokken, attributter.rekkefoelge, attributter.flateIndeks, attributter.hullIndeks))
                    is Kommunegrense -> stemmekrets.kommunegrenser.add(StemmekretsKommunegrense(stemmekrets, importertGrense, attributter))
                    is Fylkesgrense -> stemmekrets.fylkesgrenser.add(StemmekretsFylkesgrense(stemmekrets, importertGrense, attributter))
                    is Avtaltavgrensningslinje -> stemmekrets.avtaltavgrensningslinjer.add(StemmekretsAvtaltavgrensningslinje(stemmekrets, importertGrense, attributter))
                    is Territorialgrense -> stemmekrets.territorialgrenser.add(StemmekretsTerritorialgrense(stemmekrets, importertGrense, attributter))
                    is Riksgrense -> stemmekrets.riksgrenser.add(StemmekretsRiksgrense(stemmekrets, importertGrense, attributter))
                }

            }
        }
    }
}
