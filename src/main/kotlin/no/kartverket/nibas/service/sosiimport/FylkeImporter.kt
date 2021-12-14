package no.kartverket.nibas.service.sosiimport

import no.kartverket.nibas.domain.*
import no.kartverket.nibas.repository.*
import no.kartverket.nibas.sosi.model.SosiFlate
import no.kartverket.nibas.sosi.model.SosiObjekt
import no.kartverket.nibas.sosi.model.SosiRef
import org.locationtech.jts.geom.LineString
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.logging.Logger

@Component
class FylkeImporter {

    val logger = Logger.getLogger(this::class.java.name)

    @Autowired
    lateinit var fylkesnummerRepository: FylkesnummerRepository

    @Autowired
    lateinit var fylkeRepository: FylkeRepository

    @Autowired
    lateinit var fylkeInformasjonRepository: FylkeInformasjonRepository

    @Autowired
    lateinit var administrativenhetnavnRepository: AdministrativenhetnavnRepository

    @Autowired
    lateinit var spraakkodeRepository: SpraakkodeRepository

    @Autowired
    lateinit var grenseImporter: GrenseImporter

    @Autowired
    lateinit var administrativEnhetImporter: AdministrativEnhetImporter

    @Transactional
    fun importerSosiFylke(flate: SosiFlate, root: SosiObjekt) {
        when (fylkeRepository.findByLokalid(flate.getIdent()?.getLokalId()!!)) {
            null -> {
                var fylke = administrativEnhetImporter.importer(flate) as Fylke

                fylke.fylkesnummer = flate.getFylkesnummer()?.let { fylkesnummerRepository.findByIdOrNull(it) }
                fylke.samiskforvaltningsomraade = flate.isSamiskForvaltningsomrade()

                val skall: List<LineString> = flate.getSkallRef().map { ref -> importerFylkeRef(ref, fylke, root) }
                val hull: List<List<LineString>> =
                    flate.getHullRef().map { liste -> liste.map { ref -> importerFylkeRef(ref, fylke, root) } }

                fylke.flate = SosiGeometryFunctions.lagFlate(skall, hull, flate.koordsys)
                fylke = fylkeRepository.save(fylke)

                if (flate.getInformasjon() != null) {
                    val fylkeInformasjon = FylkeInformasjon()
                    fylkeInformasjon.informasjon = flate.getInformasjon()
                    fylkeInformasjon.fylkeFk = fylke
                    fylkeInformasjonRepository.save(fylkeInformasjon)
                }

                flate.getFylkesnavn().forEach { sosiFylkesnavn ->
                    val fylkesnavn = Administrativenhetnavn()
                    fylkesnavn.navn = sosiFylkesnavn.getNavn()
                    fylkesnavn.spraak = sosiFylkesnavn.getSprak()?.let { spraakkodeRepository.findByIdOrNull(it) }
                    fylkesnavn.rekkefoelge = sosiFylkesnavn.getRekkefolge()
                    fylkesnavn.fylkeFk = fylke
                    administrativenhetnavnRepository.save(fylkesnavn)
                }
            }
            else -> {}
        }
    }

    private fun importerFylkeRef(ref: SosiRef, fylke: Fylke, root: SosiObjekt): LineString {
        val referanse = ref.getVerdiUtenRetningOgParentes()
        when (val grenseRef = root.getObjektMedId(referanse)) {
            null -> throw RuntimeException("Finner ikke grense som refereres til. Feil i datagrunnlaget. Ref: $referanse")
            else -> {
                val importertGrense = grenseImporter.importerGrense(grenseRef)
                when (importertGrense) {
                    is Fylkesgrense -> fylke.fylkesgrenser.add(importertGrense)
                    is Avtaltavgrensningslinje -> fylke.avtaltavgrensningslinjer.add(importertGrense)
                    is Territorialgrense -> fylke.territorialgrenser.add(importertGrense)
                    is Riksgrense -> fylke.riksgrenser.add(importertGrense)
                }
                return when (ref.isRetningReversed()) {
                    true -> importertGrense.grense!!.reverse()
                    false -> importertGrense.grense!!
                }
            }
        }
    }
}