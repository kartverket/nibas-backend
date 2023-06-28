package no.kartverket.nibas.testcontainer

import no.kartverket.nibas.builders.FylkeBuilder
import no.kartverket.nibas.builders.GrenseBuilder
import no.kartverket.nibas.builders.GrunnkretsBuilder
import no.kartverket.nibas.builders.KommuneBuilder
import no.kartverket.nibas.builders.NasjonBuilder
import no.kartverket.nibas.builders.StemmekretsBuilder
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.domain.Fylke
import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.domain.Grunnkrets
import no.kartverket.nibas.domain.Kommune
import no.kartverket.nibas.domain.Stemmekrets
import no.kartverket.nibas.repository.FylkeRepository
import no.kartverket.nibas.repository.GrenseRepository
import no.kartverket.nibas.repository.GrunnkretsRepository
import no.kartverket.nibas.repository.KommuneRepository
import no.kartverket.nibas.repository.NasjonRepository
import no.kartverket.nibas.repository.StemmekretsRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import java.time.LocalDate
import java.time.Month

@SpringBootTest(webEnvironment = WebEnvironment.MOCK)
class TestWithDbContainer {

    @Autowired
    private lateinit var nasjonRepository: NasjonRepository

    @Autowired
    private lateinit var fylkeRepository: FylkeRepository

    @Autowired
    private lateinit var kommuneRepository: KommuneRepository

    @Autowired
    private lateinit var grunnkretsRepository: GrunnkretsRepository

    @Autowired
    private lateinit var stemmekretsRepository: StemmekretsRepository

    @Autowired
    private lateinit var grenseRepository: GrenseRepository

    companion object dbContainer : PostgreSQLContainer<dbContainer>(
        DockerImageName.parse("postgis/postgis:14-3.2-alpine").asCompatibleSubstituteFor("postgres")
    ) {
        init {
            withDatabaseName("nibas")
            withUsername("nibas")
            withPassword("nibas")
            withCreateContainerCmdModifier { cmd -> cmd.withName("nibas-test-container") }
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
                   lokalid: Lokalid = Lokalid("nasjon-fb07-4277-ad95-63c32c1370d0"),
                   gyldigFra: LocalDate = LocalDate.of(2022, Month.OCTOBER, 1),
                   gyldigTil: LocalDate? = null,
                   fylker: List<Fylke> = emptyList(),
                   opphav: String = "Eidsvoll") {

        val nasjon = NasjonBuilder(
            id = id,
            lokalId = lokalid,
            fylker = fylker,
            gyldigFra = gyldigFra,
            gyldigTil = gyldigTil,
            opphav = opphav)
            .buildWithoutAdministrativenhetnavn()

        nasjonRepository.save(nasjon)
    }

    fun saveFylke(id: String = "fylkeEnKommune-9000",
                  lokalid: Lokalid = Lokalid("fylkeEnKommune-fb07-4277-ad95-63c32c1370d0"),
                  fylkesnummer: String = "89",
                  gyldigFra: LocalDate = LocalDate.of(2022, Month.OCTOBER, 1),
                  gyldigTil: LocalDate? = null,
                  kommune: Kommune) {

        val fylke = FylkeBuilder(
            id = id,
            lokalId = lokalid,
            gyldigFra = gyldigFra,
            gyldigTil = gyldigTil,
            kommuner = listOf(kommune),
            fylkesnummer = fylkesnummer)
            .buildWithoutAdministrativenhetnavn()

        fylkeRepository.save(fylke)
    }

    fun saveFylke(id: String = "fylkeFlereKommuner-9000",
                  lokalid: Lokalid = Lokalid("fylkeFlereKommuner-fb07-4277-ad95-63c32c1370d0"),
                  fylkesnummer: String = "89",
                  gyldigFra: LocalDate = LocalDate.of(2022, Month.OCTOBER, 1),
                  gyldigTil: LocalDate? = null,
                  kommuner: List<Kommune> = emptyList()): Fylke {

        val fylke = FylkeBuilder(
            id = id,
            lokalId = lokalid,
            gyldigFra = gyldigFra,
            gyldigTil = gyldigTil,
            kommuner = kommuner,
            fylkesnummer = fylkesnummer)
            .buildWithoutAdministrativenhetnavn()

        return fylkeRepository.save(fylke)
    }

    // Kommunenummer er egentlig en uuid, forenklet med en firesifret verdi
    fun saveKommune(
        id: String = "kommune-9000",
        lokalid: Lokalid = Lokalid("abc-123"),
        kommunenummer: String = "0301",
        gyldigFra: LocalDate = LocalDate.of(2022, Month.OCTOBER, 1),
        gyldigTil: LocalDate? = null,
        grunnkretser: List<Grunnkrets> = emptyList(),
        stemmekretser: List<Stemmekrets> = emptyList()): Kommune {

        val kommune = KommuneBuilder(
            id = id,
            lokalId = lokalid,
            gyldigFra = gyldigFra,
            gyldigTil = gyldigTil,
            kommunenummer = kommunenummer,
            grunnkretser = grunnkretser,
            stemmekretser = stemmekretser
        ).buildWithoutAdministrativenhetnavn()

        return kommuneRepository.save(kommune)
    }

    fun saveGrunnkrets(id: String = "grunnkrets-9000",
                       lokalid: Lokalid = Lokalid("1001"),
                       grunnkretsnummer: String = "42270304",
                       grunnkretsnavn: String = "Dåsvatn",
                       kommune: Kommune = KommuneBuilder.DEFAULT_KOMMUNE,
                       gyldigFra: LocalDate = LocalDate.of(2022, Month.OCTOBER, 1),
                       gyldigTil: LocalDate? = null,
                       grenser: List<Grense> = emptyList()): Grunnkrets {

        val grunnkrets = GrunnkretsBuilder(
            id = id,
            lokalId = lokalid,
            gyldigFra = gyldigFra,
            gyldigTil = gyldigTil,
            grunnkretsnummer = grunnkretsnummer,
            grunnkretsnavn = grunnkretsnavn,
            kommune = kommune,
            grenser = grenser)
            .build()

        return grunnkretsRepository.save(grunnkrets)
    }

    fun saveStemmekrets(
        id: String = "stemmekrets-9000",
        lokalid: Lokalid,
        stemmekretsnummer: String = "9",
        stemmekretsnavn: String = "Øvre Singsaker",
        gyldigFra: LocalDate = LocalDate.of(2022, Month.OCTOBER, 1),
        gyldigTil: LocalDate? = null,
        grenser: List<Grense> = emptyList()
    ): Stemmekrets {

        val stemmekrets = StemmekretsBuilder(
            id = id,
            lokalId = lokalid,
            gyldigFra = gyldigFra,
            gyldigTil = gyldigTil,
            stemmekretsnummer = stemmekretsnummer,
            stemmekretsnavn = stemmekretsnavn,
            kommune = null,
            grenser = grenser)
            .build()

        return stemmekretsRepository.save(stemmekrets)
    }

    fun saveGrenseAvTypeKommunegrense(id: String = "kommunegrense-id",
                                      lokalid: Lokalid = Lokalid("kommunegrense-fb07-4277-ad95-63c32c1370d0"),
                                      gyldigFra: LocalDate = LocalDate.of(2022, Month.OCTOBER, 1),
                                      gyldigTil: LocalDate? = null,
                                      opphav: String = "Eidsvoll",
                                      nesteVersjon: Grense? = null): Grense {
        return grenseRepository.save(GrenseBuilder(
            id = id,
            lokalId = lokalid,
            gyldigFra = gyldigFra,
            gyldigTil = gyldigTil,
            opphav = opphav,
            nesteVersjon = nesteVersjon
        ).buildKommunegrense())
    }

    fun saveGrenseAvTypeRiksgrense(id: String = "riksgrense-id",
                                   lokalid: Lokalid = Lokalid("riksgrense-fb07-4277-ad95-63c32c1370d0"),
                                   gyldigFra: LocalDate = LocalDate.of(2022, Month.OCTOBER, 1),
                                   gyldigTil: LocalDate? = null,
                                   opphav: String = "Eidsvoll",
                                   nesteVersjon: Grense? = null): Grense {
        return grenseRepository.save(GrenseBuilder(
            id = id,
            lokalId = lokalid,
            gyldigFra = gyldigFra,
            gyldigTil = gyldigTil,
            opphav = opphav,
            nesteVersjon = nesteVersjon
        ).buildRiksgrense())
    }

}
