package no.kartverket.nibas.service

import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.api.v1.common.KontekstEnhetType
import no.kartverket.nibas.api.v1.common.geojson.Feature
import no.kartverket.nibas.api.v1.common.geojson.FeatureCollection
import no.kartverket.nibas.api.v1.common.geojson.metadata.CommonGrenseMetadata
import no.kartverket.nibas.api.v1.common.geojson.metadata.CommonMetadata
import no.kartverket.nibas.api.v1.common.geojson.metadata.HarCommonGrense
import no.kartverket.nibas.api.v1.common.geojson.metadata.HarCommonMetadata
import no.kartverket.nibas.api.v1.common.geojson.metadata.HarDokumentasjonsreferanse
import no.kartverket.nibas.api.v1.common.geojson.metadata.toEmbeddable
import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.common.ResponseException.BadRequestException
import no.kartverket.nibas.common.ResponseException.NotFoundException
import no.kartverket.nibas.common.toNorskFormat
import no.kartverket.nibas.domain.Bopliktomraade
import no.kartverket.nibas.domain.CommonMetadataProperties
import no.kartverket.nibas.domain.Endringstype
import no.kartverket.nibas.domain.Flate
import no.kartverket.nibas.domain.Fylke
import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.domain.GrenseKobling
import no.kartverket.nibas.domain.Grunnkrets
import no.kartverket.nibas.domain.GrunnkretsGrenseKobling
import no.kartverket.nibas.domain.KobletTilGrense
import no.kartverket.nibas.domain.Kommune
import no.kartverket.nibas.domain.Nasjon
import no.kartverket.nibas.domain.Stemmekrets
import no.kartverket.nibas.domain.StemmekretsGrenseKobling
import no.kartverket.nibas.extensions.geometry.toLineString
import no.kartverket.nibas.repository.BopliktomraadeRepository
import no.kartverket.nibas.repository.GrenseRepository
import no.kartverket.nibas.repository.GrunnkretsRepository
import no.kartverket.nibas.repository.StemmekretsRepository
import no.kartverket.nibas.repository.findAllGyldigeObjekterForIdentifikatorer
import no.kartverket.nibas.repository.findGyldigObjektForIdentifikator
import no.kartverket.nibas.sporing.Laasbar
import org.locationtech.jts.geom.LineString
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
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
    private val dokumentasjonsreferanseService: DokumentasjonsreferanseService,
    private val bopliktomraadeRepository: BopliktomraadeRepository,
    private val bopliktomraadeService: BopliktomraadeService,
) {
    fun hentGrense(identifikator: Identifikator): Grense? = grenseRepository.findGyldigObjektForIdentifikator(identifikator)

    fun hentAlleVersjonerMedGyldigFraEtterGittDato(lokalid: String, gyldigFra: LocalDate): List<Grense> =
        grenseRepository.findAllByLokalid(lokalid)
            .filter { it.gyldighet.gyldigFra.isAfter(gyldigFra) }

    /**
     * Henter alle gyldige grenser på en gitt dato med paginering.
     * Returnerer kun Kommunegrense, Fylkesgrense, Riksgrense og Territorialgrense.
     *
     * @param gyldighetsdato Dato for gyldighet
     * @param pageable Paginering
     * @return Side med Kommunegrense, Fylkesgrense, Riksgrense og Territorialgrense som er gyldige på gitt dato
     */
    fun hentAlleGrenser(gyldighetsdato: LocalDate, pageable: Pageable): Page<Grense> =
        grenseRepository.findAllGyldigeGrenser(gyldighetsdato, pageable)


    @Transactional(readOnly = false)
    fun lagreGrenser(grenser: FeatureCollection, gyldighetsdato: LocalDate = LocalDate.now(), endringstype: Endringstype): Set<Flate> {
        val (featuresMedArkivering, featuresMedEndringer) = grenser.features.partition { it.properties.shouldArchive }
        val stemmekretserForTilhorigheter = createStemmekretserVersjonerForTilhorigheterIFeatures(grenser.features, gyldighetsdato, endringstype)
        val grunnkretserForTilhorigheter = createGrunnkretsVersjonerForTilhorigheterIFeatures(grenser.features, gyldighetsdato, endringstype)
        val bopliktomraaderForTilhorigheter = createBopliktomraadeVersjonerForTilhorigheterIFeatures(grenser.features, gyldighetsdato, endringstype)

        featuresMedArkivering.map { arkiverGrense(it, gyldighetsdato) }

        val endredeGrenserMedTilhorigheter = featuresMedEndringer.map { feature ->
            val grenseUtenTilhorighet = createGrenseForFeatureUtenTilhorigheter(feature, gyldighetsdato, endringstype)
            addTilhorigheterTilGrense(grenseUtenTilhorighet, feature, stemmekretserForTilhorigheter, grunnkretserForTilhorigheter,
                bopliktomraaderForTilhorigheter, gyldighetsdato)
        }

        val flaterSomSkalOppdatereHoyereIHierakiet = createNyeVersjonerAvFlaterHoyereIHierakietForGrenser(endredeGrenserMedTilhorigheter, gyldighetsdato, endringstype)
        val flaterSomSkalFlatedannes = (stemmekretserForTilhorigheter + grunnkretserForTilhorigheter + bopliktomraaderForTilhorigheter +
            flaterSomSkalOppdatereHoyereIHierakiet)
            .distinctBy { it.id }
            .toSet()

        flateService.oppdaterFlaterFor(flaterSomSkalFlatedannes)
        return flaterSomSkalFlatedannes
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

    fun createNyGrenseKopiertFraAnnenGrenseMedNyLokalid(kopierFraGrense: Grense, nyLineString: LineString, gyldigFra: LocalDate, endringstype: Endringstype): Grense {
        val nyGrense = grenseRepository.saveAndFlush(
            Grense(grensetype = kopierFraGrense.grensetype)
                .apply {
                    applyMappingFromExistingGrense(kopierFraGrense)
                    setGyldigFra(gyldigFra)
                    grense = nyLineString
                    version = 1
                    auditInfo.typeEndring = endringstype
                }
        )

        return nyGrense
            .applyCascadingOperationsBasedOnExistingGrense(kopierFraGrense)
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

    fun validerGrenserFinnesOgHentGrenser(features: Collection<Feature>, gyldighetsdato: LocalDate): List<Grense> {
        // Henter eksisterende grenser på identifikator (kun allerede eksisterende grenser). Sorterer på lokalid for testens skyld.
        val featuresForEksisterendeGrenser = features.filter { it.id != null }
        val grenseIdentifikatorer = featuresForEksisterendeGrenser.map { featureIdToIdentifikator(it.id, gyldighetsdato) }.sortedBy { it.lokalid.value }
        val identifikatorToGrenseMap = grenseRepository.findAllGyldigeObjekterForIdentifikatorer(grenseIdentifikatorer)

        // Hvis det er innkommende features med id som ikke ligger i databasen kastes HTTP 400
        val identifikatorDiffs: List<Identifikator> = grenseIdentifikatorer - identifikatorToGrenseMap.keys
        if (identifikatorDiffs.isNotEmpty()) {
            throw BadRequestException(
                title = "Klarte ikke finne grenser",
                msg = "En eller flere grenser du har endret på eksisterer ikke lengre. For å sørge for at du endrer på en gjeldende grense må du dessverre opprette et nytt utkast og gjennomføre endringene dine på nytt.",
                additionalInfo = "Feilen gjelder følgende grenser: ${identifikatorDiffs.joinToString(",") { "(lokalid: ${it.lokalid.value}), gyldig fra: ${it.gyldighetsdato})" }}")
        }

        return identifikatorToGrenseMap.values.toList()
    }

    fun validerLaasbareForGrenseendringerFinnesOgLagListeMedLaasbarOgOensketVersjon(features: Collection<Feature>, gyldighetsdato: LocalDate)
        : List<Pair<Laasbar, Int>> {
        val eksisterendeGrenser: List<Grense> = validerGrenserFinnesOgHentGrenser(features, gyldighetsdato)

        val laasbarMedOensketVersjonListe = lagListeMedLaasbareOgOensketVersjonForGrenser(features, eksisterendeGrenser, gyldighetsdato)
            .plus(validerTilhorendeKretserFinnesOgLagListeMedLaasbarOgOensketVersjon(features, gyldighetsdato))
        return laasbarMedOensketVersjonListe
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

    private fun <G, E> MutableMap<G, MutableSet<E>>.addToMap(key: G, value: E) {
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
                    it.grunnkrets.replaceGrensekobling(gammelGrense, nyGrense)
                }

                val gamleStemmekretsKoblinger = mutableSetOf<StemmekretsGrenseKobling>()
                gamleStemmekretsKoblinger.addAll(gammelGrense.gyldigeStemmekretsGrenseKoblinger(gyldigFra))

                gamleStemmekretsKoblinger.forEach {
                    //Den nye stemmekretsen har fått kobling til alle grensene som den gamle har
                    //Må bytte ut koblinga til den oppdaterte grensa
                    it.stemmekrets.replaceGrensekobling(gammelGrense, nyGrense)
                }

            }
        }

        grenseRepository.flush()
    }

    private fun createGrenseForFeatureUtenTilhorigheter(feature: Feature, gyldighetsdato: LocalDate, endringstype: Endringstype): Grense {
        val featureId = feature.id

        if (featureId != null) {
            val gammelGrense = grenseRepository.findGyldigObjektForIdentifikator(Identifikator(Lokalid(featureId), gyldighetsdato))
                ?: throw BadRequestException("Kan ikke endre på grense med lokalid $featureId. Fant ingen gyldige grenser med denne IDen på gyldighetsdato $gyldighetsdato.")
            return createNyVersjonAvGrense(gammelGrense, gyldighetsdato, endringstype)
                .applyChangesFromRequest(feature, gyldighetsdato, endringstype)
        }

        return createNyGrenseMedNyLokalid(feature, gyldighetsdato, endringstype)
    }

    private fun addTilhorigheterTilGrense(
        grense: Grense,
        feature: Feature,
        stemmekretser: Collection<Stemmekrets>,
        grunnkretser: Collection<Grunnkrets>,
        bopliktomraader: Collection<Bopliktomraade>,
        gyldighetsdato: LocalDate
    ): Grense {
        hentStemmekretserForTilhorigheterIFeature(feature, stemmekretser, gyldighetsdato)
            .forEach { it.addGrensekobling(grense) }
        hentGrunnkretserForTilhorigheterIFeature(feature, grunnkretser)
            .forEach { it.addGrensekobling(grense) }
        hentBopliktomraaderForTilhorigheterIFeature(feature, bopliktomraader)
            .forEach { it.addGrensekobling(grense) }

        return grense
    }

    private fun hentStemmekretserForTilhorigheterIFeature(feature: Feature, stemmekretser: Collection<Stemmekrets>, gyldighetsdato: LocalDate)
        : List<Stemmekrets> {
        val tilhorigheterTilNyeKretser = feature.properties.kontekstEgenskaper
            .filter { it.type == KontekstEnhetType.STEMMEKRETS && it.id == null }
            .map { kontekstEgenskap ->
                stemmekretser.firstOrNull { it.stemmekretsnummer == kontekstEgenskap.kretsNummer && kontekstEgenskap.kommuneId?.lokalid?.value == it.getKommune(gyldighetsdato)?.lokalid }
                    ?: throw RuntimeException("Kunne ikke sette tilhørighet på grensen. Fant ikke stemmekrets med stemmekretsnummer: ${kontekstEgenskap.kretsNummer}")
            }

        val tilhorigheterTilEksisterendeKretser = feature.properties.kontekstEgenskaper
            .filter { it.type == KontekstEnhetType.STEMMEKRETS && it.id != null }
            .map { kontekstEgenskap ->
                stemmekretser.firstOrNull { it.lokalid == kontekstEgenskap.id?.lokalid?.value }
                    ?: throw BadRequestException("Kunne ikke sette tilhørighet på grensen. Fant ikke stemmekrets med lokalid: ${kontekstEgenskap.id?.lokalid?.value}")
            }

        return tilhorigheterTilNyeKretser + tilhorigheterTilEksisterendeKretser
    }

    private fun hentGrunnkretserForTilhorigheterIFeature(feature: Feature, grunnkretser: Collection<Grunnkrets>): List<Grunnkrets> {
        val tilhorigheterTilNyeKretser = feature.properties.kontekstEgenskaper
            .filter { it.type == KontekstEnhetType.GRUNNKRETS && it.id == null }
            .map { kontekstEgenskap ->
                grunnkretser.firstOrNull { it.grunnkretsnummer == kontekstEgenskap.kretsNummer }
                    ?: throw RuntimeException("Kunne ikke sette tilhørighet på grensen. Fant ikke grunnkrets med grunnkretsnummer: ${kontekstEgenskap.kretsNummer}")
            }

        val tilhorigheterTilEksisterendeKretser = feature.properties.kontekstEgenskaper
            .filter { it.type == KontekstEnhetType.GRUNNKRETS && it.id != null }
            .map { kontekstEgenskap ->
                grunnkretser.firstOrNull { it.lokalid == kontekstEgenskap.id?.lokalid?.value }
                    ?: throw BadRequestException("Kunne ikke sette tilhørighet på grensen. Fant ikke grunnkrets med lokalid: ${kontekstEgenskap.id?.lokalid?.value}")
            }

        return tilhorigheterTilNyeKretser + tilhorigheterTilEksisterendeKretser
    }

    private fun hentBopliktomraaderForTilhorigheterIFeature(feature: Feature, bopliktomraader: Collection<Bopliktomraade>): List<Bopliktomraade> {
        //Har ikke med tilhørighet for deling
        val tilhorigheterTilEksisterendeKretser = feature.properties.kontekstEgenskaper
            .filter { it.type == KontekstEnhetType.BOPLIKTOMRAADE && it.id != null }
            .map { kontekstEgenskap ->
                bopliktomraader.firstOrNull { it.lokalid == kontekstEgenskap.id?.lokalid?.value }
                    ?: throw BadRequestException("Kunne ikke sette tilhørighet på grensen. Fant ikke bopliktområde med lokalid: ${kontekstEgenskap.id?.lokalid?.value}")
            }

        return tilhorigheterTilEksisterendeKretser
    }

    fun createNyeVersjonerAvFlaterHoyereIHierakietForGrenser(
        grenser: Collection<Grense>,
        gyldighetsdato: LocalDate,
        endringstype: Endringstype,
        flaterSomHarFaattNyVersjon: Map<Lokalid, Flate> = emptyMap()
    ): Set<Flate> {
        val nyeVersjonAvNasjoner = grenser
            .filter { Nasjon.tillatteGrensetyper().contains(it.grensetype) }
            .flatMap { it.nasjoner(gyldighetsdato) }
            .distinctBy { it.lokalid }
            .filterNot { flaterSomHarFaattNyVersjon.containsKey(Lokalid(it.lokalid)) }
            .map { nasjonService.createNyVersjonAvNasjon(it, gyldighetsdato, endringstype) }
            .toSet()

        val nyeVersjonerAvFylker = grenser
            .filter { Fylke.tillatteGrensetyper().contains(it.grensetype) }
            .flatMap { it.fylker(gyldighetsdato) }
            .distinctBy { it.lokalid }
            .filterNot { flaterSomHarFaattNyVersjon.containsKey(Lokalid(it.lokalid)) }
            .map { fylkeService.createNyVersjonAvFylke(it, gyldighetsdato, endringstype) }
            .toSet()

        val nyeVersjonerAvKomuner = grenser
            .filter { Kommune.tillatteGrensetyper().contains(it.grensetype) }
            .flatMap { it.kommuner(gyldighetsdato) }
            .distinctBy { it.lokalid }
            .filterNot { flaterSomHarFaattNyVersjon.containsKey(Lokalid(it.lokalid)) }
            .map { kommuneService.createNyVersjonAvKommune(it, gyldighetsdato, endringstype) }
            .toSet()

        return nyeVersjonAvNasjoner + nyeVersjonerAvFylker + nyeVersjonerAvKomuner
    }

    private fun createStemmekretserVersjonerForTilhorigheterIFeatures(features: List<Feature>, gyldighetsdato: LocalDate, endringstype: Endringstype): Set<Stemmekrets> {
        return getNyopprettedeStemmekretserForFeatures(features, gyldighetsdato) + getNyeVersjonerAvEksisterendeStemmekretserForFeatures(features, gyldighetsdato, endringstype)
    }

    private fun createGrunnkretsVersjonerForTilhorigheterIFeatures(features: List<Feature>, gyldighetsdato: LocalDate, endringstype: Endringstype): Set<Grunnkrets> {
        return getNyopprettedeGrunnkretserForFeatures(features, gyldighetsdato) + getNyeVersjonerAvEksisterendeGrunnkretserForFeatures(features, gyldighetsdato, endringstype)
    }

    private fun createBopliktomraadeVersjonerForTilhorigheterIFeatures(features: List<Feature>, gyldighetsdato: LocalDate, endringstype: Endringstype): Set<Bopliktomraade> {
        //Har ikke med nyopprettede bopliktmråder oppstått ved deling
        return getNyeVersjonerAvEksisterendeBopliktomraaderForFeatures(features, gyldighetsdato, endringstype)
    }

    private fun getNyopprettedeStemmekretserForFeatures(features: List<Feature>, gyldighetsdato: LocalDate): Set<Stemmekrets> {
        val kontekstEgenskaper = features
            .flatMap { it.properties.kontekstEgenskaper }
            .filter { it.type == KontekstEnhetType.STEMMEKRETS }

        return kontekstEgenskaper
            .filter { it.id == null }
            .distinctBy { it.kretsNummer + it.kommuneId }
            .map {
                if (it.kretsNummer == null || it.kommuneId == null) {
                    throw BadRequestException("Kretsnummer og kommune-id må være satt når man setter tilhørighet til en ny stemmekrets.")
                }
                stemmekretsService.hentStemmekretsForStemmekretsNummerIKommune(it.kommuneId, it.kretsNummer, gyldighetsdato)
                    ?: throw RuntimeException("Feil ved tilhørighet for grense. Fant ikke den nye stemmekretsen med kretsnummer ${it.kretsNummer} i kommune med id ${it.kommuneId} på gyldighetsdato ${gyldighetsdato.toNorskFormat()}.")
            }.toSet()
    }

    private fun getNyeVersjonerAvEksisterendeStemmekretserForFeatures(features: Collection<Feature>, gyldighetsdato: LocalDate, endringstype: Endringstype): Set<Stemmekrets> {
        val kontekstEgenskaper = features
            .flatMap { it.properties.kontekstEgenskaper }
            .filter { it.type == KontekstEnhetType.STEMMEKRETS }

        return kontekstEgenskaper
            .mapNotNull { it.id }
            .distinctBy { it.lokalid.value }
            .map {
                val gammelVersjon = stemmekretsService.hentStemmekrets(Identifikator(it.lokalid, gyldighetsdato))
                    ?: throw RuntimeException("Fant ikke stemmekrets med id ${it.lokalid} på gyldighetsdato ${gyldighetsdato.toNorskFormat()}")
                stemmekretsService.createNyVersjonAvStemmekretsUtenGrenser(gammelVersjon, gyldighetsdato, endringstype)
                    .kopierTilhorigheterSomIkkeErMedIUtkastet(gammelVersjon, features, gyldighetsdato) as Stemmekrets
            }.toSet()
    }

    private fun getNyopprettedeGrunnkretserForFeatures(features: List<Feature>, gyldighetsdato: LocalDate): Set<Grunnkrets> {
        val kontekstEgenskaper = features
            .flatMap { it.properties.kontekstEgenskaper }
            .filter { it.type == KontekstEnhetType.GRUNNKRETS }

        return kontekstEgenskaper
            .filter { it.id == null }
            .distinctBy { it.kretsNummer + it.kommuneId }
            .map {
                if (it.kretsNummer == null) {
                    throw BadRequestException("Kretsnummer må være satt når man setter tilhørighet til en ny grunnkrets.")
                }
                grunnkretsService.hentGrunnkretsForGrunnkretsNummer(it.kretsNummer, gyldighetsdato)
                    ?: throw RuntimeException("Feil ved tilhørighet for grense. Fant ikke den nye grunnkretsen med kretsnummer ${it.kretsNummer} på gyldighetsdato ${gyldighetsdato.toNorskFormat()}.")
            }.toSet()
    }

    private fun getNyeVersjonerAvEksisterendeGrunnkretserForFeatures(features: Collection<Feature>, gyldighetsdato: LocalDate, endringstype: Endringstype)
        : Set<Grunnkrets> {
        val unikeGrunnkretsLokalIder = features
            .flatMap { it.properties.kontekstEgenskaper }
            .filter { it.type == KontekstEnhetType.GRUNNKRETS }
            .mapNotNull { it.id }
            .distinctBy { it.lokalid.value }

        return unikeGrunnkretsLokalIder
            .map {
                val gammelVersjon = grunnkretsService.hentGrunnkrets(Identifikator(it.lokalid, gyldighetsdato))
                    ?: throw RuntimeException("Fant ikke grunnkrets med id ${it.lokalid.value} på gyldighetsdato ${gyldighetsdato.toNorskFormat()}")
                grunnkretsService.createNyVersjonAvGrunnkretsUtenGrenser(gammelVersjon, gyldighetsdato, endringstype)
                    .kopierTilhorigheterSomIkkeErMedIUtkastet(gammelVersjon, features, gyldighetsdato) as Grunnkrets
            }.toSet()
    }

    private fun getNyeVersjonerAvEksisterendeBopliktomraaderForFeatures(features: Collection<Feature>, gyldighetsdato: LocalDate, endringstype: Endringstype)
        : Set<Bopliktomraade> {
        val unikeBopliktomraadeLokalIder = features
            .flatMap { it.properties.kontekstEgenskaper }
            .filter { it.type == KontekstEnhetType.BOPLIKTOMRAADE }
            .mapNotNull { it.id }
            .distinctBy { it.lokalid.value }

        return unikeBopliktomraadeLokalIder
            .map {
                val gammelVersjon = bopliktomraadeService.hentBopliktomraade(Identifikator(it.lokalid, gyldighetsdato))
                    ?: throw RuntimeException("Fant ikke bopliktomraåde med id ${it.lokalid.value} på gyldighetsdato ${gyldighetsdato.toNorskFormat()}")
                bopliktomraadeService.createNyVersjonAvBopliktomraadeUtenGrenser(gammelVersjon, gyldighetsdato, endringstype)
                    .kopierTilhorigheterSomIkkeErMedIUtkastet(gammelVersjon, features, gyldighetsdato) as Bopliktomraade
            }.toSet()
    }

    private fun arkiverGrense(feature: Feature, gyldighetsdato: LocalDate): Grense {
        val grense = feature.id?.let {
            grenseRepository.findGyldigObjektForIdentifikator(Identifikator(Lokalid(it), gyldighetsdato))
        }
            ?: throw BadRequestException("Kan ikke arkivere grense med id ${feature.id}. Ingen grense funnet med denne IDen på gyldighetsdato ${gyldighetsdato.toNorskFormat()}.")
        grense.setGyldigTil(gyldighetsdato)
        return grense
    }

    private fun lagListeMedLaasbareOgOensketVersjonForGrenser(features: Collection<Feature>, eksisterendeGrenser: List<Grense>, gyldighetsdato: LocalDate)
        : List<Pair<Laasbar, Int>> {
        return features.filter { it.id != null }.map { feature ->
            val featureIdentifikator = featureIdToIdentifikator(feature.id, gyldighetsdato)
            Pair(findGrenseForIdentifikator(eksisterendeGrenser, featureIdentifikator), feature.properties.version)
        }
    }

    private fun validerTilhorendeKretserFinnesOgLagListeMedLaasbarOgOensketVersjon(features: Collection<Feature>, gyldighetsdato: LocalDate)
        : List<Pair<Laasbar, Int>> {
        val laasbarMedOensketVersjonListe = features.flatMap { feature ->
            feature.properties.kontekstEgenskaper
                .filter { kontekst -> !(kontekst.id == null && kontekst.kretsNummer != null) } // vi skal ikke trenge å sjekke versjon på nyopprettet krets
                .map { kontekstEgenskaper ->
                    if (kontekstEgenskaper.id == null) {
                        throw BadRequestException("KontekstEgenskaper for feature med id: ${feature.id} er null")
                    }
                    val identifikator = Identifikator(kontekstEgenskaper.id.lokalid, gyldighetsdato)
                    val laasbar: Laasbar = when (kontekstEgenskaper.type) {
                        KontekstEnhetType.GRUNNKRETS -> grunnkretsRepository.findGyldigObjektForIdentifikator(identifikator)
                            ?: throw NotFoundException("Fant ikke Grunnkrets med lokalid: ${identifikator.lokalid}")

                        KontekstEnhetType.STEMMEKRETS -> stemmekretsRepository.findGyldigObjektForIdentifikator(identifikator)
                            ?: throw NotFoundException("Fant ikke Stemmekrets med lokalid: ${identifikator.lokalid}")

                        KontekstEnhetType.BOPLIKTOMRAADE -> bopliktomraadeRepository.findGyldigObjektForIdentifikator(identifikator)
                            ?: throw NotFoundException("Fant ikke Bopliktområde med lokalid: ${identifikator.lokalid}")
                    }
                    Pair(laasbar, kontekstEgenskaper.version)
                }
        }
        return laasbarMedOensketVersjonListe
    }


    private fun Grense.applyChangesFromRequest(feature: Feature, gyldighetsdato: LocalDate, endringstype: Endringstype): Grense =
        this.apply {
            grense = feature.geometry.toLineString(feature.properties.srid)
            grensetype = Grensetype.fromValue(feature.properties.type)
            updateMetadataOnGrense(feature, this, gyldighetsdato, endringstype)
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

    private fun KobletTilGrense.kopierTilhorigheterSomIkkeErMedIUtkastet(gammelFlate: Flate, features: Collection<Feature>, gyldighetsdato: LocalDate): KobletTilGrense {
        gammelFlate.getGrensekoblinger(gyldighetsdato)
            .map { it.grense }
            .filter { features.none { feature -> feature.id == it.lokalid } }
            .forEach { this.addGrensekobling(it) }
        return this
    }

    private fun updateMetadataOnGrense(
        feature: Feature,
        grense: Grense,
        gyldighetsdato: LocalDate,
        endringstype: Endringstype
    ): Grense {
        val featureMetadata = feature.properties.metadata

        if (featureMetadata is HarCommonMetadata) {
            grense.updateWithCommonMetadata(featureMetadata.common)
        }
        if (featureMetadata is HarCommonGrense) {
            updateWithGrenseMetadata(grense, featureMetadata.commonGrense)
        }
        if (featureMetadata is HarDokumentasjonsreferanse) {
            grenseRepository.saveAndFlush(grense)
            dokumentasjonsreferanseService.updateDokumentasjonsreferanser(
                dokumentasjonsreferanseDTOer = featureMetadata.dokumentasjonsreferanser,
                endringstype = endringstype,
                gyldighetsdato = gyldighetsdato,
                grense = grense
            )
        }

        return grense
    }

    private fun CommonMetadataProperties.updateWithCommonMetadata(common: CommonMetadata) {
        this.datafangstdato = common.datafangstdato
        informasjon = common.informasjon
        this.opphav = common.opphav
    }

    private fun updateWithGrenseMetadata(grense: Grense, commonGrenseMetadata: CommonGrenseMetadata) {
        grense.kvalitet = commonGrenseMetadata.posisjonskvalitet.toEmbeddable()
    }
}
