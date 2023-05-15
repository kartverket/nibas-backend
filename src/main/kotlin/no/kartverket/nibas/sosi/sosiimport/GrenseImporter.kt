package no.kartverket.nibas.sosi.sosiimport

import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.domain.GrenseKoblingAttributter
import no.kartverket.nibas.domain.GrenseMedAttributter
import no.kartverket.nibas.domain.KobletTilGrense
import no.kartverket.nibas.sosi.common.SosiKoder
import no.kartverket.nibas.sosi.model.SosiGeometri
import no.kartverket.nibas.sosi.model.SosiReferanse
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.operation.distance.DistanceOp
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class GrenseImporter {

    @Autowired
    lateinit var avtaltAvgrensningslinjeImporter: AvtaltAvgrensningslinjeImporter

    @Autowired
    lateinit var lovVirkeomraadeGrenseImporter: LovVirkeomraadeGrenseImporter

    @Autowired
    lateinit var grunnlinjeImporter: GrunnlinjeImporter

    @Autowired
    lateinit var riksgrenseImporter: RiksgrenseImporter

    @Autowired
    lateinit var territorialgrenseImporter: TerritorialgrenseImporter

    @Autowired
    lateinit var kommunegrenseImporter: KommunegrenseImporter

    @Autowired
    lateinit var fylkesgrenseImporter: FylkesgrenseImporter

    @Autowired
    lateinit var grunnkretsgrenseImporter: GrunnkretsgrenseImporter

    @Autowired
    lateinit var delomraadegrenseImporter: DelomraadegrenseImporter

    @Autowired
    lateinit var stemmekretsgrenseImporter: StemmekretsgrenseImporter

    private val logger = LoggerFactory.getLogger(this.javaClass.name)

    fun importerGrenseRef(
        krets: KobletTilGrense,
        ref: SosiReferanse,
        kretsgrenser: List<SosiGeometri>,
        skallIndex: Int,
        rekkefoelge: Int,
        hullIndex: Int? = null,
        erstatningskoordinater: MutableMap<Coordinate, Erstatningskoordinat>,
        matchedeGrenser: MutableMap<String, List<SplitTilImport>>
    ) {
        val referanse = ref.getVerdiUtenRetningOgParentes()
        // Man kunne hentet ut grensen fra listen over grenser som skal importeres. Alle disse skal ha samme originale grense.
        when (val grense: SosiGeometri? = kretsgrenser.find { it.getId() == referanse }) {
            null -> throw RuntimeException("Finner ikke grense som refereres til. Feil i datagrunnlaget. Ref: $referanse")
            else -> {
                grense.erRetningMedKlokken = ref.erRetningMedKlokken()
                val grenseMedRiktigRetning = when (grense.erRetningMedKlokken) {
                    true -> grense.asLineString()
                    false -> grense.asLineString().reverse()
                }

                var skalImporteres: List<SplitTilImport>? = matchedeGrenser[ref.getVerdiUtenParentes()]


                if (skalImporteres.isNullOrEmpty()) {
                    logger.warn("Grense $referanse har ikke blitt lagt i matchede grenser, trolig pga feil i grensematching (enten på denne) eller på en tidligere grense for tilhørende grunnkrets")
                } else {
                    logger.debug("Skal importere grense-ref ${ref.getVerdiUtenParentes()}, matchet opp av ${skalImporteres.size} grenser")
                    skalImporteres.forEachIndexed {index, grenseIntern ->
                        logger.debug("\tGrense ($index) som dekker: ${grenseIntern.splitRef.basertPaGrense.getVerdiMedIndex(0)}, opprinnelig grense: ${grenseIntern.originalRef.getVerdiUtenRetningOgParentes()}, ny lokalid: ${grenseIntern.lokalId()}")
                    }

                    val startKoordinatOpprinneligGrense = grense.startKoordinat(grense.erRetningMedKlokken)
                    val sluttKoordinatOpprinneligGrense = grense.sluttKoordinat(grense.erRetningMedKlokken)
                    val startKoordinatIOpprinneligGrenseErFlyttet = erstatningskoordinater.containsKey(startKoordinatOpprinneligGrense)
                    val sluttKoordinatIOpprinneligGrenseErFlyttet = erstatningskoordinater.containsKey(sluttKoordinatOpprinneligGrense)

                    if (startKoordinatIOpprinneligGrenseErFlyttet || sluttKoordinatIOpprinneligGrenseErFlyttet) {
                        logger.debug("Opprinnelig grense $referanse inneholder punkter som har blitt flyttet, start flyttes: $startKoordinatIOpprinneligGrenseErFlyttet, slutt flyttes: $sluttKoordinatIOpprinneligGrenseErFlyttet")

                        if (erKretsgrense(grense)) {
                            //Opprinnelig grense er ikke matchet, dvs den er en kretsgrense og kun ett element. Da ønsker vi å flytte fra gammelt knutepunkt til nytt knutepunkt
                            skalImporteres.forEach {
                                val koordinaterForGrenseMedRiktigRetning: Array<Coordinate> = grenseMedRiktigRetning.coordinates

                                //Enten finner man nytt punkt i mappet, ellers brukes opprinnelig punkt
                                koordinaterForGrenseMedRiktigRetning[0] = erstatningskoordinater.getOrDefault(startKoordinatOpprinneligGrense, Erstatningskoordinat(false, startKoordinatOpprinneligGrense)).nyttKoordinat
                                koordinaterForGrenseMedRiktigRetning[koordinaterForGrenseMedRiktigRetning.size - 1] = erstatningskoordinater.getOrDefault(sluttKoordinatOpprinneligGrense, Erstatningskoordinat(false, sluttKoordinatOpprinneligGrense)).nyttKoordinat

                                //Dersom grensa opprinnelig var mot klokken må vi snu den igjen før lagring (slik at den stemmer overens med retningMedKlokken-flagget)
                                val splittetGrense = when (grense.erRetningMedKlokken) {
                                    true -> SosiGeometryFunctions.getGeometryFactory().createLineString(koordinaterForGrenseMedRiktigRetning)
                                    false -> SosiGeometryFunctions.getGeometryFactory().createLineString(koordinaterForGrenseMedRiktigRetning.reversed().toTypedArray())
                                }

                                val splittetGrenseRef = SplitTilImport(SplitRef(splittetGrense, it.splitRef.basertPaGrense, it.splitRef.basertPaGrense.erRetningMedKlokken), it.originalRef, it.originalGrense)
                                oppdaterKrets(krets, splittetGrenseRef, skallIndex, hullIndex)
                            }
                        }
                        else {
                            logger.debug("Grense har blitt matchet. Startpunkt er flyttet: $startKoordinatIOpprinneligGrenseErFlyttet, sluttpunkt er flyttet: $sluttKoordinatIOpprinneligGrenseErFlyttet")

                            var erstatningsKoordinatForStart: Erstatningskoordinat? = erstatningskoordinater[startKoordinatOpprinneligGrense]
                            if (knutepunktSkalBrukesForKoordinat(erstatningsKoordinatForStart)) {
                                logger.debug("Knutepunkt skal brukes for start, fant startkoordinat: $startKoordinatOpprinneligGrense som skal flyttes til knutepunkt med koordinat: ${erstatningsKoordinatForStart!!.nyttKoordinat}")
                            }
                            var erstatningsKoordinatForSlutt: Erstatningskoordinat? = erstatningskoordinater[sluttKoordinatOpprinneligGrense]
                            if (knutepunktSkalBrukesForKoordinat(erstatningsKoordinatForSlutt)) {
                                logger.debug("Knutepunkt skal brukes for slutt, fant sluttkoordinat: $sluttKoordinatOpprinneligGrense som skal flyttes til knutepunkt med koordinat: ${erstatningsKoordinatForSlutt!!.nyttKoordinat}")
                            }

                            skalImporteres.filter{
                                skalGrensenImporteres(it, erstatningsKoordinatForStart, erstatningsKoordinatForSlutt)
                            }.map{
                                oppdaterEndepunktSomIkkeAlleredeErFlyttet(it, erstatningskoordinater)
                            }.map{
                                forlengGrenserTilNyttKnutepunkt(it, erstatningsKoordinatForStart, erstatningsKoordinatForSlutt)
                            }.forEach {
                                oppdaterKrets(krets, it, skallIndex, hullIndex)
                            }

                        }
                    } else {
                        logger.debug("Grense $referanse har ikke registrert flytting av noen koordinater, bør kunne importeres direkte.")
                        skalImporteres.forEach {
                            oppdaterKrets(krets, it, skallIndex, hullIndex)
                        }
                    }

                }

            }
        }
    }

    private fun oppdaterKrets(
        krets: KobletTilGrense,
        importSplitRef: SplitTilImport,
        skallIndex: Int,
        hullIndex: Int?
    ) {
        krets.oppdaterEllerLeggTilGrenseKobling(
            GrenseMedAttributter(
                grense = importerGrense(importSplitRef),
                attributter = GrenseKoblingAttributter(importSplitRef.splitRef.opprinneligRetningMedKlokken, krets.antallGrenser(), skallIndex, hullIndex)))
    }

    private fun skalGrensenImporteres(
        splitTilImport: SplitTilImport,
        erstatningsKoordinatForStart: Erstatningskoordinat?,
        erstatningsKoordinatForSlutt: Erstatningskoordinat?
    ): Boolean {
        //Små grensesegmenter som har blitt feilaktig lagt til fordi vi egentlig skulle gå til et knutepunkt litt tidligere langs grensa skal ikke importeres
        //Algoritme: Dersom grensa er kortere enn maksimal buffer-størrelse og inneholder enten nytt start- eller sluttknutepunkt skal den forkastes.
        var grenseSkalImporteres = true

        if (knutepunktSkalBrukesForKoordinat(erstatningsKoordinatForStart) || knutepunktSkalBrukesForKoordinat(erstatningsKoordinatForSlutt)) {
            val splittetGrenseMedRiktigRetning = when (splitTilImport.splitRef.opprinneligRetningMedKlokken) {
                true -> splitTilImport.splitRef.splittetGrense
                false -> splitTilImport.splitRef.splittetGrense.reverse()
            }

            //Dersom grensa er lang og starter/slutter like ved nytt start/sluttknutepunkt skal man flytte start/sluttpunkt til nytt start/sluttknutepunkt
            val lengdeSplittetGrense = splittetGrenseMedRiktigRetning.length

            val lokalId = splitTilImport.lokalId()

            if (erstatningsKoordinatForStart != null && erstatningsKoordinatForStart.skalBrukeKnutepunkt) {
                val avstand = DistanceOp(splittetGrenseMedRiktigRetning, SosiGeometryFunctions.coordinateToPoint(erstatningsKoordinatForStart.nyttKoordinat)).distance()
                logger.debug("KP for start, grense $lokalId har lengde $lengdeSplittetGrense, avstand til nytt knutepunkt: $avstand")
                if (avstand == 0.0 && lengdeSplittetGrense <= 5.0) {
                    logger.debug("Grense $lokalId skal forkastes (knutepunkt start)")
                    grenseSkalImporteres = false
                }
            }
            if (erstatningsKoordinatForSlutt != null && erstatningsKoordinatForSlutt.skalBrukeKnutepunkt) {
                val avstand = DistanceOp(splittetGrenseMedRiktigRetning, SosiGeometryFunctions.coordinateToPoint(erstatningsKoordinatForSlutt.nyttKoordinat)).distance()
                logger.debug("KP for slutt, grense $lokalId har lengde $lengdeSplittetGrense, avstand til nytt knutepunkt: $avstand")
                if (avstand == 0.0 && lengdeSplittetGrense <= 5.0) {
                    logger.debug("Grense $lokalId skal forkastes (knutepunkt slutt)")
                    grenseSkalImporteres = false
                }
            }
        }
        return grenseSkalImporteres
    }
    private fun oppdaterEndepunktSomIkkeAlleredeErFlyttet(
        splitTilImport: SplitTilImport,
        erstatningskoordinater: MutableMap<Coordinate, Erstatningskoordinat>
    ): SplitTilImport {
        // Dersom knutepunkt flyttes litt langs grensen vil kun en av grensene oppdatere endepunktet (eksempel: grensene 6997 og 9531, hvor kun 6997 flyttes).
        // Vi må derfor sjekke alle endepunkter på splittede grenser for å sjekke om noen av dem faktisk skulle ha blitt flyttet
        val splittetGrense = splitTilImport.splitRef.splittetGrense.coordinates
        if (erstatningskoordinater.containsKey(splittetGrense[0])) {
            val erstatningsKoordinat = erstatningskoordinater[splittetGrense[0]]
            logger.debug("Startpunkt: ${splittetGrense[0]} har faktisk blitt flyttet til ${erstatningsKoordinat?.nyttKoordinat}")
            splittetGrense[0] = erstatningsKoordinat?.nyttKoordinat
        }

        val sisteIndex = splittetGrense.size - 1
        if (erstatningskoordinater.containsKey(splittetGrense[sisteIndex])) {
            val erstatningsKoordinat = erstatningskoordinater[splittetGrense[sisteIndex]]
            logger.debug("Sluttpunkt: ${splittetGrense[sisteIndex]} har faktisk blitt flyttet til ${erstatningsKoordinat?.nyttKoordinat}")
            splittetGrense[sisteIndex] = erstatningsKoordinat?.nyttKoordinat
        }
        val nySplittetGrense = SosiGeometryFunctions.getGeometryFactory().createLineString(splittetGrense)
        return SplitTilImport(SplitRef(nySplittetGrense, splitTilImport.splitRef.basertPaGrense, splitTilImport.splitRef.opprinneligRetningMedKlokken), splitTilImport.originalRef, splitTilImport.originalGrense)
    }

    private fun forlengGrenserTilNyttKnutepunkt(
        splitTilImport: SplitTilImport,
        erstatningsKoordinatForStart: Erstatningskoordinat?,
        erstatningsKoordinatForSlutt: Erstatningskoordinat?
    ): SplitTilImport {
        // Algoritme: Dersom grensa skal bruke nytt felles knutepunkt må den forlenges dersom:
        // Grensa er lang nok og den slutter mindre enn 5 meter fra felles knutepunkt (men ikke på knutepunkt)
        val splittetGrenseMedRiktigRetning = when (splitTilImport.splitRef.opprinneligRetningMedKlokken) {
            true -> splitTilImport.splitRef.splittetGrense
            false -> splitTilImport.splitRef.splittetGrense.reverse()
        }
        var koordinaterForSplittetGrenseMedRiktigRetning: Array<Coordinate> = splittetGrenseMedRiktigRetning.coordinates

        if (knutepunktSkalBrukesForKoordinat(erstatningsKoordinatForStart) || knutepunktSkalBrukesForKoordinat(erstatningsKoordinatForSlutt)) {

            val lengdeSplittetGrense = splittetGrenseMedRiktigRetning.length
            if (knutepunktSkalBrukesForKoordinat(erstatningsKoordinatForStart)) {
                val nyttKnutepunktForStartpunkt: Coordinate = erstatningsKoordinatForStart!!.nyttKoordinat
                val avstand = DistanceOp(splittetGrenseMedRiktigRetning, SosiGeometryFunctions.coordinateToPoint(nyttKnutepunktForStartpunkt)).distance()
                logger.debug("KP for start, grense ${splitTilImport.lokalId()} har lengde $lengdeSplittetGrense, avstand til nytt knutepunkt: $avstand")

                if (avstand > 0.0 && avstand <= 5.0 && lengdeSplittetGrense > 5.0) {
                    logger.debug("Grense ${splitTilImport.lokalId()} skal forlenges, dvs startpunkt ${splittetGrenseMedRiktigRetning.startPoint} flyttes til nytt knutepunkt ${nyttKnutepunktForStartpunkt}")
                    koordinaterForSplittetGrenseMedRiktigRetning[0] = nyttKnutepunktForStartpunkt
                }

            }
            if (knutepunktSkalBrukesForKoordinat(erstatningsKoordinatForSlutt)) {
                val nyttKnutepunktForSluttpunkt: Coordinate = erstatningsKoordinatForSlutt!!.nyttKoordinat
                val avstand = DistanceOp(splittetGrenseMedRiktigRetning, SosiGeometryFunctions.coordinateToPoint(nyttKnutepunktForSluttpunkt)).distance()
                logger.debug("KP for slutt, grense ${splitTilImport.lokalId()} har lengde $lengdeSplittetGrense, avstand til nytt knutepunkt: $avstand")

                if (avstand > 0.0 && avstand <= 5.0 && lengdeSplittetGrense > 5.0) {
                    logger.debug("Grense ${splitTilImport.lokalId()} skal forlenges, dvs sluttpunkt ${splittetGrenseMedRiktigRetning.endPoint} flyttes til nytt knutepunkt $nyttKnutepunktForSluttpunkt")
                    koordinaterForSplittetGrenseMedRiktigRetning[koordinaterForSplittetGrenseMedRiktigRetning.size - 1] = nyttKnutepunktForSluttpunkt
                }
            }
        }
        //Dersom grensa opprinnelig var mot klokken må vi snu den igjen før lagring (slilk at den stemmer overens med retningMedKlokken-flagget)
        val muligForlengetGrense = when (splitTilImport.splitRef.opprinneligRetningMedKlokken) {
            true -> SosiGeometryFunctions.getGeometryFactory().createLineString(koordinaterForSplittetGrenseMedRiktigRetning)
            false -> SosiGeometryFunctions.getGeometryFactory().createLineString(koordinaterForSplittetGrenseMedRiktigRetning.reversed().toTypedArray())
        }

        return SplitTilImport(SplitRef(muligForlengetGrense, splitTilImport.splitRef.basertPaGrense, splitTilImport.splitRef.opprinneligRetningMedKlokken), splitTilImport.originalRef, splitTilImport.originalGrense)
    }

    private fun knutepunktSkalBrukesForKoordinat(erstatningsKoordinat: Erstatningskoordinat?) =
        erstatningsKoordinat != null && erstatningsKoordinat.skalBrukeKnutepunkt

    private fun erKretsgrense(grense: SosiGeometri) = !GrenseMatcher.skalMatches(grense)

    fun importerGrense(skalImporteres: SplitTilImport): Grense {
        return when (skalImporteres.splitRef.basertPaGrense.getObjType()) {
            SosiKoder.KOMMUNEGRENSE -> kommunegrenseImporter.importerKommunegrense(skalImporteres)
            SosiKoder.FYLKESGRENSE -> fylkesgrenseImporter.importerFylkesgrense(skalImporteres)
            SosiKoder.TERRITORIALGRENSE -> territorialgrenseImporter.importSosiTerritorialgrense(skalImporteres)
            SosiKoder.RIKSGRENSE -> riksgrenseImporter.importSosiRiksgrense(skalImporteres)
            SosiKoder.AVTALT_AVGRENSNINGSLINJE -> avtaltAvgrensningslinjeImporter.importAvtaltAvgrensningslinje(skalImporteres)
            SosiKoder.LOV_VIRKEOMRADEGRENSE -> lovVirkeomraadeGrenseImporter.importLovVirkeomradeGrense(skalImporteres)
            SosiKoder.GRUNNLINJE -> grunnlinjeImporter.importGrunnlinje(skalImporteres)
            SosiKoder.GRUNNKRETSGRENSE -> grunnkretsgrenseImporter.importerGrunnkretsgrense(skalImporteres)
            SosiKoder.STEMMEKRETSGRENSE -> stemmekretsgrenseImporter.importerStemmekretsgrense(skalImporteres)
            SosiKoder.DELOMRAADEGRENSE -> delomraadegrenseImporter.importerDelomraadegrense(skalImporteres)
            else -> throw RuntimeException("Found no importer for ${skalImporteres.splitRef.basertPaGrense.getObjType()}")
        }
    }
}

/**
 * Oppretter Grense med lokalid fra eksternt system hvis den er gyldig.
 */
fun createGrense(lokalid: String?, grensetype: Grensetype): Grense {
    return if (lokalid != null) {
        Grense(lokalid = lokalid, grensetype = grensetype)
    } else {
        Grense(grensetype = grensetype)
    }
}
