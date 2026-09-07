package no.kartverket.nibas.api.v1.ekstern.response

import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema
import no.kartverket.nibas.api.v1.common.AdministrativEnhetNavn
import no.kartverket.nibas.api.v1.common.Fylkesnummer
import no.kartverket.nibas.api.v1.common.GyldighetResponse
import no.kartverket.nibas.api.v1.common.Kommunenummer
import no.kartverket.nibas.api.v1.common.ObjektIdentifikator
import no.kartverket.nibas.api.v1.common.geojson.FeatureCollection
import no.kartverket.nibas.domain.Endringstype
import no.kartverket.nibas.domain.GjeldendeMaterieltVilkaar
import java.time.ZonedDateTime

@Schema(description = "En referanse til et fylke")
data class EksternFylkeRef(

    @Schema(description = "ID-en til fylket")
    val id: ObjektIdentifikator,

    @Schema(description = "Liste over navn til fylket")
    val navn: List<AdministrativEnhetNavn>,

    @Schema(description = "URL til full representasjon av fylket")
    val href: String,

    @Schema(description = "Fylkesnummeret til fylket")
    val fylkesnummer: Fylkesnummer,

    @Schema(description = "Antall publiserte framtidige gyldige versjoner.")
    val antallFramtidigeVersjoner: Int = 0
)

@Schema(description = "En referanse til en kommune")
data class EksternKommuneRef(

    @Schema(description = "ID-en til kommunen")
    val id: ObjektIdentifikator,

    @Schema(description = "Liste over navn til kommunen")
    val navn: List<AdministrativEnhetNavn>,

    @Schema(description = "URL til full representasjon av kommunen")
    val href: String,

    @Schema(description = "Kommunenummeret til kommunen")
    val kommunenummer: Kommunenummer,

    @Schema(description = "Antall publiserte framtidige gyldige versjoner.")
    val antallFramtidigeVersjoner: Int = 0
)

@Schema(description = "Representasjon av et fylke")
data class EksternFylkeResponse(

    @Schema(description = "ID-en til fylket")
    val id: ObjektIdentifikator,

    @Schema(description = "Liste over navn til fylket")
    val administrativenhetnavn: List<AdministrativEnhetNavn>,

    @Schema(description = "Fylkesnummeret til fylket")
    val fylkesnummer: Fylkesnummer,

    @Schema(description = "Angir om fylket er et samisk forvaltningsområde eller ikke")
    val samiskforvaltningsomraade: Boolean,

    @Schema(description = "Angir når dette fylket ble sist oppdatert")
    val oppdateringsdato: ZonedDateTime,

    @Schema(description = "Geometrien til fylket. Ikke grenser")
    val features: FeatureCollection,

    @Schema(description = "Teknisk versjon for å støtte samhandling og redigering")
    val version: Int
)

@Schema(description = "Representasjon av en kommune")
data class EksternKommuneResponse(
    @Schema(description = "ID-en til kommunen")
    val id: ObjektIdentifikator,

    @Schema(description = "Liste over navn til kommunen")
    val administrativenhetnavn: List<AdministrativEnhetNavn>,

    @Schema(description = "Kommunenummeret til kommunen")
    val kommunenummer: Kommunenummer,

    @Schema(description = "Angir om kommunen er et samisk forvaltningsområde eller ikke")
    val samiskforvaltningsomraade: Boolean,

    @Schema(description = "Angir når denne kommunen ble sist oppdatert")
    val oppdateringsdato: ZonedDateTime,

    @Schema(description = "Geometrien til kommunen. Ikke grenser")
    val features: FeatureCollection,

    @Schema(description = "Teknisk versjon for å støtte samhandling og redigering")
    val version: Int
)

@Schema(description = "Representasjon av en grunnkrets")
data class EksternGrunnkretsResponse(
    @Schema(description = "ID-en til grunnkretsen")
    val id: ObjektIdentifikator,

    @Schema(description = "Navnet på grunnkretsen")
    val navn: String,

    @Schema(description = "Grunnkretsnummeret til grunnkretsen")
    val grunnkretsnummer: String,

    @Schema(description = "Gyldighetsintervall for objektet")
    val gyldighet: GyldighetResponse,

    @Schema(description = "Siste oppdateringstidspunkt for objektet")
    val oppdateringsdato: ZonedDateTime,

    @Schema(description = "Datafangsdato for objektet")
    val datafangstdato: ZonedDateTime?,

    @Schema(description = "Kommunenummeret til grunnkretsen")
    val kommunenummer: Kommunenummer,

    @Schema(description = "Typen endring som ble gjort på objektet")
    val endringstype: Endringstype,

    @Schema(description = "Geometrien til grunnkretsen, dvs representasjonspunkt og flaten")
    val features: FeatureCollection,

    @Schema(description = "Teknisk versjon for å støtte samhandling og redigering")
    val version: Int
)

@Schema(description = "Representasjon av en stemmekrets")
data class EksternStemmekretsResponse(
    @Schema(description = "ID-en til stemmekretsen")
    val id: ObjektIdentifikator,

    @Schema(description = "Gyldighetsintervall for objektet")
    val gyldighet: GyldighetResponse,

    @Schema(description = "Typen endring som ble gjort på objektet")
    val endringstype: Endringstype,

    @Schema(description = "Siste oppdateringstidspunkt for objektet")
    val oppdateringsdato: ZonedDateTime,

    @Schema(description = "Datafangsdato for objektet")
    val datafangstdato: ZonedDateTime?,

    @Schema(description = "Navnet på stemmekretsen")
    val stemmekretsnavn: String,

    @Schema(description = "Stemmekretsnummeret til stemmekretsen")
    val stemmekretsnummer: String,

    @Schema(description = "Kommunenummeret til stemmekretsen")
    val kommunenummer: Kommunenummer,

    @Schema(description = "Tellekretsnummer til stemmekretsen")
    var tellekretsnummer: String? = null,

    @Schema(description = "Tellekretsnavn til stemmekretsen")
    var tellekretsnavn: String? = null,

    @Schema(description = "Valgdistriktsnummer til stemmekretsen")
    var valgdistriktsnummer: String? = null,

    @Schema(description = "Geometrien til stemmekretsen, dvs representasjonspunkt og flaten")
    val features: FeatureCollection,

    @Schema(description = "Teknisk versjon for å støtte samhandling og redigering")
    val version: Int
)

@Schema(description = "Representasjon av et bopliktområde")
data class EksternBopliktomraadeRespons(

    @Schema(description = "ID-en til bopliktområdet")
    val id: ObjektIdentifikator,

    @Schema(description = "Gyldighetsintervall for objektet")
    val gyldighet: GyldighetResponse,

    @Schema(description = "Typen endring som ble gjort på objektet")
    val endringstype: Endringstype,

    @Schema(description = "Siste oppdateringstidspunkt for objektet")
    val oppdateringsdato: ZonedDateTime,

    @Schema(description = "Datafangstdato for objektet")
    val datafangstdato: ZonedDateTime?,

    @Schema(description = "Kommunenummeret til bopliktområdet")
    val kommunenummer: Kommunenummer,

    @Schema(description = "Indikerer om bopliktomraadet dekker deler av kommunen eller ikke")
    val gjelderKunDelAvKommunen: Boolean,

    @Schema(description = "Forskriftsreferanse til boplikten")
    val forskriftsreferanse: String?,

    @ArraySchema(
        arraySchema = Schema(description = "Materielle vilkår for bopliktområdet (nye enum-verdier, erstatter materielleVilkaar)"),
        schema = Schema(
            description = "Gjeldende materielt vilkår",
            implementation = GjeldendeMaterieltVilkaar::class
        )
    )
    val gjeldendeMaterielleVilkaar: Set<GjeldendeMaterieltVilkaar> = emptySet(),

    @Schema(description = "Andre lokale avgrensninger (nytt navn for andreAvgrensninger)")
    val andreLokaleAvgrensninger: String? = null,

    @Schema(description = "Har usikker avgrensning (nytt navn for usikkerAvgrensning)")
    val harUsikkerAvgrensning: Boolean,

    @Schema(description = "URL til informasjon om boplikten")
    val url: String?,

    @Schema(description = "Geometrien til bopliktområdet, dvs representasjonspunkt og flaten")
    val features: FeatureCollection,

    @Schema(description = "Teknisk versjon for å støtte samhandling og redigering")
    val version: Int
)


@Schema(description = "Representasjon av en krets. Response-type kun for Matrikkelen. Kan være grunnkrets, stemmekrets etc.")
data class EksternKretsResponse(
    @Schema(description = "Lokalid-en til kretsen.")
    val id: String,

    @Schema(description = "Navnet på kretsen.")
    val navn: String,

    @Schema(description = "Nummeret til kretsen.")
    val nummer: String,

    @Schema(description = "Kommunenummeret til kretsen.")
    val kommunenummer: Kommunenummer,

    @Schema(description = "Geometrien til kretsen, dvs representasjonspunkt og flaten")
    val features: FeatureCollection
)
