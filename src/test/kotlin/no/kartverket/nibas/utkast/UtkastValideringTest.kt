package no.kartverket.nibas.utkast

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import no.kartverket.nibas.api.v1.common.Flatetype
import no.kartverket.nibas.api.v1.common.Grenseendringer
import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.api.v1.common.KontekstEnhetType
import no.kartverket.nibas.api.v1.common.Metadataendringer
import no.kartverket.nibas.api.v1.common.ObjektIdentifikator
import no.kartverket.nibas.api.v1.common.Operasjoner
import no.kartverket.nibas.api.v1.common.geojson.KontekstEgenskaper
import no.kartverket.nibas.api.v1.common.geojson.metadata.IdentifikatorMedVersjon
import no.kartverket.nibas.api.v1.request.GrunnkretsSammenslaaingsendringRequest
import no.kartverket.nibas.api.v1.request.KretsDelingEndringRequest
import no.kartverket.nibas.api.v1.request.KretsNavnOgNummer
import no.kartverket.nibas.api.v1.request.StemmekretsSammenslaaingsendringRequest
import no.kartverket.nibas.builders.FeatureBuilder
import no.kartverket.nibas.builders.GrunnkretsBuilder
import no.kartverket.nibas.builders.GrunnkretsRequestBuilder
import no.kartverket.nibas.builders.StemmekretsRequestBuilder
import no.kartverket.nibas.common.CurrentTime
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.domain.utkast.Utkast
import no.kartverket.nibas.domain.utkast.UtkastBuilder
import no.kartverket.nibas.domain.utkast.UtkastStatus
import no.kartverket.nibas.kodelister.KodelisteService
import no.kartverket.nibas.service.GrenseService
import no.kartverket.nibas.service.GrunnkretsService
import no.kartverket.nibas.service.StemmekretsService
import no.kartverket.nibas.service.utkast.OperasjonerValidering
import no.kartverket.nibas.service.utkast.UtkastValidatorService
import no.kartverket.nibas.service.utkast.UtkastValidering
import no.kartverket.nibas.utkast.UtkastServiceTest.Companion.operasjonerMedIkkeEditerbareGrenser
import no.kartverket.nibas.utkast.UtkastServiceTest.Companion.operasjonerMedKunEditerbareGrenser
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate


class UtkastValideringTest {

    @MockK
    private lateinit var grunnkretsServiceMock: GrunnkretsService

    @MockK
    private lateinit var stemmekretsServiceMock: StemmekretsService

    @MockK
    private lateinit var kodelisteServiceMock: KodelisteService

    @MockK
    private lateinit var grenseServiceMock: GrenseService

    @MockK
    private lateinit var currentTime: CurrentTime

    private lateinit var utkastValidatorService: UtkastValidatorService

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
        every { currentTime.asLocalDate() } returns LocalDate.of(2022, 6, 1)
        every { grunnkretsServiceMock.hentGrunnkrets(any()) } returns GrunnkretsBuilder.DEFAULT_GRUNNKRETS
        every { grunnkretsServiceMock.hentAlleVersjonerMedGyldigFraEtterGittDato(any(), any()) } returns emptyList()

        every { stemmekretsServiceMock.hentAlleVersjonerMedGyldigFraEtterGittDato(any(), any()) } returns emptyList()

        every { grenseServiceMock.hentAlleVersjonerMedGyldigFraEtterGittDato(any(), any()) } returns emptyList()

        utkastValidatorService = UtkastValidatorService(grunnkretsServiceMock, grenseServiceMock, stemmekretsServiceMock, kodelisteServiceMock, currentTime)
    }

    @Test
    fun `valider utkast med annen status enn opprettet gir ugyldig utkast`() { //publiserUtkastMedAnnenStatusEnnOpprettetGirBadRequestEx
        val publisertUtkast = UtkastBuilder.builder("Utkast for publisering")
            .withStatus(UtkastStatus.PUBLISERT)
            .build()

        val validateUtkastKlartForPublisering = utkastValidatorService.validateUtkastKlartForPublisering(publisertUtkast)
        assertThat(validateUtkastKlartForPublisering).isInstanceOf(UtkastValidering.UgyldigUtkast::class.java)
    }

    @Test
    fun `valider utkast uten operasjoner gir ugyldig utkast`() {
        val utkastUtenOperasjoner = UtkastBuilder.builder("Utkast for publisering")
            .build()

        val validateUtkastKlartForPublisering = utkastValidatorService.validateUtkastKlartForPublisering(utkastUtenOperasjoner)
        assertThat(validateUtkastKlartForPublisering).isInstanceOf(UtkastValidering.UgyldigUtkast::class.java)
    }

    @Test
    fun `valider utkast med gyldig fra i fortiden gir ugyldig utkast`() {
        val utkastMedGyldigFraIFortiden = UtkastBuilder.builder("Utkast for publisering")
            .withGyldigFra(LocalDate.of(2021, 1, 1))
            .build()

        val validateUtkastKlartForPublisering = utkastValidatorService.validateUtkastKlartForPublisering(utkastMedGyldigFraIFortiden)
        assertThat(validateUtkastKlartForPublisering).isInstanceOf(UtkastValidering.UgyldigUtkast::class.java)
    }

    @Test
    fun `valider operasjoner med ikke-editerbare grenser gir ugyldige operasjoner`() {
        val operasjoner = operasjonerMedIkkeEditerbareGrenser()

        val validateGyldigeOperasjoner = utkastValidatorService.validateGyldigeOperasjoner(operasjoner)
        assertThat(validateGyldigeOperasjoner).isInstanceOf(OperasjonerValidering.UgyldigeOperasjoner::class.java)
    }

    @Test
    fun `valider at metadataoperasjoner med blankt grunnkretsnavn gir ugyldige operasjoner`() {
        val operasjoner =
            Operasjoner(
                metadataendringer =
                Metadataendringer(
                    grunnkretsendringer =
                    mapOf(
                        "1" to GrunnkretsRequestBuilder(navn = "gyldigNavn").build(),
                        "2" to GrunnkretsRequestBuilder(navn = "").build(),
                    )
                )
            )

        val validateGyldigeOperasjoner =
            utkastValidatorService.validateGyldigeOperasjoner(operasjoner)
        assertThat(validateGyldigeOperasjoner)
            .isInstanceOf(OperasjonerValidering.UgyldigeOperasjoner::class.java)
    }

    @Test
    fun `valider at metadataoperasjoner med blankt stemmekretsnavn gir ugyldige operasjoner`() {
        val operasjoner =
            Operasjoner(
                metadataendringer =
                Metadataendringer(
                    stemmekretsendringer =
                    mapOf("1" to StemmekretsRequestBuilder(stemmekretsnavn = "").build())
                )
            )

        val validateGyldigeOperasjoner =
            utkastValidatorService.validateGyldigeOperasjoner(operasjoner)
        assertThat(validateGyldigeOperasjoner)
            .isInstanceOf(OperasjonerValidering.UgyldigeOperasjoner::class.java)
    }

    @Test
    fun `valider at sammenslaaingsoperasjoner med blankt stemmekretsnavn gir ugyldige operasjoner`() {
        val operasjoner =
            Operasjoner(
                stemmekretsSammenslaaingsendring =
                StemmekretsSammenslaaingsendringRequest(
                    viderefoertStemmekrets = IdentifikatorMedVersjon("1", 5),
                    stemmekretserTilSammenslaaing = listOf(IdentifikatorMedVersjon("2", 1)),
                    navn = "",
                    nummer = "1234567"
                )
            )

        val validateGyldigeOperasjoner =
            utkastValidatorService.validateGyldigeOperasjoner(operasjoner)
        assertThat(validateGyldigeOperasjoner)
            .isInstanceOf(OperasjonerValidering.UgyldigeOperasjoner::class.java)
    }

    @Test
    fun `valider at metadataoperasjoner med blankt grunnkretsnummer gir ugyldige operasjoner`() {
        val operasjoner =
            Operasjoner(
                metadataendringer =
                Metadataendringer(
                    grunnkretsendringer =
                    mapOf("1" to GrunnkretsRequestBuilder(grunnkretsnummer = "").build())
                )
            )

        val validateGyldigeOperasjoner =
            utkastValidatorService.validateGyldigeOperasjoner(operasjoner)
        assertThat(validateGyldigeOperasjoner)
            .isInstanceOf(OperasjonerValidering.UgyldigeOperasjoner::class.java)
    }

    @Test
    fun `valider at metadataoperasjoner med 0 som stemmekretsnummer gir ugyldige operasjoner`() {
        val operasjoner =
            Operasjoner(
                metadataendringer =
                Metadataendringer(
                    stemmekretsendringer =
                    mapOf("1" to StemmekretsRequestBuilder(stemmekretsnummer = "0").build())
                )
            )

        val validateGyldigeOperasjoner =
            utkastValidatorService.validateGyldigeOperasjoner(operasjoner)
        assertThat(validateGyldigeOperasjoner)
            .isInstanceOf(OperasjonerValidering.UgyldigeOperasjoner::class.java)
    }

    @Test
    fun `valider at sammenslaaingsoperasjoner med stemmekretsnummer med bokstaver gir ugyldige operasjoner`() {
        val operasjoner =
            Operasjoner(
                stemmekretsSammenslaaingsendring =
                StemmekretsSammenslaaingsendringRequest(
                    viderefoertStemmekrets = IdentifikatorMedVersjon("1", 5),
                    stemmekretserTilSammenslaaing = listOf(IdentifikatorMedVersjon("2", 1)),
                    navn = "gyldigNavn",
                    nummer = "ugyldigNummer"
                )
            )

        val validateGyldigeOperasjoner =
            utkastValidatorService.validateGyldigeOperasjoner(operasjoner)
        assertThat(validateGyldigeOperasjoner)
            .isInstanceOf(OperasjonerValidering.UgyldigeOperasjoner::class.java)
    }

    @Test
    fun `valider at grunnkretsnummer med mindre enn 8 siffer gir ugyldige operasjoner`() {
        val operasjoner =
            Operasjoner(
                metadataendringer =
                Metadataendringer(
                    grunnkretsendringer =
                    mapOf("1" to GrunnkretsRequestBuilder(grunnkretsnummer = "1234567").build())
                )
            )

        val validateGyldigeOperasjoner =
            utkastValidatorService.validateGyldigeOperasjoner(operasjoner)
        assertThat(validateGyldigeOperasjoner)
            .isInstanceOf(OperasjonerValidering.UgyldigeOperasjoner::class.java)
    }

    @Test
    fun `valider at grunnkretsnummer med mer enn 8 siffer gir ugyldige operasjoner`() {
        val operasjoner =
            Operasjoner(
                metadataendringer =
                Metadataendringer(
                    grunnkretsendringer =
                    mapOf("1" to GrunnkretsRequestBuilder(grunnkretsnummer = "123456789").build())
                )
            )

        val validateGyldigeOperasjoner =
            utkastValidatorService.validateGyldigeOperasjoner(operasjoner)
        assertThat(validateGyldigeOperasjoner)
            .isInstanceOf(OperasjonerValidering.UgyldigeOperasjoner::class.java)
    }

    @Test
    fun `valider at negativt grunnkretsnummer gir ugyldige operasjoner`() {
        val operasjoner =
            Operasjoner(
                metadataendringer =
                Metadataendringer(
                    grunnkretsendringer =
                    mapOf("1" to GrunnkretsRequestBuilder(grunnkretsnummer = "-12345678").build())
                )
            )

        val validateGyldigeOperasjoner =
            utkastValidatorService.validateGyldigeOperasjoner(operasjoner)
        assertThat(validateGyldigeOperasjoner)
            .isInstanceOf(OperasjonerValidering.UgyldigeOperasjoner::class.java)
    }

    @Test
    fun `valider at stemmekretsnummer med mer enn 4 siffer gir ugyldige operasjoner`() {
        val operasjoner =
            Operasjoner(
                metadataendringer =
                Metadataendringer(
                    stemmekretsendringer =
                    mapOf("1" to StemmekretsRequestBuilder(stemmekretsnummer = "12345").build())
                )
            )

        val validateGyldigeOperasjoner =
            utkastValidatorService.validateGyldigeOperasjoner(operasjoner)
        assertThat(validateGyldigeOperasjoner)
            .isInstanceOf(OperasjonerValidering.UgyldigeOperasjoner::class.java)
    }

    @Test
    fun `valider at negativt stemmekretsnummer gir ugyldige operasjoner`() {
        val operasjoner =
            Operasjoner(
                metadataendringer =
                Metadataendringer(
                    stemmekretsendringer =
                    mapOf("1" to StemmekretsRequestBuilder(stemmekretsnummer = "-123").build())
                )
            )

        val validateGyldigeOperasjoner =
            utkastValidatorService.validateGyldigeOperasjoner(operasjoner)
        assertThat(validateGyldigeOperasjoner)
            .isInstanceOf(OperasjonerValidering.UgyldigeOperasjoner::class.java)
    }

    @Test
    fun `valider at grunnkretsnummer som ikke matcher med kommunenummer gir ugyldige operasjoner`() {
        val operasjoner =
            Operasjoner(
                metadataendringer =
                Metadataendringer(
                    grunnkretsendringer =
                    mapOf("1" to GrunnkretsRequestBuilder(grunnkretsnummer = "12345678").build())
                )
            )

        val validateGyldigeOperasjoner =
            utkastValidatorService.validateGyldigeOperasjoner(operasjoner)
        assertThat(validateGyldigeOperasjoner)
            .isInstanceOf(OperasjonerValidering.UgyldigeOperasjoner::class.java)
    }

    @Test
    fun `valider at godkjente operasjoner gir gyldige operasjoner`() {
        val operasjoner =
            Operasjoner(
                metadataendringer =
                Metadataendringer(
                    grunnkretsendringer =
                    mapOf(
                        "1" to GrunnkretsRequestBuilder.DEFAULT_REQUEST,
                        "2" to GrunnkretsRequestBuilder(lokalId = "2").build(),
                    ),
                    stemmekretsendringer =
                    mapOf("1" to StemmekretsRequestBuilder(stemmekretsnummer = "123").build())
                ),
                stemmekretsSammenslaaingsendring =
                StemmekretsSammenslaaingsendringRequest(
                    viderefoertStemmekrets = IdentifikatorMedVersjon("1", 5),
                    stemmekretserTilSammenslaaing = listOf(IdentifikatorMedVersjon("2", 1)),
                    navn = "gyldigNavn",
                    nummer = "4"
                ),
                grenseendringer = operasjonerMedKunEditerbareGrenser().grenseendringer
            )

        val validateGyldigeOperasjoner =
            utkastValidatorService.validateGyldigeOperasjoner(operasjoner)
        assertThat(validateGyldigeOperasjoner)
            .isInstanceOf(OperasjonerValidering.GyldigeOperasjoner::class.java)
    }

    private fun testValiderKretsDelingIUtkast(gyldighetsdato: LocalDate, kretsnummer: String, flatetype: Flatetype, grenseendringer: Grenseendringer ): Pair<Utkast, UtkastValidering> {
        val utkast = Utkast(navn = "test", operasjoner = Operasjoner(
            // disse featurene har kontekst til to grunnkretser som IKKE har kretsnummer 25
            grenseendringer = grenseendringer,
            // deling av krets 2 som lager ny krets med kretsnummer 25
            kretsDelingEndringer = listOf(KretsDelingEndringRequest(
                IdentifikatorMedVersjon("2", 1),
                ObjektIdentifikator(Lokalid("1"), gyldighetsdato),
                flatetype = flatetype,
                nyeKretser = listOf(KretsNavnOgNummer(kretsNavn = "nyGrunnkrets", kretsNummer = kretsnummer)),
            ))
        ))

        return Pair(utkast, utkastValidatorService.validateUtkastKlartForPublisering(utkast))
    }


    @Test
    fun `utkast er ugyldig med deling uten grenseendringer som refererer til ny krets`(){
        val gyldighetsdato = LocalDate.now()
        val kretsnummer = "25"

        val (utkast, utkastValidering) = testValiderKretsDelingIUtkast(gyldighetsdato, kretsnummer, Flatetype.GRUNNKRETS, operasjonerMedKunEditerbareGrenser().grenseendringer)

        assertThat(utkastValidering).isInstanceOf(UtkastValidering.UgyldigUtkast::class.java)

        assertThat((utkastValidering as UtkastValidering.UgyldigUtkast).feil.message)
            .isEqualTo("Krets(er) med navn: " +
                "${utkast.operasjoner?.kretsDelingEndringer?.first()?.nyeKretser?.first()?.kretsNavn} " +
                "har ikke fått noen grenser tildelt. Dette er en feil, og den kan rettes opp ved å sette tilhørighet på de relevante grensene som skal høre til enten: " +
                "${utkast.operasjoner?.kretsDelingEndringer?.first()?.nyeKretser?.first()?.kretsNavn}")
    }

    @Test
    fun `utkast er ugyldig med deling uten grenseendringer`(){
        val gyldighetsdato = LocalDate.now()
        val kretsnummer = "SPILLER INGEN ROLLE HER FORDI VI HAR IKKE GRENSEENDRINGER"

        val (utkast, utkastValidering) = testValiderKretsDelingIUtkast(gyldighetsdato, kretsnummer, Flatetype.GRUNNKRETS, Grenseendringer())

        assertThat(utkastValidering).isInstanceOf(UtkastValidering.UgyldigUtkast::class.java)
        assertThat((utkastValidering as UtkastValidering.UgyldigUtkast).feil.message).isEqualTo("Krets(er) med navn: " +
            "${utkast.operasjoner?.kretsDelingEndringer?.first()?.nyeKretser?.first()?.kretsNavn} " +
            "har ikke fått noen grenser tildelt. Dette er en feil, og den kan rettes opp ved å sette tilhørighet på de relevante grensene som skal høre til enten: " +
            "${utkast.operasjoner?.kretsDelingEndringer?.first()?.nyeKretser?.first()?.kretsNavn}")
    }

    @Test
    fun `utkast er gyldig med deling og grensejusteringer som refererer til stemmekrets`() {
        val gyldighetsdato = LocalDate.now()

        val kretsnummer = "50"
        val kommuneLokalid = Lokalid("1")

        val featureSomPekerTilStemmekretsMedKretsnummer = FeatureBuilder()
            .withGrensetype(Grensetype.Stemmekretsgrense)
            .withKontekstEgenskaper(
                listOf(KontekstEgenskaper(
                    id = ObjektIdentifikator(Lokalid("G2"), gyldighetsdato),
                    kommuneId = ObjektIdentifikator(kommuneLokalid, gyldighetsdato),
                    type = KontekstEnhetType.STEMMEKRETS,
                    version = 1,
                    kretsNummer = kretsnummer
                ), KontekstEgenskaper(
                    id = ObjektIdentifikator(Lokalid("G3"), gyldighetsdato),
                    type = KontekstEnhetType.STEMMEKRETS,
                    version = 1
                )
            ))
            .build()

        val grenseendringer = Grenseendringer(endredeFeatures = listOf(featureSomPekerTilStemmekretsMedKretsnummer))

        every { grenseServiceMock.hentAlleVersjonerMedGyldigFraEtterGittDato(any(), any()) } returns listOf()

        val res = testValiderKretsDelingIUtkast(gyldighetsdato, kretsnummer, Flatetype.STEMMEKRETS, grenseendringer)

        assertThat(res.second).isInstanceOf(UtkastValidering.KlartForPublisering::class.java)
    }

    @Test
    fun `utkast er gyldig med deling og grensejusteringer som refererer til grunnkrets`(){
        val gyldighetsdato = LocalDate.now()

        val kretsnummer = "03012354"
        val kommuneLokalid = Lokalid("1")

        val featureSomPekerTilStemmekretsMedKretsnummer = FeatureBuilder()
            .withGrensetype(Grensetype.Grunnkretsgrense)
            .withKontekstEgenskaper(
                listOf(KontekstEgenskaper(
                    id = ObjektIdentifikator(Lokalid("G2"), gyldighetsdato),
                    kommuneId = ObjektIdentifikator(kommuneLokalid, gyldighetsdato),
                    type = KontekstEnhetType.GRUNNKRETS,
                    version = 1,
                    kretsNummer = kretsnummer
                ), KontekstEgenskaper(
                    id = ObjektIdentifikator(Lokalid("G3"), gyldighetsdato),
                    type = KontekstEnhetType.GRUNNKRETS,
                    version = 1
                )
                ))
            .build()

        val grenseendringer = Grenseendringer(endredeFeatures = listOf(featureSomPekerTilStemmekretsMedKretsnummer))

        every { grenseServiceMock.hentAlleVersjonerMedGyldigFraEtterGittDato(any(), any()) } returns listOf()

        val res = testValiderKretsDelingIUtkast(gyldighetsdato, kretsnummer, Flatetype.GRUNNKRETS, grenseendringer)

        assertThat(res.second).isInstanceOf(UtkastValidering.KlartForPublisering::class.java)
    }

    @Test
    fun `utkast er gyldig uten grenseendringer hvis det ikke er en deling i utkastet`() {
        val utkast = Utkast(navn = "test", operasjoner = Operasjoner(
            metadataendringer =
            Metadataendringer(
                grunnkretsendringer =
                mapOf(
                    "1" to GrunnkretsRequestBuilder.DEFAULT_REQUEST,
                    "2" to GrunnkretsRequestBuilder(lokalId = "2").build(),
                )
            ),
        ))

        every { grunnkretsServiceMock.hentAlleVersjonerMedGyldigFraEtterGittDato(any(), any()) } returns listOf()

        val utkastValidering = utkastValidatorService.validateUtkastKlartForPublisering(utkast)
        assertThat(utkastValidering).isInstanceOf(UtkastValidering.KlartForPublisering::class.java)
    }

    @Test
    fun `utkast er ugyldig dersom det kommer inn flere grenseendring-features med samme id`() {
        val grenseendringFeature1 = FeatureBuilder("1").build()
        val grenseendringFeature2 = FeatureBuilder("2").build()

        val validateGyldigeOperasjoner = utkastValidatorService.validateGyldigeOperasjoner(Operasjoner(
            grenseendringer = Grenseendringer(endredeFeatures = listOf(grenseendringFeature1, grenseendringFeature1, grenseendringFeature2, grenseendringFeature2))
        ))
        assertThat(validateGyldigeOperasjoner).isInstanceOf(OperasjonerValidering.UgyldigeOperasjoner::class.java)
        assertThat((validateGyldigeOperasjoner as OperasjonerValidering.UgyldigeOperasjoner).feil.message).contains(
            "Utkast inneholder grenseendringer for samme grense(r) flere ganger. Grensene (representert ved lokalid) det gjelder er: [1, 2]")
    }

    @Test
    fun `utkast er ugyldig hvis tilhørighet for endrede features er ugyldig`(){
        val gyldighetsdato = LocalDate.now()
        val featureMedFeilAntallTilhorigheter = FeatureBuilder("1")
            .withGrensetype(Grensetype.Grunnkretsgrense)
            .withKontekstEgenskaper(listOf())
            .build()
        val featureMedToLikeTilhorigheter = FeatureBuilder("1")
            .withGrensetype(Grensetype.Grunnkretsgrense)
            .withKontekstEgenskaper(
                listOf(KontekstEgenskaper(
                    id = ObjektIdentifikator(Lokalid("G3"), gyldighetsdato),
                    type = KontekstEnhetType.GRUNNKRETS,
                    version = 1,
                ), KontekstEgenskaper(
                    id = ObjektIdentifikator(Lokalid("G3"), gyldighetsdato),
                    type = KontekstEnhetType.GRUNNKRETS,
                    version = 1
                ))
            ).build()
        val kommuneGrenseFeatureMedTilhorigheterISammeKommune = FeatureBuilder("1")
            .withGrensetype(Grensetype.Kommunegrense)
            .withKontekstEgenskaper(
                listOf(KontekstEgenskaper(
                    id = ObjektIdentifikator(Lokalid("G3"), gyldighetsdato),
                    kommuneId = ObjektIdentifikator(Lokalid("1"), gyldighetsdato),
                    type = KontekstEnhetType.GRUNNKRETS,
                    version = 1,
                ), KontekstEgenskaper(
                    id = ObjektIdentifikator(Lokalid("G2"), gyldighetsdato),
                    kommuneId = ObjektIdentifikator(Lokalid("1"), gyldighetsdato),
                    type = KontekstEnhetType.GRUNNKRETS,
                    version = 1
                ))
            )
            .build()
        val featureMedToFeil = FeatureBuilder("1")
            .withGrensetype(Grensetype.Kommunegrense)
            .withKontekstEgenskaper(
                listOf(KontekstEgenskaper(
                    id = ObjektIdentifikator(Lokalid("G3"), gyldighetsdato),
                    kommuneId = ObjektIdentifikator(Lokalid("1"), gyldighetsdato),
                    type = KontekstEnhetType.GRUNNKRETS,
                    version = 1,
                ), KontekstEgenskaper(
                    id = ObjektIdentifikator(Lokalid("G3"), gyldighetsdato),
                    kommuneId = ObjektIdentifikator(Lokalid("1"), gyldighetsdato),
                    type = KontekstEnhetType.GRUNNKRETS,
                    version = 1
                ))
            )
            .build()
        val utkast1 = Utkast(navn = "test", operasjoner = Operasjoner(grenseendringer =
            Grenseendringer(endredeFeatures = listOf(
                featureMedFeilAntallTilhorigheter
            ))
        ))
        val utkast2 = Utkast(navn = "test", operasjoner = Operasjoner(grenseendringer =
            Grenseendringer(endredeFeatures = listOf(
                featureMedToLikeTilhorigheter
            ))
        ))
        val utkast3 = Utkast(navn = "test", operasjoner = Operasjoner(grenseendringer =
            Grenseendringer(endredeFeatures = listOf(
                kommuneGrenseFeatureMedTilhorigheterISammeKommune
            ))
        ))
        val utkast4 = Utkast(navn = "test", operasjoner = Operasjoner(grenseendringer =
            Grenseendringer(endredeFeatures = listOf(
                featureMedToFeil
            ))
        ))

        val utkast1Validering = utkastValidatorService.validerTilhorighetEndringer(utkast1)
        assertThat(utkast1Validering).isInstanceOf(UtkastValidering.UgyldigUtkast::class.java)
        assertThat((utkast1Validering as UtkastValidering.UgyldigUtkast).feil.message)
            .contains("Grensen har 0 tilhørighet(er) satt")

        val utkast2Validering = utkastValidatorService.validerTilhorighetEndringer(utkast2)
        assertThat(utkast2Validering).isInstanceOf(UtkastValidering.UgyldigUtkast::class.java)
        assertThat((utkast2Validering as UtkastValidering.UgyldigUtkast).feil.message)
            .contains("Grensen har tilhørighet til samme krets på hver side")

        val utkast3Validering = utkastValidatorService.validerTilhorighetEndringer(utkast3)
        assertThat(utkast3Validering).isInstanceOf(UtkastValidering.UgyldigUtkast::class.java)
        assertThat((utkast3Validering as UtkastValidering.UgyldigUtkast).feil.message)
            .contains("Grensen er kommunegrense og har tilhørighet til to kretser i samme kommune")

        val utkast4Validering = utkastValidatorService.validerTilhorighetEndringer(utkast4)
        assertThat(utkast4Validering).isInstanceOf(UtkastValidering.UgyldigUtkast::class.java)
        assertThat((utkast4Validering as UtkastValidering.UgyldigUtkast).feil.message)
            .contains("Grense med id: 1 har ugyldig tilhørighet. Årsak(er): \n" +
                "\t- Grensen har tilhørighet til samme krets på hver side.\n" +
                "\t- Grensen er kommunegrense og har tilhørighet til to kretser i samme kommune.\n")
    }

    @Test
    fun `valider at godkjente operasjoner gir gyldigUtkast`() {
        val operasjoner =
            Operasjoner(
                metadataendringer =
                Metadataendringer(
                    grunnkretsendringer =
                    mapOf(
                        "1" to GrunnkretsRequestBuilder.DEFAULT_REQUEST,
                        "2" to GrunnkretsRequestBuilder(lokalId = "2").build(),
                    ),
                    stemmekretsendringer =
                    mapOf("1" to StemmekretsRequestBuilder(stemmekretsnummer = "123").build())
                ),
                stemmekretsSammenslaaingsendring =
                StemmekretsSammenslaaingsendringRequest(
                    viderefoertStemmekrets = IdentifikatorMedVersjon("1", 5),
                    stemmekretserTilSammenslaaing = listOf(IdentifikatorMedVersjon("2", 1)),
                    navn = "gyldigNavn",
                    nummer = "4"
                ),
                grunnkretsSammenslaaingsendring =
                GrunnkretsSammenslaaingsendringRequest(
                    viderefoertGrunnkrets = IdentifikatorMedVersjon("1", 5),
                    grunnkretserTilSammenslaaing = listOf(IdentifikatorMedVersjon("2", 1)),
                    navn = "gyldigNavn",
                    nummer = "03010567"
                ),
                grenseendringer = operasjonerMedKunEditerbareGrenser().grenseendringer
            )
        val gyldigUtkast = UtkastBuilder.builder("Gyldignavn")
            .withOperasjoner(operasjoner)
            .build()

        val validateGyldigUtkastResult = utkastValidatorService.validateGyldigUtkast(gyldigUtkast)
        assertThat(validateGyldigUtkastResult).isInstanceOf(UtkastValidering.GyldigUtkast::class.java)
    }

    @Test
    fun `validateGyldigUtkast utkast uten operasjoner gir gyldig utkast`() {
        val utkastUtenOperasjoner = UtkastBuilder.builder("Utkast for publisering")
            .build()

        val validateGyldigUtkastResult = utkastValidatorService.validateGyldigUtkast(utkastUtenOperasjoner)
        assertThat(validateGyldigUtkastResult).isInstanceOf(UtkastValidering.GyldigUtkast::class.java)
    }
}
