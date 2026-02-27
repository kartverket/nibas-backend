@file:Suppress("SpringJavaInjectionPointsAutowiringInspection")

package no.kartverket.nibas.testcontainer

import no.kartverket.nibas.api.v1.common.Flatetype
import no.kartverket.nibas.api.v1.common.Grenseendringer
import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.api.v1.common.KontekstEnhetType
import no.kartverket.nibas.api.v1.common.ObjektIdentifikator
import no.kartverket.nibas.api.v1.common.Operasjoner
import no.kartverket.nibas.api.v1.common.geojson.Feature
import no.kartverket.nibas.api.v1.common.geojson.FeatureCollection
import no.kartverket.nibas.api.v1.common.geojson.KontekstEgenskaper
import no.kartverket.nibas.api.v1.common.geojson.metadata.IdentifikatorMedVersjon
import no.kartverket.nibas.api.v1.request.KretsDelingEndringRequest
import no.kartverket.nibas.api.v1.request.KretsNavnOgNummer
import no.kartverket.nibas.builders.BopliktomraadeBuilder
import no.kartverket.nibas.builders.FeatureBuilder
import no.kartverket.nibas.builders.FylkeBuilder
import no.kartverket.nibas.builders.GrenseBuilder
import no.kartverket.nibas.builders.GrunnkretsBuilder
import no.kartverket.nibas.builders.GrunnkretsRequestBuilder
import no.kartverket.nibas.builders.KommuneBuilder
import no.kartverket.nibas.builders.MaalemetodeKodeEnum
import no.kartverket.nibas.builders.NasjonBuilder
import no.kartverket.nibas.builders.StemmekretsBuilder
import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.domain.Administrativenhetnavn
import no.kartverket.nibas.domain.Bopliktomraade
import no.kartverket.nibas.domain.BopliktomraadeGrenser
import no.kartverket.nibas.domain.Endringstype
import no.kartverket.nibas.domain.Flate
import no.kartverket.nibas.domain.Fylke
import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.domain.GrenseKobling
import no.kartverket.nibas.domain.Grunnkrets
import no.kartverket.nibas.domain.Kommune
import no.kartverket.nibas.domain.Nasjon
import no.kartverket.nibas.domain.PosisjonskvalitetEmbeddable
import no.kartverket.nibas.domain.Spraakkode
import no.kartverket.nibas.domain.Stemmekrets
import no.kartverket.nibas.domain.UnikLokalid
import no.kartverket.nibas.domain.utkast.Utkast
import no.kartverket.nibas.domain.utkast.UtkastBuilder
import no.kartverket.nibas.extensions.geometry.geometryToDto
import no.kartverket.nibas.extensions.geometry.grenseToFeature
import no.kartverket.nibas.repository.AdministrativenhetnavnRepository
import no.kartverket.nibas.repository.BopliktomraadeRepository
import no.kartverket.nibas.repository.DokrefGrenseKoblingRepository
import no.kartverket.nibas.repository.DokumentasjonsreferanseRepository
import no.kartverket.nibas.repository.FylkeRepository
import no.kartverket.nibas.repository.GrenseRepository
import no.kartverket.nibas.repository.GrunnkretsRepository
import no.kartverket.nibas.repository.KommuneRepository
import no.kartverket.nibas.repository.NasjonRepository
import no.kartverket.nibas.repository.StemmekretsRepository
import no.kartverket.nibas.repository.UnikLokalidRepository
import no.kartverket.nibas.repository.UtkastRepository
import no.kartverket.nibas.service.AdministrativenhetnavnService
import no.kartverket.nibas.service.BopliktomraadeService
import no.kartverket.nibas.service.FlateService
import no.kartverket.nibas.service.FylkeService
import no.kartverket.nibas.service.GrenseService
import no.kartverket.nibas.service.GrunnkretsService
import no.kartverket.nibas.service.InndelingerService
import no.kartverket.nibas.service.KommuneService
import no.kartverket.nibas.service.NasjonService
import no.kartverket.nibas.service.StemmekretsService
import no.kartverket.nibas.service.deltgeometri.DeltGeometriAdministrativeGrenserService
import no.kartverket.nibas.service.utkast.UtkastService
import no.kartverket.nibas.sporing.Gyldighet
import no.kartverket.nibas.sporing.Laasbar
import no.kartverket.nibas.testsupport.TestUserFacadeConfig
import no.kartverket.nibas.utils.GrenseHelper
import org.assertj.core.api.Assertions.assertThat
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.MultiPolygon
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment
import org.springframework.context.annotation.Import
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import java.time.LocalDate
import java.time.Month
import java.util.*
import jakarta.persistence.EntityManager

@SpringBootTest(webEnvironment = WebEnvironment.MOCK)
@Import(TestUserFacadeConfig::class)
class TestWithDbContainer {

    @Autowired
    lateinit var entityManager: EntityManager

    @Autowired
    lateinit var unikLokalidRepository: UnikLokalidRepository

    @Autowired
    lateinit var nasjonRepository: NasjonRepository

    @Autowired
    lateinit var fylkeRepository: FylkeRepository

    @Autowired
    lateinit var kommuneRepository: KommuneRepository

    @Autowired
    lateinit var grunnkretsRepository: GrunnkretsRepository

    @Autowired
    lateinit var stemmekretsRepository: StemmekretsRepository

    @Autowired
    lateinit var bopliktomraadeRepository: BopliktomraadeRepository

    @Autowired
    lateinit var grenseRepository: GrenseRepository

    @Autowired
    lateinit var utkastRepository: UtkastRepository

    @Autowired
    lateinit var administrativenhetnavnRepository: AdministrativenhetnavnRepository

    @Autowired
    lateinit var grunnkretsService: GrunnkretsService

    @Autowired
    lateinit var stemmekretsService: StemmekretsService

    @Autowired
    lateinit var kommuneService: KommuneService

    @Autowired
    lateinit var fylkeService: FylkeService

    @Autowired
    lateinit var nasjonService: NasjonService

    @Autowired
    lateinit var flateService: FlateService

    @Autowired
    lateinit var grenseService: GrenseService

    @Autowired
    lateinit var utkastService: UtkastService

    @Autowired
    lateinit var administrativenhetnavnService: AdministrativenhetnavnService

    @Autowired
    lateinit var bopliktomraadeService: BopliktomraadeService

    @Autowired
    lateinit var inndelingerService: InndelingerService

    @Autowired
    lateinit var deltGeometriAdministrativeGrenserService: DeltGeometriAdministrativeGrenserService

    @Autowired
    lateinit var dokumentasjonsreferanseRepository: DokumentasjonsreferanseRepository

    @Autowired
    lateinit var dokrefGrenseKoblingRepository: DokrefGrenseKoblingRepository

    companion object dbContainer : PostgreSQLContainer<dbContainer>(
        DockerImageName.parse("postgis/postgis:14-3.4-alpine").asCompatibleSubstituteFor("postgres")
    ) {
        private val randomContainerSuffix = Random().nextInt().toString()

        init {
            withDatabaseName("nibas")
            withUsername("nibas")
            withPassword("nibas")
            withCreateContainerCmdModifier { cmd -> cmd.withName("nibas-test-container$randomContainerSuffix") }
            start()
        }

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", dbContainer::getJdbcUrl)
            registry.add("spring.datasource.username", dbContainer::getUsername)
            registry.add("spring.datasource.password", dbContainer::getPassword)
        }
    }

    fun saveNasjon(id: String = "nasjon-9000",
                   unikLokalid: UnikLokalid = UnikLokalid("nasjon-fb07-4277-ad95-63c32c1370d0", Flatetype.NASJON),
                   gyldigFra: LocalDate = LocalDate.of(2022, Month.OCTOBER, 1),
                   gyldigTil: LocalDate? = null,
                   fylker: List<Fylke> = emptyList(),
                   administrativenhetnavn: List<Administrativenhetnavn> = emptyList(),
                   opphav: String = "Eidsvoll"): Nasjon {

        val nasjon = NasjonBuilder(
            id = id,
            unikLokalid = unikLokalid,
            gyldigFra = gyldigFra,
            gyldigTil = gyldigTil,
            opphav = opphav)
            .buildUtenUnikLokalidOgTilknytninger()

        val save = nasjonRepository.save(nasjon)
        save.addNasjonTilUnikLokalid(unikLokalid)

        fylker.forEach { save.addFylke(it) }
        administrativenhetnavn.forEach { save.addAdministrativenhetNavn(it) }

        return save
    }

    fun saveNasjonOgUnikLokalid(id: String = "nasjon-9000",
                                lokalid: String = "nasjon-fb07-4277-ad95-63c32c1370d0",
                                gyldigFra: LocalDate = LocalDate.of(2022, Month.OCTOBER, 1),
                                gyldigTil: LocalDate? = null,
                                fylker: List<Fylke> = emptyList(),
                                administrativenhetnavn: List<Administrativenhetnavn> = emptyList(),
                                opphav: String = "Eidsvoll"): Nasjon {

        val unikLokalid = saveUnikLokalid(lokalid, Flatetype.NASJON)

        return saveNasjon(
            id = id,
            unikLokalid = unikLokalid,
            gyldigFra = gyldigFra,
            gyldigTil = gyldigTil,
            fylker = fylker,
            administrativenhetnavn = administrativenhetnavn,
            opphav = opphav
        )
    }

    fun saveUnikLokalid(
        lokalid: String = UUID.randomUUID().toString(),
        flatetype: Flatetype
    ): UnikLokalid {
        val unikLokalid = UnikLokalid(lokalid, flatetype)
        return unikLokalidRepository.save(unikLokalid)
    }

    fun saveAdministrativenhetnavn(
        lokalid: String = UUID.randomUUID().toString(),
        navn: String,
        spraakid: String,
        rekkefoelge: Int? = null,
        gyldigFra: LocalDate = LocalDate.of(2022, Month.OCTOBER, 1),
        gyldigTil: LocalDate? = null
    ): Administrativenhetnavn {
        val administrativenhetnavn = Administrativenhetnavn(lokalid = lokalid)
        administrativenhetnavn.navn = navn
        val spraakkode = Spraakkode()
        spraakkode.id = spraakid
        spraakkode.description = spraakid
        administrativenhetnavn.spraak = spraakkode
        administrativenhetnavn.rekkefoelge = rekkefoelge
        administrativenhetnavn.gyldighet = Gyldighet(gyldigFra, gyldigTil)

        return administrativenhetnavnRepository.save(administrativenhetnavn)
    }

    fun saveFylke(id: String = "fylke-9000",
                  unikLokalid: UnikLokalid = UnikLokalid("fylke-fb07-4277-ad95-63c32c1370d0", Flatetype.FYLKE),
                  fylkesnummerUuid: String = "89",
                  fylkesnummer: String = "89",
                  gyldigFra: LocalDate = LocalDate.of(2022, Month.OCTOBER, 1),
                  gyldigTil: LocalDate? = null,
                  kommuner: List<Kommune> = emptyList(),
                  administrativenhetnavn: List<Administrativenhetnavn> = emptyList()): Fylke {

        val fylke = FylkeBuilder(
            id = id,
            unikLokalid = unikLokalid,
            gyldigFra = gyldigFra,
            gyldigTil = gyldigTil,
            fylkesnummerUuid = fylkesnummerUuid,
            fylkesnummer = fylkesnummer)
            .buildUtenUnikLokalidOgTilknytninger()

        val save = fylkeRepository.save(fylke)
        save.addFylkeTilUnikLokalid(unikLokalid)

        kommuner.forEach { save.addKommune(it) }
        administrativenhetnavn.forEach { save.addAdministrativenhetNavn(it) }

        return save
    }

    fun saveFylkeOgUnikLokalid(id: String = "fylke-9000",
                               lokalid: String = "fylke-fb07-4277-ad95-63c32c1370d0",
                               fylkesnummerUuid: String = "89",
                               fylkesnummer: String = "89",
                               gyldigFra: LocalDate = LocalDate.of(2022, Month.OCTOBER, 1),
                               gyldigTil: LocalDate? = null,
                               kommuner: List<Kommune> = emptyList(),
                               administrativenhetnavn: List<Administrativenhetnavn> = emptyList()): Fylke {

        val unikLokalid = saveUnikLokalid(lokalid, Flatetype.FYLKE)

        return saveFylke(
            id = id,
            unikLokalid = unikLokalid,
            fylkesnummerUuid = fylkesnummerUuid,
            fylkesnummer = fylkesnummer,
            gyldigFra = gyldigFra,
            gyldigTil = gyldigTil,
            kommuner = kommuner,
            administrativenhetnavn = administrativenhetnavn
        )
    }

    // Kommunenummer er egentlig en uuid, forenklet med en firesifret verdi
    fun saveKommune(
        id: String = "kommune-9000",
        unikLokalid: UnikLokalid = UnikLokalid("abc-123", Flatetype.KOMMUNE),
        kommunenummerUuid: String = "0301",
        kommunenummer: String = "0301",
        gyldigFra: LocalDate = LocalDate.of(2022, Month.OCTOBER, 1),
        gyldigTil: LocalDate? = null,
        administrativenhetnavn: List<Administrativenhetnavn> = emptyList(),
        grunnkretser: List<Grunnkrets> = emptyList(),
        stemmekretser: List<Stemmekrets> = emptyList(),
        bopliktomraader: List<Bopliktomraade> = emptyList(),
    ): Kommune {

        val kommune = KommuneBuilder(
            id = id,
            unikLokalid = unikLokalid,
            gyldigFra = gyldigFra,
            gyldigTil = gyldigTil,
            kommunenummerUuid = kommunenummerUuid,
            kommunenummer = kommunenummer
        ).buildUtenUnikLokalidOgTilknytninger()

        val save = kommuneRepository.save(kommune)
        save.addKommuneTilUnikLokalid(unikLokalid)

        grunnkretser.forEach { save.addGrunnkrets(it) }
        stemmekretser.forEach { save.addStemmekrets(it) }
        administrativenhetnavn.forEach { save.addAdministrativenhetNavn(it) }
        bopliktomraader.forEach { save.addBopliktomraade(it) }

        return save
    }

    fun saveKommuneOgUnikLokalid(
        id: String = "kommune-9000",
        lokalid: String = "abc-123",
        kommunenummerUuid: String = "0301",
        kommunenummer: String = "0301",
        gyldigFra: LocalDate = LocalDate.of(2022, Month.OCTOBER, 1),
        gyldigTil: LocalDate? = null,
        administrativenhetnavn: List<Administrativenhetnavn> = emptyList(),
        grunnkretser: List<Grunnkrets> = emptyList(),
        stemmekretser: List<Stemmekrets> = emptyList(),
        bopliktomraader: List<Bopliktomraade> = emptyList(),
    ): Kommune {

        val unikLokalid = saveUnikLokalid(lokalid = lokalid, flatetype = Flatetype.KOMMUNE)

        return saveKommune(
            id = id,
            unikLokalid = unikLokalid,
            kommunenummerUuid = kommunenummerUuid,
            kommunenummer = kommunenummer,
            gyldigFra = gyldigFra,
            gyldigTil = gyldigTil,
            administrativenhetnavn = administrativenhetnavn,
            grunnkretser = grunnkretser,
            stemmekretser = stemmekretser,
            bopliktomraader = bopliktomraader,
        )
    }

    fun saveGrunnkrets(
        id: String = "grunnkrets-9000",
        lokalid: Lokalid = Lokalid("1001"),
        grunnkretsnummer: String = "42270304",
        grunnkretsnavn: String = "Dåsvatn",
        gyldigFra: LocalDate = LocalDate.of(2022, Month.OCTOBER, 1),
        gyldigTil: LocalDate? = null,
        grenser: List<Grense> = emptyList()
    ): Grunnkrets {
        val grunnkrets = GrunnkretsBuilder(
            id = id,
            lokalId = lokalid,
            gyldigFra = gyldigFra,
            gyldigTil = gyldigTil,
            grunnkretsnummer = grunnkretsnummer,
            grunnkretsnavn = grunnkretsnavn,
            kommune = null,
            grenser = grenser
        ).buildUtenKommuneOgGrenser()

        val save = grunnkretsRepository.save(grunnkrets)
        grenser.forEach { grense ->
            save.addGrensekobling(grense)
        }
        //Hack fordi noen tester lager grenser uten linestring
        try {
            if (grenser.isNotEmpty() && grenser.all { !it.grense.isEmpty }) {
                save.oppdaterOmraadeOgPosisjon()
                if (!save.omraade!!.isEmpty) {
                    save.posisjon = save.omraade?.interiorPoint
                }
            }
        } catch (_: UninitializedPropertyAccessException) {
        }

        return save
    }

    fun saveStemmekrets(
        id: String = "stemmekrets-9000",
        lokalid: Lokalid,
        stemmekretsnummer: String = "9",
        stemmekretsnavn: String = "Øvre Singsaker",
        gyldigFra: LocalDate = LocalDate.of(2022, Month.OCTOBER, 1),
        gyldigTil: LocalDate? = null,
        grenser: List<Grense> = emptyList(),
        informasjon: String = "Informasjon opprinnelig"
    ): Stemmekrets {

        val stemmekrets = StemmekretsBuilder(
            id = id,
            lokalId = lokalid,
            gyldigFra = gyldigFra,
            gyldigTil = gyldigTil,
            stemmekretsnummer = stemmekretsnummer,
            stemmekretsnavn = stemmekretsnavn,
            kommune = null,
            grenser = grenser,
            informasjonstekst = informasjon
        ).buildUtenKommuneOgGrenser()

        val save = stemmekretsRepository.save(stemmekrets)

        grenser.forEach { save.addGrensekobling(it) }
        if (grenser.isNotEmpty() && grenser.none { it.grense.isEmpty }) {
            save.oppdaterOmraadeOgPosisjon()
            if (!save.omraade!!.isEmpty) {
                save.posisjon = save.omraade?.interiorPoint
            }
        }

        return save
    }

    fun saveBopliktomraade(
        id: String = "bopliktomraade-9000",
        lokalid: Lokalid,
        bopliktomraadenummer: String = "01",
        bopliktomraadenavn: String = "Bopliktområde i kommune",
        gyldigFra: LocalDate = LocalDate.of(2022, Month.OCTOBER, 1),
        gyldigTil: LocalDate? = null,
        grenser: List<Grense> = emptyList(),
        informasjon: String = "Opprinnelig informasjon",
        delvisBoplikt: Boolean = true
    ): Bopliktomraade {

        val bopliktomraade = BopliktomraadeBuilder(
            id = id,
            lokalId = lokalid,
            gyldigFra = gyldigFra,
            gyldigTil = gyldigTil,
            bopliktomraadenummer = bopliktomraadenummer,
            bopliktomraadenavn = bopliktomraadenavn,
            kommune = null,
            grenser = grenser,
            informasjonstekst = informasjon
        ).buildUtenKommuneOgGrenser()

        if (!delvisBoplikt) {
            bopliktomraade.bopliktomraadeGrenser = BopliktomraadeGrenser.FullBoplikt()
        }

        val save = bopliktomraadeRepository.save(bopliktomraade)

        grenser.forEach { save.addGrensekobling(it) }
        if (grenser.isNotEmpty() && grenser.none { it.grense.isEmpty }) {
            save.oppdaterOmraadeOgPosisjon()
            if (!save.omraade!!.isEmpty) {
                save.posisjon = save.omraade?.interiorPoint
            }
        }

        return save
    }

    fun saveGrense(
        id: String = "grense-id",
        lokalid: Lokalid = Lokalid("grense-fb07-4277-ad95-63c32c1370d0"),
        gyldigFra: LocalDate = LocalDate.of(2022, Month.OCTOBER, 1),
        gyldigTil: LocalDate? = null,
        opphav: String = "Eidsvoll",
        grensetype: Grensetype = Grensetype.Kommunegrense
    ): Grense {
        return grenseRepository.save(GrenseBuilder(
            id = id,
            lokalId = lokalid,
            gyldigFra = gyldigFra,
            gyldigTil = gyldigTil,
            opphav = opphav
        ).withGrensetype(grensetype).build())
    }

    fun saveGrenseAvTypeRiksgrense(id: String = "riksgrense-id",
                                   lokalid: Lokalid = Lokalid("riksgrense-fb07-4277-ad95-63c32c1370d0"),
                                   gyldigFra: LocalDate = LocalDate.of(2022, Month.OCTOBER, 1),
                                   gyldigTil: LocalDate? = null,
                                   opphav: String = "Eidsvoll"): Grense {
        return grenseRepository.save(GrenseBuilder(
            id = id,
            lokalId = lokalid,
            gyldigFra = gyldigFra,
            gyldigTil = gyldigTil,
            opphav = opphav
        ).withGrensetype(Grensetype.Riksgrense).build())
    }

    /**
     * Sjekker at topologien til to flater er topologisk like, dvs ikke nødvendigvis samme start/slutt eller samme retning.
     * Ikke det samme som isEqualTo
     */
    fun assertTopologi(flate1: MultiPolygon?, flate2: MultiPolygon?, lik: Boolean = true) {
        assertThat(flate1).isNotNull()
        assertThat(flate2).isNotNull()
        assertThat(flate1?.equals(flate2)).`as`("Flate1: $flate1 Flate2: $flate2").isEqualTo(lik)
    }

    fun saveNaboGrunnkretser(gyldigFra: LocalDate, medHull: Boolean = false, medTredjeKrets: Boolean = false, medToDelteGrenser: Boolean = false, medKommunegrense: Boolean = false)
        : List<Grunnkrets> {
        val grenserForHull = if (medHull) {
            GrenseHelper.getHullVestligKrets(gyldigFra, Grensetype.Grunnkretsgrense)
        } else {
            emptyList()
        }
        val grenserForVest = GrenseHelper.getGrenserForVestligKrets(gyldigFra, Grensetype.Grunnkretsgrense, medToDelteGrenser)

        //Lagrer grensene først, for å være sikker på at de er tilgjengelige for å knyttes til kretsene
        val lagredeGrenserForHull = if (medHull) grenseRepository.saveAll(grenserForHull) else emptyList()
        val lagredeGrenserForVest = grenseRepository.saveAll(grenserForVest)
        val lagredeGrenserForOest = grenseRepository.saveAll(GrenseHelper.getGrenserForOestligKrets(gyldigFra, Grensetype.Grunnkretsgrense, medToDelteGrenser, medKommunegrense))
        grenseRepository.flush()

        val kretsVest = saveGrunnkrets(
            id = "1",
            lokalid = Lokalid("123"),
            grunnkretsnummer = "03010020",
            grunnkretsnavn = "Vestre krets",
            gyldigFra = gyldigFra,
            grenser = lagredeGrenserForVest + lagredeGrenserForHull
        )

        val kretsOest = saveGrunnkrets(
            id = "2",
            lokalid = Lokalid("234"),
            grunnkretsnummer = "03010021",
            grunnkretsnavn = "Østre krets",
            gyldigFra = gyldigFra,
            grenser = lagredeGrenserForOest
        )

        val grunnkretsMutableList = mutableListOf(kretsVest, kretsOest)
        if (medTredjeKrets) {
            val tredjeKrets = saveGrunnkrets(
                id = "3",
                lokalid = Lokalid("345"),
                grunnkretsnummer = "03010022",
                grunnkretsnavn = "Tredje krets",
                gyldigFra = gyldigFra,
                grenser = lagredeGrenserForHull
            )
            grunnkretsMutableList.add(tredjeKrets)
        }

        val grunnkretser = grunnkretsMutableList.toList()
        saveKommuneOgUnikLokalid(grunnkretser = grunnkretser, gyldigFra = gyldigFra)
        grenseRepository.flush()
        return grunnkretser
    }

    fun assertOmradeGyldighetVersjon(nyFlate: Flate, gammelFlate: Flate, endringsdato: LocalDate?, endringstype: Endringstype) {
        assertThat(gammelFlate.gyldighet.gyldigTil).isEqualTo(endringsdato)
        assertThat(nyFlate.gyldighet.gyldigFra).isEqualTo(endringsdato)
        assertThat(nyFlate.gyldighet.gyldigTil).isNull()
        assertThat(nyFlate.id).isNotEqualTo(gammelFlate.id)
        assertThat(nyFlate.omraade).isNotEqualTo(gammelFlate.omraade)
        assertThat(endringstype).isEqualTo(nyFlate.auditInfo.typeEndring)
        assertThat(nyFlate.posisjon).isNotEqualTo(gammelFlate.posisjon)

        assertThat((nyFlate as Laasbar).version).isGreaterThan((gammelFlate as Laasbar).version)
    }

    fun saveNaboGrunnkretserOgStemmekretserIHverSinKommune(gyldigFra: LocalDate, medHull: Boolean = false, medToDelteGrenser: Boolean = false,
                                                           kommunenummer1: String = "0301", kommunenummer2: String = "0302"): List<Grunnkrets> {

        val fylkesnummer1 = kommunenummer1.slice(0..1)
        val fylkesnummer2 = kommunenummer2.slice(0..1)

        val grenseType = if (fylkesnummer1 == fylkesnummer2) Grensetype.Kommunegrense else Grensetype.Fylkesgrense

        val grenserForVest = if (medHull) {
            GrenseHelper.getGrenserForVestligKrets(gyldigFra, grenseType, medToDelteGrenser) +
                GrenseHelper.getHullVestligKrets(gyldigFra, grenseType)
        } else {
            GrenseHelper.getGrenserForVestligKrets(gyldigFra, grenseType, medToDelteGrenser)
        }

        //Lagrer grensene først, for å være sikker på at de er tilgjengelige for å knyttes til kretsene
        val lagredeGrenserForVest = grenseRepository.saveAll(grenserForVest)
        val lagredeGrenserForOest = grenseRepository.saveAll(GrenseHelper.getGrenserForOestligKrets(gyldigFra, grenseType, medToDelteGrenser))
        grenseRepository.flush()

        val kretsVest = saveGrunnkrets(
            id = "1",
            lokalid = Lokalid("123"),
            grunnkretsnummer = kommunenummer1 + "0020",
            grunnkretsnavn = "Vestre krets",
            gyldigFra = gyldigFra,
            grenser = lagredeGrenserForVest
        )

        val kretsOest = saveGrunnkrets(
            id = "2",
            lokalid = Lokalid("234"),
            grunnkretsnummer = kommunenummer2 + "0021",
            grunnkretsnavn = "Østre krets",
            gyldigFra = gyldigFra,
            grenser = lagredeGrenserForOest
        )

        val stemmekrets1 = saveStemmekrets(id = "S1", lokalid = Lokalid("S1"),
            stemmekretsnummer = "20", stemmekretsnavn = "Vestre stemmekrets", gyldigFra = gyldigFra, grenser = lagredeGrenserForVest, informasjon = "Informasjon")
        val stemmekrets2 = saveStemmekrets(id = "S2", lokalid = Lokalid("S2"),
            stemmekretsnummer = "21", stemmekretsnavn = "Østre stemmekrets", gyldigFra = gyldigFra, grenser = lagredeGrenserForOest, informasjon = "Informasjon")

        val kommune1 = saveKommuneOgUnikLokalid(
            id = "idKommuneVest", lokalid = "kommuneVest", gyldigFra = gyldigFra,
            kommunenummerUuid = kommunenummer1, kommunenummer = kommunenummer1,
            grunnkretser = listOf(kretsVest), stemmekretser = listOf(stemmekrets1))
        val kommune2 = saveKommuneOgUnikLokalid(
            id = "idKommuneOest", lokalid = "kommuneOest", gyldigFra = gyldigFra,
            kommunenummerUuid = kommunenummer2, kommunenummer = kommunenummer2,
            grunnkretser = listOf(kretsOest), stemmekretser = listOf(stemmekrets2))

        grenseRepository.flush()
        flateService.oppdaterFlaterFor(setOf(kommune1, kommune2))

        val nasjon = saveNasjonOgUnikLokalid()
        if (fylkesnummer1 != fylkesnummer2) {
            val fylke1 = saveFylkeOgUnikLokalid(id = "fylke1", lokalid = "fylke1", fylkesnummer = fylkesnummer1, kommuner = listOf(kommune1))
            val fylke2 = saveFylkeOgUnikLokalid(id = "fylke2", lokalid = "fylke2", fylkesnummer = fylkesnummer2, kommuner = listOf(kommune2))
            nasjon.addFylke(fylke1)
            nasjon.addFylke(fylke2)
            flateService.oppdaterFlaterFor(setOf(fylke1, fylke2))
        } else {
            val fylke = saveFylkeOgUnikLokalid(kommuner = listOf(kommune1, kommune2))
            nasjon.addFylke(fylke)
        }

        grenseRepository.flush()
        return listOf(kretsVest, kretsOest)
    }

    fun utfoerGrenseendring(
        kretsSomSkalHaGrenseendring: Flate,
        opprinneligDato: LocalDate,
        endringsdato: LocalDate,
        featureAnnenOppdatertGrense: Feature? = null,
    ): Set<Flate> {
        val grenseKobling = kretsSomSkalHaGrenseendring.getGrensekoblinger(gyldighetsdato = opprinneligDato)
            .first { it.grense.lokalid == "grenseDeltLokalid" }

        val oppdatertGeometri = GeometryFactory().createLineString(arrayOf(
            Coordinate(10.0, 10.0),
            Coordinate(5.0, 5.0),
            Coordinate(10.0, 0.0)
        ))

        val oppdatertGrensefeature = grenseKobling.grense.grenseToFeature(opprinneligDato)
        oppdatertGeometri.geometryToDto().also { oppdatertGrensefeature.geometry = it }

        val features = if (featureAnnenOppdatertGrense != null) listOf(oppdatertGrensefeature, featureAnnenOppdatertGrense) else listOf(oppdatertGrensefeature)
        return grenseService.lagreGrenser(
            grenser = FeatureCollection(features = features),
            gyldighetsdato = endringsdato,
            endringstype = Endringstype.VEDTATT_GRENSEJUSTERING
        )
    }

    fun utfoerMetadataoppdatering(kretsSomSkalOppdateres: Grunnkrets, datoForOppdatering: LocalDate): Grunnkrets? {
        val grunnkretsRequest = GrunnkretsRequestBuilder().withLokalId(kretsSomSkalOppdateres.lokalid)
            .withGrunnkretsnummer(kretsSomSkalOppdateres.grunnkretsnummer!!).build()
        val identifikator = Identifikator(Lokalid(grunnkretsRequest.identifikasjon.lokalid), datoForOppdatering)

        grunnkretsService.oppdaterGrunnkrets(
            lokalId = identifikator.lokalid,
            oppdatertGrunnkrets = grunnkretsRequest,
            gyldigFra = datoForOppdatering,
            endringstype = Endringstype.RETTING
        )
        return grunnkretsService.hentGrunnkrets(identifikator)
    }

    fun assertDeltGrenseEtterGrenseendring(
        @Suppress("SameParameterValue") deltGrenseLokalid: String, datoForGrenseendring: LocalDate, opprinneligGrense: Grense,
        vestreKretsEtterLagring: Grunnkrets, oestreKretsEtterLagring: Grunnkrets, opprinneligVestreKrets: Grunnkrets, opprinneligOestreKrets: Grunnkrets)
        : Grense {

        val deltGrenseEtterGrenseendring = grenseService.hentGrense(Identifikator(Lokalid(deltGrenseLokalid), datoForGrenseendring))!!
        assertThat(deltGrenseEtterGrenseendring.grunnkretser).hasSize(2)
        assertThat(deltGrenseEtterGrenseendring.grunnkretser.map { it.grunnkrets.id })
            .containsExactlyInAnyOrder(vestreKretsEtterLagring.id, oestreKretsEtterLagring.id)

        assertThat(opprinneligGrense.gyldighet.gyldigTil).isEqualTo(datoForGrenseendring)
        assertThat(deltGrenseEtterGrenseendring.gyldighet.gyldigTil).isNull()
        assertThat(opprinneligGrense.lokalid).isEqualTo(deltGrenseEtterGrenseendring.lokalid)
        assertThat(opprinneligGrense.grense).isNotEqualTo(deltGrenseEtterGrenseendring.grense)
        assertThat(deltGrenseEtterGrenseendring.version).isGreaterThan(opprinneligGrense.version)

        assertThat(opprinneligVestreKrets.grenser.map { it.grense.id }).contains("grenseDelt")
        assertThat(opprinneligOestreKrets.grenser.map { it.grense.id }).contains("grenseDelt")
        assertThat(vestreKretsEtterLagring.grenser.map { it.grense.id }).doesNotContain("grenseDelt")
        assertThat(oestreKretsEtterLagring.grenser.map { it.grense.id }).doesNotContain("grenseDelt")
        assertThat(vestreKretsEtterLagring.grenser.map { it.grense.lokalid }).contains(deltGrenseLokalid)
        assertThat(oestreKretsEtterLagring.grenser.map { it.grense.lokalid }).contains(deltGrenseLokalid)

        return deltGrenseEtterGrenseendring
    }

    fun saveNaboStemmekretser(gyldigFra: LocalDate, medHull: Boolean = false, medTredjeKrets: Boolean = false, medToDelteGrenser: Boolean = false)
        : List<Stemmekrets> {
        val grenserForHull = if (medHull) {
            GrenseHelper.getHullVestligKrets(gyldigFra, Grensetype.Stemmekretsgrense)
        } else {
            emptyList()
        }
        val grenserForVest = GrenseHelper.getGrenserForVestligKrets(gyldigFra, Grensetype.Stemmekretsgrense, medToDelteGrenser)

        //Lagrer grensene først, for å være sikker på at de er tilgjengelige for å knyttes til kretsene
        val lagredeGrenserForHull = if (medHull) grenseRepository.saveAll(grenserForHull) else emptyList()
        val lagredeGrenserForVest = grenseRepository.saveAll(grenserForVest)
        val lagredeGrenserForOest = grenseRepository.saveAll(GrenseHelper.getGrenserForOestligKrets(gyldigFra, Grensetype.Stemmekretsgrense, medToDelteGrenser))
        grenseRepository.flush()

        val kretsVest = saveStemmekrets(
            id = "1",
            lokalid = Lokalid("123"),
            stemmekretsnummer = "20",
            stemmekretsnavn = "Vestre krets",
            gyldigFra = gyldigFra,
            grenser = lagredeGrenserForVest + lagredeGrenserForHull,
            informasjon = "Informasjon opprinnelig"
        )

        val kretsOest = saveStemmekrets(
            id = "2",
            lokalid = Lokalid("234"),
            stemmekretsnummer = "21",
            stemmekretsnavn = "Østre krets",
            gyldigFra = gyldigFra,
            grenser = lagredeGrenserForOest,
            informasjon = "Informasjon opprinnelig"
        )

        val stemmekretsMutableList = mutableListOf(kretsVest, kretsOest)
        if (medTredjeKrets) {
            val tredjeKrets = saveStemmekrets(
                id = "3",
                lokalid = Lokalid("345"),
                stemmekretsnummer = "22",
                stemmekretsnavn = "Tredje krets",
                gyldigFra = gyldigFra,
                grenser = lagredeGrenserForHull,
                informasjon = "Informasjon opprinnelig"
            )
            stemmekretsMutableList.add(tredjeKrets)
        }

        val stemmekretser = stemmekretsMutableList.toList()
        saveKommuneOgUnikLokalid(stemmekretser = stemmekretser, gyldigFra = gyldigFra)
        grenseRepository.flush()
        return stemmekretser
    }

    fun lagNyGrense(id: String, grensetype: Grensetype, lokalId: String, gyldigFra: LocalDate, coordinates: List<Coordinate>): Grense {
        return Grense(id = id, lokalid = lokalId, grensetype = grensetype)
            .apply {
                setGyldigFra(gyldigFra)
                grense = GrenseHelper.geometryFactory.createLineString(
                    coordinates.toTypedArray()
                )
                kvalitet = PosisjonskvalitetEmbeddable(
                    maalemetode = MaalemetodeKodeEnum.TERRENGMAALT.uuid,
                    noeyaktighet = null
                )
            }
    }

    fun lagUtkastMedDelingAvGrunnkretsOgGrenseendringer(
        tredjeKrets: Grunnkrets,
        vestreKrets: Grunnkrets,
        kretsNummer: String,
        kretsNavn: String,
        gyldighetsdato: LocalDate,
        flatetype: Flatetype,
        kontekstEnhetType: KontekstEnhetType,
        grensetype: Grensetype
    ): Utkast {
        val kommuneLokalid = vestreKrets.kommuneLokalid.lokalid
        val omkringliggendeKretsLokalid = vestreKrets.lokalid
        val omkringliggendeKretsVersion = vestreKrets.version
        val omkringliggendeKretsKretsnummer = vestreKrets.grunnkretsnummer!!
        val kretsSomDelesLokalid = tredjeKrets.lokalid
        val kretsSomDelesKretsnummer = tredjeKrets.grunnkretsnummer!!
        val kretsSomDelesVersion = tredjeKrets.version
        val grense0SomSkalEndreTilhoerighet = tredjeKrets.grenser.find { it.grense.lokalid == "hullGrense0lokalid" }!!.grense
        val grense1SomSkalEndreTilhoerighet = tredjeKrets.grenser.find { it.grense.lokalid == "hullGrense1lokalid" }!!.grense

        val utkast = lagUtkastMedDelingAvKretsOgNyGrense(omkringliggendeKretsLokalid, omkringliggendeKretsVersion, omkringliggendeKretsKretsnummer,
            kommuneLokalid, gyldighetsdato, kretsNavn, kretsNummer, kretsSomDelesLokalid, kretsSomDelesKretsnummer, kretsSomDelesVersion,
            grense0SomSkalEndreTilhoerighet, grense1SomSkalEndreTilhoerighet, kontekstEnhetType, flatetype, grensetype)
        return utkast
    }

    fun lagUtkastMedDelingAvStemmekretsOgGrenseendringer(
        tredjeKrets: Stemmekrets,
        vestreKrets: Stemmekrets,
        kretsNummer: String,
        kretsNavn: String,
        gyldighetsdato: LocalDate,
        flatetype: Flatetype,
        kontekstEnhetType: KontekstEnhetType,
        grensetype: Grensetype
    ): Utkast {
        val kommuneLokalid = vestreKrets.kommuneLokalid.lokalid
        val omkringliggendeKretsLokalid = vestreKrets.lokalid
        val omkringliggendeKretsVersion = vestreKrets.version
        val omkringliggendeKretsKretsnummer = vestreKrets.stemmekretsnummer!!
        val kretsSomDelesLokalid = tredjeKrets.lokalid
        val kretsSomDelesKretsnummer = tredjeKrets.stemmekretsnummer!!
        val kretsSomDelesVersion = tredjeKrets.version
        val grense0SomSkalEndreTilhoerighet = tredjeKrets.grenser.find { it.grense.lokalid == "hullGrense0lokalid" }!!.grense
        val grense1SomSkalEndreTilhoerighet = tredjeKrets.grenser.find { it.grense.lokalid == "hullGrense1lokalid" }!!.grense

        val utkast = lagUtkastMedDelingAvKretsOgNyGrense(omkringliggendeKretsLokalid, omkringliggendeKretsVersion, omkringliggendeKretsKretsnummer,
            kommuneLokalid, gyldighetsdato, kretsNavn, kretsNummer, kretsSomDelesLokalid, kretsSomDelesKretsnummer, kretsSomDelesVersion,
            grense0SomSkalEndreTilhoerighet, grense1SomSkalEndreTilhoerighet, kontekstEnhetType, flatetype, grensetype)
        return utkast
    }

    fun lagUtkastMedDelingAvKretsOgNyGrense(
        omkringliggendeKretsLokalid: String,
        omkringliggendeKretsVersion: Int,
        omkringliggendeKretsKretsnummer: String,
        kommuneLokalid: String,
        gyldighetsdato: LocalDate,
        kretsNavn: String,
        kretsNummer: String,
        kretsSomDelesLokalid: String,
        kretsSomDelesKretsnummer: String,
        kretsSomDelesVersion: Int,
        grense0SomSkalEndreTilhoerighet: Grense,
        grense1SomSkalEndreTilhoerighet: Grense,
        kontekstEnhetType: KontekstEnhetType,
        flatetype: Flatetype,
        grensetype: Grensetype
    ): Utkast {
        val delingEndringRequest = KretsDelingEndringRequest(
            IdentifikatorMedVersjon(omkringliggendeKretsLokalid, omkringliggendeKretsVersion),
            ObjektIdentifikator(Lokalid(kommuneLokalid), gyldighetsdato),
            flatetype,
            nyeKretser = listOf(KretsNavnOgNummer(kretsNavn, kretsNummer))
        )
        val tilhorighetTilGrense0 = listOf(
            KontekstEgenskaper(
                id = null,
                kommuneId = ObjektIdentifikator(Lokalid(kommuneLokalid), gyldighetsdato),
                kretsNummer = kretsNummer,
                type = kontekstEnhetType,
                version = 0
            ),
            KontekstEgenskaper(
                id = ObjektIdentifikator(Lokalid(omkringliggendeKretsLokalid), gyldighetsdato),
                kommuneId = ObjektIdentifikator(Lokalid(kommuneLokalid), gyldighetsdato),
                kretsNummer = omkringliggendeKretsKretsnummer,
                type = kontekstEnhetType,
                version = omkringliggendeKretsVersion
            )
        )
        val tilhorighetTilGrense1 = listOf(
            KontekstEgenskaper(
                id = null,
                kommuneId = ObjektIdentifikator(Lokalid(kommuneLokalid), gyldighetsdato),
                kretsNummer = kretsNummer,
                type = kontekstEnhetType,
                version = 0
            ),
            KontekstEgenskaper(
                id = ObjektIdentifikator(Lokalid(omkringliggendeKretsLokalid), gyldighetsdato),
                kommuneId = ObjektIdentifikator(Lokalid(kommuneLokalid), gyldighetsdato),
                kretsNummer = omkringliggendeKretsKretsnummer,
                type = kontekstEnhetType,
                version = omkringliggendeKretsVersion
            )
        )
        val tilhorighetTilNyGrense = listOf(
            KontekstEgenskaper(
                id = null,
                kommuneId = ObjektIdentifikator(Lokalid(kommuneLokalid), gyldighetsdato),
                kretsNummer = kretsNummer,
                type = kontekstEnhetType,
                version = 0
            ),
            KontekstEgenskaper(
                id = ObjektIdentifikator(Lokalid(kretsSomDelesLokalid), gyldighetsdato),
                kommuneId = ObjektIdentifikator(Lokalid(kommuneLokalid), gyldighetsdato),
                kretsNummer = kretsSomDelesKretsnummer,
                type = kontekstEnhetType,
                version = kretsSomDelesVersion
            )
        )
        val grense0MedEndretTilhorighet = FeatureBuilder(id = grense0SomSkalEndreTilhoerighet.lokalid).copy(grense0SomSkalEndreTilhoerighet.grenseToFeature())
            .withKontekstEgenskaper(tilhorighetTilGrense0)
            .withVersion(grense0SomSkalEndreTilhoerighet.version)
            .build()
        val grense1MedEndretTilhorighet = FeatureBuilder(id = grense1SomSkalEndreTilhoerighet.lokalid).copy(grense1SomSkalEndreTilhoerighet.grenseToFeature())
            .withKontekstEgenskaper(tilhorighetTilGrense1)
            .withVersion(grense1SomSkalEndreTilhoerighet.version)
            .build()

        val gyldigNyGrense = lagNyGrense(
            "NotARealID",
            grensetype,
            "NotARealID",
            gyldighetsdato,
            listOf(Coordinate(7.0, 7.0), Coordinate(2.0, 2.0)))

        val feature = gyldigNyGrense.grenseToFeature(gyldighetsdato, nyGrense = true)

        val nyGrense = FeatureBuilder(id = null).copy(feature)
            .withKontekstEgenskaper(tilhorighetTilNyGrense)
            .withVersion(1)
            .build()

        val utkast = UtkastBuilder.builder("Test av deling", gyldigFra = gyldighetsdato, endringstype = Endringstype.VEDTATT_DELING)
            .withOperasjoner(Operasjoner(kretsDelingEndringer = listOf(delingEndringRequest),
                grenseendringer = Grenseendringer(listOf(grense0MedEndretTilhorighet, grense1MedEndretTilhorighet, nyGrense))))
            .build()

        return utkastRepository.save(utkast)
    }

    fun lagFeatureEndreYkoordinat(kretsSomSkalHaGrenseendring: Flate, opprinneligDato: LocalDate, lokalidGrenseSomSkalEndres: String): Feature {
        val grenseKobling = kretsSomSkalHaGrenseendring.getGrensekoblinger(gyldighetsdato = opprinneligDato)
            .first { it.grense.lokalid == lokalidGrenseSomSkalEndres }
        val grense = grenseKobling.grense.grense
        val oppdatertGeometri = GeometryFactory().createLineString(arrayOf(
            grense.getCoordinateN(0).copy(),
            grense.getCoordinateN(1).copy().also { it.setY(it.getY() + 4) },
            grense.getCoordinateN(2).copy()
        ))

        val oppdatertGrensefeature = grenseKobling.grense.grenseToFeature(opprinneligDato)
        oppdatertGeometri.geometryToDto().also { oppdatertGrensefeature.geometry = it }
        return oppdatertGrensefeature
    }

}

fun assertGrenser(grensekoblinger: List<GrenseKobling>,
                  grenseIGrensekoblingId: String,
                  enhetKobletTilGrenseLokalid: Lokalid) {
    assertThat(grensekoblinger).hasSize(1)
    val grensekobling = grensekoblinger[0]
    val grenseIGrensekobling = grensekobling.grense
    assertThat(grenseIGrensekobling.id).isEqualTo(grenseIGrensekoblingId)
    assertThat(grensekobling.getEnhetKobletTilGrense().lokalid).isEqualTo(enhetKobletTilGrenseLokalid.value)
}
