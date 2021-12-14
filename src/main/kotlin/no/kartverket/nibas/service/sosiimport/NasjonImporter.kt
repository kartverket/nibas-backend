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
class NasjonImporter {

    val logger = Logger.getLogger(this::class.java.name)

    @Autowired
    lateinit var nasjonRepository: NasjonRepository

    @Autowired
    lateinit var nasjonInformasjonRepository: NasjonInformasjonRepository

    @Autowired
    lateinit var spraakkodeRepository: SpraakkodeRepository

    @Autowired
    lateinit var administrativenhetnavnRepository: AdministrativenhetnavnRepository

    @Autowired
    lateinit var grenseImporter: GrenseImporter

    @Autowired
    lateinit var administrativEnhetImporter: AdministrativEnhetImporter

    @Transactional
    fun importerSosiNasjon(flate: SosiFlate, root: SosiObjekt) {
        when(nasjonRepository.findByLokalid(flate.getIdent()?.getLokalId()!!)) {
            null -> {
                var nasjon = administrativEnhetImporter.importer(flate) as Nasjon

                val skall: List<LineString> = flate.getSkallRef().map { ref -> importerNasjonRef(ref, nasjon, root) }
                val hull: List<List<LineString>> =
                    flate.getHullRef().map { liste -> liste.map { ref -> importerNasjonRef(ref, nasjon, root) } }

                nasjon.flate = SosiGeometryFunctions.lagFlate(skall, hull, flate.koordsys)
                nasjon = nasjonRepository.save(nasjon)

                if (flate.getInformasjon() != null) {
                    val nasjonInformasjon = NasjonInformasjon()
                    nasjonInformasjon.informasjon = flate.getInformasjon()
                    nasjonInformasjon.nasjonFk = nasjon
                    nasjonInformasjonRepository.save(nasjonInformasjon)
                }

                flate.getNasjonnavn().forEach { sosiNasjonnavn ->
                    val nasjonnavn = Administrativenhetnavn()
                    nasjonnavn.navn = sosiNasjonnavn.getNavn()
                    nasjonnavn.spraak = sosiNasjonnavn.getSprak()?.let { spraakkodeRepository.findByIdOrNull(it) }
                    nasjonnavn.rekkefoelge = sosiNasjonnavn.getRekkefolge()
                    nasjonnavn.nasjonFk = nasjon
                    administrativenhetnavnRepository.save(nasjonnavn)
                }
            }
            else -> {}
        }
    }

    private fun importerNasjonRef(ref: SosiRef, nasjon: Nasjon, root: SosiObjekt): LineString {
        val referanse = ref.getVerdiUtenRetningOgParentes()
        when (val grenseRef = root.getObjektMedId(referanse)) {
            null -> throw RuntimeException("Finner ikke grense som refereres til. Feil i datagrunnlaget. Ref: $referanse")
            else -> {
                val importertGrense = grenseImporter.importerGrense(grenseRef)
                when (importertGrense) {
                    is Avtaltavgrensningslinje -> nasjon.avtaltavgrensningslinjer.add(importertGrense)
                    is Territorialgrense -> nasjon.territorialgrenser.add(importertGrense)
                    is Riksgrense -> nasjon.riksgrenser.add(importertGrense)
                }
                return when (ref.isRetningReversed()) {
                    true -> importertGrense.grense!!.reverse()
                    false -> importertGrense.grense!!
                }
            }
        }
    }
}