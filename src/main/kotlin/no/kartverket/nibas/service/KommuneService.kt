package no.kartverket.nibas.service

import no.kartverket.nibas.api.v1.common.Flatetype
import no.kartverket.nibas.api.v1.request.KommuneRequest
import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.common.ResponseException
import no.kartverket.nibas.common.UgyldigTilstandException
import no.kartverket.nibas.domain.Administrativenhetnavn
import no.kartverket.nibas.domain.Endringstype
import no.kartverket.nibas.domain.Grunnkrets
import no.kartverket.nibas.domain.GrunnkretsGrenseKobling
import no.kartverket.nibas.domain.Kommune
import no.kartverket.nibas.domain.Stemmekrets
import no.kartverket.nibas.domain.StemmekretsGrenseKobling
import no.kartverket.nibas.domain.UnikLokalid
import no.kartverket.nibas.kodelister.KodelisteService
import no.kartverket.nibas.repository.AdministrativenhetnavnRepository
import no.kartverket.nibas.repository.FylkeRepository
import no.kartverket.nibas.repository.GrunnkretsGrenseKoblingRepository
import no.kartverket.nibas.repository.KommuneRepository
import no.kartverket.nibas.repository.StemmekretsGrenseKoblingRepository
import no.kartverket.nibas.repository.UnikLokalidRepository
import no.kartverket.nibas.repository.findAllGyldigPaaDato
import no.kartverket.nibas.repository.findAntallFramtidigeVersjonerForIdentifikator
import no.kartverket.nibas.repository.findGyldigFylkeForFylkesnummer
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
    private var unikLokalidRepository: UnikLokalidRepository,
    private val kodelisteService: KodelisteService,
    private val administrativenhetnavnService: AdministrativenhetnavnService,
    private val grunnkretsGrenseKoblingRepository: GrunnkretsGrenseKoblingRepository,
    private val stemmekretsGrenseKoblingRepository: StemmekretsGrenseKoblingRepository
) {

    fun hentAlleKommuner(gyldighetsdato: LocalDate = LocalDate.now()): List<Kommune> = kommuneRepository.findAllGyldigPaaDato(gyldighetsdato)
    fun hentKommune(identifikator: Identifikator): Kommune? = kommuneRepository.findGyldigObjektForIdentifikator(identifikator)

    fun hentKommuneByKommunenummer(kommunenummer: String, gyldighetsdato: LocalDate): Kommune? {
        return kommuneRepository.findGyldigKommuneForKommunenummer(kommunenummer, gyldighetsdato)
    }

    fun hentKommunerForFylke(fylkeIdentifikator: Identifikator): List<Kommune> =
        fylkeRepository.findGyldigObjektForIdentifikator(fylkeIdentifikator)?.getKommuner(fylkeIdentifikator.gyldighetsdato) ?: emptyList()

    fun hentGrensekoblingerForKommune(identifikator: Identifikator): Set<GrunnkretsGrenseKobling> {
        return hentKommune(identifikator)?.getGrensekoblinger(identifikator.gyldighetsdato) ?: emptySet()
    }

    fun hentGrensekoblingerForAlleGrunnkretserIKommune(identifikator: Identifikator): Set<GrunnkretsGrenseKobling> {
        return hentKommune(identifikator)
            ?.let { grunnkretsGrenseKoblingRepository.findByGrunnkretsKommuneLokalid(it.kommuneLokalid) }
            ?.filter { it.grense.gyldigPaaDato(identifikator.gyldighetsdato) && it.grunnkrets.gyldigPaaDato(identifikator.gyldighetsdato) }
            ?.distinctBy { it.grense.lokalid }
            ?.toSet() ?: emptySet()
    }

    fun hentGrensekoblingerForAlleStemmekretserIKommune(identifikator: Identifikator): Set<StemmekretsGrenseKobling> {
        return hentKommune(identifikator)
            ?.let { stemmekretsGrenseKoblingRepository.findByStemmekretsKommuneLokalid(it.kommuneLokalid) }
            ?.filter { it.grense.gyldigPaaDato(identifikator.gyldighetsdato) && it.stemmekrets.gyldigPaaDato(identifikator.gyldighetsdato) }
            ?.distinctBy { it.grense.lokalid }
            ?.toSet() ?: emptySet()
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

        verifiserOptimistiskLaas(oppdatertKommune.version, gammelKommune)?.throwException()

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
                .applyMappingFromExistingKommune(kommune)
                .apply {
                    kommune.gyldighet.gyldigTil?.let { setGyldigTil(it) }
                    setGyldigFra(gyldigFra)
                    version = nesteVersjonForKommune(Lokalid(kommune.lokalid))
                    auditInfo.typeEndring = endringstype
                }
        )
        return nyKommune
            .applyCascadingOperationsBasedOnExistingKommune(kommune)
            .also { kommune.setGyldigTil(gyldigFra)  }
    }

    fun createNyKommuneMedNyUnikLokalid(
        kopierFraKommune: Kommune,
        nyttKommunenummer: String,
        admenhetnavnList: List<Administrativenhetnavn> = emptyList(),
        gyldigFra: LocalDate,
        endringstype: Endringstype
    ): Kommune {
        var nyKommune = Kommune()
            .applyMappingFromExistingKommune(kopierFraKommune)
            .apply {
                setGyldigFra(gyldigFra)
                kommunenummer = nyttKommunenummer
                kommunenummerUuid = kodelisteService.getKommunenummerIdByCodeValue(nyttKommunenummer)
                version = 1
                auditInfo.typeEndring = endringstype
                utledetFra = kopierFraKommune
            }

        var unikLokalid = UnikLokalid(nyKommune.lokalid, Flatetype.KOMMUNE)
        unikLokalid = unikLokalidRepository.save(unikLokalid)

        nyKommune = kommuneRepository.save(nyKommune)
        nyKommune.addKommuneTilUnikLokalid(unikLokalid)

        // Fylkesnummer er de to første sifrene i kommunenummeret
        val fylkesnummer = nyttKommunenummer.substring(0, 2)
        val fylke = fylkeRepository.findGyldigFylkeForFylkesnummer(fylkesnummer, gyldigFra)
            ?: throw RuntimeException("Fant ikke fylke med fylkesnummer $fylkesnummer og gyldig fra $gyldigFra")

        fylke.addKommune(nyKommune)

        administrativenhetnavnService.createNyeAdmEnhetNavnOgKnyttTilEnhet(
            admenhetnavnList = admenhetnavnList.ifEmpty { kopierFraKommune.getAdministrativenhetNavn(gyldigFra) },
            gyldigFra = gyldigFra,
            endringstype = endringstype,
            admEnhet = nyKommune
        )

        nyKommune.addInformasjonselementerFrom(kopierFraKommune.informasjonselementer)
        return nyKommune
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

    @Transactional
    fun settKommuneSomHistorisk(kommune: Kommune, settHistoriskFraDato: LocalDate) {
        val grunnkretser = kommune.getGrunnkretser(settHistoriskFraDato).map {
            "$it.grunnkretsnummer: ${it.grunnkretsnavn}"
        }
        val stemmekretser = kommune.getStemmekretser(settHistoriskFraDato).map {
            "${it.stemmekretsnummer}: ${it.stemmekretsnavn}"
        }

        if(grunnkretser.isNotEmpty() ){
            throw UgyldigTilstandException("Kan ikke sette kommunen med lokalId: ${kommune.lokalid} til historisk da den har grunnkretser som er gyldige etter $settHistoriskFraDato. Gjelder grunnkretser: $grunnkretser")
        }
        if(stemmekretser.isNotEmpty() ){
            throw UgyldigTilstandException("Kan ikke sette kommunen med lokalId: ${kommune.lokalid} til historisk da den har stemmekretser som er gyldige etter $settHistoriskFraDato. Gjelder stemmekretser: $stemmekretser")
        }

        kommune.getAdministrativenhetNavn().forEach { it.setGyldigTil(settHistoriskFraDato) }
        kommune.setGyldigTil(settHistoriskFraDato)
    }

    private fun hentKommuneFraNumeriskNummer(numeriskKommunenummer: String, gyldighetsdato: LocalDate): Kommune {
        return hentKommuneByKommunenummer(numeriskKommunenummer, gyldighetsdato)
            ?: throw ResponseException.NotFoundException("Fant ikke kommune med nummer: $numeriskKommunenummer")
    }
}

fun Kommune.applyChangesFromRequest(kommuneRequest: KommuneRequest): Kommune =
    this.apply {
        kommuneRequest.samiskforvaltningsomraade.let { this.samiskforvaltningsomraade = it }
    }

