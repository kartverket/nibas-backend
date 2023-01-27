package no.kartverket.nibas.repository

import no.kartverket.nibas.domain.Administrativenhetnavn
import no.kartverket.nibas.domain.Dokumentasjonsreferanse
import no.kartverket.nibas.domain.DokumentasjonsreferanseDokumentlink
import no.kartverket.nibas.domain.Fylke
import no.kartverket.nibas.domain.FylkeInformasjon
import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.domain.Grunnkrets
import no.kartverket.nibas.domain.Kommune
import no.kartverket.nibas.domain.KommuneInformasjon
import no.kartverket.nibas.domain.Landkode
import no.kartverket.nibas.domain.Nasjon
import no.kartverket.nibas.domain.Spraakkode
import no.kartverket.nibas.domain.Stemmekrets
import no.kartverket.nibas.domain.Typeendring
import no.kartverket.nibas.domain.utkast.Utkast
import no.kartverket.nibas.domain.utkast.UtkastStatus
import no.kartverket.nibas.sporing.Laasbar
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.NoRepositoryBean

@NoRepositoryBean
interface CrudRepositoryWithLokalIdFinder<T, ID> : CrudRepository<T, ID> {
    fun findByLokalid(lokalid: String): T?
    fun findAllByLokalid(lokalid: String): List<T>
}

@NoRepositoryBean
interface JpaRepositoryWithLokalIdFinder<T, ID> : JpaRepository<T, ID> {
    fun findByLokalid(lokalid: String): T?

    fun findAllByLokalid(lokalid: String): List<T>
}

interface LandKodeRepository : CrudRepository<Landkode, String>
interface TypeendringRepository : CrudRepository<Typeendring, String>
interface DokumentasjonsreferanseRepository : CrudRepository<Dokumentasjonsreferanse, String>
interface DokumentasjonsreferanseDokumentlinkRepository : CrudRepository<DokumentasjonsreferanseDokumentlink, String>
interface SpraakkodeRepository : CrudRepository<Spraakkode, String>
interface AdministrativenhetnavnRepository : CrudRepository<Administrativenhetnavn, String>
interface NasjonRepository : CrudRepositoryWithLokalIdFinder<Nasjon, String>
interface FylkeInformasjonRepository : CrudRepository<FylkeInformasjon, String>
interface FylkeRepository : CrudRepositoryWithLokalIdFinder<Fylke, String> {
    fun findByFylkesnummer(fylkesnummer_id: String): Fylke?
}

interface KommuneInformasjonRepository : CrudRepository<KommuneInformasjon, String>
interface KommuneRepository : CrudRepositoryWithLokalIdFinder<Kommune, String> {
    fun findByKommunenummer(kommunenummer: String): Kommune?
}

interface GrunnkretsRepository : CrudRepositoryWithLokalIdFinder<Grunnkrets, String>

interface StemmekretsRepository : CrudRepositoryWithLokalIdFinder<Stemmekrets, String>

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
