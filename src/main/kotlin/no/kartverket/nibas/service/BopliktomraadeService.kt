package no.kartverket.nibas.service

import no.kartverket.nibas.api.v1.common.Flatetype
import no.kartverket.nibas.api.v1.common.UnikKretsDelingEndring
import no.kartverket.nibas.api.v1.request.BopliktomraadeRequest
import no.kartverket.nibas.api.v1.request.KretsDelingEndringRequest
import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.common.ResponseException
import no.kartverket.nibas.common.UgyldigTilstandException
import no.kartverket.nibas.domain.Bopliktomraade
import no.kartverket.nibas.domain.BopliktomraadeGrenseKobling
import no.kartverket.nibas.domain.Endringstype
import no.kartverket.nibas.repository.BopliktomraadeRepository
import no.kartverket.nibas.repository.findAllByLokalidSortedByGyldigFra
import no.kartverket.nibas.repository.findAllGyldigPaaDato
import no.kartverket.nibas.repository.findAntallFramtidigeVersjonerForIdentifikator
import no.kartverket.nibas.repository.findGyldigObjektForIdentifikator
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.MultiPolygon
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
@Transactional(readOnly = true)
class BopliktomraadeService(
    private val bopliktomraadeRepository: BopliktomraadeRepository
) {
    fun hentAlleBopliktomraader(dato: LocalDate): List<Bopliktomraade> =
        bopliktomraadeRepository.findAllGyldigPaaDato(dato)

    fun hentBopliktomraade(identifikator: Identifikator): Bopliktomraade? =
        bopliktomraadeRepository.findGyldigObjektForIdentifikator(identifikator)

    fun hentGrensekoblingerForBopliktomraade(identifikator: Identifikator): List<BopliktomraadeGrenseKobling> {
        return hentBopliktomraade(identifikator)
            ?.getGrensekoblinger(identifikator.gyldighetsdato)?.toList()
            ?: emptyList()
    }

    fun hentAlleVersjonerMedGyldigFraEtterGittDato(lokalId: Lokalid, dato: LocalDate): List<Bopliktomraade> =
        bopliktomraadeRepository.findAllByLokalidSortedByGyldigFra(lokalId)
            .filter { it.gyldighet.gyldigFra.isAfter(dato) }

    fun hentAntallGyldigeVersjonerFramITidFor(identifikator: Identifikator): Int =
        bopliktomraadeRepository.findAntallFramtidigeVersjonerForIdentifikator(identifikator)

    fun hentFramtidigeVersjonerForBopliktomraade(lokalId: Lokalid, gyldighetsdato: LocalDate?): List<Bopliktomraade> {
        val identifikator = Identifikator(lokalId, gyldighetsdato ?: LocalDate.now())
        val gyldigBopliktomraadeForDagensDato = hentBopliktomraade(identifikator)
            ?: throw UgyldigTilstandException("Det finnes ingen gyldig bopliktomraade for identifikator $identifikator")
        return listOf(gyldigBopliktomraadeForDagensDato)
            .plus(hentAlleVersjonerMedGyldigFraEtterGittDato(lokalId, identifikator.gyldighetsdato))
    }

    @Transactional(readOnly = false)
    fun oppdaterBopliktomraade(
        lokalId: Lokalid,
        gyldigFra: LocalDate,
        endringstype: Endringstype,
        oppdatertBopliktomraade: BopliktomraadeRequest
    ): Bopliktomraade {

        val identifikator = Identifikator(lokalId, gyldigFra)
        val gammeltBopliktomraade = hentBopliktomraade(identifikator)
            ?: throw ResponseException.NotFoundException(title = "Bopliktområde ikke funnet", msg = "Bopliktområde med $identifikator ble ikke funnet.")

        val nyttBopliktomraade = createNyVersjonAvBopliktomraade(gammeltBopliktomraade, gyldigFra, endringstype)
            .applyChangesFromRequest(oppdatertBopliktomraade)

        return nyttBopliktomraade
    }

    @Transactional(readOnly = false)
    fun createNyVersjonAvBopliktomraade(bopliktomraade: Bopliktomraade, gyldigFra: LocalDate, endringstype: Endringstype): Bopliktomraade {
        return createNyVersjonAvBopliktomraadeUtenGrenser(bopliktomraade, gyldigFra, endringstype)
            .copyGrenserFromExistingBopliktomraade(bopliktomraade, gyldigFra)
    }

    @Transactional(readOnly = false)
    fun createNyVersjonAvBopliktomraadeUtenGrenser(bopliktomraade: Bopliktomraade, gyldigFra: LocalDate, endringstype: Endringstype): Bopliktomraade {
        val nyttBopliktomraade = kopierOgSaveBopliktomraade(bopliktomraade, gyldigFra, endringstype)

        val kommune = bopliktomraade.getKommune(gyldigFra) ?: throw ResponseException.NotFoundException(title = "Kommune ikke funnet",
            msg = "Fant ikke kommune for bopliktområde med id ${bopliktomraade.id} og gyldig fra $gyldigFra")
        kommune.addBopliktomraade(nyttBopliktomraade)
        return nyttBopliktomraade
    }

    fun validerBopliktomraadeForDelingFinnesOgLagPairMedBopliktomraadeOgOensketVersjon(kretsDelingEndringRequest: KretsDelingEndringRequest, gyldigFra: LocalDate)
        : Pair<Bopliktomraade, Int> {
        val bopliktomraadeForSjekkeOptimistiskLaas =
            Identifikator(lokalid = Lokalid(kretsDelingEndringRequest.opprinneligKrets.lokalId), gyldighetsdato = gyldigFra)
                .let {
                    hentBopliktomraade(it) ?: throw ResponseException.NotFoundException(title = "Bopliktomraade ikke funnet",
                        msg = "Fant ikke bopliktomraade for deling med lokalid: ${it.lokalid.value} gyldig på $gyldigFra")
                }.let { Pair(it, kretsDelingEndringRequest.opprinneligKrets.version) }
        return bopliktomraadeForSjekkeOptimistiskLaas
    }

    fun delBopliktomraade(
        kretsDelingEndringRequest: UnikKretsDelingEndring, delingsDato: LocalDate, endringstype: Endringstype
    ): Bopliktomraade {
        if (kretsDelingEndringRequest.flatetype != Flatetype.BOPLIKTOMRAADE) {
            throw ResponseException.BadRequestException("Kan ikke dele og opprette en ny ${kretsDelingEndringRequest.flatetype} fra en bopliktområde")
        }
        val identifikator = Identifikator(Lokalid(kretsDelingEndringRequest.opprinneligKrets.lokalId), delingsDato)
        val bopliktomraade = hentBopliktomraade(identifikator)
            ?: throw ResponseException.NotFoundException("Fant ikke bopliktområde med lokalid: ${kretsDelingEndringRequest.opprinneligKrets.lokalId} gyldig på $delingsDato")

        val geometryFactory = GeometryFactory()

        val nyBopliktomraade = Bopliktomraade()
            .applyMappingFromExistingBopliktomraade(bopliktomraade)
            .apply {
                navn = kretsDelingEndringRequest.kretsNavn
                nummer = kretsDelingEndringRequest.kretsNummer
                kommuneLokalid = bopliktomraade.kommuneLokalid
                utledetFra = bopliktomraade
                omraade = MultiPolygon(arrayOfNulls(0), geometryFactory)
                posisjon = geometryFactory.createPoint()
                setGyldigFra(delingsDato)
            }

        return bopliktomraadeRepository.save(nyBopliktomraade)
    }

    private fun kopierOgSaveBopliktomraade(bopliktomraade: Bopliktomraade, gyldigFra: LocalDate, endringstype: Endringstype): Bopliktomraade {
        val nyttBopliktomraade = bopliktomraadeRepository.save(
            Bopliktomraade(lokalid = bopliktomraade.lokalid)
                .applyMappingFromExistingBopliktomraade(bopliktomraade)
                .apply {
                    setGyldigFra(gyldigFra)
                    bopliktomraade.gyldighet.gyldigTil?.let { setGyldigTil(it) }
                    version = nesteVersjonForBopliktomraade(Lokalid(bopliktomraade.lokalid))
                    auditInfo.typeEndring = endringstype
                }
        )

        return nyttBopliktomraade
            .also { bopliktomraade.setGyldigTil(gyldigFra) }
    }

    private fun nesteVersjonForBopliktomraade(lokalId: Lokalid): Int =
        bopliktomraadeRepository
            .findAllByLokalid(lokalId.value)
            .maxBy { it.version }
            .version + 1

}

fun Bopliktomraade.applyChangesFromRequest(request: BopliktomraadeRequest): Bopliktomraade =
    this.apply {
        this.navn = request.navn
        this.nummer = request.nummer
        this.delvisBoplikt = request.delvisBoplikt
        this.forskriftsreferanse = request.forskriftsreferanse
        this.url = request.url
        informasjon = request.informasjon
    }

