package no.kartverket.nibas.builders

import no.kartverket.nibas.api.v1.common.geojson.metadata.Identifikasjon
import no.kartverket.nibas.api.v1.request.BopliktomraadeRequest
import no.kartverket.nibas.domain.GjeldendeMaterieltVilkaar

class BopliktomraadeRequestBuilder(
    private var lokalId: String = "00c5ecde-145c",
    private var bopliktomraadenummer: String = "138",
    private var bopliktomraadenavn: String = "Mosekollen øst 2",
    private var gjelderKunDelAvKommunen: Boolean = true,
    private var harUsikkerAvgrensning: Boolean = true,
    private var gjelderForBoligIkkeTattIBruk: Boolean = false,
    private var gjelderForBruktSomHelarsbolig: Boolean = false,
    private var gjelderForUbebygdBoligtomt: Boolean = false,
    private var harUnntakFraSlektskapsunntak: Boolean = false,
    private var forskriftsreferanse: String? = "Mosekollen minikrets 3",
    private var url: String? = "https://www.kartverket.no/",
    private var version: Int = 1,
    private var kommuneLokalid: Identifikasjon = Identifikasjon("lokalid"),
) {
    companion object {
        val DEFAULT_REQUEST = BopliktomraadeRequestBuilder().build()
    }

    fun withLokalId(lokalId: String): BopliktomraadeRequestBuilder {
        this.lokalId = lokalId
        return this
    }

    fun withBopliktomraadenummer(bopliktomraadenummer: String): BopliktomraadeRequestBuilder {
        this.bopliktomraadenummer = bopliktomraadenummer
        return this
    }
    fun withNavn(navn: String): BopliktomraadeRequestBuilder {
        this.bopliktomraadenavn = navn
        return this
    }

    fun withVersion(version: Int): BopliktomraadeRequestBuilder {
        this.version = version
        return this
    }

    fun withGjelderForBoligIkkeTattIBruk(gjelderForBoligIkkeTattIBruk: Boolean): BopliktomraadeRequestBuilder {
        this.gjelderForBoligIkkeTattIBruk = gjelderForBoligIkkeTattIBruk
        return this
    }

    fun withGjelderForBruktSomHelarsbolig(gjelderForBruktSomHelarsbolig: Boolean): BopliktomraadeRequestBuilder {
        this.gjelderForBruktSomHelarsbolig = gjelderForBruktSomHelarsbolig
        return this
    }

    fun withGjelderForUbebygdBoligtomt(gjelderForUbebygdBoligtomt: Boolean): BopliktomraadeRequestBuilder {
        this.gjelderForUbebygdBoligtomt = gjelderForUbebygdBoligtomt
        return this
    }

    fun withHarUnntakFraSlektskapsunntak(harUnntakFraSlektskapsunntak: Boolean): BopliktomraadeRequestBuilder {
        this.harUnntakFraSlektskapsunntak = harUnntakFraSlektskapsunntak
        return this
    }

    fun build(): BopliktomraadeRequest {
        val gjeldendeMaterielleVilkaar: Set<GjeldendeMaterieltVilkaar> = buildSet {
            if (gjelderForBoligIkkeTattIBruk) add(GjeldendeMaterieltVilkaar.GJELDER_FOR_BOLIG_IKKE_TATT_I_BRUK)
            if (gjelderForBruktSomHelarsbolig) add(GjeldendeMaterieltVilkaar.GJELDER_FOR_BRUKT_SOM_HELARSBOLIG)
            if (gjelderForUbebygdBoligtomt) add(GjeldendeMaterieltVilkaar.GJELDER_FOR_UBEBYGD_BOLIGTOMT)
            if (harUnntakFraSlektskapsunntak) add(GjeldendeMaterieltVilkaar.HAR_UNNTAK_FRA_SLEKTSKAPSUNNTAK)
        }

        return BopliktomraadeRequest(
            identifikasjon = Identifikasjon(
                lokalid = lokalId,
            ),
            nummer = bopliktomraadenummer,
            navn = bopliktomraadenavn,
            forskriftsreferanse = forskriftsreferanse,
            url = url,
            kommuneIdentifikasjon = kommuneLokalid,
            version = version,
            gjelderKunDelAvKommunen = gjelderKunDelAvKommunen,
            harUsikkerAvgrensning = harUsikkerAvgrensning,
            gjeldendeMaterielleVilkaar = gjeldendeMaterielleVilkaar,
        )
    }
}

