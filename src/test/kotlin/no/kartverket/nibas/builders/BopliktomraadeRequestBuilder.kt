package no.kartverket.nibas.builders

import no.kartverket.nibas.api.v1.common.geojson.metadata.Identifikasjon
import no.kartverket.nibas.api.v1.request.BopliktomraadeRequest
import no.kartverket.nibas.domain.MaterieltVilkaar

class BopliktomraadeRequestBuilder(
    private var lokalId: String = "00c5ecde-145c",
    private var bopliktomraadenummer: String = "138",
    private var bopliktomraadenavn: String = "Mosekollen øst 2",
    private var delvisBoplikt: Boolean = true,
    private var gjelderKunDelAvKommunen: Boolean = true,
    private var usikkerAvgrensning: Boolean = true,
    private var harUsikkerAvgrensning: Boolean = true,
    private var bebygdEiendom: Boolean = false,
    private var ikkeHelaarsboligUnderOppforing: Boolean = false,
    private var ubebygdTomt: Boolean = false,
    private var unntakFraSlektskapsunntak: Boolean = false,
    private var andreAvgrensninger: String? = null,
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

    fun withDelvisBoplikt(delvisBoplikt: Boolean): BopliktomraadeRequestBuilder {
        this.delvisBoplikt = delvisBoplikt
        return this
    }

    fun withUsikkerAvgrensning(usikkerAvgrensning: Boolean): BopliktomraadeRequestBuilder {
        this.usikkerAvgrensning = usikkerAvgrensning
        return this
    }

    fun withBebygdEiendom(bebygdEiendom: Boolean): BopliktomraadeRequestBuilder {
        this.bebygdEiendom = bebygdEiendom
        return this
    }

    fun withIkkeHelaarsboligUnderOppforing(ikkeHelaarsboligUnderOppforing: Boolean): BopliktomraadeRequestBuilder {
        this.ikkeHelaarsboligUnderOppforing = ikkeHelaarsboligUnderOppforing
        return this
    }

    fun withUbebygdTomt(ubebygdTomt: Boolean): BopliktomraadeRequestBuilder {
        this.ubebygdTomt = ubebygdTomt
        return this
    }

    fun withUnntakFraSlektskapsunntak(unntakFraSlektskapsunntak: Boolean): BopliktomraadeRequestBuilder {
        this.unntakFraSlektskapsunntak = unntakFraSlektskapsunntak
        return this
    }

    fun withAndreAvgrensninger(andreAvgrensninger: String?): BopliktomraadeRequestBuilder {
        this.andreAvgrensninger = andreAvgrensninger
        return this
    }

    fun build(): BopliktomraadeRequest {
        val materielleVilkaar: Set<MaterieltVilkaar> = buildSet {
            if (bebygdEiendom) add(MaterieltVilkaar.BEBYGDEIENDOM)
            if (ikkeHelaarsboligUnderOppforing) add(MaterieltVilkaar.IKKEHELAARSBOLIGUNDEROPPFORING)
            if (ubebygdTomt) add(MaterieltVilkaar.UBEBYGDTOMT)
            if (unntakFraSlektskapsunntak) add(MaterieltVilkaar.UNNTAKFRASLEKTSKAPSUNNTAK)
        }

        return BopliktomraadeRequest(
            identifikasjon = Identifikasjon(
                lokalid = lokalId,
            ),
            nummer = bopliktomraadenummer,
            navn = bopliktomraadenavn,
            delvisBoplikt = delvisBoplikt,
            usikkerAvgrensning = usikkerAvgrensning,
            forskriftsreferanse = forskriftsreferanse,
            materielleVilkaar = materielleVilkaar,
            andreAvgrensninger = andreAvgrensninger,
            url = url,
            kommuneIdentifikasjon = kommuneLokalid,
            version = version,
            gjelderKunDelAvKommunen = gjelderKunDelAvKommunen,
            harUsikkerAvgrensning = harUsikkerAvgrensning,
        )
    }
}
