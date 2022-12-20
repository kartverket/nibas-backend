package no.kartverket.nibas.utkast

import no.kartverket.nibas.api.v1.common.geojson.metadata.Identifikasjon
import no.kartverket.nibas.api.v1.request.ConflictResolved
import no.kartverket.nibas.api.v1.request.GrunnkretsRequest
import no.kartverket.nibas.api.v1.request.GrunnkretsRequestResolved
import no.kartverket.nibas.common.FeilVedOppdatering
import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.common.ResponseException
import no.kartverket.nibas.domain.Endringstype
import no.kartverket.nibas.domain.Grunnkrets
import no.kartverket.nibas.domain.utkast.UtkastBuilder
import no.kartverket.nibas.domain.utkast.UtkastStatus
import no.kartverket.nibas.repository.GrunnkretsRepository
import no.kartverket.nibas.repository.UtkastRepository
import no.kartverket.nibas.sporing.AuditInfo
import no.kartverket.nibas.sporing.Gyldighet
import no.kartverket.nibas.sporing.UserFacade
import no.kartverket.nibas.testcontainer.TestWithDbContainer
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month
import java.time.ZoneId
import java.time.ZonedDateTime
import org.mockito.Mockito.`when` as whenever

/**
 * Vil at testene skal være transaksjonelle for å rulle tilbake.
 *
 * OBS: Hvis fila slutter på IntegrationTest blir den ignorert.
 */
@Transactional
class UtkastServiceIT : TestWithDbContainer() {

    @MockBean
    private lateinit var userFacade: UserFacade

    @Autowired
    private lateinit var utkastService: UtkastService
    @Autowired
    private lateinit var utkastRepository: UtkastRepository
    @Autowired
    private lateinit var grunnkretsRepository: GrunnkretsRepository

    private val lokalidOfGrunnkrets = "c54fb55e-15d0-4528-8b88-f58d6c74bd87"

    // ID for Kommune med kommunenummerId som matcher i Geonorge og vår cache
    private val kommuneIdGeonorge = "ee7a81d3-6321-4157-a364-7fda34919067"

    private val moi = "Moi"
    private val voi = "Voi"
    private val opprinneligGrunnkretsnr = "42190204"
    private val grunnkretsnr1234 = "1234"
    private val grunnkretsnr4589 = "4589"

    private val october23In2022 = ZonedDateTime.of(LocalDateTime.of(2022, Month.OCTOBER, 23, 12, 0), ZoneId.systemDefault())
    private val october24In2022 = ZonedDateTime.of(LocalDateTime.of(2022, Month.OCTOBER, 24, 12, 0), ZoneId.systemDefault())
    private val november25In2022 = LocalDate.of(2022, Month.NOVEMBER, 25)
    private val january1In2023 = LocalDate.of(2023, Month.JANUARY, 1)
    private val july1In2023 = LocalDate.of(2023, Month.JULY, 1)
    private val january1In2024 = LocalDate.of(2024, Month.JANUARY, 1)
    private val february1In2024 = LocalDate.of(2024, Month.FEBRUARY, 1)

    @BeforeEach
    fun beforeEach() {
        whenever(userFacade.getAuthenticatedUser()).thenReturn("Testbruker 123")
    }

    /**
     * Starttilstand: Grunnkrets
     *  -42270304 Moi
     *  -lokalid: c54fb55e-15d0-4528-8b88-f58d6c74bd87
     *  -id: 762f5591-b3ba-49ef-96b3-05c626356580
     *  -gyldigFra 2022-11-25
     *  -gyldigTil null
     *
     * Request 1:
     * GrunnkretsRequest: Grunnkretsnr 1234, grunnkretsnavn: Moi gyldigFra 1.1.23
     */
    @Test
    fun publiserUtkast01MedNyttGrunnkretsnummer() {
        assert1GrunnkretsPresent()

        // 1. Endrer grunnkretsnummer
        nyttGrunnkretsnummerJan23()
    }

    /**
     * Starttilstand: Grunnkrets
     *  -42270304 Moi
     *  -lokalid: c54fb55e-15d0-4528-8b88-f58d6c74bd87
     *  -id: 762f5591-b3ba-49ef-96b3-05c626356580
     *  -gyldigFra 2022-11-25
     *  -gyldigTil null
     *
     * Request 1:
     * GrunnkretsRequest: Grunnkretsnr 1234, grunnkretsnavn: Moi gyldigFra 1.1.23
     *
     * Request 2:
     * GrunnkretsRequest: Grunnkretsnr 1234, grunnkretsnavn: Voi, gyldigFra 1.1.24
     *
     */
    @Test
    fun publiserUtkast02NummerPlussNavn() {
        assert1GrunnkretsPresent()

        // 1. Endrer grunnkretsnummer
        nyttGrunnkretsnummerJan23()

        // 2. Endrer grunnkretsnavn
        nyttGrunnkretsnavnJan24()
    }

    /**
     * Starttilstand: Grunnkrets
     *  -42270304 Moi
     *  -lokalid: f5ada1ad-f1d3-4cd3-b04f-1b29aa97aa5a
     *  -id: 2c106a7a-372a-4cc1-afac-96b3387a944d
     *  -gyldigFra 21.10.22
     *  -gyldigTil null
     *
     * Request 1:
     * GrunnkretsRequest: Grunnkretsnr 1234, grunnkretsnavn: Moi gyldigFra 1.1.23
     *
     * Request 2:
     * GrunnkretsRequest: Grunnkretsnr 1234, grunnkretsnavn: Voi, gyldigFra 1.1.24
     *
     * Request 3:
     * GrunnkretsRequest: Grunnkretsnr 4589, grunnkretsnavn: Moi, gyldigFra 1.7.23
     *
     */
    @Test
    fun publiserUtkast03FinnesRaderMedNyereGyldigFra() {
        assert1GrunnkretsPresent()

        // 1. Endrer grunnkretsnummer
        nyttGrunnkretsnummerJan23()

        // 2. Endrer grunnkretsnavn
        nyttGrunnkretsnavnJan24()

        // 3. Forsøker å endre grunnkretsnummer med gyldigFra 1.7.23 (selv om vi allerede har publisert endring som gjelder fra 1.1.24)
        // Bruker her version 2 fordi den skal være basert på tilstanden i databasen 1.7.23
        nyttNummerFraJuli23SkalGiFeil()
    }

    /**
     * Starttilstand: Grunnkrets
     *  -42270304 Moi
     *  -lokalid: f5ada1ad-f1d3-4cd3-b04f-1b29aa97aa5a
     *  -id: 2c106a7a-372a-4cc1-afac-96b3387a944d
     *  -gyldigFra 21.10.22
     *  -gyldigTil null
     *
     * Request 1 "Nytt grunnkretsnr 1234 fra 1.1.23":
     * GrunnkretsRequest: Grunnkretsnr 1234, grunnkretsnavn: Moi gyldigFra 1.1.23
     *
     * Request 2 "Nytt grunnkretsnavn Voi fra 1.1.24":
     * GrunnkretsRequest: Grunnkretsnr 1234, grunnkretsnavn: Voi, gyldigFra 1.1.24
     *
     * Request 3 "Nytt grunnkretsnr 4589 fra 1.7.23":
     * GrunnkretsRequest: Grunnkretsnr 4589, grunnkretsnavn: Moi, gyldigFra 1.7.23
     *
     * Fikser konflikt i nr 3 med kall til resolveKonflikter med følgende requester:
     * - GrunnkretsRequest: Grunnkretsnr 4589, grunnkretsnavn: Moi, gyldigFra 1.7.23
     * - GrunnkretsRequest: Grunnkretsnr 4589, grunnkretsnavn: Voi, gyldigFra 1.1.24
     */
    @Test
    fun publiserUtkast04ResolverKonfliktPaaAlleredePubliserteFramtidigeEndringer() {
        assert1GrunnkretsPresent()

        // 1. Endrer grunnkretsnummer
        nyttGrunnkretsnummerJan23()

        // 2. Endrer grunnkretsnavn
        nyttGrunnkretsnavnJan24()

        // 3. Forsøker å endre grunnkretsnummer til 4589 med gyldigFra 1.7.23 (selv om vi allerede har publisert endring som gjelder fra 1.1.24)
        // (Bruker her version 2 fordi den vil være basert på tilstanden i databasen 1.7.23)
        val resultatFraForsoekPaaPublisering = nyttNummerFraJuli23SkalGiFeil()
        val utkastId = resultatFraForsoekPaaPublisering.idForUtkastSomBleOpprettet

        // Utkastet som feilet publisering står som opprettet
        assertUtkastIsOpprettet(utkastId)

        // 4. Sender inn struktur som skal fikse konflikt som oppsto i 3.
        // En rad gjelder fra 1.7.23
        // En rad gjelder fra 1.1.24
        utkastService.resolveConflictForFramtidigeVersjoner(
            utkastId = utkastId,
            conflictResolved = ConflictResolved(
                lokalid = Lokalid(lokalidOfGrunnkrets),
                grunnkretsRequests = listOf(
                    // Gjentar endring av grunnkretsnr til 4589 fra 1.7.23
                    GrunnkretsRequestResolved(
                        gyldigFra = july1In2023,
                        endringstype = Endringstype.KVALITETSHEVING.value,
                        grunnkretsRequest = createGrunnkretsRequest(moi, grunnkretsnr4589, 2)),
                    // Gjentar endring av nytt navn Voi fra 1.1.24 (men her er også rett grunnkretsnr 4589 med)
                    GrunnkretsRequestResolved(
                        gyldigFra = january1In2024,
                        endringstype = Endringstype.KVALITETSHEVING.value,
                        grunnkretsRequest = createGrunnkretsRequest(voi, grunnkretsnr4589, 3)
                    )
                )))

        // Nå skal utkastet være publisert
        assertUtkastIsPublisert(utkastId)

        val grunnkretsList = findAllGrunnkretserSortedByVersion()
        assertThat(grunnkretsList.size).isEqualTo(5)
        assertGrunnkrets(grunnkretsList[0], opprinneligGrunnkretsnr, moi, Gyldighet(gyldigFra = november25In2022, gyldigTil = january1In2023), 1)
        assertGrunnkrets(grunnkretsList[1], grunnkretsnr1234, moi, Gyldighet(gyldigFra = january1In2023, gyldigTil = july1In2023), 2)
        assertGrunnkrets(grunnkretsList[2], grunnkretsnr1234, voi, Gyldighet(gyldigFra = january1In2024, gyldigTil = null, slettet = true), 3)
        assertGrunnkrets(grunnkretsList[3], grunnkretsnr4589, moi, Gyldighet(gyldigFra = july1In2023, gyldigTil = january1In2024), 4)
        assertGrunnkrets(grunnkretsList[4], grunnkretsnr4589, voi, Gyldighet(gyldigFra = january1In2024), 5)
    }

    /**
     * Starttilstand: Grunnkrets
     *  -42270304 Moi
     *  -lokalid: f5ada1ad-f1d3-4cd3-b04f-1b29aa97aa5a
     *  -id: 2c106a7a-372a-4cc1-afac-96b3387a944d
     *  -gyldigFra 21.10.22
     *  -gyldigTil null
     *
     * Request 1 "Nytt grunnkretsnr 1234 fra 1.1.23":
     * GrunnkretsRequest: Grunnkretsnr 1234, grunnkretsnavn: Moi gyldigFra 1.1.23
     *
     * Request 2 "Nytt grunnkretsnavn Voi fra 1.1.24":
     * GrunnkretsRequest: Grunnkretsnr 1234, grunnkretsnavn: Voi, gyldigFra 1.1.24
     *
     * Request 3 "Nytt grunnkretsnr 4589 fra 1.7.23":
     * GrunnkretsRequest: Grunnkretsnr 4589, grunnkretsnavn: Moi, gyldigFra 1.7.23
     *
     * Forsøker å fikse konflikt i nr 3 med kall til resolveKonflikter med følgende requester:
     * - GrunnkretsRequest: Grunnkretsnr 4589, grunnkretsnavn: Moi, gyldigFra 1.7.23
     * - GrunnkretsRequest: Grunnkretsnr 4589, grunnkretsnavn: Voi, gyldigFra 1.2.24 (Her er gyldigFra endret fra 1.1 til 1.2)
     */
    @Test
    fun publiserUtkast05ResolverKonfliktMedFeilTidspunktSkalGiFeil() {
        assert1GrunnkretsPresent()

        // 1. Endrer grunnkretsnummer
        nyttGrunnkretsnummerJan23()

        // 2. Endrer grunnkretsnavn
        nyttGrunnkretsnavnJan24()

        // 3. Forsøker å endre grunnkretsnummer til 4589 med gyldigFra 1.7.23 (selv om vi allerede har publisert endring som gjelder fra 1.1.24)
        // (Bruker her version 2 fordi den vil være basert på tilstanden i databasen 1.7.23)
        val resultatFraForsoekPaaPublisering = nyttNummerFraJuli23SkalGiFeil()
        val utkastId = resultatFraForsoekPaaPublisering.idForUtkastSomBleOpprettet

        // Utkastet som feilet publisering står som opprettet
        assertUtkastIsOpprettet(utkastId)

        // 4. Sender inn struktur som forsøker å fikse konflikt som oppsto i 3. Men sender inn feil gyldigFra på rad 2 her.
        // En rad gjelder fra 1.7.23
        // En rad gjelder fra 1.2.24 <--- Dato endret
        assertThatThrownBy {
            utkastService.resolveConflictForFramtidigeVersjoner(
                utkastId = utkastId,
                conflictResolved = ConflictResolved(
                    lokalid = Lokalid(lokalidOfGrunnkrets),
                    grunnkretsRequests = listOf(
                        // Gjentar endring av grunnkretsnr til 4589 fra 1.7.23
                        GrunnkretsRequestResolved(
                            gyldigFra = july1In2023,
                            endringstype = Endringstype.KVALITETSHEVING.value,
                            grunnkretsRequest = createGrunnkretsRequest(moi, grunnkretsnr4589, 2)),
                        // Gjentar endring av nytt navn Voi, men denne gangen gyldigFra 1.2.24 (her er rett grunnkretsnr 4589 med, men feil dato)
                        GrunnkretsRequestResolved(
                            gyldigFra = february1In2024,
                            endringstype = Endringstype.KVALITETSHEVING.value,
                            grunnkretsRequest = createGrunnkretsRequest(voi, grunnkretsnr4589, 3)
                        )
                    )))
        }.isInstanceOf(ResponseException.BadRequestException::class.java)
            .hasMessageContaining("Ikke lov å modifisere gyldigFra-tidspunkt ved resolving av en framtidig versjon-konflikt. " +
                "Siste kjente gyldigFra for dette objektet er: 2024-01-01 (2024-02-01).")
    }

    private fun assert1GrunnkretsPresent() {
        assertThat(findAllGrunnkretserSortedByVersion().size).isEqualTo(1)
    }

    data class ResultatFraForsoekPaaPublisering(
        val idForUtkastSomBleOpprettet: String,
    )

    private fun nyttNummerFraJuli23SkalGiFeil(): ResultatFraForsoekPaaPublisering {
        val grunnkretsRequest = createGrunnkretsRequest(
            grkrNavn = moi,
            grkrNr = grunnkretsnr4589,
            version = 2)

        val utkastId = saveutkast(
            navn = "Jeg vil endre grunnkretsnr til 4589 fra 1.7.23",
            oppdateringsdato = october24In2022,
            gyldigFra = july1In2023,
            grunnkretsRequest
        )

        // Kall til publiser skal feile.
        val oppdateringException = catchThrowableOfType({
            publiser(utkastId)
        }, FeilVedOppdatering.OppdateringException::class.java)

        assertThat(oppdateringException).isNotNull
        val oppdateringInfo = oppdateringException.oppdateringInfo
        val lokalid = Lokalid(lokalidOfGrunnkrets)
        assertThat(oppdateringInfo.id).isEqualTo(Identifikator(lokalid, july1In2023))
        assertThat(oppdateringInfo.type).isEqualTo(FeilVedOppdatering.Type.GRUNNKRETS)
        assertThat(oppdateringInfo.affectedIds.toSet()).isEqualTo(setOf(Identifikator(lokalid, january1In2024)))

        return ResultatFraForsoekPaaPublisering(utkastId)
    }

    private fun nyttGrunnkretsnavnJan24(): List<Grunnkrets> {
        saveAndPubliserUtkast(
            navn = "Jeg vil endre grunnkretsens navn til Voi",
            grkrNr = grunnkretsnr1234,
            grkrNavn = voi,
            oppdateringsdato = october24In2022,
            gyldigFra = january1In2024,
            version = 2)

        val grunnkretsList = findAllGrunnkretserSortedByVersion()
        assertThat(grunnkretsList.size).isEqualTo(3)
        assertGrunnkrets(grunnkretsList[0], opprinneligGrunnkretsnr, moi, Gyldighet(gyldigFra = november25In2022, gyldigTil = january1In2023), 1)
        assertGrunnkrets(grunnkretsList[1], grunnkretsnr1234, moi, Gyldighet(gyldigFra = january1In2023, gyldigTil = january1In2024), 2)
        assertGrunnkrets(grunnkretsList[2], grunnkretsnr1234, voi, Gyldighet(gyldigFra = january1In2024), 3)
        return grunnkretsList
    }

    private fun nyttGrunnkretsnummerJan23() {
        saveAndPubliserUtkast(
            navn = "Jeg vil endre grunnkretsnr til 1234",
            grkrNr = grunnkretsnr1234,
            grkrNavn = moi,
            oppdateringsdato = october23In2022,
            gyldigFra = january1In2023,
            version = 1,
        )

        val grunnkretsList = findAllGrunnkretserSortedByVersion()
        assertThat(grunnkretsList.size).isEqualTo(2)
        assertGrunnkrets(grunnkretsList[0], opprinneligGrunnkretsnr, moi, Gyldighet(gyldigFra = november25In2022, gyldigTil = january1In2023), 1)
        assertGrunnkrets(grunnkretsList[1], grunnkretsnr1234, moi, Gyldighet(gyldigFra = january1In2023), 2)
    }

    private fun saveAndPubliserUtkast(navn: String, grkrNr: String, grkrNavn: String,
                                      oppdateringsdato: ZonedDateTime, gyldigFra: LocalDate, version: Int) {
        val utkastId = saveutkast(navn, grkrNavn, grkrNr, version, oppdateringsdato, gyldigFra)
        publiser(utkastId)
    }

    private fun publiser(utkastId: String) {
        utkastService.publiserUtkast(utkastId)
        assertUtkastIsPublisert(utkastId)
    }

    private fun saveutkast(
        navn: String,
        oppdateringsdato: ZonedDateTime,
        gyldigFra: LocalDate,
        grunnkretsRequest: GrunnkretsRequest
    ): String {
        val utkast = UtkastBuilder.builder(navn)
            .withGrunnkretsRequests(
                mapOf(lokalidOfGrunnkrets to grunnkretsRequest))
            .withAuditInfo(AuditInfo(oppdateringsdato, "DUMMY"))
            .withGyldigFra(gyldigFra)
            .build()
        utkastRepository.save(utkast)
        return utkast.id
    }

    private fun saveutkast(
        navn: String,
        grkrNavn: String,
        grkrNr: String,
        version: Int,
        oppdateringsdato: ZonedDateTime,
        gyldigFra: LocalDate
    ): String = saveutkast(navn, oppdateringsdato, gyldigFra, createGrunnkretsRequest(grkrNavn, grkrNr, version))

    private fun createGrunnkretsRequest(
        grkrNavn: String,
        grkrNr: String,
        version: Int
    ): GrunnkretsRequest =
        GrunnkretsRequest(
            navn = grkrNavn,
            grunnkretsnummer = grkrNr,
            identifikasjon = Identifikasjon(
                lokalid = lokalidOfGrunnkrets,
            ),
            version = version,
        )

    private fun findAllGrunnkretserSortedByVersion(): List<Grunnkrets> = grunnkretsRepository.findAllByLokalid(lokalidOfGrunnkrets).sortedBy { it.version }

    private fun assertGrunnkrets(grunnkrets: Grunnkrets,
                                 grunnkretsnummer: String,
                                 grunnkretsnavn: String,
                                 gyldighetsintervall: Gyldighet,
                                 version: Int) {
        assertThat(grunnkrets.lokalid).isEqualTo(lokalidOfGrunnkrets)
        assertThat(grunnkrets.grunnkretsnummer).isEqualTo(grunnkretsnummer)
        assertThat(grunnkrets.grunnkretsnavn).isEqualTo(grunnkretsnavn)
        assertThat(grunnkrets.gyldighet).isEqualTo(gyldighetsintervall)
        assertThat(grunnkrets.version).isEqualTo(version)
        assertThat(grunnkrets.kommune.id).isEqualTo(kommuneIdGeonorge)
    }

    private fun assertUtkastIsPublisert(utkastId: String) = assertStatusEr(utkastId, UtkastStatus.PUBLISERT)
    private fun assertUtkastIsOpprettet(utkastId: String) = assertStatusEr(utkastId, UtkastStatus.OPPRETTET)
    private fun assertStatusEr(utkastId: String, status: UtkastStatus) = assertThat(utkastRepository.findById(utkastId).get().status).isEqualTo(status)

}
