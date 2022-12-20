package no.kartverket.nibas.common

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

internal class Md5Test {

    @Test
    fun testThatMd5HashReturnsAnExpectedHash() {
        assertEquals("5f4dcc3b5aa765d61d8327deb882cf99", "password".md5())
    }

}
