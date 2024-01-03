package no.kartverket.nibas.repository

import no.kartverket.nibas.domain.Administrativenhetnavn
import no.kartverket.nibas.domain.Dokumentasjonsreferanse
import no.kartverket.nibas.domain.DokumentasjonsreferanseDokumentlink
import no.kartverket.nibas.domain.Fylke
import no.kartverket.nibas.domain.FylkeInformasjon
import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.domain.Grunnkrets
import no.kartverket.nibas.domain.KommuneInformasjon
import no.kartverket.nibas.domain.Landkode
import no.kartverket.nibas.domain.Nasjon
import no.kartverket.nibas.domain.Spraakkode
import no.kartverket.nibas.domain.Stemmekrets
import no.kartverket.nibas.domain.Typeendring
import no.kartverket.nibas.domain.UnikLokalid
import no.kartverket.nibas.domain.utkast.Utkast
import no.kartverket.nibas.domain.utkast.UtkastStatus
import no.kartverket.nibas.sporing.Laasbar
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
interface TypeendringRepository : CrudRepository<Typeendring, String>
interface DokumentasjonsreferanseRepository : CrudRepository<Dokumentasjonsreferanse, String>
interface DokumentasjonsreferanseDokumentlinkRepository : CrudRepository<DokumentasjonsreferanseDokumentlink, String>
interface SpraakkodeRepository : CrudRepository<Spraakkode, String>
interface AdministrativenhetnavnRepository : CrudRepository<Administrativenhetnavn, String>
interface UnikLokalidRepository : CrudRepositoryWithLokalIdFinder<UnikLokalid, String>
interface NasjonRepository : CrudRepositoryWithLokalIdFinder<Nasjon, String>
interface FylkeInformasjonRepository : CrudRepository<FylkeInformasjon, String>
interface FylkeRepository : CrudRepositoryWithLokalIdFinder<Fylke, String> {

    fun findAllByFylkesnummer(fylkesnummer: String): List<Fylke>
}

interface KommuneInformasjonRepository : CrudRepository<KommuneInformasjon, String>

interface GrunnkretsRepository : CrudRepositoryWithLokalIdFinder<Grunnkrets, String> {
    /**
     * Finn alle grunnkretser med gitt grunnkretsnummer hvor lokalid ikke er lik paramert lokalid.
     * Trenger ikke ha med lokalid for kommune, siden grunnkretsnummer inneholder kommunenummer
     */
    fun findAllByGrunnkretsnummerAndLokalidIsNot(grunnkretsnummer: String, lokalid: String): List<Grunnkrets>
}

interface StemmekretsRepository : CrudRepositoryWithLokalIdFinder<Stemmekrets, String> {
    /**
     * Finn alle stemmekretser med gitt stemmekretsnummer hvor lokalid ikke er lik paramert lokalid, og med kommune med ønsket lokalid for kommune.
     * Sender inn en kolleksjon med stemmekretsnumre, for å kunne søke med en og flere ledende nuller
     * Stemmekretsnumrene 5, 05, 005 og 0005 skal tolkes som like
     */
    @Query(value = "SELECT * from stemmekrets s where s.stemmekretsnummer in (:stemmekretsnummer) and s.lokalid != (:lokalid) and s.kommunelokalid = (:kommune_lokalid) ", nativeQuery = true)
    fun findAllByStemmekretsnummerInAndLokalidIsNotAndKommuneLokalid(stemmekretsnummer: Set<String>, lokalid: String, kommune_lokalid: String): List<Stemmekrets>
}

interface GrenseRepository : JpaRepositoryWithLokalIdFinder<Grense, String>

interface UtkastRepository : CrudRepository<Utkast, String> {
    fun findAllByStatus(status: UtkastStatus): List<Utkast>
}

fun verifiserOptimistiskLaas(nyVersion: Int, eksisterende: Laasbar): KonfliktVersjon? =
    if (nyVersion == eksisterende.version) null else KonfliktVersjon(nyVersion, eksisterende)

data class KonfliktVersjon(val nyVersjon: Int, val eksisterende: Laasbar) {
    fun getMessage(): String =
        "Det er ikke samsvar med versjon. Ønsket versjon = ${nyVersjon}, eksisterende versjon = ${eksisterende.version} for objekt med id = ${eksisterende.id}"
}

class OptimistiskLaasException(vararg val konflikter: KonfliktVersjon)
    : RuntimeException("Det er ikke samsvar med versjon")

fun FylkeRepository.findGyldigFylkeForFylkesnummer(fylkesnummer: String, gyldighetsdato: LocalDate = LocalDate.now()): Fylke? {
    return findGyldigObjektPaaGyldighetsdato(findAllByFylkesnummer(fylkesnummer), gyldighetsdato)
}
