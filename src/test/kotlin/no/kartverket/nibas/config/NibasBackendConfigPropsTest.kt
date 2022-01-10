package no.kartverket.nibas.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class NibasBackendConfigPropsTest {

    @Test
    fun testToString() {
        val nibasBackendConfigProps = NibasBackendConfigProps()
        assertThat(nibasBackendConfigProps.toString())
            .isEqualTo(
                "NibasBackendConfigProps(" +
                        "geonorgeBaseUrl='https://register.geonorge.no/')"
            )
    }
}
