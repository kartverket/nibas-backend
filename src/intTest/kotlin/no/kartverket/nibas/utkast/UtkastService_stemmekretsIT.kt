package no.kartverket.nibas.utkast

import no.kartverket.nibas.api.v1.common.geojson.metadata.Identifikasjon
import no.kartverket.nibas.api.v1.request.ConflictResolved
import no.kartverket.nibas.api.v1.request.StemmekretsRequest
import no.kartverket.nibas.api.v1.request.StemmekretsRequestResolved
import no.kartverket.nibas.common.CurrentTime
import no.kartverket.nibas.common.FeilVedOppdatering
import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.common.ResponseException
import no.kartverket.nibas.domain.Endringstype
import no.kartverket.nibas.domain.Stemmekrets
import no.kartverket.nibas.domain.utkast.UtkastBuilder
import no.kartverket.nibas.domain.utkast.UtkastStatus
import no.kartverket.nibas.repository.StemmekretsRepository
import no.kartverket.nibas.repository.UtkastRepository
import no.kartverket.nibas.sporing.AuditInfo
import no.kartverket.nibas.sporing.Gyldighet
import no.kartverket.nibas.sporing.UserFacade
import no.kartverket.nibas.testcontainer.TestWithDbContainer
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month
import java.time.ZoneId
import java.time.ZonedDateTime

@Transactional
class UtkastService_stemmekretsIT : TestWithDbContainer() {

    @MockBean
    private lateinit var userFacade: UserFacade

    @MockBean
    private lateinit var currentTime: CurrentTime

    @Autowired
    private lateinit var utkastService: UtkastService
    @Autowired
    private lateinit var utkastRepository: UtkastRepository

    @Autowired
    private lateinit var stemmekretsRepository: StemmekretsRepository

    // ID for Kommune med kommunenummerId som matcher i Geonorge og vår cache
    private val kommuneIdGeonorge = "e24e9dcf-4ae9-475c-afc2-1a5c49f166e4"

    private val moi = "Moi"
    private val voi = "Voi"

    private val lokalidOfStemmekrets = "e5ff0e76-ab1b-4e25-83bf-66e1bd2e6946"
    private val opprinneligStemmekretsnr = "5"
    private val stemmekretsnr1234 = "1234"
    private val stemmekretsnr4567 = "4567"


    private val october23In2022 = ZonedDateTime.of(LocalDateTime.of(2022, Month.OCTOBER, 23, 12, 0), ZoneId.systemDefault())
    private val october24In2022 = ZonedDateTime.of(LocalDateTime.of(2022, Month.OCTOBER, 24, 12, 0), ZoneId.systemDefault())
    private val november25In2022 = LocalDate.of(2022, Month.NOVEMBER, 25)
    private val january1In2023 = LocalDate.of(2023, Month.JANUARY, 1)
    private val july1In2023 = LocalDate.of(2023, Month.JULY, 1)
    private val january1In2024 = LocalDate.of(2024, Month.JANUARY, 1)
    private val february1In2024 = LocalDate.of(2024, Month.FEBRUARY, 1)


    @BeforeEach
    fun beforeEach() {
        Mockito.`when`(currentTime.asLocalDate()).thenReturn(LocalDate.of(2022, 6, 1))
        Mockito.`when`(userFacade.getAuthenticatedUser()).thenReturn("Testbruker 123")
    }

    @Test
    fun publiserUtkast01MedNyttStemmekretsnummer() {
        assert1StemmekretsPresent()

        // 1. Endrer stemmekretsnummer
        nyttStemmekretsnummerJan23()
    }

    @Test
    fun publiserUtkast02NummerPlussNavn() {
        assert1StemmekretsPresent()

        // 1. Endrer stemmekretsnummer
        nyttStemmekretsnummerJan23()

        // 2. Endrer stemmekretsnavn
        nyttStemmekretsnavnJan24()
    }

    @Test
    fun publiserUtkast03FinnesRaderMedNyereGyldigFra() {
        assert1StemmekretsPresent()

        // 1. Endrer stemmekretsnummer
        nyttStemmekretsnummerJan23()

        // 2. Endrer stemmekretsnavn
        nyttStemmekretsnavnJan24()

        // 3. Forsøker å endre stemmekretsnummer med gyldigFra 1.7.23 (selv om vi allerede har publisert endring som gjelder fra 1.1.24)
        // Bruker her version 2 fordi den skal være basert på tilstanden i databasen 1.7.23
        nyttStemmekretsNummerFraJuli23SkalGiFeil()
    }

    private fun nyttStemmekretsNummerFraJuli23SkalGiFeil(): ResultatFraForsoekPaaPublisering {

        val stemmekretsRequest = createStemmekretsRequest(
            stemmekretsNavn = moi,
            stemmekretsNr = stemmekretsnr4567,
            version = 2)

        val utkastId = saveStemmekretsUtkast(
            navn = "Jeg vil endre stemmekretsnr til 4589 fra 1.7.23",
            oppdateringsdato = october24In2022,
            gyldigFra = july1In2023,
            stemmekretsRequest
        )

        // Kall til publiser skal feile.
        val oppdateringException = Assertions.catchThrowableOfType({
            publiser(utkastId, july1In2023)
        }, FeilVedOppdatering.OppdateringException::class.java)

        Assertions.assertThat(oppdateringException).isNotNull
        val oppdateringInfo = oppdateringException.oppdateringInfo
        val lokalid = Lokalid(lokalidOfStemmekrets)
        Assertions.assertThat(oppdateringInfo.id).isEqualTo(Identifikator(lokalid, july1In2023))
        Assertions.assertThat(oppdateringInfo.type).isEqualTo(FeilVedOppdatering.Type.STEMMEKRETS)
        Assertions.assertThat(oppdateringInfo.affectedIds.toSet()).isEqualTo(setOf(Identifikator(lokalid, january1In2024)))

        return ResultatFraForsoekPaaPublisering(utkastId)
    }

    @Test
    fun publiserUtkast04ResolverKonfliktPaaAlleredePubliserteFramtidigeEndringer() {
        assert1StemmekretsPresent()

        // 1. Endrer stemmekretsnummer
        nyttStemmekretsnummerJan23()

        // 2. Endrer stemmekretsnavn
        nyttStemmekretsnavnJan24()

        // 3. Forsøker å endre stemmekretsnr til 4589 med gyldigFra 1.7.23 (selv om vi allerede har publisert endring som gjelder fra 1.1.24)
        // (Bruker her version 2 fordi den vil være basert på tilstanden i databasen 1.7.23)
        val resultatFraForsoekPaaPublisering = nyttStemmekretsNummerFraJuli23SkalGiFeil()
        val utkastId = resultatFraForsoekPaaPublisering.idForUtkastSomBleOpprettet

        // Utkastet som feilet publisering står som opprettet
        assertUtkastIsOpprettet(utkastId)

        // 4. Sender inn struktur som skal fikse konflikt som oppsto i 3.
        // En rad gjelder fra 1.7.23
        // En rad gjelder fra 1.1.24
        utkastService.resolveConflictForFramtidigeVersjoner(
            utkastId = utkastId,
            conflictResolved = ConflictResolved(
                lokalid = Lokalid(lokalidOfStemmekrets),
                stemmekretsRequests = listOf(
                    // Gjentar endring av stemmekretsnr til 4589 fra 1.7.23
                    StemmekretsRequestResolved(
                        gyldigFra = july1In2023,
                        endringstype = Endringstype.KVALITETSHEVING.value,
                        stemmekretsRequest = createStemmekretsRequest(moi, stemmekretsnr4567, 2)),
                    // Gjentar endring av nytt navn Voi fra 1.1.24 (men her er også rett stemmekretsnr 4589 med)
                    StemmekretsRequestResolved(
                        gyldigFra = january1In2024,
                        endringstype = Endringstype.KVALITETSHEVING.value,
                        stemmekretsRequest = createStemmekretsRequest(voi, stemmekretsnr4567, 3)
                    )
                )))

        // Nå skal utkastet være publisert
        assertUtkastIsPublisert(utkastId)

        val stemmekretsList = findAllStemmekretserSortedByVersion()
        Assertions.assertThat(stemmekretsList.size).isEqualTo(5)
        assertStemmekrets(stemmekretsList[0], opprinneligStemmekretsnr, moi, Gyldighet(gyldigFra = november25In2022, gyldigTil = january1In2023), 1)
        assertStemmekrets(stemmekretsList[1], stemmekretsnr1234, moi, Gyldighet(gyldigFra = january1In2023, gyldigTil = july1In2023), 2)
        assertStemmekrets(stemmekretsList[2], stemmekretsnr1234, voi, Gyldighet(gyldigFra = january1In2024, gyldigTil = null, slettet = true), 3)
        assertStemmekrets(stemmekretsList[3], stemmekretsnr4567, moi, Gyldighet(gyldigFra = july1In2023, gyldigTil = january1In2024), 4)
        assertStemmekrets(stemmekretsList[4], stemmekretsnr4567, voi, Gyldighet(gyldigFra = january1In2024), 5)
    }

    @Test
    fun publiserUtkast05ResolverKonfliktMedFeilTidspunktSkalGiFeil() {
        assert1StemmekretsPresent()

        // 1. Endrer stemmekretsnummer
        nyttStemmekretsnummerJan23()

        // 2. Endrer stemmekretsnavn
        nyttStemmekretsnavnJan24()

        // 3. Forsøker å endre stemmekretsnummer til 4589 med gyldigFra 1.7.23 (selv om vi allerede har publisert endring som gjelder fra 1.1.24)
        // (Bruker her version 2 fordi den vil være basert på tilstanden i databasen 1.7.23)
        val resultatFraForsoekPaaPublisering = nyttStemmekretsNummerFraJuli23SkalGiFeil()
        val utkastId = resultatFraForsoekPaaPublisering.idForUtkastSomBleOpprettet

        // Utkastet som feilet publisering står som opprettet
        assertUtkastIsOpprettet(utkastId)

        // 4. Sender inn struktur som forsøker å fikse konflikt som oppsto i 3. Men sender inn feil gyldigFra på rad 2 her.
        // En rad gjelder fra 1.7.23
        // En rad gjelder fra 1.2.24 <--- Dato endret
        Assertions.assertThatThrownBy {
            utkastService.resolveConflictForFramtidigeVersjoner(
                utkastId = utkastId,
                conflictResolved = ConflictResolved(
                    lokalid = Lokalid(lokalidOfStemmekrets),
                    stemmekretsRequests = listOf(
                        // Gjentar endring av stemmekretsnr til 4589 fra 1.7.23
                        StemmekretsRequestResolved(
                            gyldigFra = july1In2023,
                            endringstype = Endringstype.KVALITETSHEVING.value,
                            stemmekretsRequest = createStemmekretsRequest(moi, stemmekretsnr4567, 2)),
                        // Gjentar endring av nytt navn Voi, men denne gangen gyldigFra 1.2.24 (her er rett stemmekretsnr 4589 med, men feil dato)
                        StemmekretsRequestResolved(
                            gyldigFra = february1In2024,
                            endringstype = Endringstype.KVALITETSHEVING.value,
                            stemmekretsRequest = createStemmekretsRequest(voi, stemmekretsnr4567, 3)
                        )
                    )))
        }.isInstanceOf(ResponseException.BadRequestException::class.java)
            .hasMessageContaining("Kan ikke publisere utkast med id: $utkastId. GyldigFra-feltet på en av endringene i utkastet er etter siste gyldig fra dato for kretsen.")
    }

    private fun nyttStemmekretsnavnJan24(): List<Stemmekrets> {
        saveAndPubliserStemmekretsUtkast(
            navn = "Jeg vil endre stemmekretsens navn til Voi",
            stemmekretsNr = stemmekretsnr1234,
            stemmekretsNavn = voi,
            oppdateringsdato = october24In2022,
            gyldigFra = january1In2024,
            version = 2
        )

        val stemmekretsList = findAllStemmekretserSortedByVersion()
        Assertions.assertThat(stemmekretsList.size).isEqualTo(3)
        assertStemmekrets(stemmekretsList[0], opprinneligStemmekretsnr, moi, Gyldighet(gyldigFra = november25In2022, gyldigTil = january1In2023), 1)
        assertStemmekrets(stemmekretsList[1], stemmekretsnr1234, moi, Gyldighet(gyldigFra = january1In2023, gyldigTil = january1In2024), 2)
        assertStemmekrets(stemmekretsList[2], stemmekretsnr1234, voi, Gyldighet(gyldigFra = january1In2024), 3)
        return stemmekretsList
    }

    private fun nyttStemmekretsnummerJan23() {
        saveAndPubliserStemmekretsUtkast(
            navn = "Jeg vil endre stemmekretsnr til 1234",
            stemmekretsNr = stemmekretsnr1234,
            stemmekretsNavn = moi,
            oppdateringsdato = october23In2022,
            gyldigFra = january1In2023,
            version = 1,
        )
    }

    private fun assert1StemmekretsPresent() {

        Assertions.assertThat(findAllStemmekretserSortedByVersion().size).isEqualTo(1)
    }

    private fun saveAndPubliserStemmekretsUtkast(navn: String, stemmekretsNr: String, stemmekretsNavn: String,
                                                 oppdateringsdato: ZonedDateTime, gyldigFra: LocalDate, version: Int) {
        val utkastId = saveStemmekretsUtkast(navn, stemmekretsNavn, stemmekretsNr, version, oppdateringsdato, gyldigFra)
        publiser(utkastId, gyldigFra)
    }

    private fun findAllStemmekretserSortedByVersion(): List<Stemmekrets> = stemmekretsRepository.findAllByLokalid(lokalidOfStemmekrets).sortedBy { it.version }

    private fun publiser(utkastId: String, gyldigFra: LocalDate) {
        utkastService.publiserUtkast(utkastId, gyldigFra)
        assertUtkastIsPublisert(utkastId)
    }

    private fun saveStemmekretsUtkast(
        navn: String,
        oppdateringsdato: ZonedDateTime,
        gyldigFra: LocalDate,
        stemmekretsRequest: StemmekretsRequest
    ): String {
        val utkast = UtkastBuilder.builder(navn)
            .withStemmekretsRequests(
                mapOf(lokalidOfStemmekrets to stemmekretsRequest))
            .withAuditInfo(AuditInfo(oppdateringsdato, "DUMMY"))
            .withGyldigFra(gyldigFra)
            .build()
        utkastRepository.save(utkast)
        return utkast.id
    }

    private fun saveStemmekretsUtkast(
        navn: String,
        stemmekretsNavn: String,
        stemmekretsNr: String,
        version: Int,
        oppdateringsdato: ZonedDateTime,
        gyldigFra: LocalDate
    ): String = saveStemmekretsUtkast(navn, oppdateringsdato, gyldigFra, createStemmekretsRequest(stemmekretsNavn, stemmekretsNr, version))


    private fun createStemmekretsRequest(
        stemmekretsNavn: String,
        stemmekretsNr: String,
        version: Int
    ): StemmekretsRequest =
        StemmekretsRequest(
            stemmekretsnavn = stemmekretsNavn,
            stemmekretsnummer = stemmekretsNr,
            identifikasjon = Identifikasjon(
                lokalid = lokalidOfStemmekrets,
            ),
            version = version,
        )
    private fun assertStemmekrets(stemmekrets: Stemmekrets,
                                  stemmekretssnummer: String,
                                  stemmekretssnavn: String,
                                  gyldighetsintervall: Gyldighet,
                                  version: Int) {
        Assertions.assertThat(stemmekrets.lokalid).isEqualTo(lokalidOfStemmekrets)
        Assertions.assertThat(stemmekrets.stemmekretsnummer).isEqualTo(stemmekretssnummer)
        Assertions.assertThat(stemmekrets.stemmekretsnavn).isEqualTo(stemmekretssnavn)
        Assertions.assertThat(stemmekrets.gyldighet).isEqualTo(gyldighetsintervall)
        Assertions.assertThat(stemmekrets.version).isEqualTo(version)
        Assertions.assertThat(stemmekrets.kommune.id).isEqualTo(kommuneIdGeonorge)
    }

    private fun assertUtkastIsPublisert(utkastId: String) = assertStatusEr(utkastId, UtkastStatus.PUBLISERT)
    private fun assertUtkastIsOpprettet(utkastId: String) = assertStatusEr(utkastId, UtkastStatus.OPPRETTET)
    private fun assertStatusEr(utkastId: String, status: UtkastStatus) = Assertions.assertThat(utkastRepository.findById(utkastId).get().status).isEqualTo(status)

    data class ResultatFraForsoekPaaPublisering(
        val idForUtkastSomBleOpprettet: String,
    )
}
