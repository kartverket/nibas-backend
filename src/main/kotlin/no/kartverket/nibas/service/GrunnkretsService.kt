package no.kartverket.nibas.service

import no.kartverket.nibas.api.v1.common.Flatetype
import no.kartverket.nibas.api.v1.common.UnikKretsDelingEndring
import no.kartverket.nibas.api.v1.request.GrunnkretsRequest
import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.common.ResponseException
import no.kartverket.nibas.common.ResponseException.NotFoundException
import no.kartverket.nibas.common.UgyldigTilstandException
import no.kartverket.nibas.common.createIdentifikator
import no.kartverket.nibas.common.getInSpraak
import no.kartverket.nibas.common.toNorskFormat
import no.kartverket.nibas.domain.Endringstype
import no.kartverket.nibas.domain.Grunnkrets
import no.kartverket.nibas.domain.GrunnkretsGrenseKobling
import no.kartverket.nibas.domain.Kommune
import no.kartverket.nibas.repository.GrunnkretsRepository
import no.kartverket.nibas.repository.findAllByLokalidSortedByGyldigFra
import no.kartverket.nibas.repository.findAllFramtidigeVersjonerByIdentifikator
import no.kartverket.nibas.repository.findAntallFramtidigeVersjonerForIdentifikator
import no.kartverket.nibas.repository.findGyldigObjektForIdentifikator
import no.kartverket.nibas.repository.findGyldigObjektPaaGyldighetsdato
import no.kartverket.nibas.repository.verifiserOptimistiskLaas
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.MultiPolygon
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Transactional
@Service
class GrunnkretsService(
    private val grunnkretsRepository: GrunnkretsRepository,
) {

    @Transactional(readOnly = true)
    fun hentGrunnkrets(identifikator: Identifikator): Grunnkrets? = grunnkretsRepository.findGyldigObjektForIdentifikator(identifikator)

    @Transactional(readOnly = true)
    fun hentGrensekoblingerForGrunnkrets(identifikator: Identifikator): List<GrunnkretsGrenseKobling> {
        return hentGrunnkrets(identifikator)?.getGrensekoblinger(identifikator.gyldighetsdato)?.toList() ?: emptyList()
    }

    @Transactional(readOnly = true)
    fun hentGrunnkretsForGrunnkretsNummer(grunnkretsNummer: String, gyldighetsdato: LocalDate): Grunnkrets? {
        return findGyldigObjektPaaGyldighetsdato(grunnkretsRepository.findAllByGrunnkretsnummer(grunnkretsNummer))
    }

    @Transactional(readOnly = true)
    fun hentGrunnkretserForIds(ids: Set<String>, gyldighetsdato: LocalDate): List<Grunnkrets> {
        return ids
            .map { createIdentifikator(it, gyldighetsdato) }
            .map { hentGrunnkrets(it) ?: throw NotFoundException("Grunnkrets med $it ble ikke funnet.") }
            .sortedBy { it.id }
    }

    fun hentAlleVersjonerMedGyldigFraEtterGittDato(lokalId: Lokalid, dato: LocalDate): List<Grunnkrets> =
        grunnkretsRepository.findAllByLokalidSortedByGyldigFra(lokalId)
            .filter { it.gyldighet.gyldigFra.isAfter(dato) }

    fun finnesAnnenGrunnkretsMedSammeGrunnkretsnummer(grunnkrets: Grunnkrets): Boolean {
        //Returnerer false hvis grunnkretsnummer ikke er gitt, isåfall vil det komme valideringsfeil fra et annet sted
        grunnkrets.grunnkretsnummer ?: return false

        val andreKretserMedSammeGrunnkretsnummer =
            grunnkretsRepository.findAllByGrunnkretsnummerAndLokalidIsNot(grunnkrets.grunnkretsnummer!!, grunnkrets.lokalid)

        if (andreKretserMedSammeGrunnkretsnummer.isEmpty()) {
            return false
        }
        //Sjekk om noen av de funnede grunnkretsene har sammenfallende gyldighet med grunnkretsen som skal valideres
        return andreKretserMedSammeGrunnkretsnummer.any { funnet -> funnet.gyldighet.overlapperMed(grunnkrets.gyldighet) }
    }

    fun oppdaterGrunnkrets(
        lokalId: Lokalid,
        gyldigFra: LocalDate,
        endringstype: Endringstype,
        oppdatertGrunnkrets: GrunnkretsRequest
    ): Grunnkrets {
        val identifikator = Identifikator(lokalId, gyldigFra)
        val gammelGrunnkrets = hentGrunnkrets(identifikator)
            ?: throw NotFoundException("Grunnkrets med $identifikator ble ikke funnet.")

        verifiserOptimistiskLaas(oppdatertGrunnkrets.version, gammelGrunnkrets)?.throwException()

        //Lar dette med GrunnkretsHelper ligge, det har noe å gjøre med evt flere fremtidige endringer på grunnkrets
        val grunnkretsHelper = GrunnkretsHelper(grunnkretsRepository.findAllByLokalidSortedByGyldigFra(lokalId))

        // Må bruke grunnkrets som matcher lokalId på gyldigFra-tidspunkt
        val relevantGrunnkrets = grunnkretsHelper.findRelevantGrunnkretsForGyldigFra(gyldigFra, lokalId)

        // Uansett om gyldigFra er identisk med grunnkretsens gyldigFra eller fram i tid: Lage ny
        // rad (ny uuid, samme lokalid).
        val nyGrunnkrets = createNyVersjonAvGrunnkrets(gammelGrunnkrets, gyldigFra, endringstype)
            .applyChangesFromRequest(oppdatertGrunnkrets)

        if (finnesAnnenGrunnkretsMedSammeGrunnkretsnummer(nyGrunnkrets)) {
            throw createDuplikatGrunnkretsnummerException(nyGrunnkrets)
        }

        return nyGrunnkrets
            .apply {
                if (grunnkretsHelper.existsRowsWithLaterGyldigFraDato(relevantGrunnkrets)) {
                    // Setter gyldigTil lik gyldigFra for neste rad
                    setGyldigTil(grunnkretsHelper.getNesteGyldigFraDato(relevantGrunnkrets))
                }
            }
    }

    private fun createDuplikatGrunnkretsnummerException(grunnkrets: Grunnkrets): ResponseException.BadRequestException {
        val kommune = grunnkrets.getKommune(grunnkrets.gyldighet.gyldigFra)
        val kommunenavn = kommune?.getAdministrativenhetNavn(grunnkrets.gyldighet.gyldigFra)?.getInSpraak("nor") ?: "UKJENT KOMMUNE"
        val kommunenummer = kommune?.kommunenummer
        val gyldigFraTekst = grunnkrets.gyldighet.gyldigFra.toNorskFormat()
        val gyldigTilTekst = grunnkrets.gyldighet.gyldigTil?.let { "til ${it.toNorskFormat()}" } ?: "og fremover"

        return ResponseException.BadRequestException(
            title = "Duplikate kretsnummer innenfor samme kommune",
            msg = "Kretsnummer ${grunnkrets.grunnkretsnummer} er brukt på flere grunnkretser innenfor kommunen (${kommunenummer} ${kommunenavn}) " +
                "med gyldighet som overlapper med $gyldigFraTekst $gyldigTilTekst. " +
                "Vennligst gi grunnkretsene unike kretsnumre før du fortsetter."
        )
    }

    fun createNyVersjonAvGrunnkrets(grunnkrets: Grunnkrets, gyldigFra: LocalDate, endringstype: Endringstype): Grunnkrets {
        return createNyVersjonAvGrunnkretsUtenGrenser(grunnkrets, gyldigFra, endringstype)
            .copyGrenserFromExistingGrunnkrets(grunnkrets, gyldigFra)
    }

    fun createNyVersjonAvGrunnkretsUtenGrenser(grunnkrets: Grunnkrets, gyldigFra: LocalDate, endringstype: Endringstype): Grunnkrets {
        val nyGrunnkrets = kopierOgSaveGrunnkrets(grunnkrets, gyldigFra, endringstype)
        // Må legge til denne Grunnkrets på eksisterende Grunnkrets sin kommune
        val kommune = grunnkrets.getKommune(gyldigFra) ?: throw NotFoundException(title = "Kommune ikke funnet",
            msg = "Fant ikke kommune for grunnkrets med id ${grunnkrets.id}, grunnkretsnummer ${grunnkrets.grunnkretsnummer} og gyldig fra $gyldigFra")
        kommune.addGrunnkrets(nyGrunnkrets)
        return nyGrunnkrets
            .copyInformasjonselementerFromExistingGrunnkrets(grunnkrets, gyldigFra)
    }

    fun delGrunnkrets(
        kretsDelingEndringRequest: UnikKretsDelingEndring, delingsDato: LocalDate, endringstype: Endringstype
    ): Grunnkrets {
        if (kretsDelingEndringRequest.flatetype != Flatetype.GRUNNKRETS ){
            throw ResponseException.BadRequestException("Kan ikke dele og opprette en ny ${kretsDelingEndringRequest.flatetype} fra en grunnkrets")
        }
        val identifikator = Identifikator(Lokalid(kretsDelingEndringRequest.opprinneligKrets.lokalId), delingsDato)
        val grunnkrets = hentGrunnkrets(identifikator)
            ?: throw NotFoundException("Fant ikke grunnkrets med lokalid: ${kretsDelingEndringRequest.opprinneligKrets.lokalId} gylidg på $delingsDato")

        verifiserOptimistiskLaas(kretsDelingEndringRequest.opprinneligKrets.version, grunnkrets)?.throwException()

        val geometryFactory = GeometryFactory()

        val nyGrunnkrets = Grunnkrets().apply {
            grunnkretsnavn = kretsDelingEndringRequest.kretsNavn
            grunnkretsnummer = kretsDelingEndringRequest.kretsNummer
            kommuneLokalid = grunnkrets.kommuneLokalid
            utledetFra = grunnkrets
            omraade = MultiPolygon(arrayOfNulls(0), geometryFactory)
            posisjon = geometryFactory.createPoint()
            setGyldigFra(delingsDato)
        }

        if (finnesAnnenGrunnkretsMedSammeGrunnkretsnummer(nyGrunnkrets)) {
            throw createDuplikatGrunnkretsnummerException(nyGrunnkrets)
        } else {
            return grunnkretsRepository.save(nyGrunnkrets)
        }
    }

    fun createNyVersjonAvGrunnkretsIAnnenKommune(grunnkrets: Grunnkrets, kommune: Kommune, nyttGrunnkretsNummer: String,
                                                 gyldigFra: LocalDate, endringstype: Endringstype): Grunnkrets {
        //Antar at kommune sitt kommunenummer stemmer med angitt nytt grunnkretsnummer, for å slippe å søke opp kommunen for hver grunnkrets som skal flyttes
        val nyGrunnkrets = kopierOgSaveGrunnkrets(grunnkrets, gyldigFra, endringstype)
            .copyInformasjonselementerFromExistingGrunnkrets(grunnkrets, gyldigFra)
            .copyGrenserFromExistingGrunnkrets(grunnkrets, gyldigFra)
        nyGrunnkrets.grunnkretsnummer = nyttGrunnkretsNummer
        kommune.addGrunnkrets(nyGrunnkrets)
        return nyGrunnkrets
    }

    private fun kopierOgSaveGrunnkrets(grunnkrets: Grunnkrets, gyldigFra: LocalDate, endringstype: Endringstype): Grunnkrets {
        val nyGrunnkrets = grunnkretsRepository.save(
            Grunnkrets(lokalid = grunnkrets.lokalid)
                .applyMappingFromExistingGrunnkrets(grunnkrets)
                .apply {
                    setGyldigFra(gyldigFra)
                    grunnkrets.gyldighet.gyldigTil?.let { setGyldigTil(it) }
                    version = nesteVersjonForGrunnkrets(Lokalid(grunnkrets.lokalid))
                    auditInfo.typeEndring = endringstype
                }
        )
        return nyGrunnkrets.also { grunnkrets.setGyldigTil(gyldigFra) }
    }

    private fun nesteVersjonForGrunnkrets(lokalId: Lokalid): Int =
        grunnkretsRepository
            .findAllByLokalid(lokalId.value)
            .maxBy { it.version }
            .version + 1

    /**
     * Returnerer liste med grunnkretser som matcher lokalid og gyldighetsdato.
     * Nåværende gyldig rad og fremtidig publiserte gyldige rader.
     *
     * -Første innslag er raden som er gyldig på gyldighetsdato, default dagens dato.
     * -Så følger de framtidige gyldige publiserte versjonene sortert på gyldigFra.
     */
    fun hentFramtidigeVersjonerForGrunnkrets(identifikator: Identifikator): List<Grunnkrets> {
        val gyldigGrunnkretsForDagensDato = grunnkretsRepository.findGyldigObjektForIdentifikator(identifikator)
            ?: throw UgyldigTilstandException("Det finnes ingen gyldig grunnkrets for identifikator $identifikator")
        return listOf(gyldigGrunnkretsForDagensDato)
            .plus(grunnkretsRepository.findAllFramtidigeVersjonerByIdentifikator(identifikator))
    }

    fun hentAntallGyldigeVersjonerFramITidFor(identifikator: Identifikator): Int = grunnkretsRepository.findAntallFramtidigeVersjonerForIdentifikator(identifikator)

}

// Setter feltene som faktisk kan endres i request
private fun Grunnkrets.applyChangesFromRequest(request: GrunnkretsRequest): Grunnkrets {
    return this.apply {
        grunnkretsnavn = request.navn
        grunnkretsnummer = request.grunnkretsnummer
    }
}

private class GrunnkretsHelper(private val grunnkretserSortedByGyldigFra: List<Grunnkrets>) {

    fun findRelevantGrunnkretsForGyldigFra(utkastetsGyldigFra: LocalDate, id: Lokalid): Grunnkrets =
        grunnkretserSortedByGyldigFra.find { grunnkrets -> grunnkrets.gyldigPaaDato(utkastetsGyldigFra) }
            ?: throw NotFoundException("Fant ikke eksisterende grunnkrets for lokalid $id og gyldigFra $utkastetsGyldigFra.")

    fun existsRowsWithLaterGyldigFraDato(grunnkrets: Grunnkrets): Boolean {
        return findIndex(grunnkrets) < grunnkretserSortedByGyldigFra.lastIndex
    }

    fun getNesteGyldigFraDato(grunnkrets: Grunnkrets): LocalDate {
        return grunnkretserSortedByGyldigFra[findIndex(grunnkrets) + 1].gyldighet.gyldigFra
    }

    private fun findIndex(grunnkrets: Grunnkrets) =
        grunnkretserSortedByGyldigFra.indices.find { grunnkretserSortedByGyldigFra[it].id == grunnkrets.id }
            ?: throw RuntimeException("Did not find index for grunnkrets $grunnkrets")
}
