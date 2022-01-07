package no.kartverket.nibas.repository

import no.kartverket.nibas.domain.Administrativenhetnavn
import no.kartverket.nibas.domain.Avtaltavgrensningslinje
import no.kartverket.nibas.domain.AvtaltavgrensningslinjeInformasjon
import no.kartverket.nibas.domain.Dokumentasjonsreferanse
import no.kartverket.nibas.domain.DokumentasjonsreferanseDokumentlink
import no.kartverket.nibas.domain.Fastsettingstype
import no.kartverket.nibas.domain.Fylke
import no.kartverket.nibas.domain.FylkeInformasjon
import no.kartverket.nibas.domain.Fylkesgrense
import no.kartverket.nibas.domain.Grensestatus
import no.kartverket.nibas.domain.Grunnlinje
import no.kartverket.nibas.domain.GrunnlinjeInformasjon
import no.kartverket.nibas.domain.Kommune
import no.kartverket.nibas.domain.KommuneInformasjon
import no.kartverket.nibas.domain.Kommunegrense
import no.kartverket.nibas.domain.Landkode
import no.kartverket.nibas.domain.Lovvirkeomraadegrense
import no.kartverket.nibas.domain.LovvirkeomraadegrenseInformasjon
import no.kartverket.nibas.domain.Nasjon
import no.kartverket.nibas.domain.NasjonInformasjon
import no.kartverket.nibas.domain.Noeyaktighetsklassekode
import no.kartverket.nibas.domain.Riksgrense
import no.kartverket.nibas.domain.Spraakkode
import no.kartverket.nibas.domain.Terrengdetaljkode
import no.kartverket.nibas.domain.Territorialgrense
import no.kartverket.nibas.domain.Typeendring
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.NoRepositoryBean

@NoRepositoryBean
interface CrudRepositoryWithLokalIdFinder<T, ID> : CrudRepository<T, ID> {
    fun findByLokalid(lokalid: String): T?
}

interface AvtaltAvgrensningslinjeRepository : CrudRepositoryWithLokalIdFinder<Avtaltavgrensningslinje, Int>
interface RiksgrenseRepository : CrudRepositoryWithLokalIdFinder<Riksgrense, Int>
interface TerritorialgrenseRepository : CrudRepositoryWithLokalIdFinder<Territorialgrense, Int>
interface AvtaltAvgrensningslinjeInformasjonRepository : CrudRepository<AvtaltavgrensningslinjeInformasjon, Int>
interface LandKodeRepository : CrudRepository<Landkode, String>
interface GrensestatusRepository : CrudRepository<Grensestatus, String>
interface FastsettingstypeRepository : CrudRepository<Fastsettingstype, String>
interface TerrengdetaljkodeRepository : CrudRepository<Terrengdetaljkode, String>
interface NoeyaktighetsklassekodeRepository : CrudRepository<Noeyaktighetsklassekode, String>
interface TypeendringRepository : CrudRepository<Typeendring, String>
interface DokumentasjonsreferanseRepository : CrudRepository<Dokumentasjonsreferanse, Int>
interface DokumentasjonsreferanseDokumentlinkRepository : CrudRepository<DokumentasjonsreferanseDokumentlink, Int>
interface SpraakkodeRepository : CrudRepository<Spraakkode, String>
interface LovVirkeomraadeGrenseRepository : CrudRepositoryWithLokalIdFinder<Lovvirkeomraadegrense, Int>
interface LovvirkeomraadeGrenseInformasjonRepository : CrudRepository<LovvirkeomraadegrenseInformasjon, Int>
interface GrunnlinjeRepository : CrudRepositoryWithLokalIdFinder<Grunnlinje, Int>
interface GrunnlinjeInformasjonRepository : CrudRepository<GrunnlinjeInformasjon, Int>
interface AdministrativenhetnavnRepository : CrudRepository<Administrativenhetnavn, Int>
interface NasjonInformasjonRepository : CrudRepository<NasjonInformasjon, Int>
interface NasjonRepository : CrudRepositoryWithLokalIdFinder<Nasjon, Int>
interface FylkesgrenseRepository : CrudRepositoryWithLokalIdFinder<Fylkesgrense, Int>
interface FylkeInformasjonRepository : CrudRepository<FylkeInformasjon, Int>
interface FylkeRepository : CrudRepositoryWithLokalIdFinder<Fylke, Int> {
    fun findByFylkesnummer(fylkesnummer_id: String): Fylke?
}
interface KommunegrenseRepository : CrudRepositoryWithLokalIdFinder<Kommunegrense, Int>
interface KommuneInformasjonRepository : CrudRepository<KommuneInformasjon, Int>
interface KommuneRepository : CrudRepositoryWithLokalIdFinder<Kommune, Int> {
    fun findByFylkeFk_Id(id: Int): List<Kommune>
}


