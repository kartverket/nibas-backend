package no.kartverket.nibas.repository

import no.kartverket.nibas.domain.*
import no.kartverket.nibas.domain.Noeyaktighetsklassekode
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.NoRepositoryBean

@NoRepositoryBean
interface CrudRepositoryWithLokalIdFinder<T, ID> : CrudRepository<T, ID> {
    fun findByLokalid(lokalid: String): T?
}

interface AvtaltAvgrensningslinjeRepository : CrudRepositoryWithLokalIdFinder<Avtaltavgrensningslinje, Int>
interface FylkeRepository : CrudRepositoryWithLokalIdFinder<Fylke, Int>
interface FylkesgrenseRepository : CrudRepositoryWithLokalIdFinder<Fylkesgrense, Int>
interface FylkeInformasjonRepository : CrudRepository<FylkeInformasjon, Int>
interface FylkesnummerRepository : CrudRepository<Fylkesnummer, String>
interface KommunegrenseRepository : CrudRepositoryWithLokalIdFinder<Kommunegrense, Int>
interface KommuneRepository : CrudRepositoryWithLokalIdFinder<Kommune, Int>
interface KommuneInformasjonRepository : CrudRepository<KommuneInformasjon, Int>
interface RiksgrenseRepository : CrudRepositoryWithLokalIdFinder<Riksgrense, Int>
interface TerritorialgrenseRepository : CrudRepositoryWithLokalIdFinder<Territorialgrense, Int>
interface AvtaltAvgrensningslinjeInformasjonRepository : CrudRepository<AvtaltavgrensningslinjeInformasjon, Int>
interface LandKodeRepository : CrudRepository<Landkode, String>
interface MaalemetodeRepository : CrudRepository<Maalemetode, String>
interface GrensestatusRepository : CrudRepository<Grensestatus, String>
interface FastsettingstypeRepository : CrudRepository<Fastsettingstype, String>
interface TerrengdetaljkodeRepository : CrudRepository<Terrengdetaljkode, String>
interface NoeyaktighetsklassekodeRepository : CrudRepository<Noeyaktighetsklassekode, String>
interface TypeendringRepository : CrudRepository<Typeendring, String>
interface DokumentasjonsreferanseRepository : CrudRepository<Dokumentasjonsreferanse, Int>
interface DokumentasjonsreferanseDokumentlinkRepository : CrudRepository<DokumentasjonsreferanseDokumentlink, Int>
interface KommunenummerRepository : CrudRepository<Kommunenummer, String>
interface SpraakkodeRepository : CrudRepository<Spraakkode, String>
interface LovVirkeomraadeGrenseRepository : CrudRepositoryWithLokalIdFinder<Lovvirkeomraadegrense, Int>
interface LovvirkeomraadeGrenseInformasjonRepository : CrudRepository<LovvirkeomraadegrenseInformasjon, Int>
interface GrunnlinjeRepository : CrudRepositoryWithLokalIdFinder<Grunnlinje, Int>
interface GrunnlinjeInformasjonRepository : CrudRepository<GrunnlinjeInformasjon, Int>
interface AdministrativenhetnavnRepository : CrudRepository<Administrativenhetnavn, Int>
interface NasjonInformasjonRepository : CrudRepository<NasjonInformasjon, Int>
interface NasjonRepository : CrudRepositoryWithLokalIdFinder<Nasjon, Int>
