package no.kartverket.nibas.common

import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.LocalDateTime

@Component
class CurrentTime {
    fun asLocalDate(): LocalDate =
        LocalDate.now()

    fun asLocalDateTime(): LocalDateTime =
        LocalDateTime.now()
}
