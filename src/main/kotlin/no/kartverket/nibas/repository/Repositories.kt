package no.kartverket.nibas.repository

import no.kartverket.nibas.domain.Administrativenhetnavn
import no.kartverket.nibas.domain.Bopliktomraade
import no.kartverket.nibas.domain.DokrefGrenseKobling
import no.kartverket.nibas.domain.Dokumentasjonsreferanse
import no.kartverket.nibas.domain.Flate
import no.kartverket.nibas.domain.Fylke
import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.domain.Grunnkrets
import no.kartverket.nibas.domain.GrunnkretsGrenseKobling
import no.kartverket.nibas.domain.Landkode
import no.kartverket.nibas.domain.Nasjon
import no.kartverket.nibas.domain.Spraakkode
import no.kartverket.nibas.domain.Stemmekrets
import no.kartverket.nibas.domain.StemmekretsGrenseKobling
import no.kartverket.nibas.domain.UnikLokalid
import no.kartverket.nibas.domain.utkast.Utkast
import no.kartverket.nibas.domain.utkast.UtkastStatus
import no.kartverket.nibas.sporing.Laasbar
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.NoRepositoryBean
import java.time.LocalDate

@NoRepositoryBean
interface CrudRepositoryWithLokalIdFinder<T, ID> : CrudRepository<T, ID> {
    fun findAllByLokalid(lokalid: String): List<T>

    fun findLastByLokalid(lokalid: String): T? =
        findAllByLokalid(lokalid).lastOrNull()
}

@NoRepositoryBean
interface JpaRepositoryWithLokalIdFinder<T, ID> : JpaRepository<T, ID> {
    fun findAllByLokalid(lokalid: String): List<T>

    fun findLastByLokalid(lokalid: String): T? =
        findAllByLokalid(lokalid).lastOrNull()
}

interface LandKodeRepository : CrudRepository<Landkode, String>
interface DokumentasjonsreferanseRepository : JpaRepositoryWithLokalIdFinder<Dokumentasjonsreferanse, String>
interface SpraakkodeRepository : CrudRepository<Spraakkode, String>
interface AdministrativenhetnavnRepository : CrudRepository<Administrativenhetnavn, String>
interface UnikLokalidRepository : JpaRepositoryWithLokalIdFinder<UnikLokalid, String>
interface NasjonRepository : CrudRepositoryWithLokalIdFinder<Nasjon, String>
interface DokrefGrenseKoblingRepository : JpaRepository<DokrefGrenseKobling, String>
interface FylkeRepository : CrudRepositoryWithLokalIdFinder<Fylke, String> {

    fun findAllByFylkesnummer(fylkesnummer: String): List<Fylke>
}

interface GrunnkretsRepository : CrudRepositoryWithLokalIdFinder<Grunnkrets, String> {
    /**
     * Finn alle grunnkretser med gitt grunnkretsnummer hvor lokalid ikke er lik paramert lokalid.
     * Trenger ikke ha med lokalid for kommune, siden grunnkretsnummer inneholder kommunenummer
     */
    fun findAllByGrunnkretsnummerAndLokalidIsNot(grunnkretsnummer: String, lokalid: String): List<Grunnkrets>

    fun findAllByGrunnkretsnummer(grunnkretsnummer: String): List<Grunnkrets>
}

interface StemmekretsRepository : CrudRepositoryWithLokalIdFinder<Stemmekrets, String> {
    /**
     * Finn alle stemmekretser med gitt stemmekretsnummer hvor lokalid ikke er lik paramert lokalid, og med kommune med ønsket lokalid for kommune.
     * Sender inn en kolleksjon med stemmekretsnumre, for å kunne søke med en og flere ledende nuller
     * Stemmekretsnumrene 5, 05, 005 og 0005 skal tolkes som like
     */
    @Query(value = "SELECT * from stemmekrets s where s.stemmekretsnummer in (:stemmekretsnummer) and s.lokalid != (:lokalid) and s.kommunelokalid = (:kommune_lokalid) ", nativeQuery = true)
    fun findAllByStemmekretsnummerInAndLokalidIsNotAndKommuneLokalid(stemmekretsnummer: Set<String>, lokalid: String, kommune_lokalid: String): List<Stemmekrets>

    fun findAllByStemmekretsnummerAndKommuneLokalid(stemmekretsnummer: String, kommuneLokalid: UnikLokalid): List<Stemmekrets>
}
interface BopliktomraadeRepository : CrudRepositoryWithLokalIdFinder<Bopliktomraade, String>

interface GrenseRepository : JpaRepositoryWithLokalIdFinder<Grense, String> {

    /**
     * Finner alle gyldige administrative grenser på en gitt dato.
     * Returnerer kun Kommunegrense, Fylkesgrense, Riksgrense og Territorialgrense.
     * Returnerer en paginert liste basert på pageable-parameteren.
     *
     * @param gyldighetsdato Dato for gyldighet
     * @param pageable Paginering (side, størrelse, sortering)
     * @return Side med administrative grenser som er gyldige på gitt dato
     */
    @Query("SELECT g FROM Grense g WHERE " +
        "g.gyldighet.gyldigFra <= :gyldighetsdato " +
        "AND (g.gyldighet.gyldigTil > :gyldighetsdato OR g.gyldighet.gyldigTil IS NULL) " +
        "AND g.gyldighet.slettet = false " +
        "AND g.grensetype IN ('Kommunegrense', 'Fylkesgrense', 'Riksgrense', 'Territorialgrense', 'AvtaltAvgrensningslinje')")
    fun findAllGyldigeGrenser(
        gyldighetsdato: LocalDate,
        pageable: Pageable
    ): Page<Grense>
}

interface GrunnkretsGrenseKoblingRepository : JpaRepository<GrunnkretsGrenseKobling, Long> {
    fun findByGrunnkretsKommuneLokalid(kommuneLokalid: UnikLokalid): List<GrunnkretsGrenseKobling>
}

interface StemmekretsGrenseKoblingRepository : JpaRepository<StemmekretsGrenseKobling, Long> {
    fun findByStemmekretsKommuneLokalid(kommuneLokalid: UnikLokalid): List<StemmekretsGrenseKobling>
}

interface UtkastRepository : CrudRepository<Utkast, String>, JpaRepository<Utkast, String> {
    fun findAllByStatus(status: UtkastStatus): List<Utkast>

    @Query("SELECT u FROM Utkast u WHERE u.status IN :statuses " +
        "AND (CAST(:gyldigFra as DATE) IS NULL OR u.gyldigFra >= :gyldigFra)")
    fun findByStatusAndGyldigFra(
        statuses: List<String>,
        gyldigFra: LocalDate?
    ): List<Utkast>
}

fun verifiserOptimistiskLaas(nyVersion: Int, eksisterende: Laasbar): KonfliktVersjon? =
    if (nyVersion == eksisterende.version) null else KonfliktVersjon(nyVersion, eksisterende)

data class KonfliktVersjon(val nyVersjon: Int, val eksisterende: Laasbar) {
    fun getMessage(): String {
        var ikkeSamsvar = "Det er ikke samsvar med versjon"
        if (eksisterende is Flate) {
            val flateInformasjon = (eksisterende as Flate).getFlateInformasjon()
            ikkeSamsvar += " for ${flateInformasjon.flatetype.toString().lowercase()} ${flateInformasjon.lesbartNavn}"
        } else if (eksisterende is Utkast) {
            ikkeSamsvar += " for utkast ${eksisterende.navn}"
        }
        return "$ikkeSamsvar. Ønsket versjon = ${nyVersjon}, eksisterende versjon = ${eksisterende.version} for objekt med id = ${eksisterende.id}"
    }
}

class OptimistiskLaasException(vararg val konflikter: KonfliktVersjon)
    : RuntimeException("Det er ikke samsvar med versjon")

fun FylkeRepository.findGyldigFylkeForFylkesnummer(fylkesnummer: String, gyldighetsdato: LocalDate = LocalDate.now()): Fylke? {
    return findGyldigObjektPaaGyldighetsdato(findAllByFylkesnummer(fylkesnummer), gyldighetsdato)
}
