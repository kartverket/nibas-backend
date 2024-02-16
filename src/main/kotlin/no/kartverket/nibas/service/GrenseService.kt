package no.kartverket.nibas.service

import no.kartverket.nibas.api.v1.common.Flatetype
import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.api.v1.common.KontekstEnhetType
import no.kartverket.nibas.api.v1.common.geojson.Feature
import no.kartverket.nibas.api.v1.common.geojson.FeatureCollection
import no.kartverket.nibas.api.v1.common.geojson.KontekstEgenskaper
import no.kartverket.nibas.api.v1.common.geojson.metadata.AdministrativGrenseMetadata
import no.kartverket.nibas.api.v1.common.geojson.metadata.AvtaltAvgrensningslinjeMetadata
import no.kartverket.nibas.api.v1.common.geojson.metadata.KommunalKretsgrenseMetadata
import no.kartverket.nibas.api.v1.common.geojson.metadata.MetadataUpdater
import no.kartverket.nibas.api.v1.common.geojson.metadata.RiksgrenseMetadata
import no.kartverket.nibas.api.v1.common.geojson.metadata.StatistiskgrenseMetadata
import no.kartverket.nibas.api.v1.common.geojson.metadata.TerritorialgrenseMetadata
import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.common.ResponseException.BadRequestException
import no.kartverket.nibas.common.ResponseException.NotFoundException
import no.kartverket.nibas.common.UgyldigTilstandException
import no.kartverket.nibas.common.createIdentifikator
import no.kartverket.nibas.domain.Endringstype
import no.kartverket.nibas.domain.Flate
import no.kartverket.nibas.domain.Fylke
import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.domain.GrenseKobling
import no.kartverket.nibas.domain.GrenseKoblingAttributter
import no.kartverket.nibas.domain.Grunnkrets
import no.kartverket.nibas.domain.GrunnkretsGrenseKobling
import no.kartverket.nibas.domain.KobletTilGrense
import no.kartverket.nibas.domain.Kommune
import no.kartverket.nibas.domain.Stemmekrets
import no.kartverket.nibas.domain.StemmekretsGrenseKobling
import no.kartverket.nibas.extensions.geometry.toLineString
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
    private val grenseKoblingService: GrenseKoblingService
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun hentGrense(identifikator: Identifikator): Grense? = grenseRepository.findGyldigObjektForIdentifikator(identifikator)

    fun hentAlleVersjonerMedGyldigFraEtterGittDato(lokalid: String, gyldigFra: LocalDate): List<Grense> =
        grenseRepository.findAllByLokalid(lokalid)
            .filter { it.gyldighet.gyldigFra.isAfter(gyldigFra) }


    @Transactional(readOnly = false)
    fun lagreGrenser(grenser: FeatureCollection, gyldighetsdato: LocalDate = LocalDate.now(), endringstype: Endringstype): Set<Flate> {

        val enheterSomSkalFlatedannes = doLagreGrenser(
            features = grenser.features,
            gyldighetsdato = gyldighetsdato,
            endringstype = endringstype
        )
        //Her er det bare å oppdatere, kretsene/adm.enhetene fikk ny versjon ved lagring av grenser og grensekoblinger
        grenseKoblingService.oppdaterGrenseKoblingAttributterForFlater(enheterSomSkalFlatedannes, gyldighetsdato)
        flateService.oppdaterFlaterFor(enheterSomSkalFlatedannes)

        return enheterSomSkalFlatedannes
    }

    // Feature.id vil være lokalid for grensa. Dersom denne ikke er satt skal det opprettes en ny grense
    private fun doLagreGrenser(features: List<Feature>, gyldighetsdato: LocalDate = LocalDate.now(), endringstype: Endringstype): Set<Flate> {
        // Henter eksisterende grenser på identifikator (kun allerede eksisterende grenser). Sorterer på lokalid for testens skyld.
        val featuresForEksisterendeGrenser = features.filter { it.id != null }
        val grenseIdentifikatorer = featuresForEksisterendeGrenser.map { featureIdToIdentifikator(it.id, gyldighetsdato) }.sortedBy { it.lokalid.value }
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

        sjekkForKonflikterForGrenser(features, eksisterendeGrenser, gyldighetsdato)
        sjekkKonflikterITilhorendeKretser(features, gyldighetsdato)

        // Ei endra grense kan ha flere knytninger til samme krets, dvs til ulike versjoner av samme krets
        // Må derfor ha oversikt over hvilke flater som har fått ny versjon
        val flaterSomHarFaattNyVersjon = mutableMapOf<Lokalid, Flate>()
        val grenserSomHarFaattNyVersjon = mutableMapOf<Lokalid, Grense>()

        // Håndtere nye grenser først
        val nyeGrenserMedSineFeatures = features.filter { it.id == null }.map { featureForNyGrense ->
            val nyopprettetGrense = createNyGrenseMedNyLokalid(featureForNyGrense, gyldighetsdato, endringstype)

            grenserSomHarFaattNyVersjon[Lokalid(nyopprettetGrense.lokalid)] = nyopprettetGrense

            //Oppretter nye versjoner for alle kretser som den nye grensa skal kobles til
            //Alt av grensekoblinger håndteres av tilhørighet ettersom vi ikke setter noen kretstilhørighet for de nye grensene.
            //Man vil se at den skal ha tilhørigheter fra kontekstegenskapene, men disse er ikke blitt satt og tilhørighetsjobben ordnder opp i dette
            featureForNyGrense.properties.kontekstEgenskaper.forEach {
                when (it.type) {
                    KontekstEnhetType.GRUNNKRETS -> {
                        val grunnkretsForNyGrense = grunnkretsService.hentGrunnkrets(createIdentifikator(it.id!!.lokalid.value, gyldighetsdato))
                            ?: throw BadRequestException("En av de innsendte featurene med endring er koblet til en grunnkrets som ikke finnes.")
                        val grunnkretsLokalid = Lokalid(grunnkretsForNyGrense.lokalid)
                        flaterSomHarFaattNyVersjon[grunnkretsLokalid] = hentEllerOpprettNyVersjonAvGrunnkrets(
                            flaterSomHarFaattNyVersjon,
                            Identifikator(grunnkretsLokalid, gyldighetsdato),
                            endringstype)
                    }

                    KontekstEnhetType.STEMMEKRETS -> {
                        val stemmekretsForNyGrense = stemmekretsService.hentStemmekrets(createIdentifikator(it.id!!.lokalid.value, gyldighetsdato))
                            ?: throw BadRequestException("En av de innsendte featurene med endring er koblet til en stemmekrets som ikke finnes.")
                        val stemmekretsLokalid = Lokalid(stemmekretsForNyGrense.lokalid)
                        flaterSomHarFaattNyVersjon[stemmekretsLokalid] = hentEllerOpprettNyVersjonAvStemmekrets(
                            flaterSomHarFaattNyVersjon,
                            Identifikator(stemmekretsLokalid, gyldighetsdato),
                            endringstype)
                    }
                }
            }

            GrenseMedFeature(nyopprettetGrense, featureForNyGrense)
        }

        //Nye grenser er allere håndtert, ser nå på resterende grenseendringer
        featuresForEksisterendeGrenser.forEach { feature ->

            val gammelGrense: Grense = findGrenseForIdentifikator(eksisterendeGrenser, featureIdToIdentifikator(feature.id, gyldighetsdato))

            // Uansett om gyldigFra er identisk med grensens gyldigFra eller fram i tid: Lage ny rad (ny uuid, samme lokalid).
            logger.info("Lagrer grense: [${gammelGrense.grensetype.name} - id ${feature.id}]")


            val nyVersjonAvGrense = if (feature.properties.shouldArchive) {
                null
            } else {
                createNyVersjonAvGrense(gammelGrense, gyldighetsdato, endringstype)
                    .applyChangesFromRequest(feature, gyldighetsdato, endringstype)
            }
            if (nyVersjonAvGrense != null) {
                grenserSomHarFaattNyVersjon[Lokalid(nyVersjonAvGrense.lokalid)] = nyVersjonAvGrense
            }
            //Det må lages en ny forekomst for grensa sine gamle tilknyttede kretser, de får samtidig oppdatert knytning til gjeldende grense.
            //De får oppdatert område etter at alle grensene er behandlet

            //Må jobbe på en kopi av lista, for ikke å få ConcurrentModificationException
            gammelGrense.gyldigeStemmekretsGrenseKoblinger(gyldighetsdato).forEach {
                val stemmekretsLokalid = Lokalid(it.stemmekrets.lokalid)
                val nyVersjonAvStemmekrets = hentEllerOpprettNyVersjonAvStemmekrets(flaterSomHarFaattNyVersjon, Identifikator(stemmekretsLokalid, gyldighetsdato), endringstype)
                val grenseKoblingAttributter = it.lagGrenseKoblingAttributter()
                nyVersjonAvStemmekrets.replaceGrensekobling(gammelGrense, nyVersjonAvGrense, grenseKoblingAttributter)
                flaterSomHarFaattNyVersjon[stemmekretsLokalid] = nyVersjonAvStemmekrets
            }

            gammelGrense.gyldigeGrunnkretsGrenseKoblinger(gyldighetsdato).forEach {
                val grunnkretsLokalid = Lokalid(it.grunnkrets.lokalid)
                val nyVersjonAvGrunnkrets = hentEllerOpprettNyVersjonAvGrunnkrets(flaterSomHarFaattNyVersjon, Identifikator(grunnkretsLokalid, gyldighetsdato), endringstype)
                val grenseKoblingAttributter = it.lagGrenseKoblingAttributter()
                nyVersjonAvGrunnkrets.replaceGrensekobling(gammelGrense, nyVersjonAvGrense, grenseKoblingAttributter)
                flaterSomHarFaattNyVersjon[grunnkretsLokalid] = nyVersjonAvGrunnkrets

                val nyeVersjonerAvFlaterHoyereIHierarkiet = lagNyeVersjonerAvFlaterHoyereIHierarkiet(
                    flaterSomHarFaattNyVersjon,
                    Identifikator(grunnkretsLokalid, gyldighetsdato),
                    endringstype,
                    gammelGrense,
                    nyVersjonAvGrense,
                    nyVersjonAvGrunnkrets
                )
                flaterSomHarFaattNyVersjon.putAll(nyeVersjonerAvFlaterHoyereIHierarkiet)
            }

            if (feature.properties.shouldArchive) {
                gammelGrense.setGyldigTil(gyldighetsdato)
            }
        }

        // Etter alle andre endringer sjekker vi om det er endringer i tilhørighet for noen grenser
        features.forEach { feature ->
            if (feature.id != null) {
                val grense = grenserSomHarFaattNyVersjon[featureIdToIdentifikator(feature.id, gyldighetsdato).lokalid]
                if (grense != null) {
                    flaterSomHarFaattNyVersjon.putAll(doEvtTilhorighetEndringer(feature, grense, gyldighetsdato, flaterSomHarFaattNyVersjon, endringstype))
                }
            } else {
                val nyOpprettetGrenseForFeature = nyeGrenserMedSineFeatures.first { it.feature == feature }.grense

                flaterSomHarFaattNyVersjon.putAll(doEvtTilhorighetEndringer(feature, nyOpprettetGrenseForFeature, gyldighetsdato, flaterSomHarFaattNyVersjon, endringstype))
            }
        }

        grenseRepository.flush()
        return flaterSomHarFaattNyVersjon.values.toSet()
    }

    private fun GrenseKobling.lagGrenseKoblingAttributter() = GrenseKoblingAttributter(
        retningMedKlokken = grenseKoblingAttributter.retningMedKlokken,
        rekkefoelge = grenseKoblingAttributter.rekkefoelge,
        flateIndeks = grenseKoblingAttributter.flateIndeks,
        hullIndeks = grenseKoblingAttributter.hullIndeks
    )

    // Hjelpeklasse for å bevare kobling mellom innsendt feature og dens (nyopprettede) grense
    data class GrenseMedFeature(
        val grense: Grense,
        val feature: Feature
    )

    private fun sjekkForKonflikterForGrenser(features: List<Feature>, eksisterendeGrenser: List<Grense>, gyldighetsdato: LocalDate) {
        val konflikterIGrenser = features.filter { it.id != null }.mapNotNull { feature ->
            val featureIdentifikator = featureIdToIdentifikator(feature.id, gyldighetsdato)
            verifiserOptimistiskLaas(feature.properties.version, findGrenseForIdentifikator(eksisterendeGrenser, featureIdentifikator))
        }
        if (konflikterIGrenser.isNotEmpty()) {
            throw OptimistiskLaasException(*konflikterIGrenser.toTypedArray())
        }
    }

    private fun sjekkKonflikterITilhorendeKretser(features: List<Feature>, gyldighetsdato: LocalDate) {
        val konflikterIKretserForGrenser = features.flatMap { feature ->
            feature.properties.kontekstEgenskaper
                .filter { kontekst -> !(kontekst.id == null && kontekst.kretsNummer != null) } // vi skal ikke trenge å sjekke versjon på nyopprettet krets
                .mapNotNull { kontekstEgenskaper ->
                    if (kontekstEgenskaper.id == null) {
                        throw BadRequestException("KontekstEgenskaper for feature med id: ${feature.id} er null")
                    }
                    val identifikator = Identifikator(kontekstEgenskaper.id.lokalid, gyldighetsdato)
                    verifiserOptimistiskLaas(
                        kontekstEgenskaper.version,
                        when (kontekstEgenskaper.type) {
                            KontekstEnhetType.GRUNNKRETS -> grunnkretsRepository.findGyldigObjektForIdentifikator(identifikator)
                                ?: throw NotFoundException("Fant ikke Grunnkrets med lokalid: ${identifikator.lokalid}")

                            KontekstEnhetType.STEMMEKRETS -> stemmekretsRepository.findGyldigObjektForIdentifikator(identifikator)
                                ?: throw NotFoundException("Fant ikke Stemmekrets med lokalid: ${identifikator.lokalid}")
                        }
                    )
                }
        }
        if (konflikterIKretserForGrenser.isNotEmpty()) {
            throw OptimistiskLaasException(*konflikterIKretserForGrenser.toTypedArray())
        }
    }

    // Hjelpeklasse for å holde oversikt over hvilke strategi en skal brukes for å hente frem krets i kontekstEgenskap

    //eksisterende og nyopprettet
    data class KretsReferanser(
        val eksisterende: List<KontekstEgenskaper>,
        val nyopprettet: List<KontekstEgenskaper>,
    )

    fun doEvtTilhorighetEndringer(
        innsendtFeature: Feature, grense: Grense, gyldighetsdato: LocalDate, flaterSomHarFaattNyVersjon: Map<Lokalid, Flate>, endringstype: Endringstype
    ): Map<Lokalid, Flate> {
        val kontekstTyperForFeature = innsendtFeature.properties.kontekstEgenskaper.distinctBy { it.type }.map { it.type }
        if (kontekstTyperForFeature.size > 1) {
            throw BadRequestException("En grense kan ikke være knyttet til forskjellige kretstyper")
        }

        val kontekstEnhetTypeForFeature = kontekstTyperForFeature[0]

        val innsendteTilhorigheter = innsendtFeature.properties.kontekstEgenskaper
        val eksisterendeGrensekoblinger = finnEksisterendeGrensekoblinger(kontekstEnhetTypeForFeature, grense, gyldighetsdato)
        val evtNyeKretser = mutableMapOf<Lokalid, Flate>()
        if (!innsendteTilhorigheter.map { it.id?.lokalid?.value }.equalsIgnoreOrder(eksisterendeGrensekoblinger.map { it.getLokalIdForKrets() })) {
            if (innsendteTilhorigheter.size != 2 || innsendteTilhorigheter[0].id?.lokalid == innsendteTilhorigheter[1].id?.lokalid) {
                throw BadRequestException("$innsendteTilhorigheter kan ikke settes som kretser for grense med id: ${grense.lokalid}")
            }
            val tilhorighetEndringer =
                grenseKoblingService.getTilhorighetEndringer(eksisterendeGrensekoblinger, innsendteTilhorigheter, gyldighetsdato)

            // Kun innsendte kontekster kan ha objektidentifikator lik null, som betyr at vi må finne krets via grunnkretsnummer i tilfellet der nyopprettede kretser blir laget i utkastet
            val kontekster = tilhorighetEndringer.nye.partition { kontekstEgenskaper -> kontekstEgenskaper.id != null }
            val nyeKontekster = KretsReferanser(eksisterende = kontekster.first, nyopprettet = kontekster.second)

            evtNyeKretser.putAll(leggTilGrenserForKretserViaKontekstEgenskaper(nyeKontekster, grense, gyldighetsdato, flaterSomHarFaattNyVersjon, endringstype, kontekstEnhetTypeForFeature))
            grenseKoblingService.fjernGrenseFraKretserViaGrenseKobling(grense, tilhorighetEndringer.frafallende, gyldighetsdato, flaterSomHarFaattNyVersjon, endringstype)
        }

        return evtNyeKretser
    }

    private fun finnEksisterendeGrensekoblinger(kontekstEnhetType: KontekstEnhetType, grense: Grense, gyldighetsdato: LocalDate): Set<GrenseKobling> {
        return when (kontekstEnhetType) {
            KontekstEnhetType.GRUNNKRETS -> grense.gyldigeGrunnkretsGrenseKoblinger(gyldighetsdato)
            KontekstEnhetType.STEMMEKRETS -> grense.gyldigeStemmekretsGrenseKoblinger(gyldighetsdato)
        }
    }

    fun leggTilGrenserForKretserViaKontekstEgenskaper(
        nyeKontekster: KretsReferanser,
        grense: Grense,
        gyldighetsDato: LocalDate,
        flaterSomHarFaattNyVersjon: Map<Lokalid, Flate>,
        endringstype: Endringstype,
        kretsType: KontekstEnhetType
    ): Map<Lokalid, Flate> {
        val nyeVersjonerAvFlater = mutableMapOf<Lokalid, Flate>()

        nyeKontekster.eksisterende.forEach { kontekstEgenskaper ->
            if (kontekstEgenskaper.id == null) { // må sjekkes da kotlin ikke klarer å se at identifikatorHelper har filtrerte kontkestet
                throw BadRequestException("KontekstEgenskap for grense med lokalid: ${grense.lokalid} er null")
            }
            // Henter nyeste versjon av grunnkrets i dette utkastet hvis det finnes, ellers henter vi nyeste fra db og lager ny versjon med den.
            val nyVersjonAvKrets = flaterSomHarFaattNyVersjon[kontekstEgenskaper.id.lokalid] ?: when (kretsType) {
                KontekstEnhetType.GRUNNKRETS -> grunnkretsService.createNyVersjonAvGrunnkrets(
                    grunnkretsRepository.findGyldigObjektForIdentifikator(Identifikator(kontekstEgenskaper.id.lokalid, gyldighetsDato))
                        ?: throw NotFoundException("Fant ikke grunnkrets med lokalid: ${kontekstEgenskaper.id.lokalid}"),
                    gyldighetsDato,
                    endringstype
                )

                KontekstEnhetType.STEMMEKRETS -> stemmekretsService.createNyVersjonAvStemmekrets(
                    stemmekretsRepository.findGyldigObjektForIdentifikator(Identifikator(kontekstEgenskaper.id.lokalid, gyldighetsDato))
                        ?: throw NotFoundException("Fant ikke stemmekrets med lokalid: ${kontekstEgenskaper.id.lokalid}"),
                    gyldighetsDato,
                    endringstype
                )
            }

            (nyVersjonAvKrets as KobletTilGrense).addGrensekobling(grense, GrenseKoblingAttributter())

            nyeVersjonerAvFlater[Lokalid(nyVersjonAvKrets.lokalid)] = nyVersjonAvKrets
        }
        nyeKontekster.nyopprettet.forEach { kontekstEgenskaper ->
            if (kontekstEgenskaper.kretsNummer == null) {
                throw BadRequestException("KontekstEgenskap for grense med lokalid: ${grense.lokalid} er null")
            }

            val nyesteVersjonAvNyopprettetKrets = flaterSomHarFaattNyVersjon.values.find {
                when (it.type) {
                    Flatetype.STEMMEKRETS -> (it as Stemmekrets).stemmekretsnummer == kontekstEgenskaper.kretsNummer && it.kommuneLokalid.lokalid == kontekstEgenskaper.kommuneId?.lokalid?.value
                    Flatetype.GRUNNKRETS -> (it as Grunnkrets).grunnkretsnummer == kontekstEgenskaper.kretsNummer
                    else -> false
                }
            } ?: when (kretsType) {
                KontekstEnhetType.GRUNNKRETS -> grunnkretsService.hentGrunnkretsForGrunnkretsNummer(kontekstEgenskaper.kretsNummer, gyldighetsDato)
                    ?: throw NotFoundException("Fant ikke den nyopprettede grunnkretsen med grunnkretsnummer: ${kontekstEgenskaper.kretsNummer}")

                KontekstEnhetType.STEMMEKRETS -> stemmekretsService.hentStemmekretsForStemmekretsNummerIKommune(kontekstEgenskaper.kommuneId
                    ?: throw BadRequestException("Identifikator for kommune manglet for å finne stemmekrets med stemmekretsNummer: ${kontekstEgenskaper.kretsNummer}"), kontekstEgenskaper.kretsNummer, gyldighetsDato)
                    ?: throw NotFoundException("Fant ikke den nyopprettede stemmekretsen med stemmekretsnummer: ${kontekstEgenskaper.kretsNummer} i kommune med identifikator: ${kontekstEgenskaper.kommuneId}")
            }

            (nyesteVersjonAvNyopprettetKrets as KobletTilGrense).addGrensekobling(grense, GrenseKoblingAttributter())

            nyeVersjonerAvFlater[Lokalid(nyesteVersjonAvNyopprettetKrets.lokalid)] = nyesteVersjonAvNyopprettetKrets
        }

        return nyeVersjonerAvFlater
    }

    fun createNyGrenseMedNyLokalid(feature: Feature, gyldigFra: LocalDate, endringstype: Endringstype): Grense {
        val grenseFraFeature = Grense(grensetype = Grensetype.fromValue(feature.properties.type))

        grenseFraFeature.apply {
            setGyldigFra(gyldigFra)
            version = 1
            auditInfo.typeEndring = endringstype
        }.applyChangesFromRequest(feature, gyldighetsdato = gyldigFra, endringstype)

        return grenseRepository.saveAndFlush(
            grenseFraFeature
        )

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
            }
    }

    fun Grense.applyChangesFromRequest(feature: Feature, gyldighetsdato: LocalDate, endringstype: Endringstype): Grense =
        this.apply {
            grense = feature.geometry.toLineString(feature.properties.srid)
            updateMetadataOnGrense(feature, this, gyldighetsdato, endringstype)
        }

    private fun hentEllerOpprettNyVersjonAvStemmekrets(
        flaterSomHarFaattNyVersjon: Map<Lokalid, Flate>,
        kontekstId: Identifikator,
        endringstype: Endringstype
    ): Stemmekrets {
        val stemmekrets = flaterSomHarFaattNyVersjon[kontekstId.lokalid]

        if (stemmekrets == null) {
            val gammelStemmekrets = stemmekretsRepository.findGyldigObjektForIdentifikator(kontekstId)
                ?: throw UgyldigTilstandException("Fant ikke gyldig stemmekrets for identifikator $kontekstId")
            return stemmekretsService.createNyVersjonAvStemmekrets(stemmekrets = gammelStemmekrets,
                gyldigFra = kontekstId.gyldighetsdato, endringstype = endringstype)
        } else if (stemmekrets !is Stemmekrets) {
            throw BadRequestException("Skal lage en ny versjon av stemmekrets for lokalid ${kontekstId.lokalid}, men denne tilhører en flate av typen ${stemmekrets.type}")
        }

        return stemmekrets
    }

    private fun hentEllerOpprettNyVersjonAvGrunnkrets(
        flaterSomHarFaattNyVersjon: Map<Lokalid, Flate>,
        kontekstId: Identifikator,
        endringstype: Endringstype
    ): Grunnkrets {
        val grunnkrets = flaterSomHarFaattNyVersjon[kontekstId.lokalid]

        if (grunnkrets == null) {
            val gammelGrunnkrets = grunnkretsRepository.findGyldigObjektForIdentifikator(kontekstId)
                ?: throw UgyldigTilstandException("Fant ikke gyldig grunnkrest for identifikator $kontekstId")
            return grunnkretsService.createNyVersjonAvGrunnkrets(grunnkrets = gammelGrunnkrets,
                gyldigFra = kontekstId.gyldighetsdato, endringstype = endringstype)
        } else if (grunnkrets !is Grunnkrets) {
            throw BadRequestException("Skal lage en ny versjon av grunnkrets for lokalid ${kontekstId.lokalid}, men denne tilhører en flate av typen ${grunnkrets.type}")
        }

        return grunnkrets
    }

    private fun lagNyeVersjonerAvFlaterHoyereIHierarkiet(
        flaterSomHarFaattNyVersjon: Map<Lokalid, Flate>,
        kontekstId: Identifikator,
        endringstype: Endringstype,
        gammelGrense: Grense,
        nyGrense: Grense?,
        nyVersjonAvGrunnkrets: Grunnkrets
    ): Map<Lokalid, Flate> {
        val nyeVersjonerAvFlater = mutableMapOf<Lokalid, Flate>()
        val gyldigFra = kontekstId.gyldighetsdato
        val grensetype = nyGrense?.grensetype ?: gammelGrense.grensetype

        //Hvis grensa er grense for kommune, fylke eller nasjon, skal det lages nye versjoner av dem
        if (Grensetype.erNasjonsgrense(grensetype) || Grensetype.Fylkesgrense == grensetype || Grensetype.Kommunegrense == grensetype) {

            //Skal lage ny versjon av kommune, hvis det ikke allerede er gjort
            val gammelKommune = nyVersjonAvGrunnkrets.getKommune(gyldighetsdato = gyldigFra)
                ?: throw UgyldigTilstandException("Fant ikke gyldig kommune for $nyVersjonAvGrunnkrets")
            val kommuneLokalid = Lokalid(gammelKommune.lokalid)
            var kommune = flaterSomHarFaattNyVersjon[kommuneLokalid]
            if (kommune == null) {
                kommune = kommuneService.createNyVersjonAvKommune(kommune = gammelKommune, gyldigFra = gyldigFra, endringstype = endringstype)
                nyeVersjonerAvFlater[kommuneLokalid] = kommune
            }

            if (Grensetype.erNasjonsgrense(grensetype) || Grensetype.Fylkesgrense == grensetype) {
                //Skal lage ny versjon av fylke, hvis det ikke allerede er gjort
                val gammeltFylke = (kommune as Kommune).getFylke(gyldigFra)
                    ?: throw UgyldigTilstandException("Fant ikke gyldig fylke for $kommune")
                val fylkeLokalid = Lokalid(gammeltFylke.lokalid)
                var fylke = flaterSomHarFaattNyVersjon[fylkeLokalid]
                if (fylke == null) {
                    fylke = fylkeService.createNyVersjonAvFylke(fylke = gammeltFylke, gyldigFra = gyldigFra, endringstype = endringstype)
                    nyeVersjonerAvFlater[fylkeLokalid] = fylke
                }

                if (Grensetype.erNasjonsgrense(grensetype)) {
                    //Skal lage ny versjon av nasjon, hvis det ikke allerede er gjort
                    val gammelNasjon = (fylke as Fylke).getNasjon(gyldigFra)
                        ?: throw UgyldigTilstandException("Fant ikke gyldig nasjon for $fylke")
                    val nasjonLokalid = Lokalid(gammelNasjon.lokalid)
                    var nasjon = flaterSomHarFaattNyVersjon[nasjonLokalid]
                    if (nasjon == null) {
                        nasjon = nasjonService.createNyVersjonAvNasjon(nasjon = gammelNasjon, gyldigFra = gyldigFra, endringstype = endringstype)
                        nyeVersjonerAvFlater[nasjonLokalid] = nasjon
                    }
                }
            }
        }
        return nyeVersjonerAvFlater
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
        grense: Grense,
        gyldighetsdato: LocalDate,
        endringstype: Endringstype
    ): Grense {
        val featureMetadata = feature.properties.metadata
        if (featureMetadata != null) {
            val metadataUpdater = MetadataUpdater()
            when (grense.grensetype) {
                Grensetype.Kommunegrense -> metadataUpdater.updateAdministrativGrense(grense, (featureMetadata as AdministrativGrenseMetadata), gyldighetsdato, endringstype)
                Grensetype.Fylkesgrense -> metadataUpdater.updateAdministrativGrense(grense, (featureMetadata as AdministrativGrenseMetadata), gyldighetsdato, endringstype)
                Grensetype.Riksgrense -> metadataUpdater.updateRiksgrense(grense, (featureMetadata as RiksgrenseMetadata), gyldighetsdato, endringstype)
                Grensetype.AvtaltAvgrensningslinje -> metadataUpdater.updateAvtaltavgrensningslinje(
                    grense = grense,
                    metadata = (featureMetadata as AvtaltAvgrensningslinjeMetadata),
                    gyldighetsdato = gyldighetsdato,
                    endringstype = endringstype
                )

                Grensetype.Territorialgrense -> metadataUpdater.updateTerritoritalgrense(grense, (featureMetadata as TerritorialgrenseMetadata), gyldighetsdato, endringstype)
                Grensetype.Delomraadegrense -> metadataUpdater.updateStatistiskGrense(grense, (featureMetadata as StatistiskgrenseMetadata))
                Grensetype.Grunnkretsgrense -> metadataUpdater.updateStatistiskGrense(grense, (featureMetadata as StatistiskgrenseMetadata))
                Grensetype.Stemmekretsgrense -> metadataUpdater.updateKommunalKretsgrense(grense, (featureMetadata as KommunalKretsgrenseMetadata))
                else -> throw RuntimeException("Ikke støttet å oppdatere metadata for grensetype: " + grense.grensetype.name)
            }
        }
        return grense
    }

    fun endreGrensetypeForGrunnkretser(grunnkretser: List<Grunnkrets>, gyldigFra: LocalDate, endringstype: Endringstype) {
        val grenserSomSkalHaNyGrensetype = mutableMapOf<Grensetype, MutableSet<Grense>>()

        for (grunnkrets: Grunnkrets in grunnkretser) {
            for (grensekobling: GrenseKobling in grunnkrets.getGrensekoblinger(gyldigFra)) {
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
        val grenserSomSkalHaNyGrensetype = mutableMapOf<Grensetype, MutableSet<Grense>>()

        for (stemmekrets: Stemmekrets in stemmekretser) {
            for (grensekobling: GrenseKobling in stemmekrets.getGrensekoblinger(gyldigFra)) {
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

    fun <T> List<T>.equalsIgnoreOrder(other: List<T>) = this.size == other.size && this.toSet() == other.toSet()

}
