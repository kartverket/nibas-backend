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
import no.kartverket.nibas.service.utkast.UtkastValidatorService
import no.kartverket.nibas.utils.GrenseHelper
import no.kartverket.nibas.utkast.UtkastServiceTest.Companion.operasjonerMedIkkeEditerbareGrenser
import no.kartverket.nibas.utkast.UtkastServiceTest.Companion.operasjonerMedKunEditerbareGrenser
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.locationtech.jts.geom.Coordinate
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

    private val june2024 = LocalDate.of(2024, 6, 1)

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
        every { currentTime.asLocalDate() } returns june2024
        every { grunnkretsServiceMock.hentGrunnkrets(any()) } returns GrunnkretsBuilder.DEFAULT_GRUNNKRETS
        every { grunnkretsServiceMock.hentAlleVersjonerMedGyldigFraEtterGittDato(any(), any()) } returns emptyList()

        every { stemmekretsServiceMock.hentAlleVersjonerMedGyldigFraEtterGittDato(any(), any()) } returns emptyList()

        every { grenseServiceMock.hentAlleVersjonerMedGyldigFraEtterGittDato(any(), any()) } returns emptyList()

        utkastValidatorService = UtkastValidatorService(grunnkretsServiceMock, grenseServiceMock, stemmekretsServiceMock, kodelisteServiceMock, currentTime)
    }

    @Test
    fun `valider utkast med annen status enn opprettet gir ugyldig utkast`() { //publiserUtkastMedAnnenStatusEnnOpprettetGirBadRequestEx
        val publisertUtkast = UtkastBuilder.builder("Utkast for publisering", gyldigFra = june2024)
            .withStatus(UtkastStatus.PUBLISERT)
            .build()

        val validateUtkastKlartForPublisering = utkastValidatorService.validateUtkastKlartForPublisering(publisertUtkast, june2024)
        assertThat(validateUtkastKlartForPublisering).contains("Kan ikke publisere et utkast uten endringer.")
    }

    @Test
    fun `valider utkast uten operasjoner gir ugyldig utkast`() {
        val utkastUtenOperasjoner = UtkastBuilder.builder("Utkast for publisering", gyldigFra = june2024)
            .build()

        val validateUtkastKlartForPublisering = utkastValidatorService.validateUtkastKlartForPublisering(utkastUtenOperasjoner, june2024)
        assertThat(validateUtkastKlartForPublisering).contains("Kan ikke publisere et utkast uten endringer.")
    }

    @Test
    fun `valider operasjoner med ikke-editerbare grenser gir ugyldige operasjoner`() {
        val operasjoner = operasjonerMedIkkeEditerbareGrenser()

        val validateGyldigeOperasjoner = utkastValidatorService.validateGyldigeOperasjoner(operasjoner)
        assertThat(validateGyldigeOperasjoner).contains("Utkastet inneholder 1 ikke-redigerbare grenser.")
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

        val validateGyldigeOperasjoner = utkastValidatorService.validateGyldigeOperasjoner(operasjoner)
        assertThat(validateGyldigeOperasjoner).contains("Kretsnavn kan ikke være tomt.")
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

        val validateGyldigeOperasjoner = utkastValidatorService.validateGyldigeOperasjoner(operasjoner)
        assertThat(validateGyldigeOperasjoner).contains("Kretsnavn kan ikke være tomt.")
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
                    nummer = "1234567",
                    informasjon = "Informasjon om sammenslaaingsendring",
                )
            )

        val validateGyldigeOperasjoner = utkastValidatorService.validateGyldigeOperasjoner(operasjoner)
        assertThat(validateGyldigeOperasjoner)
            .contains("Stemmekretsnummer (1234567) kan ikke være på flere enn fire tegn.")
            .contains("Kretsnavn kan ikke være tomt.")
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

        val validateGyldigeOperasjoner = utkastValidatorService.validateGyldigeOperasjoner(operasjoner)
        assertThat(validateGyldigeOperasjoner).contains("Kretsnummer kan ikke være tomt.")
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

        val validateGyldigeOperasjoner = utkastValidatorService.validateGyldigeOperasjoner(operasjoner)
        assertThat(validateGyldigeOperasjoner).contains("Kretsnummer (0) kan ikke være 0.")
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
                    nummer = "ugyldigNummer",
                    informasjon = "Informasjon om sammenslaaingsendring",
                )
            )

        val validateGyldigeOperasjoner = utkastValidatorService.validateGyldigeOperasjoner(operasjoner)
        assertThat(validateGyldigeOperasjoner).contains("Kretsnummer (ugyldigNummer) kan bare inneholde sifre.")
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

        val validateGyldigeOperasjoner = utkastValidatorService.validateGyldigeOperasjoner(operasjoner)
        assertThat(validateGyldigeOperasjoner).contains("Grunnkretsnummer (1234567) må være på åtte tegn.")
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

        val validateGyldigeOperasjoner = utkastValidatorService.validateGyldigeOperasjoner(operasjoner)
        assertThat(validateGyldigeOperasjoner).isNotNull.contains("Grunnkretsnummer (123456789) må være på åtte tegn.")
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

        val validateGyldigeOperasjoner = utkastValidatorService.validateGyldigeOperasjoner(operasjoner)
        assertThat(validateGyldigeOperasjoner).contains("Kretsnummer (-12345678) kan ikke være negativt.")
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

        val validateGyldigeOperasjoner = utkastValidatorService.validateGyldigeOperasjoner(operasjoner)
        assertThat(validateGyldigeOperasjoner).contains("Stemmekretsnummer (12345) kan ikke være på flere enn fire tegn.")
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

        val validateGyldigeOperasjoner = utkastValidatorService.validateGyldigeOperasjoner(operasjoner)
        assertThat(validateGyldigeOperasjoner).contains("Kretsnummer (-123) kan ikke være negativt.")
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
        val validateGyldigeOperasjoner = utkastValidatorService.validateGyldigeOperasjoner(operasjoner)
        assertThat(validateGyldigeOperasjoner).contains("De fire første sifrene i grunnkretsnummer (12345678) må være kommunenummer (0301).")
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
                    nummer = "4",
                    informasjon = "Informasjon om sammenslaaingsendring",
                ),
                grenseendringer = operasjonerMedKunEditerbareGrenser().grenseendringer
            )

        val validateGyldigeOperasjoner = utkastValidatorService.validateGyldigeOperasjoner(operasjoner)
        assertThat(validateGyldigeOperasjoner).isEmpty()
    }

    private fun testValiderKretsDelingIUtkast(gyldighetsdato: LocalDate, kretsnummer: String, flatetype: Flatetype, grenseendringer: Grenseendringer): Pair<Utkast, List<String>> {
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
        ), gyldigFra = june2024)

        return Pair(utkast, utkastValidatorService.validateUtkastKlartForPublisering(utkast, june2024))
    }


    @Test
    fun `utkast er ugyldig med deling uten grenseendringer som refererer til ny krets`() {
        val gyldighetsdato = LocalDate.now()
        val kretsnummer = "25"

        val (utkast, utkastValidering) = testValiderKretsDelingIUtkast(gyldighetsdato, kretsnummer, Flatetype.GRUNNKRETS, operasjonerMedKunEditerbareGrenser().grenseendringer)

        assertThat(utkastValidering).isNotEmpty
            .contains("Grunnkretsnummer (25) må være på åtte tegn.")
            .contains("Krets(er) med navn: " +
                "${utkast.operasjoner?.kretsDelingEndringer?.first()?.nyeKretser?.first()?.kretsNavn} " +
                "har ikke fått noen grenser tildelt. Dette er en feil, og den kan rettes opp ved å sette tilhørighet på de relevante grensene som skal høre til enten: " +
                "${utkast.operasjoner?.kretsDelingEndringer?.first()?.nyeKretser?.first()?.kretsNavn}")
            .contains("Grense med id: 4 har ugyldig tilhørighet. Årsak(er):")
            .contains("	- Grensen har 2 tilhørighet(er) satt, men må ha 4.")
            .contains("	- Grensen har 1 tilhørighetstyper satt, men må ha 2.")
            .contains("	- Grensen er kommune-/fylkesgrense og har tilhørighet til to kretser i samme kommune.")

    }

    @Test
    fun `utkast er ugyldig med deling uten grenseendringer`() {
        val gyldighetsdato = LocalDate.now()
        val kretsnummer = "SPILLER INGEN ROLLE HER FORDI VI HAR IKKE GRENSEENDRINGER"

        val (utkast, utkastValidering) = testValiderKretsDelingIUtkast(gyldighetsdato, kretsnummer, Flatetype.GRUNNKRETS, Grenseendringer())

        assertThat(utkastValidering).isNotEmpty
            .contains("Krets(er) med navn: " +
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
        assertThat(res.second).isEmpty()
    }

    @Test
    fun `utkast er gyldig med deling og grensejusteringer som refererer til grunnkrets`() {
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
        assertThat(res.second).isEmpty()
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
        ), gyldigFra = june2024)

        every { grunnkretsServiceMock.hentAlleVersjonerMedGyldigFraEtterGittDato(any(), any()) } returns listOf()

        val utkastValidering = utkastValidatorService.validateUtkastKlartForPublisering(utkast, june2024)
        assertThat(utkastValidering).isEmpty()
    }

    @Test
    fun `utkast er ugyldig dersom det kommer inn flere grenseendring-features med samme id`() {
        val grenseendringFeature1 = FeatureBuilder("1").build()
        val grenseendringFeature2 = FeatureBuilder("2").build()

        val validateGyldigeOperasjoner = utkastValidatorService.validateGyldigeOperasjoner(Operasjoner(
            grenseendringer = Grenseendringer(endredeFeatures = listOf(grenseendringFeature1, grenseendringFeature1, grenseendringFeature2, grenseendringFeature2))
        ))
        assertThat(validateGyldigeOperasjoner)
            .contains("Utkastet inneholder grenseendringer for samme grense(r) flere ganger. Grensene (representert ved lokalid) det gjelder er: [1, 2].")
    }

    @Test
    fun `utkast uten kontekstegenskaper er ugyldig`() {

        val featureUtenTilhorigheter = FeatureBuilder("1")
            .withGrensetype(Grensetype.Grunnkretsgrense)
            .withKontekstEgenskaper(listOf())
            .build()
        val utkast1 = Utkast(navn = "test",
            operasjoner = Operasjoner(grenseendringer = Grenseendringer(endredeFeatures = listOf(featureUtenTilhorigheter))), gyldigFra = LocalDate.now())

        assertThat(utkastValidatorService.validerTilhorighetEndringer(utkast1).joinToString("\n"))
            .contains("Grensen har 0 tilhørighet(er) satt, men må ha 2.")
            .contains("Grensen har 0 tilhørighetstyper satt, men må ha 1.")
    }

    @Test
    fun `utkast med grunnkretsgrensefeature med kun en tilhørighet er ugyldig`() {
        val gyldighetsdato = LocalDate.now()
        val featureMedFeilAntallTilhorigheter = FeatureBuilder("1")
            .withGrensetype(Grensetype.Grunnkretsgrense)
            .withKontekstEgenskaper(listOf(KontekstEgenskaper(
                id = ObjektIdentifikator(Lokalid("G3"), gyldighetsdato),
                type = KontekstEnhetType.GRUNNKRETS,
                version = 1,
            )))
            .build()
        val utkast1b = Utkast(navn = "test",
            operasjoner = Operasjoner(grenseendringer = Grenseendringer(endredeFeatures = listOf(featureMedFeilAntallTilhorigheter))), gyldigFra = gyldighetsdato)

        assertThat(utkastValidatorService.validerTilhorighetEndringer(utkast1b).joinToString("\n"))
            .contains("Grensen har 1 tilhørighet(er) satt")
    }

    @Test
    fun `utkast med grunnkretsgrensefeature med to nye tilhørigheter er gyldig`() {
        val featureMedFeilToNyeTilhorigheter = FeatureBuilder("1")
            .withGrensetype(Grensetype.Grunnkretsgrense)
            .withKontekstEgenskaper(listOf(KontekstEgenskaper(
                type = KontekstEnhetType.GRUNNKRETS,
                kretsNummer = "12345678",
                version = 1,
            ), KontekstEgenskaper(
                type = KontekstEnhetType.GRUNNKRETS,
                kretsNummer = "12345679",
                version = 1,
            )))
            .build()
        val utkast1c = Utkast(navn = "test",
            operasjoner = Operasjoner(grenseendringer = Grenseendringer(endredeFeatures = listOf(featureMedFeilToNyeTilhorigheter))), gyldigFra = LocalDate.now())
        assertThat(utkastValidatorService.validerTilhorighetEndringer(utkast1c)).isEmpty()
    }

    @Test
    fun `utkast med grunnkretsgrensefeature med tilhørigheter med samme lokalid og likt kretsnr er ugyldig`() {
        val gyldighetsdato = LocalDate.now()
        val featureMedToLikeTilhorigheter = FeatureBuilder("1")
            .withGrensetype(Grensetype.Grunnkretsgrense)
            .withKontekstEgenskaper(
                listOf(KontekstEgenskaper(
                    id = ObjektIdentifikator(Lokalid("G3"), gyldighetsdato),
                    type = KontekstEnhetType.GRUNNKRETS,
                    kretsNummer = "12345678",
                    version = 1,
                ), KontekstEgenskaper(
                    id = ObjektIdentifikator(Lokalid("G3"), gyldighetsdato),
                    type = KontekstEnhetType.GRUNNKRETS,
                    kretsNummer = "12345678",
                    version = 1
                ))
            ).build()
        val utkast2 = Utkast(navn = "test",
            operasjoner = Operasjoner(grenseendringer = Grenseendringer(endredeFeatures = listOf(featureMedToLikeTilhorigheter))), gyldigFra = gyldighetsdato)

        assertThat(utkastValidatorService.validerTilhorighetEndringer(utkast2).joinToString("\n"))
            .contains("Grensen har flere tilhørigheter til samme krets. Gjelder kretsnr 12345678 med lokalid G3")
    }

    @Test
    fun `utkast med grunnkretsgrensefeature med tilhørigheter med samme lokalid og ulikt kretsnr er ugyldig`() {
        val gyldighetsdato = LocalDate.now()
        val featureMedToTilhorigheterMedSammeLokalid = FeatureBuilder("1")
            .withGrensetype(Grensetype.Grunnkretsgrense)
            .withKontekstEgenskaper(
                listOf(KontekstEgenskaper(
                    id = ObjektIdentifikator(Lokalid("G3"), gyldighetsdato),
                    type = KontekstEnhetType.GRUNNKRETS,
                    kretsNummer = "11111111",
                    version = 1,
                ), KontekstEgenskaper(
                    id = ObjektIdentifikator(Lokalid("G3"), gyldighetsdato),
                    type = KontekstEnhetType.GRUNNKRETS,
                    kretsNummer = "12345678",
                    version = 1
                ))
            ).build()
        val utkast2b = Utkast(navn = "test",
            operasjoner = Operasjoner(grenseendringer = Grenseendringer(endredeFeatures = listOf(featureMedToTilhorigheterMedSammeLokalid))), gyldigFra = gyldighetsdato)

        assertThat(utkastValidatorService.validerTilhorighetEndringer(utkast2b).joinToString("\n"))
            .contains("Grensen har flere tilhørigheter til samme krets")
    }

    @Test
    fun `utkast med grunnkretsgrensefeature med tilhørigheter til samme grunnkrets er ugyldig`() {
        val gyldighetsdato = LocalDate.now()
        val featureMedToTilhorigheterMedSammeData = FeatureBuilder("1")
            .withGrensetype(Grensetype.Grunnkretsgrense)
            .withKontekstEgenskaper(
                listOf(KontekstEgenskaper(
                    type = KontekstEnhetType.GRUNNKRETS,
                    kretsNummer = "12345678",
                    version = 1,
                ), KontekstEgenskaper(
                    id = ObjektIdentifikator(Lokalid("G3"), gyldighetsdato),
                    type = KontekstEnhetType.GRUNNKRETS,
                    kretsNummer = "12345678",
                    version = 1
                ))
            ).build()
        val utkast2c = Utkast(navn = "test",
            operasjoner = Operasjoner(grenseendringer = Grenseendringer(endredeFeatures = listOf(featureMedToTilhorigheterMedSammeData))), gyldigFra = gyldighetsdato)

        assertThat(utkastValidatorService.validerTilhorighetEndringer(utkast2c).joinToString("\n"))
            .contains("Grensen har flere tilhørigheter til samme krets. Gjelder kretsnr 12345678")
    }

    @Test
    fun `utkast med kommunegrensefeature med kun to grunnkretstilhørigheter i samme kommune er ugyldig`() {
        val gyldighetsdato = LocalDate.now()
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
        val utkast3 = Utkast(navn = "test",
            operasjoner = Operasjoner(grenseendringer = Grenseendringer(endredeFeatures = listOf(kommuneGrenseFeatureMedTilhorigheterISammeKommune))),
            gyldigFra = gyldighetsdato)

        assertThat(utkastValidatorService.validerTilhorighetEndringer(utkast3).joinToString("\n"))
            .contains("Grensen har 2 tilhørighet(er) satt, men må ha 4")
            .contains("Grensen har 1 tilhørighetstyper satt, men må ha 2")
            .contains("Grensen er kommune-/fylkesgrense og har tilhørighet til to kretser i samme kommune")
    }

    @Test
    fun `utkast med fylkegrensefeature med to tilhørigheter til samme grunnkrets er ugyldig`() {
        val gyldighetsdato = LocalDate.now()
        val featureMedToFeil = FeatureBuilder("1")
            .withGrensetype(Grensetype.Fylkesgrense)
            .withKontekstEgenskaper(
                listOf(KontekstEgenskaper(
                    id = ObjektIdentifikator(Lokalid("G3"), gyldighetsdato),
                    kommuneId = ObjektIdentifikator(Lokalid("1"), gyldighetsdato),
                    type = KontekstEnhetType.GRUNNKRETS,
                    kretsNummer = "12345678",
                    version = 1,
                ), KontekstEgenskaper(
                    id = ObjektIdentifikator(Lokalid("G3"), gyldighetsdato),
                    kommuneId = ObjektIdentifikator(Lokalid("1"), gyldighetsdato),
                    type = KontekstEnhetType.GRUNNKRETS,
                    kretsNummer = "12345678",
                    version = 1
                ))
            )
            .build()
        val utkast4 = Utkast(navn = "test",
            operasjoner = Operasjoner(grenseendringer = Grenseendringer(endredeFeatures = listOf(featureMedToFeil))), gyldigFra = gyldighetsdato)

        assertThat(utkastValidatorService.validerTilhorighetEndringer(utkast4).joinToString("\n"))
            .contains("Grense med id: 1 har ugyldig tilhørighet. Årsak(er)")
            .contains("Grensen har 2 tilhørighet(er) satt, men må ha 4")
            .contains("Grensen har 1 tilhørighetstyper satt, men må ha 2")
            .contains("Grensen har flere tilhørigheter til samme krets. Gjelder kretsnr 12345678 med lokalid G3")
    }

    @Test
    fun `utkast med kommunegrensefeature med to tilhørigheter er ugyldig`() {
        val gyldighetsdato = LocalDate.now()
        val featureMedStemmekretsOgGrunnkrets = FeatureBuilder("1")
            .withGrensetype(Grensetype.Kommunegrense)
            .withKontekstEgenskaper(
                listOf(KontekstEgenskaper(
                    id = ObjektIdentifikator(Lokalid("G3"), gyldighetsdato),
                    kommuneId = ObjektIdentifikator(Lokalid("1"), gyldighetsdato),
                    type = KontekstEnhetType.GRUNNKRETS,
                    kretsNummer = "12345678",
                    version = 1,
                ), KontekstEgenskaper(
                    id = ObjektIdentifikator(Lokalid("S3"), gyldighetsdato),
                    kommuneId = ObjektIdentifikator(Lokalid("1"), gyldighetsdato),
                    type = KontekstEnhetType.STEMMEKRETS,
                    kretsNummer = "1234",
                    version = 1
                ))
            )
            .build()
        val utkast5 = Utkast(navn = "test",
            operasjoner = Operasjoner(grenseendringer = Grenseendringer(endredeFeatures = listOf(featureMedStemmekretsOgGrunnkrets))), gyldigFra = gyldighetsdato)

        assertThat(utkastValidatorService.validerTilhorighetEndringer(utkast5).joinToString("\n"))
            .contains("Grense med id: 1 har ugyldig tilhørighet. Årsak(er)")
            .contains("Grensen har 2 tilhørighet(er) satt, men må ha 4")
            .doesNotContain("Grensen har 1 tilhørighetstyper satt, men må ha 2")
    }

    @Test
    fun `utkast med kommunegrensefeature med tre tilhørigheter er ugyldig`() {
        val gyldighetsdato = LocalDate.now()
        val featureMedTreKontekstegenskaper = FeatureBuilder("1")
            .withGrensetype(Grensetype.Kommunegrense)
            .withKontekstEgenskaper(
                listOf(KontekstEgenskaper(
                    id = ObjektIdentifikator(Lokalid("G3"), gyldighetsdato),
                    kommuneId = ObjektIdentifikator(Lokalid("1"), gyldighetsdato),
                    type = KontekstEnhetType.GRUNNKRETS,
                    kretsNummer = "12345678",
                    version = 1,
                ), KontekstEgenskaper(
                    id = ObjektIdentifikator(Lokalid("S3"), gyldighetsdato),
                    kommuneId = ObjektIdentifikator(Lokalid("1"), gyldighetsdato),
                    type = KontekstEnhetType.STEMMEKRETS,
                    kretsNummer = "1234",
                    version = 1
                ), KontekstEgenskaper(
                    id = ObjektIdentifikator(Lokalid("S4"), gyldighetsdato),
                    kommuneId = ObjektIdentifikator(Lokalid("2"), gyldighetsdato),
                    type = KontekstEnhetType.STEMMEKRETS,
                    kretsNummer = "1234",
                    version = 1
                ))
            )
            .build()
        val utkast6 = Utkast(navn = "test",
            operasjoner = Operasjoner(grenseendringer = Grenseendringer(endredeFeatures = listOf(featureMedTreKontekstegenskaper))), gyldigFra = gyldighetsdato)

        assertThat(utkastValidatorService.validerTilhorighetEndringer(utkast6).joinToString("`n"))
            .contains("Grensen har 3 tilhørighet(er) satt, men må ha 4")
            .doesNotContain("Grensen har 1 tilhørighetstyper satt, men må ha 2")
            .doesNotContain("Grensen er kommune-/fylkesgrense og har tilhørighet til to kretser i samme kommune")
    }

    @Test
    fun `utkast med kommunegrensefeature med tilhørigheterer til samme kommune er ugyldig`() {
        val gyldighetsdato = LocalDate.now()
        val featureMedFireKontekstegenskaper = FeatureBuilder("1")
            .withGrensetype(Grensetype.Kommunegrense)
            .withKontekstEgenskaper(
                listOf(KontekstEgenskaper(
                    id = ObjektIdentifikator(Lokalid("G1"), gyldighetsdato),
                    kommuneId = ObjektIdentifikator(Lokalid("1"), gyldighetsdato),
                    type = KontekstEnhetType.GRUNNKRETS,
                    kretsNummer = "12345678",
                    version = 1,
                ), KontekstEgenskaper(
                    id = ObjektIdentifikator(Lokalid("G3"), gyldighetsdato),
                    kommuneId = ObjektIdentifikator(Lokalid("1"), gyldighetsdato),
                    type = KontekstEnhetType.GRUNNKRETS,
                    kretsNummer = "87654321",
                    version = 1,
                ), KontekstEgenskaper(
                    id = ObjektIdentifikator(Lokalid("S3"), gyldighetsdato),
                    kommuneId = ObjektIdentifikator(Lokalid("1"), gyldighetsdato),
                    type = KontekstEnhetType.STEMMEKRETS,
                    kretsNummer = "1234",
                    version = 1
                ), KontekstEgenskaper(
                    id = ObjektIdentifikator(Lokalid("S4"), gyldighetsdato),
                    kommuneId = ObjektIdentifikator(Lokalid("1"), gyldighetsdato),
                    type = KontekstEnhetType.STEMMEKRETS,
                    kretsNummer = "4321",
                    version = 1
                ))
            )
            .build()
        val utkast7 = Utkast(navn = "test",
            operasjoner = Operasjoner(grenseendringer = Grenseendringer(endredeFeatures = listOf(featureMedFireKontekstegenskaper))), gyldigFra = gyldighetsdato)

        assertThat(utkastValidatorService.validerTilhorighetEndringer(utkast7).joinToString("\n"))
            .contains("Grensen er kommune-/fylkesgrense og har tilhørighet til to kretser i samme kommune")
    }

    @Test
    fun `utkast med kommunegrensefeature med gyldige tilhørigheter er gyldig`() {
        val gyldighetsdato = LocalDate.now()
        val korrektKommunegrenseFeature = FeatureBuilder("1")
            .withGrensetype(Grensetype.Kommunegrense)
            .withKontekstEgenskaper(
                listOf(KontekstEgenskaper(
                    id = ObjektIdentifikator(Lokalid("G3"), gyldighetsdato),
                    kommuneId = ObjektIdentifikator(Lokalid("1"), gyldighetsdato),
                    type = KontekstEnhetType.GRUNNKRETS,
                    kretsNummer = "12345678",
                    version = 1,
                ), KontekstEgenskaper(
                    id = ObjektIdentifikator(Lokalid("G4"), gyldighetsdato),
                    kommuneId = ObjektIdentifikator(Lokalid("2"), gyldighetsdato),
                    type = KontekstEnhetType.GRUNNKRETS,
                    kretsNummer = "87654321",
                    version = 1,
                ), KontekstEgenskaper(
                    id = ObjektIdentifikator(Lokalid("S3"), gyldighetsdato),
                    kommuneId = ObjektIdentifikator(Lokalid("1"), gyldighetsdato),
                    type = KontekstEnhetType.STEMMEKRETS,
                    kretsNummer = "1234",
                    version = 1
                ), KontekstEgenskaper(
                    id = ObjektIdentifikator(Lokalid("S4"), gyldighetsdato),
                    kommuneId = ObjektIdentifikator(Lokalid("2"), gyldighetsdato),
                    type = KontekstEnhetType.STEMMEKRETS,
                    kretsNummer = "1234",
                    version = 1
                ))
            )
            .build()
        val utkastOk = Utkast(navn = "test",
            operasjoner = Operasjoner(grenseendringer = Grenseendringer(endredeFeatures = listOf(korrektKommunegrenseFeature))), gyldigFra = gyldighetsdato)
        assertThat(utkastValidatorService.validerTilhorighetEndringer(utkastOk)).isEmpty()
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
                    nummer = "4",
                    informasjon = "Informasjon om sammenslaaingsendring",
                ),
                grunnkretsSammenslaaingsendring =
                GrunnkretsSammenslaaingsendringRequest(
                    viderefoertGrunnkrets = IdentifikatorMedVersjon("1", 5),
                    grunnkretserTilSammenslaaing = listOf(IdentifikatorMedVersjon("2", 1)),
                    navn = "gyldigNavn",
                    nummer = "03010567",
                    informasjon = "Informasjon om sammenslaaingsendring",
                ),
                grenseendringer = operasjonerMedKunEditerbareGrenser().grenseendringer
            )
        val gyldigUtkast = UtkastBuilder.builder("Gyldignavn", gyldigFra = LocalDate.now())
            .withOperasjoner(operasjoner)
            .build()

        val validateGyldigUtkastResult =
            utkastValidatorService.validateUtkastMedOperasjoner(gyldigUtkast, gyldigUtkast.auditInfo.typeEndring.value, june2024)
        assertThat(validateGyldigUtkastResult).isEmpty()
    }

    @Test
    fun `valider utkast uten operasjoner gir gyldig utkast`() {
        val utkastUtenOperasjoner = UtkastBuilder.builder("Utkast uten operasjoner", gyldigFra = LocalDate.now())
            .build()

        val validateGyldigUtkastResult =
            utkastValidatorService.validateUtkastMedOperasjoner(utkastUtenOperasjoner, utkastUtenOperasjoner.auditInfo.typeEndring.value, june2024)
        assertThat(validateGyldigUtkastResult).isEmpty()
    }

    @Test
    fun `valider at metadataoperasjoner med 0 som tellekretsnummer gir feilmelding`() {
        val operasjoner =
            Operasjoner(
                metadataendringer =
                Metadataendringer(
                    stemmekretsendringer =
                    mapOf("1" to StemmekretsRequestBuilder(tellekretsnummer = "0").build())
                )
            )

        val valideringsmelding = utkastValidatorService.validateTellekretserOgValgdistrikt(operasjoner)
        assertThat(valideringsmelding).contains("Tellekretsnummer (0) kan ikke være 0 (stemmekrets 138).")
    }

    @Test
    fun `valider at metadataoperasjoner med negativt tellekretsnummer gir feilmelding`() {
        val operasjoner =
            Operasjoner(
                metadataendringer =
                Metadataendringer(
                    stemmekretsendringer =
                    mapOf("1" to StemmekretsRequestBuilder(tellekretsnummer = "-1").build())
                )
            )

        val valideringsmelding = utkastValidatorService.validateTellekretserOgValgdistrikt(operasjoner)
        assertThat(valideringsmelding).contains("Tellekretsnummer (-1) kan ikke være negativt (stemmekrets 138).")
    }

    @Test
    fun `valider at metadataoperasjoner med tekstlig tellekretsnummer gir feilmelding`() {
        val operasjoner =
            Operasjoner(
                metadataendringer =
                Metadataendringer(
                    stemmekretsendringer =
                    mapOf("1" to StemmekretsRequestBuilder(tellekretsnummer = "xx").build())
                )
            )

        val valideringsmelding = utkastValidatorService.validateTellekretserOgValgdistrikt(operasjoner)
        assertThat(valideringsmelding).contains("Tellekretsnummer (xx) kan bare inneholde sifre (stemmekrets 138).")
    }

    @Test
    fun `valider at metadataoperasjoner med flere enn 4 tegn i tellekretsnummer gir feilmelding`() {
        val operasjoner =
            Operasjoner(
                metadataendringer =
                Metadataendringer(
                    stemmekretsendringer =
                    mapOf("1" to StemmekretsRequestBuilder(tellekretsnummer = "00005").build())
                )
            )

        val valideringsmelding = utkastValidatorService.validateTellekretserOgValgdistrikt(operasjoner)
        assertThat(valideringsmelding).contains("Tellekretsnummer (00005) kan ikke være på flere enn fire tegn (stemmekrets 138).")
    }

    @Test
    fun `valider at metadataoperasjoner med blankt tellekretsnummer med tellekretsnavn gir feilmelding`() {
        val operasjoner =
            Operasjoner(
                metadataendringer =
                Metadataendringer(
                    stemmekretsendringer =
                    mapOf("1" to StemmekretsRequestBuilder(tellekretsnummer = "").build())
                )
            )

        val valideringsmelding = utkastValidatorService.validateTellekretserOgValgdistrikt(operasjoner)
        assertThat(valideringsmelding).contains("Tellekretsnummer må gis når tellekretsnavn er gitt (Mosekollen minikrets 3) (stemmekrets 138).")
    }

    @Test
    fun `valider at metadataoperasjoner med null tellekretsnummer med tellekretsnavn gir feilmelding`() {
        val operasjoner =
            Operasjoner(
                metadataendringer =
                Metadataendringer(
                    stemmekretsendringer =
                    mapOf("1" to StemmekretsRequestBuilder(tellekretsnummer = null).build())
                )
            )

        val valideringsmelding = utkastValidatorService.validateTellekretserOgValgdistrikt(operasjoner)
        assertThat(valideringsmelding).contains("Tellekretsnummer må gis når tellekretsnavn er gitt (Mosekollen minikrets 3) (stemmekrets 138).")
    }

    @Test
    fun `valider at metadataoperasjoner med tellekretsnummer med blankt tellekretsnavn gir feilmelding`() {
        val operasjoner =
            Operasjoner(
                metadataendringer =
                Metadataendringer(
                    stemmekretsendringer =
                    mapOf("1" to StemmekretsRequestBuilder(tellekretsnavn = "").build())
                )
            )

        val valideringsmelding = utkastValidatorService.validateTellekretserOgValgdistrikt(operasjoner)
        assertThat(valideringsmelding).contains("Tellekretsnavn må gis når tellekretsnummer er gitt (13) (stemmekrets 138).")
    }

    @Test
    fun `valider at metadataoperasjoner med tellekretsnummer med null tellekretsnavn gir feilmelding`() {
        val operasjoner =
            Operasjoner(
                metadataendringer =
                Metadataendringer(
                    stemmekretsendringer =
                    mapOf("1" to StemmekretsRequestBuilder(tellekretsnavn = null).build())
                )
            )

        val valideringsmelding = utkastValidatorService.validateTellekretserOgValgdistrikt(operasjoner)
        assertThat(valideringsmelding).contains("Tellekretsnavn må gis når tellekretsnummer er gitt (13) (stemmekrets 138).")
    }

    @Test
    fun `valider at metadataoperasjoner med null valgdistriktsnummer gir feilmelding`() {
        val operasjoner =
            Operasjoner(
                metadataendringer =
                Metadataendringer(
                    stemmekretsendringer =
                    mapOf("1" to StemmekretsRequestBuilder(valgdistriktsnummer = null).build())
                )
            )

        val valideringsmelding = utkastValidatorService.validateTellekretserOgValgdistrikt(operasjoner)
        assertThat(valideringsmelding).contains("Valgdistriktsnummer må gis (stemmekrets 138).")
    }

    @Test
    fun `valider at metadataoperasjoner med blankt valgdistriktsnummer gir feilmelding`() {
        val operasjoner =
            Operasjoner(
                metadataendringer =
                Metadataendringer(
                    stemmekretsendringer =
                    mapOf("1" to StemmekretsRequestBuilder(valgdistriktsnummer = " ").build())
                )
            )

        val valideringsmelding = utkastValidatorService.validateTellekretserOgValgdistrikt(operasjoner)
        assertThat(valideringsmelding).contains("Valgdistriktsnummer må gis (stemmekrets 138).")
    }

    @Test
    fun `valider grunnkretsgrense med ulik kommune er ugyldig`() {
        val feature = FeatureBuilder("1")
            .withGrensetype(Grensetype.Grunnkretsgrense)
            .withKontekstEgenskaper(listOf(
                KontekstEgenskaper(
                    type = KontekstEnhetType.GRUNNKRETS,
                    kretsNummer = "33010907",
                    version = 1
                ),
                KontekstEgenskaper(
                    type = KontekstEnhetType.GRUNNKRETS,
                    // Delområde og kommune er ulik
                    kretsNummer = "33021007",
                    version = 1
                )
            ))
            .build()

        val utkast = UtkastBuilder.builder("Test utkast", gyldigFra = june2024)
            .withOperasjoner(Operasjoner(grenseendringer = Grenseendringer(endredeFeatures = listOf(feature))))
            .build()

        val errors = utkastValidatorService.validerTilhorighetEndringer(utkast)

        assertThat(errors).hasSize(4)
        assertThat(errors).anyMatch { it.contains("er av type Grunnkretsgrense, men har ulik kommune på hver side") }
    }

    @Test
    fun `valider grunnkretsgrense med samme kommune og delområde er gyldig`() {
        val feature = FeatureBuilder("1")
            .withGrensetype(Grensetype.Grunnkretsgrense)
            .withKontekstEgenskaper(listOf(
                KontekstEgenskaper(
                    type = KontekstEnhetType.GRUNNKRETS,
                    kretsNummer = "33010907",
                    version = 1
                ),
                KontekstEgenskaper(
                    type = KontekstEnhetType.GRUNNKRETS,
                    kretsNummer = "33010908",
                    version = 1
                )
            ))
            .build()

        val utkast = UtkastBuilder.builder("Test utkast", gyldigFra = june2024)
            .withOperasjoner(Operasjoner(grenseendringer = Grenseendringer(endredeFeatures = listOf(feature))))
            .build()

        val errors = utkastValidatorService.validerTilhorighetEndringer(utkast)
        assertThat(errors).isEmpty()
    }


    @Test
    fun `valider delområdegrense med ulik kommune er ugyldig`() {
        val feature = FeatureBuilder("1")
            .withGrensetype(Grensetype.Delomraadegrense)
            .withKontekstEgenskaper(listOf(
                KontekstEgenskaper(
                    type = KontekstEnhetType.GRUNNKRETS,
                    kretsNummer = "33010907",
                    version = 1
                ),
                KontekstEgenskaper(
                    type = KontekstEnhetType.GRUNNKRETS,
                    kretsNummer = "33021107",
                    version = 1
                )
            ))
            .build()

        val utkast = UtkastBuilder.builder("Test utkast", gyldigFra = june2024)
            .withOperasjoner(Operasjoner(grenseendringer = Grenseendringer(endredeFeatures = listOf(feature))))
            .build()

        val errors = utkastValidatorService.validerTilhorighetEndringer(utkast)

        assertThat(errors).hasSize(4)
        assertThat(errors).anyMatch { it.contains("Grense er av type Delomraadegrense, men har ulik kommune på hver side:") }
    }

    @Test
    fun `valider delområdegrense med samme kommune og ulik delområde er gyldig`() {
        val feature = FeatureBuilder("1")
            .withGrensetype(Grensetype.Delomraadegrense)
            .withKontekstEgenskaper(listOf(
                KontekstEgenskaper(
                    type = KontekstEnhetType.GRUNNKRETS,
                    kretsNummer = "33010907",
                    version = 1
                ),
                KontekstEgenskaper(
                    type = KontekstEnhetType.GRUNNKRETS,
                    kretsNummer = "33011107",
                    version = 1
                )
            ))
            .build()

        val utkast = UtkastBuilder.builder("Test utkast", gyldigFra = june2024)
            .withOperasjoner(Operasjoner(grenseendringer = Grenseendringer(endredeFeatures = listOf(feature))))
            .build()

        val errors = utkastValidatorService.validerTilhorighetEndringer(utkast)
        assertThat(errors).isEmpty()
    }

    @Test
    fun `valider stemmekretsgrense med ulik kommune er ugyldig`() {
        val feature = FeatureBuilder("1")
            .withGrensetype(Grensetype.Stemmekretsgrense)
            .withKontekstEgenskaper(listOf(
                KontekstEgenskaper(
                    type = KontekstEnhetType.STEMMEKRETS,
                    kommuneId = ObjektIdentifikator(Lokalid("3301"), june2024),
                    kretsNummer = "12",
                    version = 1
                ),
                KontekstEgenskaper(
                    type = KontekstEnhetType.STEMMEKRETS,
                    kommuneId = ObjektIdentifikator(Lokalid("3302"), june2024),
                    kretsNummer = "13",
                    version = 1
                )
            ))
            .build()

        val utkast = UtkastBuilder.builder("Test utkast", gyldigFra = june2024)
            .withOperasjoner(Operasjoner(grenseendringer = Grenseendringer(endredeFeatures = listOf(feature))))
            .build()

        val errors = utkastValidatorService.validerTilhorighetEndringer(utkast)
        
        assertThat(errors).hasSize(4)
        assertThat(errors).anyMatch { it.contains("er av type Stemmekretsgrense, men har ulik kommune på hver side") }
    }

    @Test
    fun `valider stemmekretsgrense med samme kommune er gyldig`() {
        val feature = FeatureBuilder("1")
            .withGrensetype(Grensetype.Stemmekretsgrense)
            .withKontekstEgenskaper(listOf(
                KontekstEgenskaper(
                    type = KontekstEnhetType.STEMMEKRETS,
                    kommuneId = ObjektIdentifikator(Lokalid("3302"), june2024),
                    id = ObjektIdentifikator(Lokalid("stemmekrets1"), june2024),
                    version = 1
                ),
                KontekstEgenskaper(
                    type = KontekstEnhetType.STEMMEKRETS,
                    kommuneId = ObjektIdentifikator(Lokalid("3302"), june2024),
                    id = ObjektIdentifikator(Lokalid("stemmekrets2"), june2024),
                    version = 1
                )
            ))
            .build()

        val utkast = UtkastBuilder.builder("Test utkast", gyldigFra = june2024)
            .withOperasjoner(Operasjoner(grenseendringer = Grenseendringer(endredeFeatures = listOf(feature))))
            .build()

        val errors = utkastValidatorService.validerTilhorighetEndringer(utkast)
        assertThat(errors).isEmpty()
    }

    @Test
    fun `valider fylkesgrense med samme fylke er ugyldig`() {
        val feature = FeatureBuilder("1")
            .withGrensetype(Grensetype.Fylkesgrense)
            .withKontekstEgenskaper(listOf(
                KontekstEgenskaper(
                    type = KontekstEnhetType.GRUNNKRETS,
                    kretsNummer = "33010907",
                    id = ObjektIdentifikator(Lokalid("grunnkrets1"), june2024),
                    kommuneId = ObjektIdentifikator(Lokalid("3301"), june2024),
                    version = 1
                ),
                KontekstEgenskaper(
                    type = KontekstEnhetType.GRUNNKRETS,
                    kretsNummer = "33020907",
                    id = ObjektIdentifikator(Lokalid("grunnkrets2"), june2024),
                    kommuneId = ObjektIdentifikator(Lokalid("3302"), june2024),
                    version = 1
                ),
                KontekstEgenskaper(
                    type = KontekstEnhetType.STEMMEKRETS,
                    kretsNummer = "33010001",
                    id = ObjektIdentifikator(Lokalid("stemmekrets1"), june2024),
                    kommuneId = ObjektIdentifikator(Lokalid("3301"), june2024),
                    version = 1
                ),
                KontekstEgenskaper(
                    type = KontekstEnhetType.STEMMEKRETS,
                    kretsNummer = "33020001",
                    id = ObjektIdentifikator(Lokalid("stemmekrets2"), june2024),
                    kommuneId = ObjektIdentifikator(Lokalid("3302"), june2024),
                    version = 1
                )
            ))
            .build()

        val utkast = UtkastBuilder.builder("Test utkast", gyldigFra = june2024)
            .withOperasjoner(Operasjoner(grenseendringer = Grenseendringer(endredeFeatures = listOf(feature))))
            .build()

        val errors = utkastValidatorService.validerTilhorighetEndringer(utkast)
        
        assertThat(errors).hasSize(4)
        assertThat(errors).anyMatch { it.contains("er av type Fylkesgrense, men har samme fylke på hver side") }
    }

    @Test
    fun `valider fylkesgrense med ulik fylke er gyldig`() {
        val feature = FeatureBuilder("1")
            .withGrensetype(Grensetype.Fylkesgrense)
            .withKontekstEgenskaper(listOf(
                KontekstEgenskaper(
                    type = KontekstEnhetType.GRUNNKRETS,
                    kretsNummer = "33010907",
                    id = ObjektIdentifikator(Lokalid("grunnkrets1"), june2024),
                    kommuneId = ObjektIdentifikator(Lokalid("3301"), june2024),
                    version = 1
                ),
                KontekstEgenskaper(
                    type = KontekstEnhetType.GRUNNKRETS,
                    kretsNummer = "34010907",
                    id = ObjektIdentifikator(Lokalid("grunnkrets2"), june2024),
                    kommuneId = ObjektIdentifikator(Lokalid("3401"), june2024),
                    version = 1
                ),
                KontekstEgenskaper(
                    type = KontekstEnhetType.STEMMEKRETS,
                    kretsNummer = "33010001",
                    id = ObjektIdentifikator(Lokalid("stemmekrets1"), june2024),
                    kommuneId = ObjektIdentifikator(Lokalid("3301"), june2024),
                    version = 1
                ),
                KontekstEgenskaper(
                    type = KontekstEnhetType.STEMMEKRETS,
                    kretsNummer = "34010001",
                    id = ObjektIdentifikator(Lokalid("stemmekrets2"), june2024),
                    kommuneId = ObjektIdentifikator(Lokalid("3401"), june2024),
                    version = 1
                )
            ))
            .build()

        val utkast = UtkastBuilder.builder("Test utkast", gyldigFra = june2024)
            .withOperasjoner(Operasjoner(grenseendringer = Grenseendringer(endredeFeatures = listOf(feature))))
            .build()

        val errors = utkastValidatorService.validerTilhorighetEndringer(utkast)
        assertThat(errors).isEmpty()
    }

    @Test
    fun `utkast med grunnkretsgrensefeature med avrundede koordinater er gyldig`() {
        val featureMedFeilToNyeTilhorigheter = FeatureBuilder("1")
            .withGrensetype(Grensetype.Grunnkretsgrense)
            .withGeometry(GrenseHelper.geometryFactory.createLineString(
                arrayOf(
                    Coordinate(0.005, 0.0),
                    Coordinate(10.0, 5.005)
                )
            ))
            .withKontekstEgenskaper(listOf(KontekstEgenskaper(
                type = KontekstEnhetType.GRUNNKRETS,
                kretsNummer = "12345678",
                version = 1,
            ), KontekstEgenskaper(
                type = KontekstEnhetType.GRUNNKRETS,
                kretsNummer = "12345679",
                version = 1,
            )))
            .build()
        val gyldighetsdato = LocalDate.now()
        val utkast1c = Utkast(navn = "test",
            operasjoner = Operasjoner(grenseendringer = Grenseendringer(endredeFeatures = listOf(featureMedFeilToNyeTilhorigheter))), gyldigFra = gyldighetsdato)
        assertThat(utkastValidatorService.validateAvrundedeKoordinater(utkast1c.operasjoner!!).isEmpty())
    }

    @Test
    fun `utkast med grunnkretsgrensefeature med uavrundede koordinater er ugyldig`() {
        val featureMedFeilToNyeTilhorigheter = FeatureBuilder("1")
            .withGrensetype(Grensetype.Grunnkretsgrense)
            .withGeometry(GrenseHelper.geometryFactory.createLineString(
                arrayOf(
                    Coordinate(0.001, 0.0),
                    Coordinate(10.004, 5.008)
                )
            ))
            .withKontekstEgenskaper(listOf(KontekstEgenskaper(
                type = KontekstEnhetType.GRUNNKRETS,
                kretsNummer = "12345678",
                version = 1,
            ), KontekstEgenskaper(
                type = KontekstEnhetType.GRUNNKRETS,
                kretsNummer = "12345679",
                version = 1,
            )))
            .build()
        val gyldighetsdato = LocalDate.now()
        val utkast1c = Utkast(navn = "test",
            operasjoner = Operasjoner(grenseendringer = Grenseendringer(endredeFeatures = listOf(featureMedFeilToNyeTilhorigheter))), gyldigFra = gyldighetsdato)
        val errors = utkastValidatorService.validateAvrundedeKoordinater(utkast1c.operasjoner!!).joinToString("\n")

        assertThat(errors).isNotEmpty()
            .contains("Utkastet inneholder 2 koordinater med for mange desimaler")
            .contains("Nord: 0.0 Øst: 0.001")
            .contains("Nord: 5.008 Øst: 10.004")
    }

    @Test
    fun `utkast med grunnkretsgrensefeature med uavrundedet kurvepunkt er ugyldig`() {
        val featureMedFeilToNyeTilhorigheter = FeatureBuilder("1")
            .withGrensetype(Grensetype.Grunnkretsgrense)
            .withGeometry(GrenseHelper.geometryFactory.createLineString(
                arrayOf(
                    Coordinate(0.005, 0.0),
                    Coordinate(5.0055, 0.0),
                    Coordinate(10.005, 5.005)
                )
            ))
            .withKontekstEgenskaper(listOf(KontekstEgenskaper(
                type = KontekstEnhetType.GRUNNKRETS,
                kretsNummer = "12345678",
                version = 1,
            ), KontekstEgenskaper(
                type = KontekstEnhetType.GRUNNKRETS,
                kretsNummer = "12345679",
                version = 1,
            )))
            .build()
        val gyldighetsdato = LocalDate.now()
        val utkast1c = Utkast(navn = "test",
            operasjoner = Operasjoner(grenseendringer = Grenseendringer(endredeFeatures = listOf(featureMedFeilToNyeTilhorigheter))), gyldigFra = gyldighetsdato)
        val errors = utkastValidatorService.validateAvrundedeKoordinater(utkast1c.operasjoner!!).joinToString("\n")

        assertThat(errors).isNotEmpty()
            .contains("Utkastet inneholder 1 koordinat med for mange desimaler")
            .contains("Nord: 0.0 Øst: 5.0055")
    }

}
