package no.kartverket.nibas.repository

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.kartverket.nibas.api.v1.common.AdministrativEnhetNavn
import no.kartverket.nibas.api.v1.request.KommuneRequest
import no.kartverket.nibas.builders.GrenseBuilder
import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.domain.Grunnkrets
import no.kartverket.nibas.domain.Spraakkode
import no.kartverket.nibas.domain.utkast.UtkastBuilder
import no.kartverket.nibas.domain.utkast.UtkastStatus
import no.kartverket.nibas.sporing.Gyldighet
import no.kartverket.nibas.sporing.UserFacade
import no.kartverket.nibas.testcontainer.TestWithDbContainer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.Month


@Transactional
class RepositoryTest : TestWithDbContainer() {

    @Autowired
    private lateinit var utkastRepository: UtkastRepository

    @Autowired
    private lateinit var spraakkodeRepository: SpraakkodeRepository

    // Later som nåværende dato er 8.10.2022
    private val currentDate = LocalDate.of(2022, Month.OCTOBER, 8)
    private val october1In2022 = LocalDate.of(2022, Month.OCTOBER, 1)
    private val january1In2023 = LocalDate.of(2023, Month.JANUARY, 1)
    private val january1In2024 = LocalDate.of(2024, Month.JANUARY, 1)

    @MockkBean
    private lateinit var userFacade: UserFacade

    @BeforeEach
    fun beforeEach() {
        every { userFacade.getAuthenticatedUser() } returns "Testbruker 123"
    }

    @Test
    fun saveUtkast() {
        val utkast = UtkastBuilder.builder("Utkast 1")
            .withKommuneRequests(kommuneRequests())
            .build()
        utkastRepository.save(utkast)
        assertThat(utkastRepository.findById(utkast.id).isPresent).isTrue
    }

    // Kan ikke gjøre equals-sjekk på set av ids fordi tidligere tester lagrer også ned Utkast som blir plukket opp i denne testen.
    // Altså ikke atomiske tester.
    @Test
    fun findAllUtkastByStatus() {
        val utkast = UtkastBuilder.builder("Utkast 1").withStatus(UtkastStatus.OPPRETTET).build()
        val utkast2 = UtkastBuilder.builder("Utkast 2").withStatus(UtkastStatus.OPPRETTET).build()
        val utkast3 = UtkastBuilder.builder("Utkast 3").withStatus(UtkastStatus.FORKASTET).build()
        utkastRepository.saveAll(listOf(utkast, utkast2, utkast3))

        val matchingUtkastIdsForStatusOpprettet = utkastRepository.findAllByStatus(UtkastStatus.OPPRETTET).map { it.id }.toSet()
        assertThat(matchingUtkastIdsForStatusOpprettet).contains(utkast.id, utkast2.id)
        assertThat(matchingUtkastIdsForStatusOpprettet).doesNotContain(utkast3.id)
    }

    @Test
    fun findAllGrunnkretserByLokalid() {
        val lokalid = Lokalid("123")
        val grunnkrets1 = createGrunnkrets(lokalid)
        val grunnkrets2MedSammeLokalid = createGrunnkrets(lokalid)
        grunnkretsRepository.saveAll(listOf(grunnkrets1, grunnkrets2MedSammeLokalid))

        val grunnkretsIdsFunnet = grunnkretsRepository.findAllByLokalid(grunnkrets1.lokalid).map { it.id }.toSet()
        val expectedGrunnkretsIds = setOf(grunnkrets1.id, grunnkrets2MedSammeLokalid.id)
        assertThat(grunnkretsIdsFunnet).isEqualTo(expectedGrunnkretsIds)
    }

    @Test
    fun findAllGrunnkretserByLokalidFramITid() {
        val lokalid = Lokalid("1001")
        val grunnkretsIds = saveGrunnkretser(lokalid)
        val identifikator = Identifikator(lokalid, currentDate)

        val idsOfActualGrunnkretserFunnet = grunnkretsRepository
            .findAllFramtidigeVersjonerByIdentifikator(identifikator)
            .map { it.id }

        val expectedResultIds = listOf(grunnkretsIds[1], grunnkretsIds[2])
        assertThat(idsOfActualGrunnkretserFunnet).isEqualTo(expectedResultIds)
    }

    @Test
    fun findGyldigGrunnkretsForTidspunkt() {
        val lokalid = Lokalid("1001")
        val grunnkretsIds = saveGrunnkretser(lokalid)
        assertThat(grunnkretsRepository.findGyldigObjektForIdentifikator(Identifikator(lokalid, currentDate))!!.id).isEqualTo(grunnkretsIds[0])
        assertThat(grunnkretsRepository.findGyldigObjektForIdentifikator(Identifikator(lokalid, january1In2023))!!.id).isEqualTo(grunnkretsIds[1])
        assertThat(grunnkretsRepository.findGyldigObjektForIdentifikator(Identifikator(lokalid, january1In2024))!!.id).isEqualTo(grunnkretsIds[2])
    }

    @Test
    fun findGrense() {
        grenseRepository.save(GrenseBuilder(id = "300").build())
        assertThat(grenseRepository.findById("300")).isNotNull
    }

    @Test
    fun saveSpraakkode() {
        val spraakkode = Spraakkode()
        spraakkode.id = "17"
        spraakkode.description = "norsk"
        spraakkodeRepository.save(spraakkode)
        assertThat(spraakkodeRepository.findById("17").isPresent).isTrue
    }

    @Test
    fun `test finn fylke med fylkesnummer`(){
        val fylkesnummer = "30"
        val fylker = fylkeRepository.findAllByFylkesnummer(fylkesnummer)
        assertThat(fylker).hasSize(2).extracting("id").contains("f2e85556-471b-4807-a1bf-6da7478e7cda", "ny-47f4ef26-f1c1-4bbf-9f1b-8e37e70cc6a1")

        var fylke = fylkeRepository.findGyldigFylkeForFylkesnummer(fylkesnummer, january1In2023)!!
        assertThat(fylke.id).isEqualTo("f2e85556-471b-4807-a1bf-6da7478e7cda")

        fylke = fylkeRepository.findGyldigFylkeForFylkesnummer(fylkesnummer, october1In2022)!!
        assertThat(fylke.id).isEqualTo("ny-47f4ef26-f1c1-4bbf-9f1b-8e37e70cc6a1")
    }

    @Test
    fun `test finn kommune med kommunenummer`(){
        val kommunenummer = "3032"
        val kommuner = kommuneRepository.findAllByKommunenummer(kommunenummer)
        assertThat(kommuner).hasSize(2).extracting("id").contains("e24e9dcf-4ae9-475c-afc2-1a5c49f166e4", "ny-e24e9dcf-4ae9-475c-afc2-1a5c49f166e4")

        var kommune = kommuneRepository.findGyldigKommuneForKommunenummer(kommunenummer, january1In2023)!!
        assertThat(kommune.id).isEqualTo("e24e9dcf-4ae9-475c-afc2-1a5c49f166e4")

        kommune = kommuneRepository.findGyldigKommuneForKommunenummer(kommunenummer, october1In2022)!!
        assertThat(kommune.id).isEqualTo("ny-e24e9dcf-4ae9-475c-afc2-1a5c49f166e4")
    }

    private fun createGrunnkrets(lokalid: Lokalid,
                                 gyldigFra: LocalDate = LocalDate.of(2023, Month.JANUARY, 1),
                                 gyldigTil: LocalDate? = null): Grunnkrets {
        return Grunnkrets(lokalid = lokalid.value)
            .apply {
                grunnkretsnummer = "1234"
                grunnkretsnavn = "Basvatn"
                gyldighet = Gyldighet(gyldigFra = gyldigFra, gyldigTil = gyldigTil)
            }
    }

    private fun saveGrunnkretser(lokalid: Lokalid): List<String> {
        // Nåværende gyldig rad
        val grunnkrets1 = createGrunnkrets(lokalid, gyldigFra = october1In2022, gyldigTil = january1In2023)
        // Framtidig rad 1
        val grunnkrets2 = createGrunnkrets(lokalid, gyldigFra = january1In2023, gyldigTil = january1In2024)
        // Framtidig rad 2
        val grunnkrets3 = createGrunnkrets(lokalid, gyldigFra = january1In2024)
        val grunnkretsList = listOf(grunnkrets1, grunnkrets2, grunnkrets3)
        grunnkretsRepository.saveAll(grunnkretsList)
        return grunnkretsList.map { it.id }
    }

    private fun kommuneRequests(): Map<String, KommuneRequest> {
        val navn = "Oslo"
        val spraak = "NOR"
        val lokalId = "123"
        val kommunenummer = "123"
        val samiskforvaltningsomraade = false
        return mapOf("UUID-1" to KommuneRequest(
            administrativenhetnavn = listOf(AdministrativEnhetNavn(navn, spraak, version = 1)),
            lokalid = lokalId,
            kommunenummerId = kommunenummer,
            samiskforvaltningsomraade = samiskforvaltningsomraade,
            version = 1
        ))
    }
}

