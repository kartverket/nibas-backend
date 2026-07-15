package no.kartverket.nibas.importjob.boplikt
import org.locationtech.jts.geom.MultiPolygon
import org.locationtech.jts.geom.Point
import java.time.OffsetDateTime

data class BopliktOmraadeRow(
    val objid: String,
    val lokalid: String,
    val navn: String,
    val nummer: String,
    val gyldigfra: OffsetDateTime,
    val gyldigtil: OffsetDateTime?,
    val datafangstdato: OffsetDateTime?,
    val foerstedigitaliseringsdato: OffsetDateTime,
    val opphav: String?,
    val endretdato: OffsetDateTime,
    val endretav: String,
    val typeendring: String,
    val omraadeGeo: MultiPolygon?,
    val posisjonGeo: Point,
    val kommunelokalid: String?,
    val usikkerAvgrensning: Boolean = false,
    val harUsikkerAvgrensning: Boolean = usikkerAvgrensning,
    val bebygdEiendom: Boolean = false,
    val gjelderForBoligIkkeTattIBruk: Boolean = bebygdEiendom,
    val ikkeHelaarsboligUnderOppforing: Boolean = false,
    val gjelderForBruktSomHelarsbolig: Boolean = ikkeHelaarsboligUnderOppforing,
    val ubebygdTomt: Boolean = false,
    val gjelderForUbebygdBoligtomt: Boolean = ubebygdTomt,
    val unntakFraSlektskapsunntak: Boolean = false,
    val harUnntakFraSlektskapsunntak: Boolean = unntakFraSlektskapsunntak,
    val andreAvgrensninger: String?,
    val andreLokaleAvgrensninger: String? = andreAvgrensninger,
    val version: Short = 1,
    val slettet: Boolean = false,
    val forskriftsreferanse: String?,
    val informasjon: String?,
    val url: String?
)
