package no.kartverket.nibas.config

import no.kartverket.aut.lib.PublicKeyJwk
import no.kartverket.aut.lib.PublicKeyJwks
import no.kartverket.aut.lib.jwk.JwksRetriever

/**
 * Mocker ut fetching av public key fra aut-modulen for test.
 */
class JwksRetrieverMock : JwksRetriever("DUMMY_VALUE") {

    override fun getJwks(): PublicKeyJwks {
        return PublicKeyJwks(
            keys = listOf(
                PublicKeyJwk(
                    kid = "test-2",
                    n = "soph9U_04XIVTF7_gapJXslah5rndA5I4tAIeuAoYab9max7WXJ_2vJKatKQDxovBTihYudsL7YlFSckQcLpNgaL3d6JNTw4bS0rIoCBWbFi7HqUYC70EpRWbvHd5MEtaZfAWi0N4_5crL6aq_9_eo4O-4mSOnVFH9e1yyqxJeqJkEbHrau9RCnghfeA-pMytPNHgcE6DysLOUwG8Vme3g6tIcjMlc8hnq6F1Dsga0cmY_Gpptg6s7smsJXhfH79GLc_pInjBDT2y5g9ibFwP5Vou5fDv1LwozEAC2dDuUZtu1mU0JsDSaH2EY0vM6nxNe7sa07SOxEKnfhhdMGcVw",
                    e = "AQAB"
                )
            )
        )
    }
}
