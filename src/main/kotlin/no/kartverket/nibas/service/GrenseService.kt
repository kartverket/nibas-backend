package no.kartverket.nibas.service

import no.kartverket.nibas.api.v1.common.Grensetype
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
import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.domain.GrenseKoblingAttributter
import no.kartverket.nibas.domain.Grunnkrets
import no.kartverket.nibas.domain.GrunnkretsGrenseKobling
import no.kartverket.nibas.domain.Stemmekrets
import no.kartverket.nibas.domain.StemmekretsGrenseKobling
import no.kartverket.nibas.extensions.geometry.toLineString
import no.kartverket.nibas.repository.GrenseRepository
import no.kartverket.nibas.repository.GrunnkretsRepository
import no.kartverket.nibas.repository.KonfliktVersjon
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
    private val stemmekretsService: StemmekretsService
    ) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun hentGrense(identifikator: Identifikator): Grense? = grenseRepository.findGyldigObjektForIdentifikator(identifikator)

    @Transactional(readOnly = false)
    fun lagreGrenser(grenser: FeatureCollection, gyldighetsdato: LocalDate = LocalDate.now(), endringstype: Endringstype) : Set<Flate> {
        val oppdaterteKretser: MutableSet<Flate> = mutableSetOf()
        val enheterSomSkalFlatedannes: Set<Flate>

        val millis = measureTimeMillis {
            val features = grenser.features
            features
                .groupBy { it.properties.type }
                .forEach { mapEntry ->
                    oppdaterteKretser.addAll(doLagreGrenser(
                        features = mapEntry.value,
                        gyldighetsdato = gyldighetsdato,
                        endringstype = endringstype
                    ))
                }

            enheterSomSkalFlatedannes = features.flatMap {
                val identifikator = featureIdToIdentifikator(it.id, gyldighetsdato)
                val grense = hentGrense(identifikator) ?: throw ResponseException.NotFoundException("Grense $identifikator ble ikke funnet.")
                when (Grensetype.fromValue(it.properties.type)) {
                    //TODO: Når vi slår sammen administrative grenser for stemme- og grunnkrets må vi huske å oppdatere stemmekretsene også når vi oppdatererer en administrativ grense. Pr nå tas ikke disse flatene med utenfor "stemmekrets-sfæren"
                    Grensetype.Kommunegrense -> grense.finnFlaterOppTilKommune()
                    Grensetype.Fylkesgrense -> grense.finnFlaterOppTilFylke()
                    Grensetype.Riksgrense -> grense.finnFlaterOppTilNasjon()
                    Grensetype.AvtaltAvgrensningslinje -> grense.finnFlaterOppTilNasjon()
                    Grensetype.Territorialgrense -> grense.finnFlaterOppTilNasjon()
                    Grensetype.Grunnkretsgrense -> grense.finnFlaterOppTilGrunnkrets()
                    Grensetype.Delomraadegrense -> grense.finnFlaterOppTilGrunnkrets()
                    Grensetype.Stemmekretsgrense -> grense.finnFlaterOppTilStemmekrets()
                    else -> throw RuntimeException("Not yet implemented for grensetype: ${it.properties.type}")
                }
            }.toSet()

            //Alle oppdaterte kretser skal bli funnet som enhet som skal flatedannes
            //Enheter som skal flatedannes kan også inneholde kommune/fylke/nasjon
            if (!enheterSomSkalFlatedannes.containsAll(oppdaterteKretser)) {
                throw RuntimeException("Ikke alle oppdaterte flater ble funnet for å flatedannes")      }

            //Her er det bare å oppdatere, kretsene fikk ny versjon ved lagring av grenser og grensekoblinger
            //TODO Mangler ny versjon for evt kommune/fylke/nasjon som skal flatedannes (TS-870)
            flateService.oppdaterFlaterFor(enheterSomSkalFlatedannes)
        }

        logger.debug("Tid brukt på å lagre grenser og oppdatere flater: $millis")
        return enheterSomSkalFlatedannes
    }

    // Feature.id vil være lokalid for grensa
    private fun doLagreGrenser(features: List<Feature>, gyldighetsdato: LocalDate = LocalDate.now(), endringstype: Endringstype) : Set<Flate> {
        // Hvis det er innkommende features med id ikke satt kastes HTTP 400
        if (features.any { feature -> feature.id == null }) {
            throw BadRequestException("Missing feature id.")
        }
        // Henter eksisterende grenser på identifikator. Sorterer på lokalid for testens skyld.
        val grenseIdentifikatorer = features.map { featureIdToIdentifikator(it.id, gyldighetsdato) }.sortedBy { it.lokalid.value }
        val identifikatorToGrenseMap = grenseRepository.findAllGyldigeObjekterForIdentifikatorer(grenseIdentifikatorer)
        val eksisterendeGrenser: List<Grense> = identifikatorToGrenseMap.values.toList()

        // Hvis det er innkommende features med id som ikke ligger i databasen kastes HTTP 400
        val identifikatorDiffs: List<Identifikator> = grenseIdentifikatorer - identifikatorToGrenseMap.keys
        if (identifikatorDiffs.isNotEmpty()) {
            throw BadRequestException("Non-existing grenseidentifikatorer: [${identifikatorDiffs.joinToString(",")}])")
        }

        val konflikterIGrenser: List<KonfliktVersjon> = features.mapNotNull { feature ->
            val featureIdentifikator = featureIdToIdentifikator(feature.id, gyldighetsdato)
            verifiserOptimistiskLaas(feature.properties.version, findGrenseForIdentifikator(eksisterendeGrenser, featureIdentifikator))
        }
        if (konflikterIGrenser.isNotEmpty()) {
            throw OptimistiskLaasException(*konflikterIGrenser.toTypedArray())
        }
        //TODO Skulle også ha sjekket lås for grensenes kretser, men det sendes ikke inn versjon

        //Flere endrede grenser kan være knyttet til samme krets.
        //Ei endra grense kan ha flere knytninger til samme krets, dvs til ulike versjoner av samme krets
        //Må derfor ha oversikt over hvilke flater som har fått ny versjon
        val flaterSomHarFaattNyVersjon: MutableMap<Lokalid, Flate> = mutableMapOf()

        features.forEach { feature ->
            val gammelGrense: Grense = findGrenseForIdentifikator(eksisterendeGrenser, featureIdToIdentifikator(feature.id, gyldighetsdato))

            // Uansett om gyldigFra er identisk med grensens gyldigFra eller fram i tid: Lage ny rad (ny uuid, samme lokalid).
            logger.info("Lagrer grense: [${gammelGrense.grensetype.name} - id ${feature.id}]")

            val nyGrense = createNyVersjonAvGrense(gammelGrense, gyldighetsdato, endringstype)
                .applyChangesFromRequest(feature)

            //Det må lages en ny forekomst for tilknyttede kretser, de får også oppdatert knytning til gjeldende grense.
            //De får oppdatert område etter at alle grensene er behandlet

            //todo Får bare inn én krets tilhørende grensa (TS-918). Må sjekke om grensa har flere kretser og oppdatere kretsen på den andre sida
            //Må jobbe på en kopi av lista, for ikke å få ConcurrentModificationException
            val gamleStemmekretsKoblinger = mutableSetOf<StemmekretsGrenseKobling>()
            gamleStemmekretsKoblinger.addAll(gammelGrense.gyldigeStemmekretsGrenseKoblinger())

            gamleStemmekretsKoblinger.forEach {
                lagEvtNyVersjonAvStemmekretsOgLeggTilNyGrense(flaterSomHarFaattNyVersjon, Identifikator(Lokalid(it.stemmekrets.lokalid), gyldighetsdato),
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
            gamleGrunnkretsKoblinger.addAll(gammelGrense.gyldigeGrunnkretsGrenseKoblinger())

            gamleGrunnkretsKoblinger.forEach {
                lagEvtNyVersjonAvGrunnkretsOgLeggTilNyGrense(flaterSomHarFaattNyVersjon, Identifikator(Lokalid(it.grunnkrets.lokalid), gyldighetsdato),
                    endringstype, gammelGrense, nyGrense,
                    GrenseKoblingAttributter(
                        it.grenseKoblingAttributter.retningMedKlokken,
                        it.grenseKoblingAttributter.rekkefoelge,
                        it.grenseKoblingAttributter.flateIndeks,
                        it.grenseKoblingAttributter.hullIndeks
                    )
                )
            }
        }
        grenseRepository.flush()
        return flaterSomHarFaattNyVersjon.values.toSet()
    }

    fun createNyVersjonAvGrense(grense: Grense, gyldigFra: LocalDate, endringstype: Endringstype): Grense {
        return grenseRepository.saveAndFlush(
            Grense(lokalid = grense.lokalid, grensetype = grense.grensetype)
                .apply {
                    applyMappingFromExistingGrense(grense)
                    setGyldigFra(gyldigFra)
                    version = nesteVersjonForGrense(Lokalid(grense.lokalid))
                    auditInfo.typeEndring = endringstype
                }
        )
            .applyCascadingOperationsBasedOnExistingGrense(grense)
            .also { grense.setGyldigTil(gyldigFra)  }
    }

    fun Grense.applyChangesFromRequest(feature: Feature): Grense =
        this.apply {
            grense = feature.geometry.toLineString(feature.properties.srid)
            updateMetadataOnGrense(feature, this)
        }

    private fun lagEvtNyVersjonAvStemmekretsOgLeggTilNyGrense(
        flaterSomHarFaattNyVersjon: MutableMap<Lokalid, Flate>,
        kontekstId: Identifikator,
        endringstype: Endringstype,
        gammelGrense: Grense,
        nyGrense: Grense,
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

    private fun lagEvtNyVersjonAvGrunnkretsOgLeggTilNyGrense(
        flaterSomHarFaattNyVersjon: MutableMap<Lokalid, Flate>,
        kontekstId: Identifikator,
        endringstype: Endringstype,
        gammelGrense: Grense,
        nyGrense: Grense,
        grenseKoblingAttributter: GrenseKoblingAttributter
    ) {
        var grunnkrets = flaterSomHarFaattNyVersjon[kontekstId.lokalid]
        if (grunnkrets == null) {
            val gammelGrunnkrets = grunnkretsRepository.findGyldigObjektForIdentifikator(kontekstId)
                ?: throw UgyldigTilstandException("Fant ikke gyldig grunnkrets for identifikator $kontekstId")
            grunnkrets = grunnkretsService.createNyVersjonAvGrunnkrets(grunnkrets = gammelGrunnkrets,
                gyldigFra = kontekstId.gyldighetsdato, endringstype = endringstype)
            flaterSomHarFaattNyVersjon[kontekstId.lokalid] = grunnkrets
        }

        //Den nye grunnkretsen har fått kobling til alle grensene som den gamle har
        //Må bytte ut koblinga til den oppdaterte grensa
        (grunnkrets as Grunnkrets).replaceGrensekobling(gammelGrense, nyGrense, grenseKoblingAttributter)
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
}
