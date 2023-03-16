package no.kartverket.nibas.service

import no.kartverket.nibas.api.v1.common.geojson.FeatureCollection
import no.kartverket.nibas.api.v1.request.StemmekretsRequest
import no.kartverket.nibas.api.v1.request.StemmekretsSammenslaaingsendringRequest
import no.kartverket.nibas.api.v1.response.KretsResponse
import no.kartverket.nibas.api.v1.response.StemmekretsRef
import no.kartverket.nibas.api.v1.response.StemmekretsResponse
import no.kartverket.nibas.api.v1.response.toGyldighetResponse
import no.kartverket.nibas.api.v1.response.toObjektIdentifikator
import no.kartverket.nibas.common.HrefProvider
import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.common.ResponseException
import no.kartverket.nibas.common.UgyldigTilstandException
import no.kartverket.nibas.common.createIdentifikator
import no.kartverket.nibas.domain.Endringstype
import no.kartverket.nibas.domain.GrenseKobling
import no.kartverket.nibas.domain.GrenseKoblingAttributter
import no.kartverket.nibas.domain.Stemmekrets
import no.kartverket.nibas.domain.StemmekretsGrenseKobling
import no.kartverket.nibas.domain.getGrenseJustertForRetning
import no.kartverket.nibas.kodelister.KodelisteService
import no.kartverket.nibas.kodelister.createKommunenummerFromUUID
import no.kartverket.nibas.repository.OptimistiskLaasException
import no.kartverket.nibas.repository.StemmekretsRepository
import no.kartverket.nibas.repository.findAllByLokalidSortedByGyldigFra
import no.kartverket.nibas.repository.findAntallFramtidigeVersjonerForIdentifikator
import no.kartverket.nibas.repository.findGyldigObjektForIdentifikator
import no.kartverket.nibas.repository.verifiserOptimistiskLaas
import no.kartverket.nibas.sosi.sosiimport.GrenseMedLokalid
import no.kartverket.nibas.sosi.sosiimport.SosiGeometryFunctions
import no.kartverket.nibas.sosi.sosiimport.SosiGeometryFunctions.lagFlate
import org.locationtech.jts.geom.Geometry
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Transactional(readOnly = true)
@Service
class StemmekretsService(private val stemmekretsRepository: StemmekretsRepository) {

    fun hentStemmekrets(identifikator: Identifikator): Stemmekrets? = stemmekretsRepository.findGyldigObjektForIdentifikator(identifikator)

    fun hentSisteGyldigeStemmekrets(lokalid: String): Stemmekrets? =
        stemmekretsRepository.findAllByLokalid(lokalid = lokalid)
            .filter { !it.gyldighet.slettet }
            .maxBy { it.gyldighet.gyldigFra }

    fun hentGrenserForStemmekrets(identifikator: Identifikator): List<GrenseKobling> {
        return hentStemmekrets(identifikator)?.getGrenser(identifikator.gyldighetsdato) ?: emptyList()
    }

    fun hentStemmekretserForIds(ids: Set<String>, gyldighetsdato: LocalDate): List<Stemmekrets> {
        return ids
            .map { createIdentifikator(it, gyldighetsdato) }
            .map { hentStemmekrets(it) ?: throw ResponseException.NotFoundException("Stemmekrets med $it ble ikke funnet.") }
            .sortedBy { it.id }
    }

    @Transactional(readOnly = false)
    fun oppdaterStemmekrets(
        lokalId: Lokalid,
        gyldigFra: LocalDate,
        endringstype: Endringstype,
        oppdatertStemmekrets: StemmekretsRequest
    ): Stemmekrets {

        val identifikator = Identifikator(lokalId, gyldigFra)
        val gammelStemmekrets = hentStemmekrets(identifikator)
            ?: throw ResponseException.NotFoundException("Stemmekrets med $identifikator ble ikke funnet.")

        verifiserOptimistiskLaas(oppdatertStemmekrets.version, gammelStemmekrets)?.let {
            throw OptimistiskLaasException(it)
        }

        return createNyVersjonAvStemmekrets(gammelStemmekrets, gyldigFra, endringstype)
            .applyChangesFromRequest(oppdatertStemmekrets)
    }

    fun hentAntallGyldigeVersjonerFramITidFor(identifikator: Identifikator): Int =
        stemmekretsRepository.findAntallFramtidigeVersjonerForIdentifikator(identifikator)

    fun hentFramtidigeVersjonerForStemmekrets(lokalId: Lokalid, gyldighetsdato: LocalDate?): List<Stemmekrets> {
        val identifikator = Identifikator(lokalId, gyldighetsdato ?: LocalDate.now())
        val gyldigStemmekretsForDagensDato = hentStemmekrets(identifikator)
            ?: throw UgyldigTilstandException("Det finnes ingen gyldig stemmekrets for identifikator $identifikator")
        return listOf(gyldigStemmekretsForDagensDato)
            .plus(hentAlleVersjonerMedGyldigFraEtterGittDato(lokalId, identifikator.gyldighetsdato))
    }

    @Transactional(readOnly = false)
    fun slaaSammenStemmekretser(
        sammenslaaingsendringRequest: StemmekretsSammenslaaingsendringRequest,
        sammenslaaingsDato: LocalDate,
        endringstype: Endringstype
    ): Pair<Stemmekrets, List<Stemmekrets>> {
        val identifikatorViderefoerStemmekrets = Identifikator(lokalid = Lokalid(sammenslaaingsendringRequest.viderefoertStemmekrets.lokalId), gyldighetsdato = sammenslaaingsDato)
        val viderefortStemmekrets = hentStemmekrets(identifikatorViderefoerStemmekrets)
            ?: throw ResponseException.NotFoundException("Videreført stemmekrets med identifikatior: $identifikatorViderefoerStemmekrets ble ikke funnet")

        val stemmekretserSomUtgaar = sammenslaaingsendringRequest.stemmekretserTilSammenslaaing
            .map { Identifikator(lokalid = Lokalid(it.lokalId), gyldighetsdato = sammenslaaingsDato)   }
            .map { hentStemmekrets(it) ?: throw ResponseException.NotFoundException("Stemmekrets til sammenslåing med identifikator: $it ble ikke funnet") }

        val stemmekretserForSjekkeOptimistiskLaas = stemmekretserSomUtgaar.map { stemmekrets ->
                Pair(stemmekrets, sammenslaaingsendringRequest.stemmekretserTilSammenslaaing.find { it.lokalId == stemmekrets.lokalid }?.version!!)
            }.plus(Pair(viderefortStemmekrets, sammenslaaingsendringRequest.viderefoertStemmekrets.version))

        stemmekretserForSjekkeOptimistiskLaas
            .forEach { (stemmekrets, versjon) ->
                verifiserOptimistiskLaas(versjon, stemmekrets)
                    ?.let { throw OptimistiskLaasException(it)}
            }

        stemmekretserSomUtgaar.forEach { it.setGyldigTil(sammenslaaingsDato) }

        val nyStemmekrets = createNyVersjonAvStemmekrets(viderefortStemmekrets, sammenslaaingsDato, endringstype)
            .apply {
                stemmekretsnavn = sammenslaaingsendringRequest.stemmekretsNavn
                stemmekretsnummer = sammenslaaingsendringRequest.stemmekretsNummer
            }

        val alleStemmekretsGrensekoblinger = viderefortStemmekrets.grenser + stemmekretserSomUtgaar.flatMap { it.grenser }

        // Hvis er grense deler 2 stemmekretser som er med i sammenslåingen så er den ikke en del
        // av et skall eller hull i den nye stemmekretsen
        val viderefoerteGrensekoblinger = alleStemmekretsGrensekoblinger
            .filter { grenseErIkkeDeltMellomToStemmekretser(it, alleStemmekretsGrensekoblinger) }

        nyStemmekrets.grenser = oppdaterGrensekoblingsAtributterForGrenser(
            stemmekrets = nyStemmekrets,
            grensekoblinger = viderefoerteGrensekoblinger,
            gamleGrensekoblinger = alleStemmekretsGrensekoblinger
        ).toMutableSet()

        nyStemmekrets.oppdaterOmraade()
        return Pair(nyStemmekrets, stemmekretserSomUtgaar)
    }

    fun createNyVersjonAvStemmekrets(stemmekrets: Stemmekrets, gyldigFra: LocalDate, endringstype: Endringstype): Stemmekrets {
        return stemmekretsRepository.save(
            Stemmekrets(lokalid = stemmekrets.lokalid)
                .apply {
                    applyMappingFromExistingStemmekrets(stemmekrets)
                    setGyldigFra(gyldigFra)
                    version = nesteVersjonForStemmekrets(Lokalid(stemmekrets.lokalid))
                    auditInfo.typeEndring = endringstype
                }
        )
            .applyCascadingOperationsBasedOnExistingStemmekrets(stemmekrets)
            .also { stemmekrets.setGyldigTil(gyldigFra)  }
    }

    private fun grenseErIkkeDeltMellomToStemmekretser(grenseKobling: StemmekretsGrenseKobling, alleGrensekoblinger: Collection<StemmekretsGrenseKobling>) =
        alleGrensekoblinger.count { grenseKobling.grense.id == it.grense.id } == 1

    private fun oppdaterGrensekoblingsAtributterForGrenser(stemmekrets: Stemmekrets, grensekoblinger: Collection<StemmekretsGrenseKobling>, gamleGrensekoblinger: Collection<StemmekretsGrenseKobling>): List<StemmekretsGrenseKobling> {
        val alleFlaterOgHull = SosiGeometryFunctions.finnFlaterForGrenser(
            grensekoblinger.map { GrenseMedLokalid(it.getGrenseJustertForRetning(), Lokalid(it.grense.lokalid)) }
        )

        val flaterSomPolygon = alleFlaterOgHull.map { grense -> lagFlate(grense.map { it.grense }, emptyList()) }
        val alleSkall = alleFlaterOgHull.filterIndexed { index, _ -> flateErEtSkall(flaterSomPolygon[index], flaterSomPolygon) }

        // Hull-index må vært strengt stigende for alle hull for en krets, selv om de er hull i ulike flater
        var hullIndex = 0
        return alleSkall.flatMapIndexed { flateIndex, skall ->
            val skallPolygon = lagFlate(skall.map { it.grense }, emptyList())
            val hullIFlaten = alleFlaterOgHull.filterIndexed { hullIndex, _ -> flateErHullISkall(flaterSomPolygon[hullIndex], skallPolygon) }

            val grensekoblingerForSkall = skall.mapIndexed { skallGrenseIndex, grenseMedLokalid ->
                lagNyGrensekobling(
                    grense = grenseMedLokalid,
                    stemmekrets = stemmekrets,
                    rekkefoelge = skallGrenseIndex,
                    flateIndex = flateIndex,
                    gamleGrensekoblinger = gamleGrensekoblinger
                )
            }

            val grensekoblingerForHull = hullIFlaten.flatMap { grenseMedLokalids ->
                grenseMedLokalids.mapIndexed { hullGrenseIndex, hullGrense ->
                    lagNyGrensekobling(
                        grense = hullGrense,
                        stemmekrets = stemmekrets,
                        rekkefoelge = hullGrenseIndex,
                        flateIndex = flateIndex,
                        hullIndex = hullIndex++,
                        gamleGrensekoblinger = gamleGrensekoblinger
                    )
                }
            }

            grensekoblingerForSkall + grensekoblingerForHull
        }
    }

    private fun lagNyGrensekobling(grense: GrenseMedLokalid, stemmekrets: Stemmekrets, rekkefoelge: Int, flateIndex: Int, hullIndex: Int? = null, gamleGrensekoblinger: Collection<StemmekretsGrenseKobling>): StemmekretsGrenseKobling {
        val gammelGrenskobling = gamleGrensekoblinger.find { grense.lokalid.value == it.grense.lokalid }
            ?: throw RuntimeException("Klarte ikke finne gammel grensekobling for grense med id ${grense.lokalid.value}. Dette skal ikke skje og tyder på programmeringsfeil.")

        return StemmekretsGrenseKobling(
            stemmekrets = stemmekrets,
            grense = gammelGrenskobling.grense,
            grenseKoblingAttributter = GrenseKoblingAttributter(
                retningMedKlokken = gammelGrenskobling.grenseKoblingAttributter.retningMedKlokken,
                rekkefoelge = rekkefoelge,
                flateIndeks = flateIndex,
                hullIndeks = hullIndex
            )
        )
    }

    private fun nesteVersjonForStemmekrets(lokalId: Lokalid): Int =
        stemmekretsRepository
            .findAllByLokalid(lokalId.value)
            .maxBy { it.version }
            .version + 1

    fun hentAlleVersjonerMedGyldigFraEtterGittDato(lokalId: Lokalid, dato: LocalDate): List<Stemmekrets> =
        stemmekretsRepository.findAllByLokalidSortedByGyldigFra(lokalId)
            .filter { it.gyldighet.gyldigFra.isAfter(dato) }

    private fun flateErHullISkall(flate: Geometry, skall: Geometry) =
        flate.within(skall) && (flate != skall)

    private fun flateErEtSkall(flate: Geometry, alleFlater: List<Geometry>) =
        alleFlater.all { flate == it || !flate.within(it) }
}

fun Stemmekrets.applyChangesFromRequest(stemmekretsRequest: StemmekretsRequest): Stemmekrets =
    this.apply {
        stemmekretsRequest.stemmekretsnavn?.let { this.stemmekretsnavn = it }
        stemmekretsRequest.stemmekretsnummer?.let { this.stemmekretsnummer = it }
        stemmekretsRequest.tellekretsnummer?.let { this.tellekretsnummer = it }
        stemmekretsRequest.tellekretsnavn?.let { this.tellekretsnavn = it }
        stemmekretsRequest.valgdistriktsnummer?.let { this.valgdistriktsnummer = it }
    }

fun Stemmekrets.toStemmekretsRef(identifikator: Identifikator, antallFramtidigeVersjoner: Int = 0, hrefProvider: HrefProvider, kodelisteService: KodelisteService): StemmekretsRef {
    return StemmekretsRef(
        id = identifikator.toObjektIdentifikator(),
        navn = this.stemmekretsnavn!!,
        nummer = this.stemmekretsnummer!!,
        href = hrefProvider.getHref(identifikator),
        kommunenummer = kodelisteService.createKommunenummerFromUUID(this.kommune.kommunenummer),
        antallFramtidigeVersjoner = antallFramtidigeVersjoner,
        version = this.version,
    )
}

fun Stemmekrets.toStemmekretsResponse(identifikator: Identifikator, kodelisteService: KodelisteService): StemmekretsResponse {
    return StemmekretsResponse(
        id = identifikator.toObjektIdentifikator(),
        stemmekretsnavn = this.stemmekretsnavn!!,
        stemmekretsnummer = this.stemmekretsnummer!!,
        kommunenummer = kodelisteService.createKommunenummerFromUUID(this.kommune.kommunenummer),
        valgdistriktsnummer = this.valgdistriktsnummer,
        tellekretsnavn = this.tellekretsnavn,
        gyldighet = this.gyldighet.toGyldighetResponse(),
        endringstype = this.auditInfo.typeEndring.value,
        oppdateringsdato = this.auditInfo.endretdato,
        tellekretsnummer = this.tellekretsnummer,
        features = FeatureCollection(features = listOf(
            posisjonToFeature(this.posisjon, this.version),
            flateToFeature(this, this.version)
        )),
        version = this.version
    )
}

fun Stemmekrets.toKretsResponse(kodelisteService: KodelisteService): KretsResponse = KretsResponse(
    id = this.lokalid,
    navn = this.stemmekretsnavn!!,
    nummer = this.stemmekretsnummer!!,
    kommunenummer = kodelisteService.createKommunenummerFromUUID(this.kommune.kommunenummer),
    features = FeatureCollection(
        features = listOf(
            posisjonToFeature(this.posisjon, this.version),
            flateToFeature(this, this.version)
        )
    )
)


