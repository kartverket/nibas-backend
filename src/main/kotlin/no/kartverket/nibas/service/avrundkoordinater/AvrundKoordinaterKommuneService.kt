package no.kartverket.nibas.service.avrundkoordinater

import no.kartverket.nibas.api.v1.common.Flatetype
import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.common.UgyldigTilstandException
import no.kartverket.nibas.common.geometry.UgyldigeFlaterException
import no.kartverket.nibas.common.geometry.avrundLineString
import no.kartverket.nibas.common.geometry.getGeometryFactory
import no.kartverket.nibas.common.getInSpraak
import no.kartverket.nibas.domain.Endringstype
import no.kartverket.nibas.domain.Flate
import no.kartverket.nibas.domain.Fylke
import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.domain.Grunnkrets
import no.kartverket.nibas.domain.Kommune
import no.kartverket.nibas.domain.Nasjon
import no.kartverket.nibas.domain.Stemmekrets
import no.kartverket.nibas.events.EventRad
import no.kartverket.nibas.events.EventType
import no.kartverket.nibas.repository.FylkeRepository
import no.kartverket.nibas.repository.GrenseRepository
import no.kartverket.nibas.repository.GrunnkretsRepository
import no.kartverket.nibas.repository.KommuneRepository
import no.kartverket.nibas.repository.NasjonRepository
import no.kartverket.nibas.repository.StemmekretsRepository
import no.kartverket.nibas.repository.findGyldigObjektForIdentifikator
import no.kartverket.nibas.service.FlateService
import no.kartverket.nibas.service.GrenseService
import no.kartverket.nibas.service.GrunnkretsService
import no.kartverket.nibas.service.KommuneService
import no.kartverket.nibas.service.StemmekretsService
import no.kartverket.nibas.unwrap
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate


@Service
class AvrundKoordinaterKommuneService(
    val kommuneService: KommuneService,
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
    private val logger = LoggerFactory.getLogger(this.javaClass)

    /**
     * Funksjonen tar inn lokalid for en kommune, gyldighetsdato og et map med eventrader for flater som evt har fått ny versjon fra tidligere kommuner i kjøringen.
     * Den returnerer et map med eventrader for flater som fikk (evt har tidligere fått) ny versjon ifm de nye grensene.
     */
    @Transactional
    fun avrundKoordinaterForKommune(
        kommuneLokalid: String, eventRaderForAlleredeOppdaterteFlater: Map<String, EventRad>,
        gyldighetsDato: LocalDate, loggmelding: String = ""
    )
        : Map<String, EventRad> {
        val kommune =
            kommuneRepository.findGyldigObjektForIdentifikator(Identifikator(Lokalid(kommuneLokalid), gyldighetsDato))
                ?: throw RuntimeException("Fant ikke kommune med lokalid $kommuneLokalid på gyldighetsdato $gyldighetsDato")

        logger.info("$loggmelding ${kommune.kommunenummer} ${kommune.getAdministrativenhetNavn(gyldighetsDato).getInSpraak("nor")}")
        val evtNyeVersjonerAvFlater = mutableMapOf<Lokalid, Flate>()

        val kommuneSineGrunnkretsGrenser = kommune.getGrunnkretser(gyldighetsDato)
            .flatMap { it.getGrensekoblinger(gyldighetsDato) }.map { it.grense }.toSet()

        kommuneSineGrunnkretsGrenser.forEach {
            avrundEvtKoordinaterOgLagNyeVersjonerAvTilknyttedeInndelinger(it, gyldighetsDato, evtNyeVersjonerAvFlater, eventRaderForAlleredeOppdaterteFlater)
        }

        // Henter indre stemmekretsgrenser i kommunen. Administrative stemmekretsgrenser er tatt ifm grunnkretsene
        val kommuneSineStemmekretsGrenser = kommune.getStemmekretser(gyldighetsDato)
            .flatMap { it.getGrensekoblinger(gyldighetsDato) }.map { it.grense }
            .filter { it.grensetype == Grensetype.Stemmekretsgrense }.toSet()

        kommuneSineStemmekretsGrenser.forEach {
            avrundEvtKoordinaterOgLagNyeVersjonerAvTilknyttedeInndelinger(it, gyldighetsDato, evtNyeVersjonerAvFlater, eventRaderForAlleredeOppdaterteFlater)
        }

        val eventRaderForOppdaterteFlater = mutableMapOf<String, EventRad>()
        evtNyeVersjonerAvFlater.forEach {
            eventRaderForOppdaterteFlater[it.key.value] = it.value.toEventRad(EventType.MODIFIED)
        }
        return eventRaderForOppdaterteFlater
    }

    private fun avrundEvtKoordinaterOgLagNyeVersjonerAvTilknyttedeInndelinger(
        gammelGrense: Grense,
        gyldighetsDato: LocalDate,
        evtNyeVersjonerAvFlater: MutableMap<Lokalid, Flate>,
        eventRaderForAlleredeOppdaterteFlater: Map<String, EventRad>
    ) {
        val originaleKoordinater = gammelGrense.grense.coordinates
        val avrundetLineString = avrundLineString(originaleKoordinater)
        var nyVersjonAvGrense: Grense? = null

        if (!avrundetLineString.contentEquals(originaleKoordinater)) {
            //Det er en eller flere koordinater som er avrundet
            if (avrundetLineString.size == 2 && avrundetLineString[0] == avrundetLineString[1]) {
                //Det finnes sju bittesmå linjebiter med kun to punkt og hvor start=slutt etter at punktene er avrundet
                //Arkiverer disse grensene
                gammelGrense.setGyldigTil(gyldighetsDato)
                logger.info("Setter grense historisk, objid: ${gammelGrense.id} ${gammelGrense.grense} Avrundet grense: ${avrundetLineString[0]} ${avrundetLineString[1]}")
            } else {
                nyVersjonAvGrense = grenseService.createNyVersjonAvGrense(gammelGrense, gyldighetsDato, Endringstype.FASTSETTING)
                    .apply {
                        grense = getGeometryFactory().createLineString(avrundetLineString)
                    }
                logger.info("Avrunder grense objid: ${gammelGrense.id} Ny grense objid: ${nyVersjonAvGrense.id} ${nyVersjonAvGrense.grense}")
            }

            //Sjekk om grensa sine grunnkretser, stemmekretser, kommuner, fylker og nasjon har fått ny versjon ifm nabokommune
            hentEvtVersjonFraBasenForFlaterSomHarFaattNyVersjonIfmNabokommune(
                gammelGrense, gyldighetsDato, evtNyeVersjonerAvFlater, eventRaderForAlleredeOppdaterteFlater
            )

            // Lag ny versjon av gammel grense sine grunnkretser og stemmekretser hvis det ikke allerede er gjort,
            // og knytt den nye grensa til kretsene
            // Lag ny versjon av grensa sine kommuner/fylker/nasjon hvis det trengs
            handterGrensekoblingForNyVersjonAvKretserOgHoyereIHierarkiet(
                gammelGrense = gammelGrense,
                gyldighetsdato = gyldighetsDato,
                evtNyeVersjonerAvFlater = evtNyeVersjonerAvFlater,
                endringstype = Endringstype.FASTSETTING,
                nyVersjonAvGrense = nyVersjonAvGrense
            )
        }
    }

    fun hentEvtVersjonFraBasenForFlaterSomHarFaattNyVersjonIfmNabokommune(
        grense: Grense,
        gyldighetsDato: LocalDate,
        evtNyeVersjonerAvFlater: MutableMap<Lokalid, Flate>,
        eventRaderForAlleredeOppdaterteFlater: Map<String, EventRad>
    ) {
        grense.grunnkretser(gyldighetsDato).forEach {
            if (!evtNyeVersjonerAvFlater.containsKey(Lokalid(it.lokalid))
                && eventRaderForAlleredeOppdaterteFlater.containsKey(it.lokalid)
            ) {
                evtNyeVersjonerAvFlater[Lokalid(it.lokalid)] = grunnkretsRepository.findById(it.id).unwrap()
                    ?: throw RuntimeException("Fant ikke grunnkrets med objid ${it.id}")
            }
        }
        grense.stemmekretser(gyldighetsDato).forEach {
            if (!evtNyeVersjonerAvFlater.containsKey(Lokalid(it.lokalid))
                && eventRaderForAlleredeOppdaterteFlater.containsKey(it.lokalid)
            ) {
                evtNyeVersjonerAvFlater[Lokalid(it.lokalid)] = stemmekretsRepository.findById(it.id).unwrap()
                    ?: throw RuntimeException("Fant ikke stemmekrets med objid ${it.id}")
            }
        }

        if (Kommune.tillatteGrensetyper().contains(grense.grensetype)) {
            grense.kommuner(gyldighetsDato).forEach {
                if (!evtNyeVersjonerAvFlater.containsKey(Lokalid(it.lokalid))
                    && eventRaderForAlleredeOppdaterteFlater.containsKey(it.lokalid)
                ) {
                    evtNyeVersjonerAvFlater[Lokalid(it.lokalid)] = kommuneRepository.findById(it.id).unwrap()
                        ?: throw RuntimeException("Fant ikke kommune med objid ${it.id}")
                }
            }
        }
        if (Fylke.tillatteGrensetyper().contains(grense.grensetype)) {
            grense.fylker(gyldighetsDato).forEach {
                if (!evtNyeVersjonerAvFlater.containsKey(Lokalid(it.lokalid))
                    && eventRaderForAlleredeOppdaterteFlater.containsKey(it.lokalid)
                ) {
                    evtNyeVersjonerAvFlater[Lokalid(it.lokalid)] = fylkeRepository.findById(it.id).unwrap()
                        ?: throw RuntimeException("Fant ikke fylke med objid ${it.id}")
                }
            }
        }
        if (Nasjon.tillatteGrensetyper().contains(grense.grensetype)) {
            grense.nasjoner(gyldighetsDato).forEach {
                if (!evtNyeVersjonerAvFlater.containsKey(Lokalid(it.lokalid))
                    && eventRaderForAlleredeOppdaterteFlater.containsKey(it.lokalid)
                ) {
                    evtNyeVersjonerAvFlater[Lokalid(it.lokalid)] = nasjonRepository.findById(it.id).unwrap()
                        ?: throw RuntimeException("Fant ikke nasjon med objid ${it.id}")
                }
            }
        }

    }

    fun handterGrensekoblingForNyVersjonAvKretserOgHoyereIHierarkiet(
        gammelGrense: Grense,
        gyldighetsdato: LocalDate,
        evtNyeVersjonerAvFlater: MutableMap<Lokalid, Flate>,
        endringstype: Endringstype,
        nyVersjonAvGrense: Grense?
    ): Map<Lokalid, Flate> {
        gammelGrense.grunnkretser(gyldighetsdato).forEach {
            val grunnkretsLokalid = Lokalid(it.lokalid)
            val identifikator = Identifikator(grunnkretsLokalid, gyldighetsdato)

            var grunnkrets = evtNyeVersjonerAvFlater[grunnkretsLokalid]
            if (grunnkrets == null) {
                val gammelGrunnkrets = grunnkretsRepository.findGyldigObjektForIdentifikator(identifikator)
                    ?: throw UgyldigTilstandException("Fant ikke gyldig grunnkrets for identifikator $identifikator")
                grunnkrets = grunnkretsService.createNyVersjonAvGrunnkrets(
                    grunnkrets = gammelGrunnkrets,
                    gyldigFra = identifikator.gyldighetsdato, endringstype = endringstype
                )
            }
            // Bytter ut gammel grense med den nye versjonen
            // Kan ikke bruke replaceGrensekobling, fordi den add-er bare ny grense hvis gammel grense kan tas vekk, og det er ikke alltid tilfelle
            (grunnkrets as Grunnkrets).removeGrensekobling(gammelGrense)
            if (nyVersjonAvGrense != null) {
                grunnkrets.addGrensekobling(nyVersjonAvGrense)
            }
            evtNyeVersjonerAvFlater[grunnkretsLokalid] = grunnkrets

            val nyeversjoner = grenseService.createNyeVersjonerAvFlaterHoyereIHierakietForGrenser(
                grenser = listOf(nyVersjonAvGrense ?: gammelGrense),
                gyldighetsdato = gyldighetsdato,
                endringstype = endringstype,
                flaterSomHarFaattNyVersjon = evtNyeVersjonerAvFlater
            )
            nyeversjoner.forEach { flate -> evtNyeVersjonerAvFlater[Lokalid(flate.lokalid)] = flate }
            grenseRepository.flush()
        }

        gammelGrense.stemmekretser(gyldighetsdato).forEach { it ->
            val stemmekretsLokalid = Lokalid(it.lokalid)
            val identifikator = Identifikator(stemmekretsLokalid, gyldighetsdato)

            var stemmekrets = evtNyeVersjonerAvFlater[stemmekretsLokalid]
            if (stemmekrets == null) {
                val gammelStemmekrets = stemmekretsRepository.findGyldigObjektForIdentifikator(identifikator)
                    ?: throw UgyldigTilstandException("Fant ikke gyldig stemmekrets for identifikator $identifikator")
                stemmekrets = stemmekretsService.createNyVersjonAvStemmekrets(
                    stemmekrets = gammelStemmekrets,
                    gyldigFra = identifikator.gyldighetsdato, endringstype = endringstype
                )
            }

            // Bytter ut gammel grense med den nye versjonen
            // Kan ikke bruke replaceGrensekobling, fordi den add-er bare ny grense hvis gammel grense kan tas vekk, og det er ikke alltid tilfelle
            (stemmekrets as Stemmekrets).removeGrensekobling(gammelGrense)
            if (nyVersjonAvGrense != null) {
                stemmekrets.addGrensekobling(nyVersjonAvGrense)
            }
            evtNyeVersjonerAvFlater[stemmekretsLokalid] = stemmekrets
            grenseRepository.flush()
        }

        return evtNyeVersjonerAvFlater
    }

    @Transactional
    fun oppdaterAlleFlaterOgLoggEvtFeil(
        eventRaderForOppdaterteFlater: MutableMap<String, EventRad>
    ) {
        eventRaderForOppdaterteFlater.values.filter { it.flateType == Flatetype.GRUNNKRETS }.map { it.uuid }.chunked(1000)
            .forEach {
                logger.info("Henter ${it.size} grunnkretser for flatedanning")
                oppdaterChunckMedFlaterOgLoggEvtFeil(grunnkretsRepository.findAllById(it).toSet())
            }

        eventRaderForOppdaterteFlater.values.filter { it.flateType == Flatetype.STEMMEKRETS }.map { it.uuid }.chunked(1000)
            .forEach {
                logger.info("Henter ${it.size} stemmekretser for flatedanning")
                oppdaterChunckMedFlaterOgLoggEvtFeil(stemmekretsRepository.findAllById(it).toSet())
            }

        oppdaterChunckMedFlaterOgLoggEvtFeil(kommuneRepository.findAllById(eventRaderForOppdaterteFlater.values.filter
        { it.flateType == Flatetype.KOMMUNE }.map { it.uuid }).toSet())

        oppdaterChunckMedFlaterOgLoggEvtFeil(fylkeRepository.findAllById(eventRaderForOppdaterteFlater.values.filter
        { it.flateType == Flatetype.FYLKE }.map { it.uuid }).toSet())

        oppdaterChunckMedFlaterOgLoggEvtFeil(nasjonRepository.findAllById(eventRaderForOppdaterteFlater.values.filter
        { it.flateType == Flatetype.NASJON }.map { it.uuid }).toSet())
    }

    fun oppdaterChunckMedFlaterOgLoggEvtFeil(enheterSomSkalFlatedannes: Set<Flate>) {
        try {
            flateService.oppdaterFlaterFor(enheterSomSkalFlatedannes)
        } catch (e: UgyldigeFlaterException) {
            //Må skrive ut her, ellers blir detaljene borte
            e.ugyldigeFlater.forEach {
                logger.error("Feil i flatedanning for ${it.flateInformasjon.flatetype} ${it.flateInformasjon.lesbartNavn} med feil ${it.topologifeil} ved koordinat ${it.koordinat}")
            }
            throw e
        }
    }

}
