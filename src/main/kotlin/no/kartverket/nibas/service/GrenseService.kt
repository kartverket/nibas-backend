package no.kartverket.nibas.service

import no.kartverket.nibas.api.v1.common.Flatetype
import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.api.v1.common.KontekstEnhetType
import no.kartverket.nibas.api.v1.common.geojson.Feature
import no.kartverket.nibas.api.v1.common.geojson.FeatureCollection
import no.kartverket.nibas.api.v1.common.geojson.metadata.AdministrativGrenseMetadata
import no.kartverket.nibas.api.v1.common.geojson.metadata.AvtaltAvgrensningslinjeMetadata
import no.kartverket.nibas.api.v1.common.geojson.metadata.KommunalKretsgrenseMetadata
import no.kartverket.nibas.api.v1.common.geojson.metadata.MetadataUpdater
import no.kartverket.nibas.api.v1.common.geojson.metadata.RiksgrenseMetadata
import no.kartverket.nibas.api.v1.common.geojson.metadata.StatistiskgrenseMetadata
import no.kartverket.nibas.api.v1.common.geojson.metadata.TerritorialgrenseMetadata
import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.common.ResponseException
import no.kartverket.nibas.common.ResponseException.BadRequestException
import no.kartverket.nibas.common.UgyldigTilstandException
import no.kartverket.nibas.domain.Endringstype
import no.kartverket.nibas.domain.Flate
import no.kartverket.nibas.domain.Fylke
import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.domain.GrenseKobling
import no.kartverket.nibas.domain.GrenseKoblingAttributter
import no.kartverket.nibas.domain.Grunnkrets
import no.kartverket.nibas.domain.GrunnkretsGrenseKobling
import no.kartverket.nibas.domain.Kommune
import no.kartverket.nibas.domain.Stemmekrets
import no.kartverket.nibas.domain.StemmekretsGrenseKobling
import no.kartverket.nibas.extensions.geometry.toLineString
import no.kartverket.nibas.kodelister.KodelisteService
import no.kartverket.nibas.repository.GrenseRepository
import no.kartverket.nibas.repository.GrunnkretsRepository
import no.kartverket.nibas.repository.OptimistiskLaasException
import no.kartverket.nibas.repository.StemmekretsRepository
import no.kartverket.nibas.repository.findAllGyldigeObjekterForIdentifikatorer
import no.kartverket.nibas.repository.findGyldigObjektForIdentifikator
import no.kartverket.nibas.repository.verifiserOptimistiskLaas
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import kotlin.system.measureTimeMillis

@Transactional(readOnly = true)
@Service
class GrenseService(
    private val grenseRepository: GrenseRepository,
    private val grunnkretsRepository: GrunnkretsRepository,
    private val stemmekretsRepository: StemmekretsRepository,
    private val flateService: FlateService,
    private val grunnkretsService: GrunnkretsService,
    private val stemmekretsService: StemmekretsService,
    private val kommuneService: KommuneService,
    private val fylkeService: FylkeService,
    private val nasjonService: NasjonService,
    private val kodelisteService: KodelisteService,
    private val grenseKoblingService: GrenseKoblingService
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun hentGrense(identifikator: Identifikator): Grense? = grenseRepository.findGyldigObjektForIdentifikator(identifikator)

    fun hentSisteVersjonAvGrense(identifikator: Identifikator): Grense? {
        return grenseRepository.findAllByLokalid(identifikator.lokalid.value).maxBy { it.version }
    }

    fun hentAlleVersjonerMedGyldigFraEtterGittDato(lokalid: String, gyldigFra: LocalDate): List<Grense> =
        grenseRepository.findAllByLokalid(lokalid)
            .filter { it.gyldighet.gyldigFra.isAfter(gyldigFra) }


    @Transactional(readOnly = false)
    fun lagreGrenser(grenser: FeatureCollection, gyldighetsdato: LocalDate = LocalDate.now(), endringstype: Endringstype): Set<Flate> {
        val enheterSomSkalFlatedannes: Set<Flate>

        val millis = measureTimeMillis {
            val features = grenser.features
            val (oppdaterteKretser, oppdaterteGrenser) = doLagreGrenser(
                features = features,
                gyldighetsdato = gyldighetsdato,
                endringstype = endringstype
            )

            enheterSomSkalFlatedannes = features.flatMap {
                val identifikator = featureIdToIdentifikator(it.id, gyldighetsdato)
                val grense = if (it.properties.shouldArchive) {
                    hentSisteVersjonAvGrense(identifikator) ?: throw ResponseException.NotFoundException("Grense $identifikator ble ikke funnet.")
                } else {
                    oppdaterteGrenser.firstOrNull { grense -> grense.lokalid == identifikator.lokalid.value } ?: throw ResponseException.NotFoundException("Grense $identifikator ble ikke funnet.")
                }

                when (Grensetype.fromValue(it.properties.type)) {
                    //TODO: Når vi slår sammen administrative grenser for stemme- og grunnkrets må vi huske å oppdatere stemmekretsene også når vi oppdatererer en administrativ grense. Pr nå tas ikke disse flatene med utenfor "stemmekrets-sfæren"
                    Grensetype.Kommunegrense -> grense.finnFlaterOppTilKommune(gyldighetsdato)
                    Grensetype.Fylkesgrense -> grense.finnFlaterOppTilFylke(gyldighetsdato)
                    Grensetype.Riksgrense -> grense.finnFlaterOppTilNasjon(gyldighetsdato)
                    Grensetype.AvtaltAvgrensningslinje -> grense.finnFlaterOppTilNasjon(gyldighetsdato)
                    Grensetype.Territorialgrense -> grense.finnFlaterOppTilNasjon(gyldighetsdato)
                    Grensetype.Grunnkretsgrense -> grense.finnFlaterOppTilGrunnkrets(gyldighetsdato)
                    Grensetype.Delomraadegrense -> grense.finnFlaterOppTilGrunnkrets(gyldighetsdato)
                    Grensetype.Stemmekretsgrense -> grense.finnFlaterOppTilStemmekrets(gyldighetsdato)
                    else -> throw RuntimeException("Not yet implemented for grensetype: ${it.properties.type}")
                }
            }.plus(oppdaterteKretser)
                .toSet()

            //Her er det bare å oppdatere, kretsene/adm.enhetene fikk ny versjon ved lagring av grenser og grensekoblinger
            flateService.oppdaterFlaterFor(enheterSomSkalFlatedannes)
        }

        logger.debug("Tid brukt på å lagre grenser og oppdatere flater: $millis")
        return enheterSomSkalFlatedannes
    }

    // Feature.id vil være lokalid for grensa
    private fun doLagreGrenser(features: List<Feature>, gyldighetsdato: LocalDate = LocalDate.now(), endringstype: Endringstype): Pair<Set<Flate>, Set<Grense>> {
        // Hvis det er innkommende features med id ikke satt kastes HTTP 400
        if (features.any { feature -> feature.id == null }) {
            throw BadRequestException("En av de innsendte featurene med endring mangler ID.")
        }
        // Henter eksisterende grenser på identifikator. Sorterer på lokalid for testens skyld.
        val grenseIdentifikatorer = features.map { featureIdToIdentifikator(it.id, gyldighetsdato) }.sortedBy { it.lokalid.value }
        val identifikatorToGrenseMap = grenseRepository.findAllGyldigeObjekterForIdentifikatorer(grenseIdentifikatorer)
        val eksisterendeGrenser: List<Grense> = identifikatorToGrenseMap.values.toList()

        // Hvis det er innkommende features med id som ikke ligger i databasen kastes HTTP 400
        val identifikatorDiffs: List<Identifikator> = grenseIdentifikatorer - identifikatorToGrenseMap.keys
        if (identifikatorDiffs.isNotEmpty()) {
            throw BadRequestException(
                title = "Klarte ikke finne grenser",
                msg = "En eller flere grenser du har endret på eksisterer ikke lengre. For å sørge for at du endrer på en gjeldende grense må du dessverre opprette et nytt utkast og gjennomføre endringene dine på nytt.",
                additionalInfo = "Feilen gjelder følgende grenser: ${identifikatorDiffs.joinToString(",") { "(lokalid: ${it.lokalid.value}), gyldig fra: ${it.gyldighetsdato})" }}")
        }

        sjekkForKonflikterForGrenserOgTilhorendeKretser(features, eksisterendeGrenser, gyldighetsdato)

        //Flere endrede grenser kan være knyttet til samme krets.
        //Ei endra grense kan ha flere knytninger til samme krets, dvs til ulike versjoner av samme krets
        //Må derfor ha oversikt over hvilke flater som har fått ny versjon
        val flaterSomHarFaattNyVersjon: MutableMap<Lokalid, Flate> = mutableMapOf()
        val grenserSomHarFaattNyVersjon = mutableMapOf<Lokalid, Grense>()

        features.forEach { feature ->
            val gammelGrense: Grense = findGrenseForIdentifikator(eksisterendeGrenser, featureIdToIdentifikator(feature.id, gyldighetsdato))

            //todo Får bare inn én krets tilhørende grensa (TS-918). Må sjekke om grensa har flere kretser og oppdatere kretsen på den andre sida
            //Må jobbe på en kopi av lista, for ikke å få ConcurrentModificationException
            val gamleStemmekretsKoblinger = mutableSetOf<StemmekretsGrenseKobling>()
            gamleStemmekretsKoblinger.addAll(gammelGrense.gyldigeStemmekretsGrenseKoblinger(gyldighetsdato))

            // Uansett om gyldigFra er identisk med grensens gyldigFra eller fram i tid: Lage ny rad (ny uuid, samme lokalid).
            logger.info("Lagrer grense: [${gammelGrense.grensetype.name} - id ${feature.id}]")

            val nyGrense = if (feature.properties.shouldArchive) {
                null
            } else {
                createNyVersjonAvGrense(gammelGrense, gyldighetsdato, endringstype)
                    .applyChangesFromRequest(feature)
            }
            if (nyGrense != null){
                grenserSomHarFaattNyVersjon[Lokalid(nyGrense.lokalid)] = nyGrense

            }

            //Det må lages en ny forekomst for tilknyttede kretser, de får også oppdatert knytning til gjeldende grense.
            //De får oppdatert område etter at alle grensene er behandlet

            gamleStemmekretsKoblinger.forEach {
                lagEvtNyVersjonAvStemmekretsOgEvtLeggTilNyGrense(flaterSomHarFaattNyVersjon, Identifikator(Lokalid(it.stemmekrets.lokalid), gyldighetsdato),
                    endringstype, gammelGrense, nyGrense,
                    GrenseKoblingAttributter(
                        it.grenseKoblingAttributter.retningMedKlokken,
                        it.grenseKoblingAttributter.rekkefoelge,
                        it.grenseKoblingAttributter.flateIndeks,
                        it.grenseKoblingAttributter.hullIndeks
                    )
                )
            }

            val gamleGrunnkretsKoblinger = mutableSetOf<GrunnkretsGrenseKobling>()
            gamleGrunnkretsKoblinger.addAll(gammelGrense.gyldigeGrunnkretsGrenseKoblinger(gyldighetsdato))

            gamleGrunnkretsKoblinger.forEach {
                lagEvtNyVersjonAvGrunnkretsOgLeggEvtTilNyGrense(flaterSomHarFaattNyVersjon, Identifikator(Lokalid(it.grunnkrets.lokalid), gyldighetsdato),
                    endringstype, gammelGrense, nyGrense,
                    GrenseKoblingAttributter(
                        it.grenseKoblingAttributter.retningMedKlokken,
                        it.grenseKoblingAttributter.rekkefoelge,
                        it.grenseKoblingAttributter.flateIndeks,
                        it.grenseKoblingAttributter.hullIndeks
                    )
                )
            }

            if (feature.properties.shouldArchive) {
                gammelGrense.setGyldigTil(gyldighetsdato)
            }
        }

        // VIKTIG! Denne flushen MÅ stå etter alle endringer har blitt gjort BORTSETT fra tilhørighetendringer
        grenseRepository.flush()

        // Etter alle andre endringer sjekker vi om det er endringer i tilhørighet for noen grenser
        features.forEach{ feature ->
            val grense = grenserSomHarFaattNyVersjon[featureIdToIdentifikator(feature.id, gyldighetsdato).lokalid]
            if(grense != null){
                doEvtTilhorighetEndringer(feature, grense, gyldighetsdato, flaterSomHarFaattNyVersjon, endringstype)
            }
        }

        return Pair(flaterSomHarFaattNyVersjon.values.toSet(), grenserSomHarFaattNyVersjon.values.toSet())
    }

    private fun sjekkForKonflikterForGrenserOgTilhorendeKretser(features: List<Feature>, eksisterendeGrenser: List<Grense>, gyldighetsdato: LocalDate){
        val konflikterIGrenser= features.mapNotNull { feature ->
            val featureIdentifikator = featureIdToIdentifikator(feature.id, gyldighetsdato)
            verifiserOptimistiskLaas(feature.properties.version, findGrenseForIdentifikator(eksisterendeGrenser, featureIdentifikator))
        }
        val konflikterIKretserForGrenser = features.flatMap { feature ->
            feature.properties.kontekstEgenskaper
                ?.mapNotNull { kontekstEgenskaper ->
                    if (kontekstEgenskaper.id == null){
                        throw RuntimeException("KontekstEgenskaper for feature med id: ${feature.id} er null")
                    }
                    val identifikator = Identifikator(kontekstEgenskaper.id.lokalid, gyldighetsdato)
                    verifiserOptimistiskLaas(
                        kontekstEgenskaper.version,
                        when (kontekstEgenskaper.type) {
                            KontekstEnhetType.GRUNNKRETS -> grunnkretsRepository.findGyldigObjektForIdentifikator(identifikator)
                                ?: throw RuntimeException("Fant ikke Grunnkrets med lokalid: ${identifikator.lokalid}")
                            KontekstEnhetType.STEMMEKRETS -> stemmekretsRepository.findGyldigObjektForIdentifikator(identifikator)
                                ?: throw RuntimeException("Fant ikke Stemmekrets med lokalid: ${identifikator.lokalid}")
                            null -> throw RuntimeException("Ukjent KontekstEnhetType")
                        }
                    )
                } ?: emptyList()
        }
        val alleKonflikter = (konflikterIGrenser+konflikterIKretserForGrenser)
        if (alleKonflikter.isNotEmpty()) {
            throw OptimistiskLaasException(*alleKonflikter.toTypedArray())
        }
    }

    fun doEvtTilhorighetEndringer(
        innsendtFeature: Feature, grense: Grense, gyldighetsdato: LocalDate, flaterSomHarFaattNyVersjon: MutableMap<Lokalid, Flate>, endringstype: Endringstype
    ){
        if (grense.grensetype == Grensetype.Grunnkretsgrense || grense.grensetype == Grensetype.Delomraadegrense){
            val innsendteTilhorigheter = innsendtFeature.properties.kontekstEgenskaper?.filter { it.type == KontekstEnhetType.GRUNNKRETS }
                ?: throw RuntimeException("Ingen kontekstegenskaper for grense med id: ${grense.lokalid}")
            val eksisterendeTilhorigheter = grense.gyldigeGrunnkretsGrenseKoblinger(gyldighetsdato).toList()
            if (!innsendteTilhorigheter.map { it.id?.lokalid?.value }.containsAll(eksisterendeTilhorigheter.map { it.grunnkrets.lokalid })){
                if(innsendteTilhorigheter.size != 2 || innsendteTilhorigheter[0].id?.lokalid == innsendteTilhorigheter[1].id?.lokalid){
                    throw RuntimeException("$innsendteTilhorigheter kan ikke settes som grunnkretser for grense med id: ${grense.lokalid}")
                }
                val tilhorighetEndringer =
                    grenseKoblingService.getTilhorighetEndringer(eksisterendeTilhorigheter, innsendteTilhorigheter, gyldighetsdato)

                val eksisterendeGrenser = mutableMapOf<Lokalid, List<Grense>>()
                val lokalidForKretser = (innsendteTilhorigheter.map { it.id?.lokalid }+eksisterendeTilhorigheter.map { Lokalid(it.grunnkrets.lokalid) }).toSet()
                lokalidForKretser.forEach {
                    if(it == null){
                        throw RuntimeException("Kontekst peker ikke til en grunnkrets")
                    }
                    val grenser = grunnkretsService.hentGrenserForGrunnkrets(Identifikator(it, gyldighetsdato)).map { gk -> gk.grense }
                    eksisterendeGrenser[it] = grenser
                }
                grenseKoblingService.leggTilGrenserForKretserViaKontekstEgenskaper(tilhorighetEndringer.nye, grense, eksisterendeGrenser, gyldighetsdato, flaterSomHarFaattNyVersjon, endringstype, Flatetype.GRUNNKRETS)
                grenseKoblingService.fjernGrenseFraKretserViaGrenseKobling(grense, eksisterendeGrenser, tilhorighetEndringer.frafallende, gyldighetsdato, flaterSomHarFaattNyVersjon, endringstype)
                grenseKoblingService.oppdaterGrenseKoblingAttributter(lokalidForKretser, flaterSomHarFaattNyVersjon, Flatetype.GRUNNKRETS)
            }
        }
        else if (grense.grensetype == Grensetype.Stemmekretsgrense){
            val innsendteTilhorigheter = innsendtFeature.properties.kontekstEgenskaper?.filter { it.type == KontekstEnhetType.STEMMEKRETS }
                ?: throw RuntimeException("Ingen kontekstegenskaper for grense med id: ${grense.lokalid}")
            val eksisterendeTilhorigheter = grense.gyldigeStemmekretsGrenseKoblinger(gyldighetsdato).toList()
            if (!innsendteTilhorigheter.map { it.id?.lokalid?.value }.containsAll(eksisterendeTilhorigheter.map { it.stemmekrets.lokalid })){
                if(innsendteTilhorigheter.size != 2 || innsendteTilhorigheter[0].id?.lokalid == innsendteTilhorigheter[1].id?.lokalid){
                    throw RuntimeException("$innsendteTilhorigheter kan ikke settes som stemmekretser for grense med id: ${grense.lokalid}")
                }
                val tilhorighetEndringer =
                    grenseKoblingService.getTilhorighetEndringer(eksisterendeTilhorigheter, innsendteTilhorigheter, gyldighetsdato)

                val eksisterendeGrenser = mutableMapOf<Lokalid, List<Grense>>()
                val lokalidForKretser = (innsendteTilhorigheter.map { it.id?.lokalid }+eksisterendeTilhorigheter.map { Lokalid(it.stemmekrets.lokalid) }).toSet()
                lokalidForKretser.forEach {
                    if(it == null){
                        throw RuntimeException("Kontekst peker ikke til en stemmekrets")
                    }
                    val grenser = stemmekretsService.hentGrenserForStemmekrets(Identifikator(it, gyldighetsdato)).map { gk -> gk.grense }
                    eksisterendeGrenser[it] = grenser
                }
                grenseKoblingService.leggTilGrenserForKretserViaKontekstEgenskaper(tilhorighetEndringer.nye, grense, eksisterendeGrenser, gyldighetsdato, flaterSomHarFaattNyVersjon, endringstype, Flatetype.STEMMEKRETS)
                grenseKoblingService.fjernGrenseFraKretserViaGrenseKobling(grense, eksisterendeGrenser, tilhorighetEndringer.frafallende, gyldighetsdato, flaterSomHarFaattNyVersjon, endringstype)
                grenseKoblingService.oppdaterGrenseKoblingAttributter(lokalidForKretser, flaterSomHarFaattNyVersjon, Flatetype.STEMMEKRETS)
            }
        }
    }

    fun createNyVersjonAvGrense(grense: Grense, gyldigFra: LocalDate, endringstype: Endringstype): Grense {
        val nyGrense = grenseRepository.saveAndFlush(
            Grense(lokalid = grense.lokalid, grensetype = grense.grensetype)
                .apply {
                    applyMappingFromExistingGrense(grense)
                    setGyldigFra(gyldigFra)
                    version = nesteVersjonForGrense(Lokalid(grense.lokalid))
                    auditInfo.typeEndring = endringstype
                }
        )

        return nyGrense
            .applyCascadingOperationsBasedOnExistingGrense(grense)
            .also {
                grense.setGyldigTil(gyldigFra)
                grense.nesteVersjon = nyGrense
            }
    }

    fun Grense.applyChangesFromRequest(feature: Feature): Grense =
        this.apply {
            grense = feature.geometry.toLineString(feature.properties.srid)
            updateMetadataOnGrense(feature, this)
        }

    private fun lagEvtNyVersjonAvStemmekretsOgEvtLeggTilNyGrense(
        flaterSomHarFaattNyVersjon: MutableMap<Lokalid, Flate>,
        kontekstId: Identifikator,
        endringstype: Endringstype,
        gammelGrense: Grense,
        nyGrense: Grense?,
        grenseKoblingAttributter: GrenseKoblingAttributter
    ) {
        var stemmekrets = flaterSomHarFaattNyVersjon[kontekstId.lokalid]
        if (stemmekrets == null) {
            val gammelStemmekrets = stemmekretsRepository.findGyldigObjektForIdentifikator(kontekstId)
                ?: throw UgyldigTilstandException("Fant ikke gyldig stemmekrets for identifikator $kontekstId")
            stemmekrets = stemmekretsService.createNyVersjonAvStemmekrets(stemmekrets = gammelStemmekrets,
                gyldigFra = kontekstId.gyldighetsdato, endringstype = endringstype)
            flaterSomHarFaattNyVersjon[kontekstId.lokalid] = stemmekrets
        }

        //Den nye stemmekretsen har fått kobling til alle grensene som den gamle har
        //Må bytte ut koblinga til den oppdaterte grensa
        (stemmekrets as Stemmekrets).replaceGrensekobling(gammelGrense, nyGrense, grenseKoblingAttributter)

    }

    private fun lagEvtNyVersjonAvGrunnkretsOgLeggEvtTilNyGrense(
        flaterSomHarFaattNyVersjon: MutableMap<Lokalid, Flate>,
        kontekstId: Identifikator,
        endringstype: Endringstype,
        gammelGrense: Grense,
        nyGrense: Grense?,
        grenseKoblingAttributter: GrenseKoblingAttributter
    ) {
        var grunnkrets = flaterSomHarFaattNyVersjon[kontekstId.lokalid]
        val gyldigFra = kontekstId.gyldighetsdato
        if (grunnkrets == null) {
            val gammelGrunnkrets = grunnkretsRepository.findGyldigObjektForIdentifikator(kontekstId)
                ?: throw UgyldigTilstandException("Fant ikke gyldig grunnkrets for identifikator $kontekstId")
            grunnkrets = grunnkretsService.createNyVersjonAvGrunnkrets(grunnkrets = gammelGrunnkrets, gyldigFra = gyldigFra, endringstype = endringstype)
            flaterSomHarFaattNyVersjon[kontekstId.lokalid] = grunnkrets
        }

        //Den nye grunnkretsen har fått kobling til alle grensene som den gamle har
        //Må bytte ut koblinga til den oppdaterte grensa
        (grunnkrets as Grunnkrets).replaceGrensekobling(gammelGrense, nyGrense, grenseKoblingAttributter)

        //En arkivert grense kan ikke være av typen nasjon/fylke/kommune, derfor returneres det her.
        if (nyGrense == null) return

        //Hvis grensa er grense for kommune, fylke eller nasjon, skal det lages nye versjoner av dem
        if (Grensetype.erNasjonsgrense(nyGrense.grensetype) ||
            Grensetype.Fylkesgrense == nyGrense.grensetype ||
            Grensetype.Kommunegrense == nyGrense.grensetype) {

            //Skal lage ny versjon av kommune, hvis det ikke allerede er gjort
            val gammelKommune = grunnkrets.getKommune(gyldighetsdato = gyldigFra)
                ?: throw UgyldigTilstandException("Fant ikke gyldig kommune for $grunnkrets")
            val kommuneLokalid = Lokalid(gammelKommune.lokalid)
            var kommune = flaterSomHarFaattNyVersjon[kommuneLokalid]
            if (kommune == null) {
                kommune = kommuneService.createNyVersjonAvKommune(kommune = gammelKommune, gyldigFra = gyldigFra, endringstype = endringstype)
                flaterSomHarFaattNyVersjon[kommuneLokalid] = kommune
            }

            if (Grensetype.erNasjonsgrense(nyGrense.grensetype) || Grensetype.Fylkesgrense == nyGrense.grensetype) {
                //Skal lage ny versjon av fylke, hvis det ikke allerede er gjort
                val gammeltFylke = (kommune as Kommune).getFylke(gyldigFra)
                    ?: throw UgyldigTilstandException("Fant ikke gyldig fylke for $kommune")
                val fylkeLokalid = Lokalid(gammeltFylke.lokalid)
                var fylke = flaterSomHarFaattNyVersjon[fylkeLokalid]
                if (fylke == null) {
                    fylke = fylkeService.createNyVersjonAvFylke(fylke = gammeltFylke, gyldigFra = gyldigFra, endringstype = endringstype)
                    flaterSomHarFaattNyVersjon[fylkeLokalid] = fylke
                }

                if (Grensetype.erNasjonsgrense(nyGrense.grensetype)) {
                    //Skal lage ny versjon av nasjon, hvis det ikke allerede er gjort
                    val gammelNasjon = (fylke as Fylke).getNasjon(gyldigFra)
                        ?: throw UgyldigTilstandException("Fant ikke gyldig nasjon for $fylke")
                    val nasjonLokalid = Lokalid(gammelNasjon.lokalid)
                    var nasjon = flaterSomHarFaattNyVersjon[nasjonLokalid]
                    if (nasjon == null) {
                        nasjon = nasjonService.createNyVersjonAvNasjon(nasjon = gammelNasjon, gyldigFra = gyldigFra, endringstype = endringstype)
                        flaterSomHarFaattNyVersjon[nasjonLokalid] = nasjon
                    }
                }
            }
        }

    }

    private fun featureIdToIdentifikator(lokalid: String?, gyldighetsdato: LocalDate): Identifikator = Identifikator(Lokalid(lokalid!!), gyldighetsdato)
    private fun findGrenseForIdentifikator(grenser: List<Grense>, identifikator: Identifikator): Grense {
        return grenser.find { it.lokalid == identifikator.lokalid.value && it.gyldigPaaDato(identifikator.gyldighetsdato) }!!
    }

    private fun nesteVersjonForGrense(lokalId: Lokalid): Int =
        grenseRepository
            .findAllByLokalid(lokalId.value)
            .maxBy { it.version }
            .version + 1

    private fun updateMetadataOnGrense(
        feature: Feature,
        grense: Grense
    ): Grense {
        val featureMetadata = feature.properties.metadata
        if (featureMetadata != null) {
            val metadataUpdater = MetadataUpdater()
            when (grense.grensetype) {
                Grensetype.Kommunegrense -> metadataUpdater.updateAdministrativGrense(grense, (featureMetadata as AdministrativGrenseMetadata))
                Grensetype.Fylkesgrense -> metadataUpdater.updateAdministrativGrense(grense, (featureMetadata as AdministrativGrenseMetadata))
                Grensetype.Riksgrense -> metadataUpdater.updateRiksgrense(grense, (featureMetadata as RiksgrenseMetadata))
                Grensetype.AvtaltAvgrensningslinje -> metadataUpdater.updateAvtaltavgrensningslinje(
                    grense = grense,
                    metadata = (featureMetadata as AvtaltAvgrensningslinjeMetadata)
                )

                Grensetype.Territorialgrense -> metadataUpdater.updateTerritoritalgrense(grense, (featureMetadata as TerritorialgrenseMetadata))
                Grensetype.Delomraadegrense -> metadataUpdater.updateStatistiskGrense(grense, (featureMetadata as StatistiskgrenseMetadata))
                Grensetype.Grunnkretsgrense -> metadataUpdater.updateStatistiskGrense(grense, (featureMetadata as StatistiskgrenseMetadata))
                Grensetype.Stemmekretsgrense -> metadataUpdater.updateKommunalKretsgrense(grense, (featureMetadata as KommunalKretsgrenseMetadata))
                else -> throw RuntimeException("Ikke støttet å oppdatere metadata for grensetype: " + grense.grensetype.name)
            }
        }
        return grense
    }

    fun endreGrensetypeForGrunnkretser(grunnkretser: List<Grunnkrets>, gyldigFra: LocalDate, endringstype: Endringstype) {
        val grenserSomSkalHaNyGrensetype: MutableMap<Grensetype, MutableSet<Grense>> = mutableMapOf()

        for (grunnkrets: Grunnkrets in grunnkretser) {
            for (grensekobling: GrenseKobling in grunnkrets.getGrenser(gyldigFra)) {
                val grunnkretserForGrense = grensekobling.grense.grunnkretser(gyldigFra)
                val kommunenumre = grunnkretserForGrense.map { it.grunnkretsnummer!!.take(4) }

                beregnNyGrensetype(kommunenumre)?.let { nyGrensetype ->
                    if (nyGrensetype != grensekobling.grense.grensetype) {
                        grenserSomSkalHaNyGrensetype.addToMap(nyGrensetype, grensekobling.grense)
                    }
                }
            }
        }

        lagNyVersjonAvGrenseOgKnyttTilKretser(grenserSomSkalHaNyGrensetype, gyldigFra, endringstype)
    }

    fun endreGrensetypeForStemmekretser(stemmekretser: List<Stemmekrets>, gyldigFra: LocalDate, endringstype: Endringstype) {
        val grenserSomSkalHaNyGrensetype: MutableMap<Grensetype, MutableSet<Grense>> = mutableMapOf()

        for (stemmekrets: Stemmekrets in stemmekretser) {
            for (grensekobling: GrenseKobling in stemmekrets.getGrenser(gyldigFra)) {
                val stemmekretserForGrense = grensekobling.grense.stemmekretser(gyldigFra)
                val kommunenumre = stemmekretserForGrense.mapNotNull { s ->
                    s.getKommune(gyldigFra)?.kommunenummer
                }

                beregnNyGrensetype(kommunenumre)?.let { nyGrensetype ->
                    if (nyGrensetype != grensekobling.grense.grensetype) {
                        grenserSomSkalHaNyGrensetype.addToMap(nyGrensetype, grensekobling.grense)
                    }
                }
            }
        }

        lagNyVersjonAvGrenseOgKnyttTilKretser(grenserSomSkalHaNyGrensetype, gyldigFra, endringstype)
    }

    private fun beregnNyGrensetype(kommunenumre: List<String>): Grensetype? {
        val erFylkesgrense = kommunenumre.distinctBy { it.take(2) }.size > 1
        val erKommuneGrense = kommunenumre.distinct().size > 1

        if (erFylkesgrense) {
            return Grensetype.Fylkesgrense
        } else if (erKommuneGrense) {
            return Grensetype.Kommunegrense
        }
        return null
    }

    fun <G, E> MutableMap<G, MutableSet<E>>.addToMap(key: G, value: E) {
        this.getOrPut(key) { mutableSetOf() }.add(value)
    }

    private fun lagNyVersjonAvGrenseOgKnyttTilKretser(
        grenserSomSkalHaNyVersjon: MutableMap<Grensetype, MutableSet<Grense>>,
        gyldigFra: LocalDate,
        endringstype: Endringstype
    ) {
        grenserSomSkalHaNyVersjon.entries.forEach { mutableEntry ->
            val nyGrensetype = mutableEntry.key
            mutableEntry.value.forEach { gammelGrense ->
                val nyGrense = createNyVersjonAvGrense(gammelGrense, gyldigFra, endringstype)
                    .apply { grensetype = nyGrensetype }

                val gamleGrunnkretsKoblinger = mutableSetOf<GrunnkretsGrenseKobling>()
                gamleGrunnkretsKoblinger.addAll(gammelGrense.gyldigeGrunnkretsGrenseKoblinger(gyldigFra))

                gamleGrunnkretsKoblinger.forEach {
                    //Den nye grunnkretsen har fått kobling til alle grensene som den gamle har
                    //Må bytte ut koblinga til den oppdaterte grensa
                    it.grunnkrets.replaceGrensekobling(gammelGrense, nyGrense, GrenseKoblingAttributter(
                        it.grenseKoblingAttributter.retningMedKlokken,
                        it.grenseKoblingAttributter.rekkefoelge,
                        it.grenseKoblingAttributter.flateIndeks,
                        it.grenseKoblingAttributter.hullIndeks
                    ))
                }

                val gamleStemmekretsKoblinger = mutableSetOf<StemmekretsGrenseKobling>()
                gamleStemmekretsKoblinger.addAll(gammelGrense.gyldigeStemmekretsGrenseKoblinger(gyldigFra))

                gamleStemmekretsKoblinger.forEach {
                    //Den nye stemmekretsen har fått kobling til alle grensene som den gamle har
                    //Må bytte ut koblinga til den oppdaterte grensa
                    it.stemmekrets.replaceGrensekobling(gammelGrense, nyGrense, GrenseKoblingAttributter(
                        it.grenseKoblingAttributter.retningMedKlokken,
                        it.grenseKoblingAttributter.rekkefoelge,
                        it.grenseKoblingAttributter.flateIndeks,
                        it.grenseKoblingAttributter.hullIndeks
                    ))
                }

            }
        }

        grenseRepository.flush()
    }

}
