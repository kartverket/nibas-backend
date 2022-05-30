package no.kartverket.nibas.repository

import no.kartverket.nibas.domain.Administrativenhetnavn
import no.kartverket.nibas.domain.Avtaltavgrensningslinje
import no.kartverket.nibas.domain.AvtaltavgrensningslinjeInformasjon
import no.kartverket.nibas.domain.Delomraadegrense
import no.kartverket.nibas.domain.Dokumentasjonsreferanse
import no.kartverket.nibas.domain.DokumentasjonsreferanseDokumentlink
import no.kartverket.nibas.domain.Fastsettingstype
import no.kartverket.nibas.domain.Fylke
import no.kartverket.nibas.domain.FylkeInformasjon
import no.kartverket.nibas.domain.Fylkesgrense
import no.kartverket.nibas.domain.Grensestatus
import no.kartverket.nibas.domain.Grunnkrets
import no.kartverket.nibas.domain.GrunnkretsInformasjon
import no.kartverket.nibas.domain.Grunnkretsgrense
import no.kartverket.nibas.domain.GrunnkretsgrenseInformasjon
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
import no.kartverket.nibas.domain.Stemmekrets
import no.kartverket.nibas.domain.Stemmekretsgrense
import no.kartverket.nibas.domain.StemmekretsgrenseInformasjon
import no.kartverket.nibas.domain.Terrengdetaljkode
import no.kartverket.nibas.domain.Territorialgrense
import no.kartverket.nibas.domain.Typeendring
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.NoRepositoryBean

@NoRepositoryBean
interface CrudRepositoryWithLokalIdFinder<T, ID> : CrudRepository<T, ID> {
    fun findByLokalid(lokalid: String): T?
}

interface AvtaltAvgrensningslinjeRepository : CrudRepositoryWithLokalIdFinder<Avtaltavgrensningslinje, String>
interface RiksgrenseRepository : CrudRepositoryWithLokalIdFinder<Riksgrense, String>
interface TerritorialgrenseRepository : CrudRepositoryWithLokalIdFinder<Territorialgrense, String>
interface AvtaltAvgrensningslinjeInformasjonRepository : CrudRepository<AvtaltavgrensningslinjeInformasjon, String>
interface LandKodeRepository : CrudRepository<Landkode, String>
interface GrensestatusRepository : CrudRepository<Grensestatus, String>
interface FastsettingstypeRepository : CrudRepository<Fastsettingstype, String>
interface TerrengdetaljkodeRepository : CrudRepository<Terrengdetaljkode, String>
interface NoeyaktighetsklassekodeRepository : CrudRepository<Noeyaktighetsklassekode, String>
interface TypeendringRepository : CrudRepository<Typeendring, String>
interface DokumentasjonsreferanseRepository : CrudRepository<Dokumentasjonsreferanse, String>
interface DokumentasjonsreferanseDokumentlinkRepository : CrudRepository<DokumentasjonsreferanseDokumentlink, String>
interface SpraakkodeRepository : CrudRepository<Spraakkode, String>
interface LovVirkeomraadeGrenseRepository : CrudRepositoryWithLokalIdFinder<Lovvirkeomraadegrense, String>
interface LovvirkeomraadeGrenseInformasjonRepository : CrudRepository<LovvirkeomraadegrenseInformasjon, String>
interface GrunnlinjeRepository : CrudRepositoryWithLokalIdFinder<Grunnlinje, String>
interface GrunnlinjeInformasjonRepository : CrudRepository<GrunnlinjeInformasjon, String>
interface AdministrativenhetnavnRepository : CrudRepository<Administrativenhetnavn, String>
interface NasjonInformasjonRepository : CrudRepository<NasjonInformasjon, String>
interface NasjonRepository : CrudRepositoryWithLokalIdFinder<Nasjon, String> {
    fun findByTerritorialgrenser_Territorialgrense_Id(id: String): List<Nasjon>
    fun findByAvtaltavgrensningslinjer_Avtaltavgrensningslinje_Id(id: String): List<Nasjon>
    fun findByRiksgrenser_Riksgrense_Id(id: String): List<Nasjon>
}
interface FylkesgrenseRepository : CrudRepositoryWithLokalIdFinder<Fylkesgrense, String>
interface FylkeInformasjonRepository : CrudRepository<FylkeInformasjon, String>
interface FylkeRepository : CrudRepositoryWithLokalIdFinder<Fylke, String> {
    fun findByFylkesnummer(fylkesnummer_id: String): Fylke?
    fun findByFylkesgrenser_Fylkesgrense_Id(id: String): List<Fylke>
    fun findByTerritorialgrenser_Territorialgrense_Id(id: String): List<Fylke>
    fun findByAvtaltavgrensningslinjer_Avtaltavgrensningslinje_Id(id: String): List<Fylke>
    fun findByRiksgrenser_Riksgrense_Id(id: String): List<Fylke>
}
interface KommunegrenseRepository : CrudRepositoryWithLokalIdFinder<Kommunegrense, String>
interface KommuneInformasjonRepository : CrudRepository<KommuneInformasjon, String>
interface KommuneRepository : CrudRepositoryWithLokalIdFinder<Kommune, String> {
    fun findByFylkeFk_Id(id: String): List<Kommune>
    fun findByKommunegrenser_Kommunegrense_Id(id: String): List<Kommune>
    fun findByFylkesgrenser_Fylkesgrense_Id(id: String): List<Kommune>
    fun findByTerritorialgrenser_Territorialgrense_Id(id: String): List<Kommune>
    fun findByAvtaltavgrensningslinjer_Avtaltavgrensningslinje_Id(id: String): List<Kommune>
    fun findByRiksgrenser_Riksgrense_Id(id: String): List<Kommune>
    fun findByKommunenummer(kommunenummer: String): Kommune?
}
interface GrunnkretsRepository : CrudRepositoryWithLokalIdFinder<Grunnkrets, String> {
    fun findByKommunenummer(kommunenummer: String): List<Grunnkrets>
    fun findByGrunnkretsgrenser_Grunnkretsgrense_Id(id: String): List<Grunnkrets>
    fun findByDelomraadegrenser_Delomraadegrense_Id(id: String): List<Grunnkrets>
    fun findByKommunegrenser_Kommunegrense_Id(id: String): List<Kommune>
    fun findByFylkesgrenser_Fylkesgrense_Id(id: String): List<Kommune>
    fun findByTerritorialgrenser_Territorialgrense_Id(id: String): List<Kommune>
    fun findByAvtaltavgrensningslinjer_Avtaltavgrensningslinje_Id(id: String): List<Kommune>
    fun findByRiksgrenser_Riksgrense_Id(id: String): List<Kommune>
}
interface GrunnkretsInformasjonRepository : CrudRepository<GrunnkretsInformasjon, String>

interface GrunnkretsgrenseRepository : CrudRepositoryWithLokalIdFinder<Grunnkretsgrense, String>
interface GrunnkretsgrenseInformasjonRepository : CrudRepository<GrunnkretsgrenseInformasjon, String>
interface DelomraadegrenseRepository : CrudRepositoryWithLokalIdFinder<Delomraadegrense, String>

interface StemmekretsRepository : CrudRepositoryWithLokalIdFinder<Stemmekrets, String>
interface StemmekretsgrenseRepository : CrudRepositoryWithLokalIdFinder<Stemmekretsgrense, String>
interface StemmekretsgrenseInformasjonRepository : CrudRepository<StemmekretsgrenseInformasjon, String>
