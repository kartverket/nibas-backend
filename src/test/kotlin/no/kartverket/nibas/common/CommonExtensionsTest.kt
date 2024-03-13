package no.kartverket.nibas.common

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

internal class CommonExtensionsTest {

    @Test
    fun `round runder av til korrekt antall desimaler`() {
        assertEquals(3.14, 3.1415926.round(2))
        assertEquals(3.1416, 3.1415926.round(4))
    }

}
