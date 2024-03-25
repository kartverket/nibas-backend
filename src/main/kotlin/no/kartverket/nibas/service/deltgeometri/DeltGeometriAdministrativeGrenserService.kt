package no.kartverket.nibas.service.deltgeometri

import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.common.UgyldigTilstandException
import no.kartverket.nibas.common.geometry.UgyldigeFlaterException
import no.kartverket.nibas.common.geometry.getGrenserIRiktigRekkefoelge
import no.kartverket.nibas.common.getInSpraak
import no.kartverket.nibas.common.pop
import no.kartverket.nibas.domain.Endringstype
import no.kartverket.nibas.domain.Flate
import no.kartverket.nibas.domain.Fylke
import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.domain.Grunnkrets
import no.kartverket.nibas.domain.Nasjon
import no.kartverket.nibas.domain.Stemmekrets
import no.kartverket.nibas.events.EventRad
import no.kartverket.nibas.events.EventType
import no.kartverket.nibas.kodelister.KodelisteService
import no.kartverket.nibas.repository.FylkeRepository
import no.kartverket.nibas.repository.GrenseRepository
import no.kartverket.nibas.repository.GrunnkretsRepository
import no.kartverket.nibas.repository.KommuneRepository
import no.kartverket.nibas.repository.NasjonRepository
import no.kartverket.nibas.repository.StemmekretsRepository
import no.kartverket.nibas.repository.findGyldigObjektForIdentifikator
import no.kartverket.nibas.service.FlateService
import no.kartverket.nibas.service.KommuneService
import no.kartverket.nibas.service.GrenseService
import no.kartverket.nibas.service.GrunnkretsService
import no.kartverket.nibas.service.StemmekretsService
import no.kartverket.nibas.unwrap
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.LineSegment
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.PrecisionModel
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class DeltGeometriAdministrativeGrenserService(
    val kommuneService: KommuneService,
    val kodelisteService: KodelisteService,
    val flateService: FlateService,
    val grenseService: GrenseService,
    val grenseRepository: GrenseRepository,
    val grunnkretsService: GrunnkretsService,
    val grunnkretsRepository: GrunnkretsRepository,
    val stemmekretsService: StemmekretsService,
    val stemmekretsRepository: StemmekretsRepository,
    val kommuneRepository: KommuneRepository,
    val fylkeRepository: FylkeRepository,
    val nasjonRepository: NasjonRepository
) {
    //Egen geometryFactory, for å en presisjonsmodell som tar vare på alle desimalene til eksisterende punkt
    private val geometryFactory = GeometryFactory(PrecisionModel(), 25833)
    private val logger = LoggerFactory.getLogger(this.javaClass)

    /**
     * Funksjonen tar inn lokalid for en kommune, gyldighetsdato og et map med eventrader for flater som evt har fått ny versjon fra tidligere kommuner i kjøringen.
     * Den returnerer ett sett med administrative grenser for kommunen og map med eventrader for flater som fikk (evt har tidligere fått) ny versjon ifm de nye grensene.
     * Grensene baserer seg på grunnkretsgrensene, siden det er de som har mest data fra importen.
     * Noen grenser vil bli splittet for å få på plass nødvendige knutepunkt fra stemmekretsgrensene. Geometrien på de nye grensene vil være lik som før.
     */
    @Transactional
    fun dannDeltGeometriForKommune(kommuneLokalid: String, eventRaderForAlleredeOppdaterteFlater: Map<String, EventRad>,
                                   gyldighetsDato: LocalDate, loggmelding: String = "")
        : Pair<List<Grense>, Map<String, EventRad>> {
        val kommune = kommuneRepository.findGyldigObjektForIdentifikator(Identifikator(Lokalid(kommuneLokalid), gyldighetsDato))
            ?: throw RuntimeException("Fant ikke kommune med lokalid $kommuneLokalid på gyldighetsdato $gyldighetsDato")

        logger.info("$loggmelding ${kommune.kommunenummer} ${kommune.getAdministrativenhetNavn(gyldighetsDato).getInSpraak("nor")}")

        //Fælt hack for liten linjebit i Farsund som ikke skal med
        val uonsketPunktFarsundKvinesdal = geometryFactory.createPoint(Coordinate(21241.58, 6482838.75))
        val erstatningsPunktFarsundKvinesdal = geometryFactory.createPoint(Coordinate(21241.53, 6482838.75))
        val uonsketLinestringFarsundKvinesdal = geometryFactory.createLineString(arrayOf(uonsketPunktFarsundKvinesdal.coordinate, erstatningsPunktFarsundKvinesdal.coordinate))

        val uonsketPunktDrammenOvreEiker = geometryFactory.createPoint(Coordinate(216384.97, 6635170.35))
        val erstatningsPunktDrammenOvreEiker = geometryFactory.createPoint(Coordinate(216384.95, 6635170.55))
        val uonsketLinestringDrammenOvreEiker =
            geometryFactory.createLineString(arrayOf(uonsketPunktDrammenOvreEiker.coordinate, erstatningsPunktDrammenOvreEiker.coordinate))

        val uonsketPunktDrammenLier = geometryFactory.createPoint(Coordinate(231594.23, 6633680.3))
        val erstatningsPunktDrammenLier = geometryFactory.createPoint(Coordinate(231594.34, 6633679.97))
        val uonsketLinestringDrammenLier =
            geometryFactory.createLineString(arrayOf(uonsketPunktDrammenLier.coordinate, erstatningsPunktDrammenLier.coordinate))

        val uonsketPunktBergenBjornafjorden = geometryFactory.createPoint(Coordinate(-31907.01, 6715694.05))
        val erstatningsPunktBergenBjornafjorden = geometryFactory.createPoint(Coordinate(-31907.15, 6715693.95))
        val uonsketLinestringBergenBjornafjorden =
            geometryFactory.createLineString(arrayOf(uonsketPunktBergenBjornafjorden.coordinate, erstatningsPunktBergenBjornafjorden.coordinate))

        val administrativeGrenser = mutableListOf<Grense>()
        val grunnkretsGrenserSomSkalArkiveres = mutableSetOf<Grense>()
        val evtNyeVersjonerAvFlater = mutableMapOf<Lokalid, Flate>()

        val administrativeGrensekoblingerForGrunnkretserIKommunen = kommune.getGrensekoblinger(gyldighetsDato)
        val lineStringsMellomKnutepunkterForGrunnkrets = administrativeGrensekoblingerForGrunnkretserIKommunen.map { it.grense.grense }
        val grensepunkterForGrunnkretser = getGrensepunkterIRiktigRekkefoelge(lineStringsMellomKnutepunkterForGrunnkrets)
        val knutepunktGrunnkrets = getGrenserIRiktigRekkefoelge(lineStringsMellomKnutepunkterForGrunnkrets)
            .flatten().map { it.startPoint.coordinate }

        val administrativeGrensekoblingerForStemmekretserIKommunen = kommune.getStemmekretser(gyldighetsDato)
            .flatMap { it.getGrensekoblinger(gyldighetsDato) }
            .filter { it.grense.erAdministrativGrense() }

        val knutepunktStemmekrets =
            getGrenserIRiktigRekkefoelge(administrativeGrensekoblingerForStemmekretserIKommunen.map { it.grense.grense }).flatten()
                .filter {
                    it.startPoint != uonsketPunktFarsundKvinesdal && it.startPoint != uonsketPunktDrammenOvreEiker
                        && it.startPoint != uonsketPunktDrammenLier && it.startPoint != uonsketPunktBergenBjornafjorden
                }
                .map { it.startPoint.coordinate }

        val grensepunkterPerPolygon = leggTilManglendeKnutepunktFraStemmekrets(grensepunkterForGrunnkretser, knutepunktStemmekrets)
        val linestringsMellomKnutepunkter = grensepunkterPerPolygon
            .flatMap { grensepunkterTilLineStringsMellomKnutepunkter(it, knutepunktGrunnkrets + knutepunktStemmekrets) }

        linestringsMellomKnutepunkter.forEach { lineString ->
            val grenseKobling = administrativeGrensekoblingerForGrunnkretserIKommunen.finnGrensekoblingForGrense(lineString)
                ?: throw RuntimeException("Kunne ikke finne en eksisterende grunnkretsgrense som matcher linestringen $lineString")

            val grense = grenseKobling.grense
            if (grense.grense.equals(lineString)) {
                // Grenseforløpet er lik en opprinnelig grunnkretsgrense og trenger ikke å deles opp.
                // Må være med i returen siden den er en del av kommunens grenser
                administrativeGrenser.add(grense)
            } else {
                //Grensa skal deles opp i flere biter. Den gamle grensa skal arkiveres etter at alle linestrings er prosessert
                grunnkretsGrenserSomSkalArkiveres.add(grense)
                val grenseMedNyUuid = grenseService.createNyGrenseKopiertFraAnnenGrenseMedNyLokalid(kopierFraGrense = grense, nyLineString = lineString,
                    gyldigFra = gyldighetsDato, endringstype = Endringstype.FASTSETTING)
                administrativeGrenser.add(grenseMedNyUuid)
                logger.info("Skal arkivere grense objid: ${grense.id} Ny grensebit objid: ${grenseMedNyUuid.id} ${grenseMedNyUuid.grense}")

                //Sjekk om grensa sine grunnkretser, kommuner, fylker og nasjon har fått ny versjon ifm nabokommune
                hentEvtVersjonFraBasenForFlaterSomHarFaattNyVersjonIfmNabokommune(grense, gyldighetsDato, evtNyeVersjonerAvFlater, eventRaderForAlleredeOppdaterteFlater)

                //Lag ny versjon av gammel grense sine grunnkretser hvis det ikke allerede er gjort, og knytt den nye grensa til grunnkretsene
                //Lag ny versjon av grensa sine kommuner/fylker/nasjon hvis det trengs
                handterGrensekoblingForNyVersjonAvGrunnkretsOgHoyereIHierarkiet(gammelGrense = grense, gyldighetsdato = gyldighetsDato,
                    evtNyeVersjonerAvFlater = evtNyeVersjonerAvFlater, endringstype = Endringstype.FASTSETTING, nyVersjonAvGrense = grenseMedNyUuid)
            }

        }

        // Fjern knytning til grunnkretser og arkiver grenser som har blitt byttet ut med kortere linjestykker
        arkiverGamleAdmGrenserforGrunnkrets(grunnkretsGrenserSomSkalArkiveres, gyldighetsDato, evtNyeVersjonerAvFlater)

        administrativeGrensekoblingerForStemmekretserIKommunen.forEach { stemmekretsGrenseKobling ->
            val grense = stemmekretsGrenseKobling.grense
            if (grense.gyldigeGrunnkretsGrenseKoblinger(gyldighetsDato).isEmpty()) {
                //Grensa er ikke knyttet til grunnkrets, dvs er ikke håndtert ifm nabokommunen.
                //Sjekk om grensa sine stemmekretser har fått ny versjon ifm nabokommune
                grense.stemmekretser(gyldighetsDato).forEach {
                    if (!evtNyeVersjonerAvFlater.containsKey(Lokalid(it.lokalid)) && eventRaderForAlleredeOppdaterteFlater.containsKey(it.lokalid)) {
                        evtNyeVersjonerAvFlater[Lokalid(it.lokalid)] = stemmekretsRepository.findById(it.id).unwrap()
                            ?: throw RuntimeException("Fant ikke stemmekrets med objid ${it.id}")
                    }
                }
                val sb = StringBuilder("Stemmekretsgrense: ${grense.id}  Erstattes av")

                //Fælt hack for små linjebiter som ikke skal med
                if (grense.grense.equals(uonsketLinestringFarsundKvinesdal) || grense.grense.equals(uonsketLinestringDrammenOvreEiker) ||
                    grense.grense.equals(uonsketLinestringDrammenLier) || grense.grense.equals(uonsketLinestringBergenBjornafjorden)) {
                    handterGrensekoblingForNyVersjonAvStemmekrets(gammelGrense = grense, gyldighetsdato = gyldighetsDato,
                        evtNyeVersjonerAvFlater = evtNyeVersjonerAvFlater, endringstype = Endringstype.FASTSETTING)
                } else {
                    val startpunkt = when (grense.grense.startPoint) {
                        uonsketPunktFarsundKvinesdal -> erstatningsPunktFarsundKvinesdal
                        uonsketPunktDrammenOvreEiker -> erstatningsPunktDrammenOvreEiker
                        uonsketPunktDrammenLier -> erstatningsPunktDrammenLier
                        uonsketPunktBergenBjornafjorden -> erstatningsPunktBergenBjornafjorden
                        else -> grense.grense.startPoint
                    }
                    val endepunkt = when (grense.grense.endPoint) {
                        uonsketPunktFarsundKvinesdal -> erstatningsPunktFarsundKvinesdal
                        uonsketPunktDrammenOvreEiker -> erstatningsPunktDrammenOvreEiker
                        uonsketPunktDrammenLier -> erstatningsPunktDrammenLier
                        uonsketPunktBergenBjornafjorden -> erstatningsPunktBergenBjornafjorden
                        else -> grense.grense.endPoint
                    }

                    //Sender med eventuelle mellompunkt på grensa (dvs eksklusiv start- og sluttpunkt)
                    val uonsketPunktHolmestrandDrammen = Coordinate(231187.66, 6624494.9)
                    val grenserSomSkalErstatteGammelGrense = finnStiMellomPunkterMedBreddeFoerstSoek(
                        startpunkt, endepunkt, administrativeGrenser,
                        grense.grense.coordinateSequence.toCoordinateArray().toList().drop(1).dropLast(1).minus(uonsketPunktHolmestrandDrammen))

                    //Lag ny versjon av gammel grense sine stemmekretser hvis det ikke allerede er gjort, og knytt den nye grensa til stemmekretsene
                    grenserSomSkalErstatteGammelGrense.forEach { nyGrense ->
                        sb.append("  ${nyGrense.id}")
                    }
                    handterGrensekoblingForNyVersjonAvStemmekrets(gammelGrense = grense, gyldighetsdato = gyldighetsDato,
                        evtNyeVersjonerAvFlater = evtNyeVersjonerAvFlater, endringstype = Endringstype.FASTSETTING,
                        nyeVersjonerAvGrense = grenserSomSkalErstatteGammelGrense)
                }
                logger.info(sb.toString())
                // Fjern knytning til stemmekretser og arkiver grensa som har blitt byttet (ut med kortere linjestykker)
                arkiverGammelAdmGrenseforStemmekrets(stemmekretsGrenseSomSkalArkiveres = grense, gyldighetsDato, evtNyeVersjonerAvFlater)
            }

        }

        grenseRepository.flush()
        try {
            flateService.oppdaterFlaterFor(evtNyeVersjonerAvFlater.values.toSet())
        } catch (e: UgyldigeFlaterException) {
            //Må skrive ut her, ellers blir detaljene borte
            e.ugyldigeFlater.forEach {
                logger.error("Feil i flatedanning for ${it.flateInformasjon.flatetype} ${it.flateInformasjon.lesbartNavn}  med feil ${it.topologifeil}")
            }
            throw e
        }

        val eventRaderForOppdaterteFlater = mutableMapOf<String, EventRad>()
        evtNyeVersjonerAvFlater.forEach { eventRaderForOppdaterteFlater[it.key.value] = it.value.toEventRad(EventType.MODIFIED) }
        return Pair(administrativeGrenser, eventRaderForOppdaterteFlater)
    }

    private fun hentEvtVersjonFraBasenForFlaterSomHarFaattNyVersjonIfmNabokommune(
        grense: Grense,
        gyldighetsDato: LocalDate,
        evtNyeVersjonerAvFlater: MutableMap<Lokalid, Flate>,
        eventRaderForAlleredeOppdaterteFlater: Map<String, EventRad>
    ) {
        grense.grunnkretser(gyldighetsDato).forEach {
            if (!evtNyeVersjonerAvFlater.containsKey(Lokalid(it.lokalid)) && eventRaderForAlleredeOppdaterteFlater.containsKey(it.lokalid)) {
                evtNyeVersjonerAvFlater[Lokalid(it.lokalid)] = grunnkretsRepository.findById(it.id).unwrap()
                    ?: throw RuntimeException("Fant ikke grunnkrets med objid ${it.id}")
            }
        }
        grense.kommuner(gyldighetsDato).forEach {
            if (!evtNyeVersjonerAvFlater.containsKey(Lokalid(it.lokalid)) && eventRaderForAlleredeOppdaterteFlater.containsKey(it.lokalid)) {
                evtNyeVersjonerAvFlater[Lokalid(it.lokalid)] = kommuneRepository.findById(it.id).unwrap()
                    ?: throw RuntimeException("Fant ikke kommune med objid ${it.id}")
            }
        }
        if (Fylke.tillatteGrensetyper().contains(grense.grensetype)) {
            grense.fylker(gyldighetsDato).forEach {
                if (!evtNyeVersjonerAvFlater.containsKey(Lokalid(it.lokalid)) && eventRaderForAlleredeOppdaterteFlater.containsKey(it.lokalid)) {
                    evtNyeVersjonerAvFlater[Lokalid(it.lokalid)] = fylkeRepository.findById(it.id).unwrap()
                        ?: throw RuntimeException("Fant ikke fylke med objid ${it.id}")
                }
            }
        }
        if (Nasjon.tillatteGrensetyper().contains(grense.grensetype)) {
            grense.nasjoner(gyldighetsDato).forEach {
                if (!evtNyeVersjonerAvFlater.containsKey(Lokalid(it.lokalid)) && eventRaderForAlleredeOppdaterteFlater.containsKey(it.lokalid)) {
                    evtNyeVersjonerAvFlater[Lokalid(it.lokalid)] = nasjonRepository.findById(it.id).unwrap()
                        ?: throw RuntimeException("Fant ikke nasjon med objid ${it.id}")
                }
            }
        }
    }

    fun handterGrensekoblingForNyVersjonAvGrunnkretsOgHoyereIHierarkiet(
        gammelGrense: Grense,
        gyldighetsdato: LocalDate,
        evtNyeVersjonerAvFlater: MutableMap<Lokalid, Flate>,
        endringstype: Endringstype,
        nyVersjonAvGrense: Grense
    ): Map<Lokalid, Flate> {
        gammelGrense.gyldigeGrunnkretsGrenseKoblinger(gyldighetsdato).forEach {
            val grunnkretsLokalid = Lokalid(it.grunnkrets.lokalid)
            val identifikator = Identifikator(grunnkretsLokalid, gyldighetsdato)

            var grunnkrets = evtNyeVersjonerAvFlater[grunnkretsLokalid]
            if (grunnkrets == null) {
                val gammelGrunnkrets = grunnkretsRepository.findGyldigObjektForIdentifikator(identifikator)
                    ?: throw UgyldigTilstandException("Fant ikke gyldig grunnkrets for identifikator $identifikator")
                grunnkrets = grunnkretsService.createNyVersjonAvGrunnkrets(grunnkrets = gammelGrunnkrets,
                    gyldigFra = identifikator.gyldighetsdato, endringstype = endringstype)
            }
            // Skal bare legge til ny grense her.
            // Det å fjerne den gamle grensa fra ny versjon av grunnkrets gjøres etter at alle grensene er prosessert
            (grunnkrets as Grunnkrets).addGrensekobling(nyVersjonAvGrense)
            evtNyeVersjonerAvFlater[grunnkretsLokalid] = grunnkrets

            val nyeversjoner = grenseService.createNyeVersjonerAvFlaterHoyereIHierakietForGrenser(
                grenser = listOf(nyVersjonAvGrense),
                gyldighetsdato = gyldighetsdato,
                endringstype = endringstype,
                flaterSomHarFaattNyVersjon = evtNyeVersjonerAvFlater
            )
            nyeversjoner.forEach { flate -> evtNyeVersjonerAvFlater[Lokalid(flate.lokalid)] = flate }
        }
        grenseRepository.flush()
        return evtNyeVersjonerAvFlater
    }

    fun handterGrensekoblingForNyVersjonAvStemmekrets(
        gammelGrense: Grense,
        gyldighetsdato: LocalDate,
        evtNyeVersjonerAvFlater: MutableMap<Lokalid, Flate>,
        endringstype: Endringstype,
        nyeVersjonerAvGrense: List<Grense> = emptyList()
    ): Map<Lokalid, Flate> {

        gammelGrense.gyldigeStemmekretsGrenseKoblinger(gyldighetsdato).forEach { it ->
            val stemmekretsLokalid = Lokalid(it.stemmekrets.lokalid)
            val identifikator = Identifikator(stemmekretsLokalid, gyldighetsdato)

            var stemmekrets = evtNyeVersjonerAvFlater[stemmekretsLokalid]
            if (stemmekrets == null) {
                val gammelStemmekrets = stemmekretsRepository.findGyldigObjektForIdentifikator(identifikator)
                    ?: throw UgyldigTilstandException("Fant ikke gyldig stemmekrets for identifikator $identifikator")
                stemmekrets = stemmekretsService.createNyVersjonAvStemmekrets(stemmekrets = gammelStemmekrets,
                    gyldigFra = identifikator.gyldighetsdato, endringstype = endringstype)
            }

            // Legger til den/de nye grensene som erstatter den gamle stemmekretsgrensa
            // Det å fjerne den gamle grensa fra ny versjon av stemmekrets gjøres etter at begge stemmekretsene er prosessert
            nyeVersjonerAvGrense.forEach { (stemmekrets as Stemmekrets).addGrensekobling(it) }
            evtNyeVersjonerAvFlater[stemmekretsLokalid] = stemmekrets
        }
        grenseRepository.flush()
        return evtNyeVersjonerAvFlater
    }

    private fun arkiverGamleAdmGrenserforGrunnkrets(
        grunnkretsGrenserSomSkalArkiveres: Set<Grense>,
        gyldighetsDato: LocalDate,
        evtNyeVersjonerAvFlater: MutableMap<Lokalid, Flate>
    ) {
        grunnkretsGrenserSomSkalArkiveres.forEach { gammelGrense ->
            gammelGrense.grunnkretser(gyldighetsDato).forEach {
                if (evtNyeVersjonerAvFlater.containsKey(Lokalid(it.lokalid))) {
                    it.removeGrensekobling(gammelGrense)
                } else {
                    throw RuntimeException("Grense (${gammelGrense.lokalid}) som skal arkiveres " +
                        "har kobling til grunnkrets (${it.grunnkretsnummer}) som det ikke er ny versjon av")
                }
            }
            if (gammelGrense.gyldigeStemmekretsGrenseKoblinger(gyldighetsDato).isNotEmpty()) {
                val sb = StringBuilder("Grense (${gammelGrense.id}) som skal arkiveres har kobling til stemmekrets: ")
                gammelGrense.stemmekretser(gyldighetsDato).forEachIndexed { index, stemmekrets ->
                    if (index > 0) sb.append(" og ")
                    sb.append(" ${stemmekrets.stemmekretsnummer} i kommune: ${stemmekrets.getKommune(gyldighetsDato)?.kommunenummer}")
                }
                throw RuntimeException(sb.toString())
            }
            gammelGrense.setGyldigTil(gyldighetsDato)
        }
        grenseRepository.flush()
    }

    private fun arkiverGammelAdmGrenseforStemmekrets(
        stemmekretsGrenseSomSkalArkiveres: Grense,
        gyldighetsDato: LocalDate,
        evtNyeVersjonerAvFlater: MutableMap<Lokalid, Flate>
    ) {
        stemmekretsGrenseSomSkalArkiveres.stemmekretser(gyldighetsDato).forEach {
            if (evtNyeVersjonerAvFlater.containsKey(Lokalid(it.lokalid))) {
                it.removeGrensekobling(stemmekretsGrenseSomSkalArkiveres)
            } else {
                throw RuntimeException("Grense (${stemmekretsGrenseSomSkalArkiveres.lokalid}) som skal arkiveres " +
                    "har kobling til stemmekrets (${it.getFlateInformasjon().lesbartNavn}) som det ikke er ny versjon av")
            }
        }
        stemmekretsGrenseSomSkalArkiveres.setGyldigTil(gyldighetsDato)
        grenseRepository.flush()
    }

    private fun leggTilManglendeKnutepunktFraStemmekrets(grensepunkterForGrunnkrets: List<List<Coordinate>>, knutepunktStemmekrets: List<Coordinate>)
        : List<List<Coordinate>> {
        val knutepunktSomBareErIStemmekrets = knutepunktStemmekrets.filter { knutepunkt ->
            grensepunkterForGrunnkrets.flatten().none { erSammePunkt(it, knutepunkt) }
        }

        val nyeGrensepunkter = grensepunkterForGrunnkrets.map { it.toMutableList() }

        knutepunktSomBareErIStemmekrets.forEach {
            nyeGrensepunkter.finnPolygonHvorPunktPasser(it).insertPunktPaaRiktigPlass(it)
        }

        return nyeGrensepunkter.map { it.toList() }
    }

    private fun MutableList<Coordinate>.insertPunktPaaRiktigPlass(punkt: Coordinate) {
        val indexToInsertAfter = this.indices.find { index ->
            val lineSegment = LineSegment(this[index], this[(index + 1) % this.size])
            erPunktPaaLinje(punkt, lineSegment)
        }
        if (indexToInsertAfter != null) {
            this.add(indexToInsertAfter + 1, punkt)
        } else {
            throw RuntimeException("Klarte ikke å finne riktig plass å legge grensepunktet i listen. Gjelder punkt $punkt")
        }
    }

    /**
     * Denne funksjonen tar inn en liste med LineStrings som representerer alle grensene for en krets,
     * og returnerer en liste med lister av koordinater for hvert polygon
     *
     * Den ytterste listen har ett element for hver flate i denne kretsen, og den indre listen er liste med alle koordinater/grensepunkter som danner flaten.
     * Den indre listen har grensepunktene i sortert rekkefølge - slik at om man hadde tegnet en strek fra punkt til punkt vil man ende opp med å tegne flaten.
     */
    private fun getGrensepunkterIRiktigRekkefoelge(grenser: List<LineString>): List<List<Coordinate>> {
        return getGrenserIRiktigRekkefoelge(grenser)
            .map { komplettGrensePolygon ->
                komplettGrensePolygon.flatMap {
                    it.coordinateSequence.toCoordinateArray().toList().dropLast(1)
                }
            }
    }

    private fun List<MutableList<Coordinate>>.finnPolygonHvorPunktPasser(punkt: Coordinate): MutableList<Coordinate> {
        return this.find { grensesirkel ->
            grensesirkel.indices.any { index ->
                val lineSegment = LineSegment(grensesirkel[index], grensesirkel[(index + 1) % grensesirkel.size])
                erPunktPaaLinje(punkt, lineSegment)
            }
        } ?: throw RuntimeException("Fant ingen polygon dette punktet er en del av: $punkt")
    }

    private fun grensepunkterTilLineStringsMellomKnutepunkter(grensepunkter: List<Coordinate>, knutepunkter: List<Coordinate>): List<LineString> {
        val lineStrings = mutableListOf<LineString>()
        val resterendeGrensepunkter = grensepunkter.toMutableList()
        var koordinaterForNesteLinestring = mutableListOf(resterendeGrensepunkter.pop())

        while (resterendeGrensepunkter.size > 0) {
            val punkt = resterendeGrensepunkter.pop()
            koordinaterForNesteLinestring.add(punkt)

            if (knutepunkter.any { erSammePunkt(it, punkt) }) {
                lineStrings.add(geometryFactory.createLineString(koordinaterForNesteLinestring.toTypedArray()))
                koordinaterForNesteLinestring = mutableListOf(punkt)
            }
        }

        koordinaterForNesteLinestring.add(lineStrings[0].startPoint.coordinate)
        lineStrings.add(geometryFactory.createLineString(koordinaterForNesteLinestring.toTypedArray()))

        return lineStrings.toList()
    }

}
