package no.kartverket.nibas.utkast

import no.kartverket.nibas.api.v1.common.Flatetype
import no.kartverket.nibas.api.v1.common.Grenseendringer
import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.api.v1.common.Metadataendringer
import no.kartverket.nibas.api.v1.common.ObjektIdentifikator
import no.kartverket.nibas.api.v1.common.Operasjoner
import no.kartverket.nibas.api.v1.common.geojson.metadata.Identifikasjon
import no.kartverket.nibas.api.v1.common.geojson.metadata.IdentifikatorMedVersjon
import no.kartverket.nibas.api.v1.request.GrunnkretsRequest
import no.kartverket.nibas.api.v1.request.KretsDelingEndringRequest
import no.kartverket.nibas.api.v1.request.KretsNavnOgNummer
import no.kartverket.nibas.api.v1.request.StemmekretsSammenslaaingsendringRequest
import no.kartverket.nibas.builders.BopliktomraadeRequestBuilder
import no.kartverket.nibas.builders.GrunnkretsRequestBuilder
import no.kartverket.nibas.builders.OpprettUtkastRequestBuilder
import no.kartverket.nibas.builders.StemmekretsRequestBuilder
import no.kartverket.nibas.builders.UtkastRequestBuilder
import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.events.NibasEventPublisher
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.domain.Endringstype
import no.kartverket.nibas.domain.utkast.UtkastBuilder
import no.kartverket.nibas.events.EventType
import no.kartverket.nibas.events.PublishEventException
import no.kartverket.nibas.extensions.geometry.grenseToFeature
import no.kartverket.nibas.repository.OptimistiskLaasException
import no.kartverket.nibas.testcontainer.TestWithDbContainer
import no.kartverket.nibas.utils.GrenseHelper
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.ZonedDateTime
import kotlin.test.assertFailsWith

@ActiveProfiles(profiles = ["localhost", "eventsDisabled"])
@Transactional
class UtkastServiceIT : TestWithDbContainer() {

    @Autowired
    private lateinit var nibasEventPublisher: NibasEventPublisher

    @Test
    fun opprettUtkast() {
        val navn = "TESTUTKAST"

        val timeAtTestStart = ZonedDateTime.now()

        val utkastRequest = OpprettUtkastRequestBuilder()
            .withNavn(navn)
            .withGyldigFra(timeAtTestStart.toLocalDate())
            .withEndringstype(Endringstype.KVALITETSHEVING)
            .build()

        val utkastId = utkastService.opprettUtkast(utkastRequest)
        val utkast = utkastService.hentUtkast(utkastId)!!

        assertThat(utkast).isNotNull
        assertThat(utkast.navn).isEqualTo(navn)
        assertThat(utkast.auditInfo.typeEndring).isEqualTo(Endringstype.KVALITETSHEVING)
        assertThat(utkast.opprettetDato).isAfterOrEqualTo(timeAtTestStart)
        assertThat(utkast.opprettetDato).isBeforeOrEqualTo(ZonedDateTime.now())
        assertThat(utkast.auditInfo.endretAv).isEqualTo("DUMMY")
    }

    @Test
    fun `skal gi optimistisklaasexception om man har en grenseendring paa utdatert versjon av grense`() {
        val grenser = grenseRepository.saveAllAndFlush(GrenseHelper.getGrenserForVestligKrets(grensetype = Grensetype.Grunnkretsgrense))
        val kommune = saveKommune()
        val grunnkrets = saveGrunnkrets(lokalid = Lokalid("test1"), grunnkretsnavn = "Gammelt navn")
        val grunnkrets2 = saveGrunnkrets(id = "test2", lokalid = Lokalid("test2"), grunnkretsnavn = "Gammelt navn2")

        grenser.forEach {
            grunnkrets.addGrensekobling(it)
            grunnkrets2.addGrensekobling(it)
        }

        kommune.addGrunnkrets(grunnkrets)
        kommune.addGrunnkrets(grunnkrets2)

        val grenseSomEndres = grenser.first().grenseToFeature()
        grenseSomEndres.properties = grenseSomEndres.properties.copy(version = 0)

        val grenseendringer = Grenseendringer(endredeFeatures = listOf(grenseSomEndres))

        val utkastRequestBuilder = UtkastRequestBuilder()
            .withOperasjoner(Operasjoner(grenseendringer = grenseendringer))

        val utkast = UtkastBuilder.builder("test", gyldigFra = LocalDate.now())
            .withOperasjoner(Operasjoner(grenseendringer = grenseendringer))
            .build()
        utkastRepository.save(utkast)

        val oppdaterutkastexception = assertFailsWith<OptimistiskLaasException> {
            utkastService.oppdaterUtkast(utkast.id, utkastRequestBuilder
                .withOperasjoner(Operasjoner(grenseendringer = grenseendringer))
                .withVersjon(2)
                .build())
        }
        assertThat(oppdaterutkastexception.message).contains("Det er ikke samsvar med versjon")
        assertThat(oppdaterutkastexception.konflikter.first().getMessage())
            .contains("Det er ikke samsvar med versjon. Ønsket versjon = 0, eksisterende versjon = 1 for objekt med id = grenseVest")

        val validerutkastexception = assertFailsWith<OptimistiskLaasException> {
            utkastService.validerUtkast(utkast.id, utkastRequestBuilder
                .withOperasjoner(Operasjoner(grenseendringer = grenseendringer))
                .withVersjon(2)
                .build())
        }
        assertThat(validerutkastexception.message).contains("Det er ikke samsvar med versjon")
        assertThat(validerutkastexception.konflikter.first().getMessage())
            .contains("Det er ikke samsvar med versjon. Ønsket versjon = 0, eksisterende versjon = 1 for objekt med id = grenseVest")

        val publiserutkastexception = assertFailsWith<OptimistiskLaasException> {
            utkastService.publiserUtkast(utkast.id)
        }
        assertThat(publiserutkastexception.message).contains("Det er ikke samsvar med versjon")
        assertThat(publiserutkastexception.konflikter.first().getMessage())
            .contains("Det er ikke samsvar med versjon. Ønsket versjon = 0, eksisterende versjon = 1 for objekt med id = grenseVest")
    }

    @Test
    fun `skal gi optimistisklaasexception om man har en grenseendring paa utdatert versjon av grunnkrets`() {
        val grenser = grenseRepository.saveAllAndFlush(GrenseHelper.getGrenserForVestligKrets(grensetype = Grensetype.Grunnkretsgrense))
        val kommune = saveKommune()
        val grunnkrets = saveGrunnkrets(lokalid = Lokalid("test1"), grunnkretsnavn = "Gammelt navn")
        val grunnkrets2 = saveGrunnkrets(id = "test2", lokalid = Lokalid("test2"), grunnkretsnavn = "Gammelt navn2")

        grenser.forEach {
            grunnkrets.addGrensekobling(it)
            grunnkrets2.addGrensekobling(it)
        }

        kommune.addGrunnkrets(grunnkrets)
        kommune.addGrunnkrets(grunnkrets2)

        val opprinneligGrunnkretsversjon = grunnkrets.version
        grunnkrets.version = 5
        val grenseSomEndres = grenser.first().grenseToFeature()
        grunnkrets.version = opprinneligGrunnkretsversjon

        val grenseendringer = Grenseendringer(endredeFeatures = listOf(grenseSomEndres))

        val utkastRequestBuilder = UtkastRequestBuilder()
            .withOperasjoner(Operasjoner(grenseendringer = grenseendringer))

        val utkast = UtkastBuilder.builder("test", gyldigFra = LocalDate.now())
            .withOperasjoner(Operasjoner(grenseendringer = grenseendringer))
            .build()
        utkastRepository.save(utkast)

        val oppdaterutkastexception = assertFailsWith<OptimistiskLaasException> {
            utkastService.oppdaterUtkast(utkast.id, utkastRequestBuilder
                .withVersjon(2)
                .build())
        }
        assertThat(oppdaterutkastexception.message).contains("Det er ikke samsvar med versjon")
        assertThat(oppdaterutkastexception.konflikter.first().getMessage())
            .contains("Det er ikke samsvar med versjon for grunnkrets 42270304 Gammelt navn. Ønsket versjon = 5, eksisterende versjon = 1 for objekt med id = grunnkrets-9000")

        val validerutkastexception = assertFailsWith<OptimistiskLaasException> {
            utkastService.validerUtkast(utkast.id, utkastRequestBuilder
                .withVersjon(2)
                .build())
        }
        assertThat(validerutkastexception.message).contains("Det er ikke samsvar med versjon")
        assertThat(validerutkastexception.konflikter.first().getMessage())
            .contains("Det er ikke samsvar med versjon for grunnkrets 42270304 Gammelt navn. Ønsket versjon = 5, eksisterende versjon = 1 for objekt med id = grunnkrets-9000")

        val publiserutkastexception = assertFailsWith<OptimistiskLaasException> {
            utkastService.publiserUtkast(utkast.id)
        }
        assertThat(publiserutkastexception.message).contains("Det er ikke samsvar med versjon")
        assertThat(publiserutkastexception.konflikter.first().getMessage())
            .contains("Det er ikke samsvar med versjon for grunnkrets 42270304 Gammelt navn. Ønsket versjon = 5, eksisterende versjon = 1 for objekt med id = grunnkrets-9000")
    }

    @Test
    fun `skal gi optimistisklaasexception om man har en grenseendring paa utdatert versjon av grense og grunnkrets`() {
        val grenser = grenseRepository.saveAllAndFlush(GrenseHelper.getGrenserForVestligKrets(grensetype = Grensetype.Grunnkretsgrense))
        val kommune = saveKommune()
        val grunnkrets = saveGrunnkrets(lokalid = Lokalid("test1"), grunnkretsnavn = "Gammelt navn")
        val grunnkrets2 = saveGrunnkrets(id = "test2", lokalid = Lokalid("test2"), grunnkretsnavn = "Gammelt navn2")

        grenser.forEach {
            grunnkrets.addGrensekobling(it)
            grunnkrets2.addGrensekobling(it)
        }

        kommune.addGrunnkrets(grunnkrets)
        kommune.addGrunnkrets(grunnkrets2)

        val opprinneligGrunnkretsversjon = grunnkrets.version
        grunnkrets.version = 5
        val grenseSomEndres = grenser.first().grenseToFeature()
        grenseSomEndres.properties = grenseSomEndres.properties.copy(version = 0)
        grunnkrets.version = opprinneligGrunnkretsversjon

        val grenseendringer = Grenseendringer(endredeFeatures = listOf(grenseSomEndres))

        val utkastRequestBuilder = UtkastRequestBuilder()
            .withOperasjoner(Operasjoner(grenseendringer = grenseendringer))

        val utkast = UtkastBuilder.builder("test", gyldigFra = LocalDate.now())
            .withOperasjoner(Operasjoner(grenseendringer = grenseendringer))
            .build()
        utkastRepository.save(utkast)

        val oppdaterutkastexception = assertFailsWith<OptimistiskLaasException> {
            utkastService.oppdaterUtkast(utkast.id, utkastRequestBuilder
                .withVersjon(2)
                .build())
        }
        assertThat(oppdaterutkastexception.message).contains("Det er ikke samsvar med versjon")
        assertThat(oppdaterutkastexception.konflikter).hasSize(2)
        oppdaterutkastexception.konflikter.toList().map { it.getMessage() }
        assertThat(oppdaterutkastexception.konflikter.toList().map { it.getMessage() })
            .contains("Det er ikke samsvar med versjon for grunnkrets 42270304 Gammelt navn. Ønsket versjon = 5, eksisterende versjon = 1 for objekt med id = grunnkrets-9000")
            .contains("Det er ikke samsvar med versjon. Ønsket versjon = 0, eksisterende versjon = 1 for objekt med id = grenseVest")

        val validerutkastexception = assertFailsWith<OptimistiskLaasException> {
            utkastService.validerUtkast(utkast.id, utkastRequestBuilder
                .withVersjon(2)
                .build())
        }
        assertThat(validerutkastexception.message).contains("Det er ikke samsvar med versjon")
        assertThat(validerutkastexception.konflikter).hasSize(2)
        validerutkastexception.konflikter.toList().map { it.getMessage() }
        assertThat(validerutkastexception.konflikter.toList().map { it.getMessage() })
            .contains("Det er ikke samsvar med versjon for grunnkrets 42270304 Gammelt navn. Ønsket versjon = 5, eksisterende versjon = 1 for objekt med id = grunnkrets-9000")
            .contains("Det er ikke samsvar med versjon. Ønsket versjon = 0, eksisterende versjon = 1 for objekt med id = grenseVest")

        val publiserutkastexception = assertFailsWith<OptimistiskLaasException> {
            utkastService.publiserUtkast(utkast.id)
        }
        assertThat(publiserutkastexception.message).contains("Det er ikke samsvar med versjon")
        assertThat(publiserutkastexception.konflikter).hasSize(2)
        publiserutkastexception.konflikter.toList().map { it.getMessage() }
        assertThat(publiserutkastexception.konflikter.toList().map { it.getMessage() })
            .contains("Det er ikke samsvar med versjon. Ønsket versjon = 0, eksisterende versjon = 1 for objekt med id = grenseVest")
            .contains("Det er ikke samsvar med versjon for grunnkrets 42270304 Gammelt navn. Ønsket versjon = 5, eksisterende versjon = 1 for objekt med id = grunnkrets-9000")
    }

    @Test
    fun `skal gi optimistisklaasexception om man har har en metadataendring paa utdatert versjon av en grunnkrets`() {
        val kommune = saveKommune()
        val grunnkrets = saveGrunnkrets(lokalid = Lokalid("test1"), grunnkretsnavn = "Gammelt navn")
        kommune.addGrunnkrets(grunnkrets)

        val grunnkretsEndring = GrunnkretsRequestBuilder()
            .withLokalId("test1")
            .withNavn("nytt navn")
            .withVersion(0)
            .build()

        val utkast = UtkastBuilder.builder("test", gyldigFra = LocalDate.now())
            .withOperasjoner(
                Operasjoner(
                    metadataendringer = Metadataendringer(
                        grunnkretsendringer = mapOf("test1" to grunnkretsEndring)
                    )
                )
            )
            .build()

        utkastRepository.save(utkast)

        assertThatThrownBy { utkastService.publiserUtkast(utkast.id) }
            .isInstanceOf(OptimistiskLaasException::class.java)
            .hasMessageContaining("Det er ikke samsvar med versjon")
            .matches {
                (it as OptimistiskLaasException).konflikter.first().getMessage()
                    .contains("Det er ikke samsvar med versjon for grunnkrets 42270304 Gammelt navn. Ønsket versjon = 0, eksisterende versjon = 1 for objekt med id = grunnkrets-9000")
            }
    }

    @Test
    fun `skal gi optimistisklaasexception om man har har en metadataendring paa utdatert versjon av et bopliktomraade`() {
        val kommune = saveKommune()

        val bopliktomraade = saveBopliktomraade(lokalid = Lokalid("test1"), bopliktomraadenavn = "Gammelt navn")
        kommune.addBopliktomraade(bopliktomraade)

        val bopliktomraadeEndring = BopliktomraadeRequestBuilder()
            .withLokalId("test1")
            .withNavn("nytt navn")
            .withVersion(0)
            .build()

        val utkast = UtkastBuilder.builder("test", gyldigFra = LocalDate.now())
            .withOperasjoner(
                Operasjoner(
                    metadataendringer = Metadataendringer(
                        bopliktomraadeendringer = mapOf("test1" to bopliktomraadeEndring)
                    )
                )
            )
            .build()

        utkastRepository.save(utkast)

        assertThatThrownBy { utkastService.publiserUtkast(utkast.id) }
            .isInstanceOf(OptimistiskLaasException::class.java)
            .hasMessageContaining("Det er ikke samsvar med versjon")
            .matches {
                (it as OptimistiskLaasException).konflikter.first().getMessage()
                    .contains("Det er ikke samsvar med versjon for bopliktomraade 01 Gammelt navn. Ønsket versjon = 0, eksisterende versjon = 1 for objekt med id = bopliktomraade-9000")
            }
    }

    @Test
    fun `skal gi optimistisklaasexception om man har en grenseendring paa utdatert versjon av grense og bopliktomraade`() {
        val grenser = grenseRepository.saveAllAndFlush(GrenseHelper.getGrenserForVestligKrets(grensetype = Grensetype.Bopliktgrense))
        val kommune = saveKommune()
        val bopliktomraade = saveBopliktomraade(lokalid = Lokalid("test1"), bopliktomraadenavn = "Gammelt navn")

        grenser.forEach {
            bopliktomraade.addGrensekobling(it)
        }

        kommune.addBopliktomraade(bopliktomraade)

        val opprinneligBopliktomraadeversjon = bopliktomraade.version
        bopliktomraade.version = 5
        val grenseSomEndres = grenser.first().grenseToFeature()
        grenseSomEndres.properties = grenseSomEndres.properties.copy(version = 0)
        bopliktomraade.version = opprinneligBopliktomraadeversjon

        val grenseendringer = Grenseendringer(endredeFeatures = listOf(grenseSomEndres))

        val utkastRequestBuilder = UtkastRequestBuilder()
            .withOperasjoner(Operasjoner(grenseendringer = grenseendringer))

        val utkast = UtkastBuilder.builder("test", gyldigFra = LocalDate.now())
            .withOperasjoner(Operasjoner(grenseendringer = grenseendringer))
            .build()
        utkastRepository.save(utkast)

        val oppdaterutkastexception = assertFailsWith<OptimistiskLaasException> {
            utkastService.oppdaterUtkast(utkast.id, utkastRequestBuilder
                .withVersjon(2)
                .build())
        }
        assertThat(oppdaterutkastexception.message).contains("Det er ikke samsvar med versjon")
        assertThat(oppdaterutkastexception.konflikter).hasSize(2)
        oppdaterutkastexception.konflikter.toList().map { it.getMessage() }
        assertThat(oppdaterutkastexception.konflikter.toList().map { it.getMessage() })
            .contains("Det er ikke samsvar med versjon for bopliktomraade 01 Gammelt navn. Ønsket versjon = 5, eksisterende versjon = 1 for objekt med id = bopliktomraade-9000")
            .contains("Det er ikke samsvar med versjon. Ønsket versjon = 0, eksisterende versjon = 1 for objekt med id = grenseVest")

        val validerutkastexception = assertFailsWith<OptimistiskLaasException> {
            utkastService.validerUtkast(utkast.id, utkastRequestBuilder
                .withVersjon(2)
                .build())
        }
        assertThat(validerutkastexception.message).contains("Det er ikke samsvar med versjon")
        assertThat(validerutkastexception.konflikter).hasSize(2)
        validerutkastexception.konflikter.toList().map { it.getMessage() }
        assertThat(validerutkastexception.konflikter.toList().map { it.getMessage() })
            .contains("Det er ikke samsvar med versjon for bopliktomraade 01 Gammelt navn. Ønsket versjon = 5, eksisterende versjon = 1 for objekt med id = bopliktomraade-9000")
            .contains("Det er ikke samsvar med versjon. Ønsket versjon = 0, eksisterende versjon = 1 for objekt med id = grenseVest")

        val publiserutkastexception = assertFailsWith<OptimistiskLaasException> {
            utkastService.publiserUtkast(utkast.id)
        }
        assertThat(publiserutkastexception.message).contains("Det er ikke samsvar med versjon")
        assertThat(publiserutkastexception.konflikter).hasSize(2)
        publiserutkastexception.konflikter.toList().map { it.getMessage() }
        assertThat(publiserutkastexception.konflikter.toList().map { it.getMessage() })
            .contains("Det er ikke samsvar med versjon. Ønsket versjon = 0, eksisterende versjon = 1 for objekt med id = grenseVest")
            .contains("Det er ikke samsvar med versjon for bopliktomraade 01 Gammelt navn. Ønsket versjon = 5, eksisterende versjon = 1 for objekt med id = bopliktomraade-9000")
    }

    @Test
    fun `skal gi optimistisklaasexception om man har en metadataendring paa utdatert versjon av en stemmekrets`() {
        saveStemmekrets(lokalid = Lokalid("test1"), stemmekretsnavn = "gammelt navn")

        val stemmekretsendring = StemmekretsRequestBuilder()
            .withLokalId("test1")
            .withNavn("nytt navn")
            .withVersion(0)
            .build()

        val utkast = UtkastBuilder.builder("test", gyldigFra = LocalDate.now())
            .withOperasjoner(
                Operasjoner(
                    metadataendringer = Metadataendringer(
                        stemmekretsendringer = mapOf("test1" to stemmekretsendring)
                    )
                )
            )
            .build()

        utkastRepository.save(utkast)

        assertThatThrownBy { utkastService.publiserUtkast(utkast.id) }
            .isInstanceOf(OptimistiskLaasException::class.java)
            .hasMessageContaining("Det er ikke samsvar med versjon")
    }

    @Test
    fun `skal gi optimistisklaasexception om man slaar sammen 2 stemmekretser hvor viderefoert er utdatert`() {
        saveStemmekrets(id = "test1", lokalid = Lokalid("test1"), stemmekretsnavn = "Gammelt navn1")
        saveStemmekrets(id = "test2", lokalid = Lokalid("test2"), stemmekretsnavn = "Gammelt navn2")

        val utkast = UtkastBuilder.builder("test", gyldigFra = LocalDate.now())
            .withOperasjoner(
                Operasjoner(
                    stemmekretsSammenslaaingsendring = StemmekretsSammenslaaingsendringRequest(
                        viderefoertStemmekrets = IdentifikatorMedVersjon(lokalId = "test1", version = 0),
                        stemmekretserTilSammenslaaing = listOf(IdentifikatorMedVersjon(lokalId = "test2", version = 1)),
                        navn = "Navn etter endring",
                        nummer = "22",
                        informasjon = "Informasjon om sammenslaaingsendring",
                    )
                )
            )
            .build()

        utkastRepository.save(utkast)

        assertThatThrownBy { utkastService.publiserUtkast(utkast.id) }
            .isInstanceOf(OptimistiskLaasException::class.java)
            .hasMessageContaining("Det er ikke samsvar med versjon")
            .matches {
                (it as OptimistiskLaasException).konflikter.first().getMessage()
                    .contains("Det er ikke samsvar med versjon for stemmekrets 9 Gammelt navn1. Ønsket versjon = 0, eksisterende versjon = 1 for objekt med id = test1")
            }
    }

    @Test
    fun `skal gi optimistisklaasexception om man slaar sammen 2 stemmekretser hvor 1 av krets i sammenslaaingen er utdatert`() {
        saveStemmekrets(id = "test1", lokalid = Lokalid("test1"), stemmekretsnavn = "gammelt navn1")
        saveStemmekrets(id = "test2", lokalid = Lokalid("test2"), stemmekretsnavn = "gammelt navn2")

        val utkast = UtkastBuilder.builder("test", gyldigFra = LocalDate.now())
            .withOperasjoner(
                Operasjoner(
                    stemmekretsSammenslaaingsendring = StemmekretsSammenslaaingsendringRequest(
                        viderefoertStemmekrets = IdentifikatorMedVersjon(lokalId = "test1", version = 1),
                        stemmekretserTilSammenslaaing = listOf(IdentifikatorMedVersjon(lokalId = "test2", version = 0)),
                        navn = "navn etter endring",
                        nummer = "22",
                        informasjon = "Informasjon om sammenslaaingsendring",
                    )
                )
            )
            .build()

        utkastRepository.save(utkast)

        assertThatThrownBy { utkastService.publiserUtkast(utkast.id) }
            .isInstanceOf(OptimistiskLaasException::class.java)
            .hasMessageContaining("Det er ikke samsvar med versjon")
    }

    @Test
    fun `skal gi optimistisklaasexception om man deler stemmekretser som er utdatert`() {
        saveStemmekrets(id = "test1", lokalid = Lokalid("test1"), stemmekretsnavn = "Gammelt navn1")

        val utkast = UtkastBuilder.builder("Testutkast", gyldigFra = LocalDate.now()).build()
        utkastRepository.save(utkast)

        val utkastRequestBuilder = UtkastRequestBuilder()
            .withNavn(utkast.navn)
            .withOperasjoner(Operasjoner(kretsDelingEndringer = listOf(KretsDelingEndringRequest(
                IdentifikatorMedVersjon("test1", 0),
                ObjektIdentifikator(Lokalid("1"), LocalDate.now()),
                flatetype = Flatetype.STEMMEKRETS,
                nyeKretser = listOf(KretsNavnOgNummer(kretsNavn = "Ny Stemmekrets", kretsNummer = "35")),
            ))))

        val utkastexception = assertFailsWith<OptimistiskLaasException> {
            utkastService.oppdaterUtkast(utkast.id, utkastRequestBuilder.build())
        }
        assertThat(utkastexception.message).contains("Det er ikke samsvar med versjon")
        assertThat(utkastexception.konflikter.first().getMessage())
            .contains("Det er ikke samsvar med versjon for utkast Testutkast. Ønsket versjon = 1, eksisterende versjon = 2")

        //valider skal gi samme feilmelding
        val validerutkastexception = assertFailsWith<OptimistiskLaasException> {
            utkastService.validerUtkast(utkast.id, utkastRequestBuilder.build())
        }
        assertThat(validerutkastexception.message).contains("Det er ikke samsvar med versjon")
        assertThat(validerutkastexception.konflikter.first().getMessage())
            .contains("Det er ikke samsvar med versjon for utkast Testutkast. Ønsket versjon = 1, eksisterende versjon = 2")

        //Retter utkastversjon, skal få annen feil
        val exception = assertFailsWith<OptimistiskLaasException> {
            utkastService.oppdaterUtkast(utkast.id, utkastRequestBuilder.withVersjon(2).build())
        }
        assertThat(exception.message).contains("Det er ikke samsvar med versjon")
        assertThat(exception.konflikter.first().getMessage())
            .contains("Det er ikke samsvar med versjon for stemmekrets 9 Gammelt navn1. Ønsket versjon = 0, eksisterende versjon = 1 for objekt med id = test1")

        //valider skal gi samme feilmelding
        val validerutkastexception2 = assertFailsWith<OptimistiskLaasException> {
            utkastService.validerUtkast(utkast.id, utkastRequestBuilder.withVersjon(2).build())
        }
        assertThat(validerutkastexception2.message).contains("Det er ikke samsvar med versjon")
        assertThat(validerutkastexception2.konflikter.first().getMessage())
            .contains("Det er ikke samsvar med versjon for stemmekrets 9 Gammelt navn1. Ønsket versjon = 0, eksisterende versjon = 1 for objekt med id = test1")
    }

    @Test
    fun `skal ikke gi optimistisklaasexception om man endrer samme grunnkrets i baade metadata og grenseendring saa lenge det er gyldig versjon`() {
        val grenser = grenseRepository.saveAllAndFlush(GrenseHelper.getGrenserForVestligKrets(grensetype = Grensetype.Grunnkretsgrense))
        val kommune = saveKommune()
        val grunnkrets = saveGrunnkrets(lokalid = Lokalid("test1"), grunnkretsnavn = "gammelt navn")
        val grunnkrets2 = saveGrunnkrets(id = "test2", lokalid = Lokalid("test2"), grunnkretsnavn = "gammelt navn2")

        grenser.forEach {
            grunnkrets.addGrensekobling(it)
            grunnkrets2.addGrensekobling(it)
        }

        kommune.addGrunnkrets(grunnkrets)
        kommune.addGrunnkrets(grunnkrets2)

        val grenseSomEndres = grenser.first().grenseToFeature()

        val grenseendringer = Grenseendringer(endredeFeatures = listOf(grenseSomEndres))

        val grunnkretsendring = GrunnkretsRequestBuilder()
            .withLokalId("test1")
            .withNavn("nytt navn")
            .withVersion(1)
            .build()

        val utkast = UtkastBuilder.builder("test", gyldigFra = LocalDate.now())
            .withOperasjoner(
                Operasjoner(
                    grenseendringer = grenseendringer,
                    metadataendringer = Metadataendringer(grunnkretsendringer = mapOf("test1" to grunnkretsendring))
                )
            )
            .build()

        utkastRepository.save(utkast)

        val eventsForPublisering = utkastService.publiserUtkast(utkast.id)
        assertThat(eventsForPublisering.eventRader).extracting("lokalid").containsExactlyInAnyOrder(grunnkrets.lokalid, grunnkrets2.lokalid)
        assertThat(eventsForPublisering.eventRader).extracting("flateType").containsOnly(Flatetype.GRUNNKRETS)
        assertThat(eventsForPublisering.eventRader).extracting("eventType").containsOnly(EventType.MODIFIED)
    }

    @Test
    fun publiserUtkastUtenEventRad() {
        assertThatThrownBy {
            nibasEventPublisher.publishNibasEvent(emptyList(), LocalDate.now())
        }
            .isInstanceOf(PublishEventException::class.java)
            .hasMessageContaining("Event må inneholde minst 1 EventRad")
    }

    @Test
    fun `publisering av utkast med passert gyldigFra-dato skal bruke dagens dato`(){
        val iDag = LocalDate.now()
        val iGår = iDag.minusDays(1)
        // opprett utkast med passert gyldig fra
        val utkast = UtkastBuilder.builder("test", gyldigFra = iGår).withOperasjoner(
            Operasjoner(
                metadataendringer = Metadataendringer(
                    grunnkretsendringer = mapOf("c54fb55e-15d0-4528-8b88-f58d6c74bd87" to GrunnkretsRequest(
                        "Moi2",
                        "42190204",
                        Identifikasjon("c54fb55e-15d0-4528-8b88-f58d6c74bd87"),
                        1
                    ))
                )
            )
        ).build()
        utkastRepository.save(utkast)

        // publiser
        utkastService.publiserUtkast(utkast.id)
        // verifiser at det går fint og at utkast og endring har dagens dato på gyldigFra feltet
        val publisertUtkast = utkastService.hentUtkast(utkast.id)!!
        val oppdatertGrunnkrets = grunnkretsService.hentGrunnkrets(Identifikator(Lokalid("c54fb55e-15d0-4528-8b88-f58d6c74bd87")))!!

        assertThat(publisertUtkast.gyldigFra).isEqualTo(iDag)
        assertThat(oppdatertGrunnkrets.gyldighet.gyldigFra).isEqualTo(iDag)
    }

    @Test
    fun `publisering av utkast med grunnkretsendring skal legge en grunnkretslokalid på endrede_inndelinger`(){
        val utkast = UtkastBuilder.builder("test", gyldigFra =  LocalDate.now()).withOperasjoner(
            Operasjoner(
                metadataendringer = Metadataendringer(
                    grunnkretsendringer = mapOf("c54fb55e-15d0-4528-8b88-f58d6c74bd87" to GrunnkretsRequest(
                        "Moi2",
                        "42190204",
                        Identifikasjon("c54fb55e-15d0-4528-8b88-f58d6c74bd87"),
                        1
                    ))
                )
            )
        ).build()
        utkastRepository.save(utkast)

        // publiser
        utkastService.publiserUtkast(utkast.id)
        val publisertUtkast = utkastService.hentUtkast(utkast.id)!!

        assertThat(publisertUtkast.endredeInndelinger!!.endredeGrunnkretser).contains("c54fb55e-15d0-4528-8b88-f58d6c74bd87")
    }
}
