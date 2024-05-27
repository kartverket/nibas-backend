package no.kartverket.nibas.utkast

import no.kartverket.nibas.api.v1.common.Flatetype
import no.kartverket.nibas.api.v1.common.Grenseendringer
import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.api.v1.common.Metadataendringer
import no.kartverket.nibas.api.v1.common.Operasjoner
import no.kartverket.nibas.api.v1.common.geojson.metadata.IdentifikatorMedVersjon
import no.kartverket.nibas.api.v1.request.StemmekretsSammenslaaingsendringRequest
import no.kartverket.nibas.builders.GrunnkretsRequestBuilder
import no.kartverket.nibas.builders.OpprettUtkastRequestBuilder
import no.kartverket.nibas.builders.StemmekretsRequestBuilder
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
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.ZonedDateTime

@ActiveProfiles(profiles = ["security-off", "eventsDisabled"])
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
            .withEndringstype(Endringstype.KVALITETSHEVING.value)
            .build()

        val utkastId = utkastService.opprettUtkast(utkastRequest)

        val utkast = utkastService.hentUtkast(utkastId)

        assertThat(utkast).isNotNull
        assertThat(utkast?.navn).isEqualTo(navn)
        assertThat(utkast?.auditInfo?.typeEndring).isEqualTo(Endringstype.KVALITETSHEVING)
        assertThat(utkast?.opprettetDato).isAfterOrEqualTo(timeAtTestStart)
        assertThat(utkast?.opprettetDato).isBeforeOrEqualTo(ZonedDateTime.now())
        assertThat(utkast?.auditInfo?.endretAv).isEqualTo("DUMMY")
    }

    @Test
    fun `skal gi optimistisklaasexception om man har en grenseendring paa utdatert versjon av grunnkrets`() {
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
        grenseSomEndres.properties = grenseSomEndres.properties.copy(version = 0)

        val grenseendringer = Grenseendringer(endredeFeatures = listOf(grenseSomEndres))

        val utkast = UtkastBuilder.builder("test")
            .withOperasjoner(
                Operasjoner(grenseendringer = grenseendringer)
            )
            .build()

        utkastRepository.save(utkast)

        assertThatThrownBy { utkastService.publiserUtkast(utkast.id) }
            .isInstanceOf(OptimistiskLaasException::class.java)
            .hasMessageContaining("Det er ikke samsvar med versjon")
    }

    @Test
    fun `skal gi optimistisklaasexception om man har har en metadataendring paa utdatert versjon av en grunnkrets`() {
        val kommune = saveKommune()
        val grunnkrets = saveGrunnkrets(lokalid = Lokalid("test1"), grunnkretsnavn = "gammelt navn")
        kommune.addGrunnkrets(grunnkrets)

        val grunnkretsEndring = GrunnkretsRequestBuilder()
            .withLokalId("test1")
            .withNavn("nytt navn")
            .withVersion(0)
            .build()

        val utkast = UtkastBuilder.builder("test")
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
    }

    @Test
    fun `skal gi optimistisklaasexception om man har en metadataendring paa utdatert versjon av en stemmekrets`() {
        saveStemmekrets(lokalid = Lokalid("test1"), stemmekretsnavn = "gammelt navn")

        val stemmekretsendring = StemmekretsRequestBuilder()
            .withLokalId("test1")
            .withNavn("nytt navn")
            .withVersion(0)
            .build()

        val utkast = UtkastBuilder.builder("test")
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
        saveStemmekrets(id = "test1", lokalid = Lokalid("test1"), stemmekretsnavn = "gammelt navn1")
        saveStemmekrets(id = "test2", lokalid = Lokalid("test2"), stemmekretsnavn = "gammelt navn2")

        val utkast = UtkastBuilder.builder("test")
            .withOperasjoner(
                Operasjoner(
                    stemmekretsSammenslaaingsendring = StemmekretsSammenslaaingsendringRequest(
                        viderefoertStemmekrets = IdentifikatorMedVersjon(lokalId = "test1", version = 0 ),
                        stemmekretserTilSammenslaaing = listOf(IdentifikatorMedVersjon(lokalId = "test2", version = 1 )),
                        navn = "navn etter endring",
                        nummer = "22"
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
    fun `skal gi optimistisklaasexception om man slaar sammen 2 stemmekretser hvor 1 av krets i sammenslaaingen er utdatert`() {
        saveStemmekrets(id = "test1", lokalid = Lokalid("test1"), stemmekretsnavn = "gammelt navn1")
        saveStemmekrets(id = "test2", lokalid = Lokalid("test2"), stemmekretsnavn = "gammelt navn2")

        val utkast = UtkastBuilder.builder("test")
            .withOperasjoner(
                Operasjoner(
                    stemmekretsSammenslaaingsendring = StemmekretsSammenslaaingsendringRequest(
                        viderefoertStemmekrets = IdentifikatorMedVersjon(lokalId = "test1", version = 1 ),
                        stemmekretserTilSammenslaaing = listOf(IdentifikatorMedVersjon(lokalId = "test2", version = 0 )),
                        navn = "navn etter endring",
                        nummer = "22"
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

        val utkast = UtkastBuilder.builder("test")
            .withOperasjoner(
                Operasjoner(
                    grenseendringer = grenseendringer,
                    metadataendringer = Metadataendringer(grunnkretsendringer = mapOf("test1" to grunnkretsendring))
                )
            )
            .build()

        utkastRepository.save(utkast)

        val eventsForPublisering = utkastService.publiserUtkast(utkast.id)
        assertThat(eventsForPublisering.eventRader).extracting("lokalid").contains(grunnkrets.lokalid)
        assertThat(eventsForPublisering.eventRader).extracting("flateType").contains(Flatetype.GRUNNKRETS)
        assertThat(eventsForPublisering.eventRader).extracting("eventType").contains(EventType.MODIFIED)
        assertThat(eventsForPublisering.eventRader).extracting("lokalid").contains(grunnkrets2.lokalid)
    }

    @Test
    fun publiserUtkastUtenEventRad() {
        assertThatThrownBy {
            nibasEventPublisher.publishNibasEvent(emptyList(), LocalDate.now())
        }
            .isInstanceOf(PublishEventException::class.java)
            .hasMessageContaining("Event må inneholde minst 1 EventRad")
    }
}
