package no.kartverket.nibas.utkast

import no.kartverket.nibas.builders.OpprettUtkastRequestBuilder
import no.kartverket.nibas.domain.Endringstype
import no.kartverket.nibas.testcontainer.TestWithDbContainer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime

@ActiveProfiles(profiles = ["security-off", "eventsDisabled"])
@Transactional
class UtkastServiceIT : TestWithDbContainer() {


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
}
