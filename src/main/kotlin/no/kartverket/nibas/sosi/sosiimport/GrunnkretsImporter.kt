package no.kartverket.nibas.sosi.sosiimport

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import no.kartverket.nibas.domain.Avtaltavgrensningslinje
import no.kartverket.nibas.domain.Delomraadegrense
import no.kartverket.nibas.domain.Fylkesgrense
import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.domain.GrenseKoblingAttributter
import no.kartverket.nibas.domain.Grunnkrets
import no.kartverket.nibas.domain.GrunnkretsAvtaltavgrensningslinje
import no.kartverket.nibas.domain.GrunnkretsDelomraadegrense
import no.kartverket.nibas.domain.GrunnkretsFylkesgrense
import no.kartverket.nibas.domain.GrunnkretsGrunnkretsgrense
import no.kartverket.nibas.domain.GrunnkretsKommunegrense
import no.kartverket.nibas.domain.GrunnkretsRiksgrense
import no.kartverket.nibas.domain.GrunnkretsTerritorialgrense
import no.kartverket.nibas.domain.Grunnkretsgrense
import no.kartverket.nibas.domain.Kommunegrense
import no.kartverket.nibas.domain.Riksgrense
import no.kartverket.nibas.domain.Territorialgrense
import no.kartverket.nibas.domain.updateSosiIdentifikasjon
import no.kartverket.nibas.kodelister.KodelisteService
import no.kartverket.nibas.repository.GrunnkretsRepository
import no.kartverket.nibas.repository.KommuneRepository
import no.kartverket.nibas.repository.TypeendringRepository
import no.kartverket.nibas.sosi.model.SosiFlate
import no.kartverket.nibas.sosi.model.SosiKurve
import no.kartverket.nibas.sosi.model.SosiReferanse
import org.locationtech.jts.geom.LineString
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.logging.Logger
import kotlin.math.min
import kotlin.system.measureTimeMillis

@Component
class GrunnkretsImporter @Autowired constructor(
    private val grenseImporter: GrenseImporter,
    private val grunnkretsRepository: GrunnkretsRepository,
    private val typeendringRepository: TypeendringRepository,
    private val kodelisteService: KodelisteService,
    private val kommuneRepository: KommuneRepository
) {

    val logger: Logger = Logger.getLogger(this::class.java.name)

    @Transactional
    fun importerSosiGrunnkrets(flater: List<SosiFlate>, kurver: List<SosiKurve>) {

        // Henter egenskapene bare fra første flate. Egenskapene er/skal være like på tvers av flatene.
        val flate = flater.first()
        val sosiIdent = flate.getIdent()
        if (grunnkretsRepository.findByLokalid(sosiIdent?.getLokalId()!!) != null) {
            logger.info("Bruker eksisterende flate")
        } else {
            val grunnkrets = Grunnkrets()

            grunnkrets.updateSosiIdentifikasjon(sosiIdent)
            grunnkrets.datafangstdato = flate.getDatafangstDato()
            grunnkrets.datauttaksdato = flate.getDataUttaksdato()
            grunnkrets.endringsflagg?.tidspunktendring = flate.getEndringsflagg()?.getTidspunktEndring()
            grunnkrets.endringsflagg?.typeendring = when (val typeendring = flate.getEndringsflagg()?.getTypeEndring()) {
                null -> null
                else -> typeendringRepository.findByIdOrNull(typeendring)
            }
            grunnkrets.gyldigFra = flate.getGyldigFra()
            grunnkrets.gyldigTil = flate.getGyldigTil()
            grunnkrets.informasjon = flate.getInformasjon()
            grunnkrets.kopidata?.kopidato = flate.getKopidata()?.getKopidato()
            grunnkrets.kopidata?.omraadeid = flate.getKopidata()?.getOmraadeId()
            grunnkrets.kopidata?.originaldatavert = flate.getKopidata()?.getOriginalDatavert()
            grunnkrets.opphav = flate.getOpphav()

            grunnkrets.registreringsversjon?.produkt = flate.getRegistreringsversjon()?.getProdukt()
            grunnkrets.registreringsversjon?.versjon = flate.getRegistreringsversjon()?.getVersjon()

            grunnkrets.sluttdato = flate.getSluttdato()

            grunnkrets.grunnkretsnummer = flate.getGrunnkretsId()?.getGrunnkretsnummer()
            grunnkrets.grunnkretsnavn = flate.getGrunnkretsId()?.getGrunnkretsnavn()

            grunnkrets.posisjon = SosiGeometryFunctions.sosiKoordinatToPoint(flate.getAlleSosiNordOest(), flate.koordsys)

            runBlocking {
                withContext(Dispatchers.Default) {
                    flater.forEachIndexed { skall_index, flate ->
                        val skall: List<Job> = flate.getSkallReferanser().mapIndexed { rekkefoelge, ref ->
                            launch {
                                importerGrunnkretsRef(grunnkrets, ref, kurver, skall_index, rekkefoelge)
                            }
                        }

                        val hull: List<List<Job>> = flate.getHullReferanser().mapIndexed { hullIndex, liste ->
                            liste.mapIndexed { rekkefoelge, ref ->
                                launch {
                                    importerGrunnkretsRef(grunnkrets, ref, kurver, skall_index, rekkefoelge, hullIndex)
                                }
                            }
                        }

                        skall.joinAll()
                        hull.forEach { it.joinAll() }
                    }
                }
            }
            val antallReferanser = grunnkrets.grunnkretsgrenser.size +
                grunnkrets.delomraadegrenser.size +
                grunnkrets.kommunegrenser.size +
                grunnkrets.fylkesgrenser.size +
                grunnkrets.riksgrenser.size +
                grunnkrets.territorialgrenser.size +
                grunnkrets.avtaltavgrensningslinjer.size
            if (flate.getHullReferanser().isEmpty() && antallReferanser != flate.getSkallReferanser().size) {
                logger.info("Noe er galt, antall skall-refs (${flate.getSkallReferanser().size}) er ikke likt med grenser på grunnkretsen (${antallReferanser})")
            }

            grunnkrets.oppdaterFlate()
            grunnkrets.kommunenummer = flate.getKommunenummer()?.let { kodelisteService.getKommunenummerIdByCodeValue(it) }

            grunnkretsRepository.save(grunnkrets)

            if (flate.getInformasjon() != null) {

            }

        }
    }

    private fun importerGrunnkretsRef(
        grunnkrets: Grunnkrets,
        ref: SosiReferanse,
        kurver: List<SosiKurve>,
        skallIndex: Int,
        rekkefoelge: Int,
        hullIndex: Int? = null
    ) {

        val referanse = ref.getVerdiUtenRetningOgParentes()

        when (val grenseRef = kurver.find{it.getId() == referanse}) {
            null -> throw RuntimeException("Finner ikke grense som refereres til. Feil i datagrunnlaget. Ref: $referanse")
            else -> {
                //                logger.info("Importerer grense ${importertGrense.lokalid}, grensetype: ${importertGrense.objtype}, referanse ${referanse}")
                when (val importertGrense = grenseImporter.importerGrense(grenseRef)) {
                    is Grunnkretsgrense -> grunnkrets.grunnkretsgrenser.add(
                        GrunnkretsGrunnkretsgrense(
                            grunnkrets,
                            importertGrense,
                            GrenseKoblingAttributter(ref.erRetningMedKlokken(), rekkefoelge, skallIndex, hullIndex)
                        )
                    )

                    is Delomraadegrense -> grunnkrets.delomraadegrenser.add(
                        GrunnkretsDelomraadegrense(
                            grunnkrets,
                            importertGrense,
                            GrenseKoblingAttributter(ref.erRetningMedKlokken(), rekkefoelge, skallIndex, hullIndex)
                        )
                    )

                    is Kommunegrense -> grunnkrets.kommunegrenser.add(
                        GrunnkretsKommunegrense(
                            grunnkrets,
                            importertGrense,
                            GrenseKoblingAttributter(ref.erRetningMedKlokken(), rekkefoelge, skallIndex, hullIndex)
                        )
                    )
                    is Fylkesgrense -> grunnkrets.fylkesgrenser.add(
                        GrunnkretsFylkesgrense(
                            grunnkrets,
                            importertGrense,
                            GrenseKoblingAttributter(ref.erRetningMedKlokken(), rekkefoelge, skallIndex, hullIndex)
                        )
                    )
                    is Avtaltavgrensningslinje -> grunnkrets.avtaltavgrensningslinjer.add(
                        GrunnkretsAvtaltavgrensningslinje(
                            grunnkrets,
                            importertGrense,
                            GrenseKoblingAttributter(ref.erRetningMedKlokken(), rekkefoelge, skallIndex, hullIndex)
                        )
                    )
                    is Territorialgrense -> grunnkrets.territorialgrenser.add(
                        GrunnkretsTerritorialgrense(
                            grunnkrets,
                            importertGrense,
                            GrenseKoblingAttributter(ref.erRetningMedKlokken(), rekkefoelge, skallIndex, hullIndex)
                        )
                    )
                    is Riksgrense -> grunnkrets.riksgrenser.add(
                        GrunnkretsRiksgrense(
                            grunnkrets,
                            importertGrense,
                            GrenseKoblingAttributter(ref.erRetningMedKlokken(), rekkefoelge, skallIndex, hullIndex)
                        )
                    )
                }
            }
        }
    }

    //TODO: Denne skal skrives om til å kjøre når grenser importeres, venter på NIBAS-304. Dette er kode som ble kjørt i etterkant for å teste ut algoritmen.
    fun match() {
        val grunnkretser = grunnkretsRepository.findAll()
        var antallBom = 0
        val tidBrukt = measureTimeMillis {
            grunnkretser.forEachIndexed { index, grunnkrets ->
                logger.info("Matcher grunnkrets: ${grunnkrets.grunnkretsnavn}, antall kommunegrenser i grunnkrets: ${grunnkrets.kommunegrenser.size}, index: $index")
                val kommune = kommuneRepository.findByKommunenummer(grunnkrets.kommunenummer!!)
                logger.info("Antall kommunegrenser i kommunen (${kommune?.administrativenhetnavn?.first()?.navn}): ${kommune?.kommunegrenser?.size}")

                val kommunegrenser: List<Kommunegrense>? = kommune?.kommunegrenser?.sortedWith(
                    compareBy(
                        { it.grenseKoblingAttributter.flateIndeks },
                        { it.grenseKoblingAttributter.hullIndeks },
                        { it.grenseKoblingAttributter.rekkefoelge }
                    )
                )?.map { it.kommunegrense }

                if (grunnkrets.kommunegrenser.isNotEmpty() && kommunegrenser != null && kommunegrenser.isEmpty()) {
                    logger.warning("Grunnkrets har kommunegrenser, men kommunen den ligger i har ikke kommunegrenser, dette bør ikke være mulig")
                } else {
                    grunnkrets.kommunegrenser.forEach { grunnkretsKommunegrense ->
                        if (segmenterSomDekkerNyGrense(kommunegrenser!!, grunnkretsKommunegrense.kommunegrense).isEmpty()) {
                            antallBom++
                        }
                    }
                }
            }
        }

        logger.info("Ferdig med matching, antall bom: $antallBom, total tid brukt: $tidBrukt")
    }

    fun <T> List<T>.overlappedWindow(windowSize: Int, step: Int = 1): List<List<T>> =
        windowed(windowSize, step, true).map { window ->
            var newWindow = window
            repeat(windowSize - window.size) { index ->
                if (index < size) {
                    newWindow = newWindow.plus(get(index))
                }
            }
            newWindow
        }

    fun segmenterSomDekkerNyGrense(skalSjekkesMot: List<Grense>, grense: Kommunegrense): List<Grense> {
        val initialWindowSize = 1
        val maxWindowSize = min(skalSjekkesMot.size, 20)
        var currentWindowSize = initialWindowSize

        while (currentWindowSize <= maxWindowSize) {
            val windows = skalSjekkesMot.overlappedWindow(currentWindowSize)
            val grenserSomDekker: List<Grense>? = windows.find { grenserIVindu ->
                val grensesegmenter: List<LineString> = grenserIVindu.fold(emptyList()) {
                    list, element ->
                    list.plus(element.grense!!)
                }
                val segment =
                    SosiGeometryFunctions.getGeometryFactory().createMultiLineString(grensesegmenter.toTypedArray())

                segment.buffer(5.0).covers(grense.grense)
            }

            if (grenserSomDekker != null) {
                logger.info("Fant treff for ${grense.lokalid} med vindustørrelse $currentWindowSize")
                return grenserSomDekker
            }
            currentWindowSize += 1
        }

        logger.info("Fant ikke treff for ${grense.lokalid}, vindustørrelse $currentWindowSize")
        return emptyList()
    }
}
