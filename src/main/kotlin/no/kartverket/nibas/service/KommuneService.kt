package no.kartverket.nibas.service

import no.kartverket.nibas.api.v1.common.AdministrativEnhetNavn
import no.kartverket.nibas.api.v1.request.KommuneRequest
import no.kartverket.nibas.api.v1.response.KommuneRef
import no.kartverket.nibas.api.v1.response.toObjektIdentifikator
import no.kartverket.nibas.common.HrefProvider
import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.common.ResponseException
import no.kartverket.nibas.domain.Endringstype
import no.kartverket.nibas.domain.GrenseKobling
import no.kartverket.nibas.domain.Grunnkrets
import no.kartverket.nibas.domain.Kommune
import no.kartverket.nibas.domain.Stemmekrets
import no.kartverket.nibas.kodelister.KodelisteService
import no.kartverket.nibas.kodelister.createKommunenummerFromUUID
import no.kartverket.nibas.repository.AdministrativenhetnavnRepository
import no.kartverket.nibas.repository.FylkeRepository
import no.kartverket.nibas.repository.KommuneRepository
import no.kartverket.nibas.repository.OptimistiskLaasException
import no.kartverket.nibas.repository.findAllGyldigPaaDato
import no.kartverket.nibas.repository.findAntallFramtidigeVersjonerForIdentifikator
import no.kartverket.nibas.repository.findGyldigKommuneForKommunenummer
import no.kartverket.nibas.repository.findGyldigObjektForIdentifikator
import no.kartverket.nibas.repository.verifiserOptimistiskLaas
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Transactional(readOnly = true)
@Service
class KommuneService(
    private val kommuneRepository: KommuneRepository,
    private val fylkeRepository: FylkeRepository,
    @Suppress("unused") //Er kun ubrukt i påvente av TS-1192
    private val administrativenhetnavnRepository: AdministrativenhetnavnRepository,
    private val kodelisteService: KodelisteService,
    private val hrefProvider: HrefProvider
) {

    fun hentAlleKommuner(gyldighetsdato: LocalDate = LocalDate.now()): List<Kommune> = kommuneRepository.findAllGyldigPaaDato(gyldighetsdato)
    fun hentKommune(identifikator: Identifikator): Kommune? = kommuneRepository.findGyldigObjektForIdentifikator(identifikator)

    fun hentKommuneByKommunenummer(kommunenummerUuid: String, gyldighetsdato: LocalDate): Kommune? {
        return kommuneRepository.findGyldigKommuneForKommunenummer(kommunenummerUuid, gyldighetsdato)
    }

    fun hentKommunerForFylke(fylkeIdentifikator: Identifikator): List<Kommune> =
        fylkeRepository.findGyldigObjektForIdentifikator(fylkeIdentifikator)?.getKommuner(fylkeIdentifikator.gyldighetsdato) ?: emptyList()

    fun hentGrenserForKommune(identifikator: Identifikator): Set<GrenseKobling> {
        return hentKommune(identifikator)?.getGrenser(identifikator.gyldighetsdato) ?: emptySet()
    }

    @Transactional(readOnly = false)
    fun oppdaterKommune(
        lokalid: Lokalid,
        gyldigFra: LocalDate,
        endringstype: Endringstype,
        oppdatertKommune: KommuneRequest
    ): Kommune? {

        val identifikator = Identifikator(lokalid, gyldigFra)
        val gammelKommune = hentKommune(identifikator)
            ?: throw ResponseException.NotFoundException("Kommune med $identifikator ble ikke funnet.")

        verifiserOptimistiskLaas(oppdatertKommune.version, gammelKommune)?.let {
            throw OptimistiskLaasException(it)
        }

        val nyKommune = createNyVersjonAvKommune(kommune = gammelKommune, gyldigFra = gyldigFra, endringstype = endringstype)
            .applyChangesFromRequest(oppdatertKommune)

        return nyKommune

        //Kommentert vekk det med adm.enhetnavn, siden klienten pr.nå ikke oppdaterer navn. Lot det ligge pga TS-1192
//            // Finner basert på språkkode, og oppdaterer den
//            oppdatertKommune.administrativenhetnavn.forEach { navn ->
//                gammel.kommuneLokalid.administrativenhetnavn.first { it.spraak?.id == navn.spraak }.let { gammeltNavn ->
//                    verifiserOptimistiskLaas(navn.version, gammeltNavn)?.let { throw OptimistiskLaasException(it) }
//                    gammeltNavn.navn = navn.navn
//                    administrativenhetnavnRepository.save(gammeltNavn)
//                }
//            }
    }

    fun createNyVersjonAvKommune(kommune: Kommune, gyldigFra: LocalDate, endringstype: Endringstype): Kommune {
        val nyKommune = kommuneRepository.save(
            Kommune(lokalid = kommune.lokalid)
                .apply {
                    applyMappingFromExistingKommune(kommune)
                    setGyldigFra(gyldigFra)
                    version = nesteVersjonForKommune(Lokalid(kommune.lokalid))
                    auditInfo.typeEndring = endringstype
                }
        )
        return nyKommune
            .applyCascadingOperationsBasedOnExistingKommune(kommune)
            .also { kommune.setGyldigTil(gyldigFra)  }
    }

    private fun nesteVersjonForKommune(lokalId: Lokalid): Int =
        kommuneRepository
            .findAllByLokalid(lokalId.value)
            .maxBy { it.version }
            .version + 1

    fun hentKommunesGrunnkretser(kommuneIdentifikator: Identifikator): List<Grunnkrets> {
        return hentKommune(kommuneIdentifikator)?.getGrunnkretser(kommuneIdentifikator.gyldighetsdato) ?: emptyList()
    }

    fun hentKommunesStemmekretser(kommuneIdentifikator: Identifikator): List<Stemmekrets> {
        return hentKommune(kommuneIdentifikator)?.getStemmekretser(kommuneIdentifikator.gyldighetsdato) ?: emptyList()
    }

    fun hentAlleGrunnkretserForKommune(kommunenummer: String, gyldighetsdato: LocalDate): List<Grunnkrets> {
        return hentKommuneFraNumeriskNummer(kommunenummer, gyldighetsdato).getGrunnkretser(gyldighetsdato)
            .sortedBy { it.id }
    }

    fun hentAlleStemmekretserForKommune(numeriskKommunenummer: String, gyldighetsdato: LocalDate): List<Stemmekrets> {
        return hentKommuneFraNumeriskNummer(numeriskKommunenummer, gyldighetsdato).getStemmekretser(gyldighetsdato)
            .sortedBy { it.id }
    }

    fun hentAntallGyldigeVersjonerFramITidFor(identifikator: Identifikator): Int = kommuneRepository.findAntallFramtidigeVersjonerForIdentifikator(identifikator)

    fun kommuneToKommuneRef(kommune: Kommune, datoToUse: LocalDate): KommuneRef {
        val identifikator = Identifikator(Lokalid(kommune.lokalid), datoToUse)
        val antallFramtidigeVersjoner = hentAntallGyldigeVersjonerFramITidFor(identifikator)

        return KommuneRef(
            id = identifikator.toObjektIdentifikator(),
            navn = kommune.getAdministrativenhetNavn(identifikator.gyldighetsdato)
                .map { AdministrativEnhetNavn(it.navn!!, it.spraak?.id!!, it.version, it.rekkefoelge) },
            href = hrefProvider.getHref(identifikator),
            kommunenummer = kodelisteService.createKommunenummerFromUUID(kommune.kommunenummer),
            antallFramtidigeVersjoner = antallFramtidigeVersjoner
        )
    }

    private fun hentKommuneFraNumeriskNummer(numeriskKommunenummer: String, gyldighetsdato: LocalDate): Kommune {
        try {
            val kommunenummerUUID = kodelisteService.getKommunenummerIdByCodeValue(numeriskKommunenummer)
            return hentKommuneByKommunenummer(kommunenummerUUID, gyldighetsdato)
                ?: throw ResponseException.NotFoundException("Fant ikke kommune med nummer: $numeriskKommunenummer")
        } catch (ex: NoSuchElementException) {
            throw ResponseException.NotFoundException("Fant ikke kommunenummer-id for $numeriskKommunenummer")
        }
    }
}

fun Kommune.applyChangesFromRequest(kommuneRequest: KommuneRequest): Kommune =
    this.apply {
        kommuneRequest.samiskforvaltningsomraade.let { this.samiskforvaltningsomraade = it }
    }

