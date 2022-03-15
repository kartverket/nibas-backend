package no.kartverket.nibas.sosi.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.ZoneId
import java.time.ZonedDateTime

internal class SosiGeometriTest {

    private val zoneIdOslo = ZoneId.of("Europe/Oslo")

    @Test
    fun stringAsZonedDateTime() {
        assertThat(stringAsZonedDateTime("20031201")).isEqualTo(zdtFromYearMonthDay(2003, 12, 1))
        assertThat(stringAsZonedDateTime("20160223")).isEqualTo(zdtFromYearMonthDay(2016, 2, 23))
    }

    @Test
    fun stringAsZonedDateTimeNull() {
        assertThat(stringAsZonedDateTime(null)).isNull()
    }

    private fun zdtFromYearMonthDay(year: Int, month: Int, day: Int) =
        ZonedDateTime.of(year, month, day, 0, 0, 0, 0, zoneIdOslo)
}
