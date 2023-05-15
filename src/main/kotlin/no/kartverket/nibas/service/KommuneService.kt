package no.kartverket.nibas.service

import no.kartverket.nibas.api.v1.common.AdministrativEnhetNavn
import no.kartverket.nibas.api.v1.request.KommuneRequest
import no.kartverket.nibas.api.v1.response.KommuneRef
import no.kartverket.nibas.api.v1.response.toObjektIdentifikator
import no.kartverket.nibas.common.HrefProvider
import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.common.ResponseException
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
    fun oppdaterKommune(lokalid: Lokalid, oppdatertKommune: KommuneRequest): Kommune? {
        return hentKommune(Identifikator(lokalid))?.let { gammel ->
            verifiserOptimistiskLaas(oppdatertKommune.version, gammel)?.let { throw OptimistiskLaasException(it) }

            gammel.samiskforvaltningsomraade = oppdatertKommune.samiskforvaltningsomraade
            gammel.kommunenummer = oppdatertKommune.kommunenummerId

            // Finner basert på språkkode, og oppdaterer den
            oppdatertKommune.administrativenhetnavn.forEach { navn ->
                gammel.administrativenhetnavn.first { it.spraak?.id == navn.spraak }.let { gammeltNavn ->
                    verifiserOptimistiskLaas(navn.version, gammeltNavn)?.let { throw OptimistiskLaasException(it) }
                    gammeltNavn.navn = navn.navn
                    administrativenhetnavnRepository.save(gammeltNavn)
                }
            }

            kommuneRepository.save(gammel)
        }
    }

    fun hentKommunesGrunnkretser(kommuneIdentifikator: Identifikator): List<Grunnkrets> {
        return hentKommune(kommuneIdentifikator)?.getGrunnkretser(kommuneIdentifikator.gyldighetsdato) ?: emptyList()
    }

    fun hentKommunesStemmekretser(kommuneIdentifikator: Identifikator): List<Stemmekrets> {
        return hentKommune(kommuneIdentifikator)?.getStemmekretser(kommuneIdentifikator.gyldighetsdato) ?: emptyList()
    }

    fun hentAlleGrunnkretserForKommune(kommunenummer: String, gyldighetsdato: LocalDate): List<Grunnkrets> {
        return hentKommuneFraNumeriskNummer(kommunenummer, gyldighetsdato).grunnkretser
            .filter { it.gyldigPaaDato(LocalDate.now()) }
            .sortedBy { it.id }
    }

    fun hentAlleStemmekretserForKommune(numeriskKommunenummer: String, gyldighetsdato: LocalDate): List<Stemmekrets> {
        return hentKommuneFraNumeriskNummer(numeriskKommunenummer, gyldighetsdato).stemmekretser
            .filter { it.gyldigPaaDato(gyldighetsdato) }
            .sortedBy { it.id }
    }

    fun hentAntallGyldigeVersjonerFramITidFor(identifikator: Identifikator): Int = kommuneRepository.findAntallFramtidigeVersjonerForIdentifikator(identifikator)

    fun kommuneToKommuneRef(kommune: Kommune, datoToUse: LocalDate): KommuneRef {
        val kommuneIdentifikator = Identifikator(Lokalid(kommune.lokalid), datoToUse)
        val antallFramtidigeVersjoner = hentAntallGyldigeVersjonerFramITidFor(kommuneIdentifikator)

        return KommuneRef(
            id = kommuneIdentifikator.toObjektIdentifikator(),
            navn = kommune.administrativenhetnavn.map { AdministrativEnhetNavn(it.navn!!, it.spraak?.id!!, it.version, it.rekkefoelge) },
            href = hrefProvider.getHref(kommuneIdentifikator),
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
