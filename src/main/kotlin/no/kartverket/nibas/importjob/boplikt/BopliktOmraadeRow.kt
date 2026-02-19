package no.kartverket.nibas.importjob.boplikt
import org.locationtech.jts.geom.MultiPolygon
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
    val omraadeGeo: MultiPolygon,
    val kommunelokalid: String?,
    val delvisBoplikt: Boolean = false,
    val bebygdEiendom: Boolean = false,
    val ikkeHelaarsboligUnderOppforing: Boolean = false,
    val ubebygdTomt: Boolean = false,
    val unntakFraSlektskapsunntak: Boolean = false,
    val andreAvgrensninger: String?,
    val version: Short = 1,
    val slettet: Boolean = false,
    val forskriftsreferanse: String?,
    val informasjon: String?,
    val url: String?
)
