package no.kartverket.nibas.service

import no.kartverket.nibas.api.v1.request.KretsDelingEndringRequest
import no.kartverket.nibas.api.v1.request.StemmekretsRequest
import no.kartverket.nibas.api.v1.request.StemmekretsSammenslaaingsendringRequest
import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.common.ResponseException
import no.kartverket.nibas.common.UgyldigTilstandException
import no.kartverket.nibas.common.createIdentifikator
import no.kartverket.nibas.common.getInSpraak
import no.kartverket.nibas.common.toNorskFormat
import no.kartverket.nibas.domain.Endringstype
import no.kartverket.nibas.domain.GrenseKoblingAttributter
import no.kartverket.nibas.domain.Kommune
import no.kartverket.nibas.domain.Stemmekrets
import no.kartverket.nibas.domain.StemmekretsGrenseKobling
import no.kartverket.nibas.domain.getGrenseJustertForRetning
import no.kartverket.nibas.kodelister.KodelisteService
import no.kartverket.nibas.repository.OptimistiskLaasException
import no.kartverket.nibas.repository.StemmekretsRepository
import no.kartverket.nibas.repository.findAllByLokalidSortedByGyldigFra
import no.kartverket.nibas.repository.findAntallFramtidigeVersjonerForIdentifikator
import no.kartverket.nibas.repository.findGyldigObjektForIdentifikator
import no.kartverket.nibas.repository.verifiserOptimistiskLaas
import no.kartverket.nibas.service.utkast.UtkastService
import no.kartverket.nibas.sosi.sosiimport.GrenseMedLokalid
import no.kartverket.nibas.sosi.sosiimport.SosiGeometryFunctions
import no.kartverket.nibas.sosi.sosiimport.SosiGeometryFunctions.lagFlate
import org.locationtech.jts.geom.Geometry
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Transactional(readOnly = true)
@Service
class StemmekretsService(
    private val stemmekretsRepository: StemmekretsRepository,
    private val kodelisteService: KodelisteService
) {

    fun hentStemmekrets(identifikator: Identifikator): Stemmekrets? = stemmekretsRepository.findGyldigObjektForIdentifikator(identifikator)

    fun hentSisteGyldigeStemmekrets(lokalid: String): Stemmekrets? =
        stemmekretsRepository.findAllByLokalid(lokalid = lokalid)
            .filter { !it.gyldighet.slettet }
            .maxBy { it.gyldighet.gyldigFra }

    fun hentGrenserForStemmekrets(identifikator: Identifikator): List<StemmekretsGrenseKobling> {
        return hentStemmekrets(identifikator)?.getGrenser(identifikator.gyldighetsdato) ?: emptyList()
    }

    fun hentStemmekretserForIds(ids: Set<String>, gyldighetsdato: LocalDate): List<Stemmekrets> {
        return ids
            .map { createIdentifikator(it, gyldighetsdato) }
            .map {
                hentStemmekrets(it) ?: throw ResponseException.NotFoundException(title = "Stemmekrets ikke funnet",
                    msg = "Stemmekrets med med id ${it.lokalid} og gyldighetsdato ${gyldighetsdato.toNorskFormat()} ble ikke funnet.")
            }
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
            ?: throw ResponseException.NotFoundException(title = "Stemmekrets ikke funnet", msg = "Stemmekrets med $identifikator ble ikke funnet.")

        verifiserOptimistiskLaas(oppdatertStemmekrets.version, gammelStemmekrets)?.let {
            throw OptimistiskLaasException(it)
        }

        val nyStemmekrets = createNyVersjonAvStemmekrets(gammelStemmekrets, gyldigFra, endringstype)
            .applyChangesFromRequest(oppdatertStemmekrets)

        if (finnesAnnenStemmekretsMedSammeKommuneOgStemmekretsnummer(nyStemmekrets)) {
            throw createDuplikatStemmekretsnummerException(nyStemmekrets)
        }

        return nyStemmekrets
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
        val identifikatorViderefoerStemmekrets =
            Identifikator(lokalid = Lokalid(sammenslaaingsendringRequest.viderefoertStemmekrets.lokalId), gyldighetsdato = sammenslaaingsDato)
        val viderefortStemmekrets = hentStemmekrets(identifikatorViderefoerStemmekrets)
            ?: throw ResponseException.NotFoundException(title = "Stemmekrets ikke funnet",
                msg = "Videreført stemmekrets med identifikator: $identifikatorViderefoerStemmekrets ble ikke funnet")

        val stemmekretserSomUtgaar = sammenslaaingsendringRequest.stemmekretserTilSammenslaaing
            .map { Identifikator(lokalid = Lokalid(it.lokalId), gyldighetsdato = sammenslaaingsDato) }
            .map {
                hentStemmekrets(it) ?: throw ResponseException.NotFoundException(title = "Stemmekrets ikke funnet",
                    msg = "Stemmekrets til sammenslåing med identifikator: $it ble ikke funnet")
            }

        val stemmekretserForSjekkeOptimistiskLaas = stemmekretserSomUtgaar.map { stemmekrets ->
            Pair(stemmekrets, sammenslaaingsendringRequest.stemmekretserTilSammenslaaing.find { it.lokalId == stemmekrets.lokalid }?.version!!)
        }.plus(Pair(viderefortStemmekrets, sammenslaaingsendringRequest.viderefoertStemmekrets.version))

        stemmekretserForSjekkeOptimistiskLaas
            .forEach { (stemmekrets, versjon) ->
                verifiserOptimistiskLaas(versjon, stemmekrets)
                    ?.let { throw OptimistiskLaasException(it) }
            }

        stemmekretserSomUtgaar.forEach { it.setGyldigTil(sammenslaaingsDato) }

        val nyStemmekrets = createNyVersjonAvStemmekrets(viderefortStemmekrets, sammenslaaingsDato, endringstype)
            .apply {
                stemmekretsnavn = sammenslaaingsendringRequest.stemmekretsNavn
                stemmekretsnummer = sammenslaaingsendringRequest.stemmekretsNummer
            }

        val alleStemmekretsGrensekoblinger = viderefortStemmekrets.grenser + stemmekretserSomUtgaar.flatMap { it.grenser }

        // Hvis en grense deler 2 stemmekretser som er med i sammenslåingen så er den ikke en del
        // av et skall eller hull i den nye stemmekretsen
        val viderefoerteGrensekoblinger = alleStemmekretsGrensekoblinger
            .filter { grenseDelerIkke2StemmekretserSomBlirSlaattSammen(it, alleStemmekretsGrensekoblinger) }

        val grensekoblingerSomUtgaar = alleStemmekretsGrensekoblinger
            .filter { grenseDeler2StemmekretserSomBlirSlaattSammen(it, alleStemmekretsGrensekoblinger) }

        grensekoblingerSomUtgaar.forEach { it.grense.setGyldigTil(sammenslaaingsDato) }

        nyStemmekrets.grenser = oppdaterGrensekoblingsAtributterForGrenser(
            stemmekrets = nyStemmekrets,
            grensekoblinger = viderefoerteGrensekoblinger,
            gamleGrensekoblinger = alleStemmekretsGrensekoblinger
        ).toMutableSet()

        nyStemmekrets.oppdaterOmraadeOgPosisjon()

        if (finnesAnnenStemmekretsMedSammeKommuneOgStemmekretsnummer(nyStemmekrets)) {
            throw createDuplikatStemmekretsnummerException(nyStemmekrets)
        }

        return Pair(nyStemmekrets, stemmekretserSomUtgaar)
    }

    fun delStemmekrets(
        kretsDelingEndringRequest: KretsDelingEndringRequest, delingsDato: LocalDate, endringstype: Endringstype
    ): Stemmekrets {
        val identifikator = Identifikator(Lokalid(kretsDelingEndringRequest.opprinneligKrets.lokalId), delingsDato)
        val stemmekrets = hentStemmekrets(identifikator)
            ?: throw RuntimeException("Fant ikke stemmekrets med lokalid: ${kretsDelingEndringRequest.opprinneligKrets.lokalId} gylidg på $delingsDato")

        val nyStemmekrets = Stemmekrets().apply {
            stemmekretsnavn = kretsDelingEndringRequest.kretsNavn
            stemmekretsnummer = kretsDelingEndringRequest.kretsNummer
            utledetFra = stemmekrets
            setGyldigFra(delingsDato)
        }

        return nyStemmekrets
    }

    fun createNyVersjonAvStemmekrets(stemmekrets: Stemmekrets, gyldigFra: LocalDate, endringstype: Endringstype): Stemmekrets {
        val nyStemmekrets = kopierOgSaveStemmekrets(stemmekrets, gyldigFra, endringstype)

        val kommune = stemmekrets.getKommune(gyldigFra) ?: throw ResponseException.NotFoundException(title = "Kommune ikke funnet",
            msg = "Fant ikke kommune for stemmekrets med id ${stemmekrets.id} og gyldig fra $gyldigFra")
        kommune.addStemmekrets(nyStemmekrets)
        return nyStemmekrets
    }

    private fun kopierOgSaveStemmekrets(stemmekrets: Stemmekrets, gyldigFra: LocalDate, endringstype: Endringstype): Stemmekrets {
        val nyStemmekrets = stemmekretsRepository.save(
            Stemmekrets(lokalid = stemmekrets.lokalid)
                .applyMappingFromExistingStemmekrets(stemmekrets)
                .apply {
                    setGyldigFra(gyldigFra)
                    stemmekrets.gyldighet.gyldigTil?.let { setGyldigTil(it) }
                    version = nesteVersjonForStemmekrets(Lokalid(stemmekrets.lokalid))
                    auditInfo.typeEndring = endringstype
                }
        )

        return nyStemmekrets
            .applyCascadingOperationsBasedOnExistingStemmekrets(stemmekrets, gyldigFra)
            .also { stemmekrets.setGyldigTil(gyldigFra) }
    }

    fun createNyVersjonAvStemmekretsIAnnenKommune(stemmekrets: Stemmekrets, kommune: Kommune, gyldigFra: LocalDate, endringstype: Endringstype): Stemmekrets {
        val nyStemmekrets = kopierOgSaveStemmekrets(stemmekrets, gyldigFra, endringstype)

        kommune.addStemmekrets(nyStemmekrets)
        return nyStemmekrets
    }

    private fun createDuplikatStemmekretsnummerException(stemmekrets: Stemmekrets): ResponseException.BadRequestException {
        val kommune = stemmekrets.getKommune(stemmekrets.gyldighet.gyldigFra)
        val kommunenavn = kommune?.getAdministrativenhetNavn(stemmekrets.gyldighet.gyldigFra)?.getInSpraak("nor") ?: "UKJENT KOMMUNE"
        val kommunenummer = kommune?.kommunenummer
        val gyldigFraTekst = stemmekrets.gyldighet.gyldigFra.toNorskFormat()
        val gyldigTilTekst = stemmekrets.gyldighet.gyldigTil?.let { "til ${it.toNorskFormat()}" } ?: "og fremover"

        return ResponseException.BadRequestException(
            title = "Duplikate kretsnummer innenfor samme kommune",
            msg = "Kretsnummer ${stemmekrets.stemmekretsnummer} er brukt på flere stemmekretser innenfor kommunen (${kommunenummer} ${kommunenavn}) " +
                "med gyldighet som overlapper med $gyldigFraTekst $gyldigTilTekst. " +
                "Vennligst gi stemmekretsene unike kretsnumre før du fortsetter."
        )
    }

    private fun grenseDelerIkke2StemmekretserSomBlirSlaattSammen(grenseKobling: StemmekretsGrenseKobling, alleGrensekoblinger: Collection<StemmekretsGrenseKobling>) =
        !grenseDeler2StemmekretserSomBlirSlaattSammen(grenseKobling, alleGrensekoblinger)

    private fun grenseDeler2StemmekretserSomBlirSlaattSammen(grenseKobling: StemmekretsGrenseKobling, alleGrensekoblinger: Collection<StemmekretsGrenseKobling>) =
        alleGrensekoblinger.count { grenseKobling.grense.id == it.grense.id } == 2

    private fun oppdaterGrensekoblingsAtributterForGrenser(
        stemmekrets: Stemmekrets,
        grensekoblinger: Collection<StemmekretsGrenseKobling>,
        gamleGrensekoblinger: Collection<StemmekretsGrenseKobling>
    ): List<StemmekretsGrenseKobling> {

        val alleFlaterOgHull = SosiGeometryFunctions.finnFlaterForGrenser(
            grensekoblinger.map { GrenseMedLokalid(it.getGrenseJustertForRetning(stemmekrets.gyldighet.gyldigFra), Lokalid(it.grense.lokalid)) }
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

    private fun lagNyGrensekobling(grense: GrenseMedLokalid, stemmekrets: Stemmekrets, rekkefoelge: Int, flateIndex: Int, hullIndex: Int? = null,
                                   gamleGrensekoblinger: Collection<StemmekretsGrenseKobling>): StemmekretsGrenseKobling {
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

    fun finnesAnnenStemmekretsMedSammeKommuneOgStemmekretsnummer(stemmekrets: Stemmekrets): Boolean {
        //Returnerer false hvis stemmekretsnummer ikke er gitt, isåfall vil det komme valideringsfeil fra et annet sted
        val muligeStemmekretsnumre = stemmekrets.stemmekretsnummer?.let { lagMuligeStemmekretsnumre(it) } ?: return false

        val kommune = stemmekrets.getKommune(stemmekrets.gyldighet.gyldigFra)
            ?: throw ResponseException.NotFoundException(title = "Kommune ikke funnet",
                msg = "Fant ikke kommune for stemmekrets med id ${stemmekrets.id} og gyldig fra ${stemmekrets.gyldighet.gyldigFra}")
        val andreKretserMedSammeStemmekretsnummer = stemmekretsRepository.findAllByStemmekretsnummerInAndLokalidIsNotAndKommuneLokalid(
            muligeStemmekretsnumre, stemmekrets.lokalid, kommune.lokalid)

        if (andreKretserMedSammeStemmekretsnummer.isEmpty()) {
            return false
        }
        //Sjekk om noen av de funnede stemmekretsene har sammenfallende gyldighet med stemmekretsen som skal valideres
        return andreKretserMedSammeStemmekretsnummer.any { funnet -> funnet.gyldighet.overlapperMed(stemmekrets.gyldighet) }
    }

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


/**
 * Trekk ut numerisk stemmekretsnummer fra paramert nummer (f.eks "05" er 5)
 * og lag et sett med de mulige tekstlige stemmekretsnumrene med ledende nuller, inntil nummeret er på 4 tegn
 */
fun lagMuligeStemmekretsnumre(stemmekretsnummer: String): Set<String> {
    val set: MutableSet<String> = mutableSetOf()

    val numeriskStemmekretsnummer = stemmekretsnummer.toIntOrNull()
    if (numeriskStemmekretsnummer != null) {
        var nummer = numeriskStemmekretsnummer.toString()
        while (nummer.length <= 4) {
            set.add(nummer)
            nummer = "0" + nummer
        }
    }
    return set.toSet()
}
