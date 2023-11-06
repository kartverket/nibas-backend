package no.kartverket.nibas.testcontainer

import no.kartverket.nibas.api.v1.common.Flatetype
import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.builders.FylkeBuilder
import no.kartverket.nibas.builders.GrenseBuilder
import no.kartverket.nibas.builders.GrunnkretsBuilder
import no.kartverket.nibas.builders.KommuneBuilder
import no.kartverket.nibas.builders.NasjonBuilder
import no.kartverket.nibas.builders.StemmekretsBuilder
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.domain.Administrativenhetnavn
import no.kartverket.nibas.domain.Fylke
import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.domain.Grunnkrets
import no.kartverket.nibas.domain.Kommune
import no.kartverket.nibas.domain.Nasjon
import no.kartverket.nibas.domain.Spraakkode
import no.kartverket.nibas.domain.Stemmekrets
import no.kartverket.nibas.domain.UnikLokalid
import no.kartverket.nibas.repository.AdministrativenhetnavnRepository
import no.kartverket.nibas.repository.FylkeRepository
import no.kartverket.nibas.repository.GrenseRepository
import no.kartverket.nibas.repository.GrunnkretsRepository
import no.kartverket.nibas.repository.KommuneRepository
import no.kartverket.nibas.repository.NasjonRepository
import no.kartverket.nibas.repository.StemmekretsRepository
import no.kartverket.nibas.repository.UnikLokalidRepository
import no.kartverket.nibas.sporing.Gyldighet
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import java.time.LocalDate
import java.time.Month
import java.util.*

@SpringBootTest(webEnvironment = WebEnvironment.MOCK)
class TestWithDbContainer {

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
    lateinit var grenseRepository: GrenseRepository

    @Autowired
    lateinit var administrativenhetnavnRepository: AdministrativenhetnavnRepository

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
                               fylkesnummer: String = "89",
                               gyldigFra: LocalDate = LocalDate.of(2022, Month.OCTOBER, 1),
                               gyldigTil: LocalDate? = null,
                               kommuner: List<Kommune> = emptyList(),
                               administrativenhetnavn: List<Administrativenhetnavn> = emptyList()): Fylke {

        val unikLokalid = saveUnikLokalid(lokalid, Flatetype.FYLKE)

        return saveFylke(
            id = id,
            unikLokalid = unikLokalid,
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
        kommunenummer: String = "0301",
        gyldigFra: LocalDate = LocalDate.of(2022, Month.OCTOBER, 1),
        gyldigTil: LocalDate? = null,
        administrativenhetnavn: List<Administrativenhetnavn> = emptyList(),
        grunnkretser: List<Grunnkrets> = emptyList(),
        stemmekretser: List<Stemmekrets> = emptyList()): Kommune {

        val kommune = KommuneBuilder(
            id = id,
            unikLokalid = unikLokalid,
            gyldigFra = gyldigFra,
            gyldigTil = gyldigTil,
            kommunenummer = kommunenummer
        ).buildUtenUnikLokalidOgTilkntninger()

        val save = kommuneRepository.save(kommune)
        save.addKommuneTilUnikLokalid(unikLokalid)

        grunnkretser.forEach { save.addGrunnkrets(it) }
        stemmekretser.forEach { save.addStemmekrets(it) }
        administrativenhetnavn.forEach { save.addAdministrativenhetNavn(it) }

        return save
    }

    fun saveKommuneOgUnikLokalid(
        id: String = "kommune-9000",
        lokalid: String = "abc-123",
        kommunenummer: String = "0301",
        gyldigFra: LocalDate = LocalDate.of(2022, Month.OCTOBER, 1),
        gyldigTil: LocalDate? = null,
        administrativenhetnavn: List<Administrativenhetnavn> = emptyList(),
        grunnkretser: List<Grunnkrets> = emptyList(),
        stemmekretser: List<Stemmekrets> = emptyList()
    ): Kommune {

        val unikLokalid = saveUnikLokalid(lokalid = lokalid, flatetype = Flatetype.KOMMUNE)

        return saveKommune(
            id = id,
            unikLokalid = unikLokalid,
            kommunenummer = kommunenummer,
            gyldigFra = gyldigFra,
            gyldigTil = gyldigTil,
            administrativenhetnavn = administrativenhetnavn,
            grunnkretser = grunnkretser,
            stemmekretser = stemmekretser
        )
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
            .buildUtenKommune()

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
        ).withGrensetype(Grensetype.Kommunegrense).build())
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
        ).withGrensetype(Grensetype.Riksgrense).build())
    }

}
