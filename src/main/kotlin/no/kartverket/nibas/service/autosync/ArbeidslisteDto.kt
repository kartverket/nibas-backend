package no.kartverket.nibas.service.autosync

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

/**
 * DTOs for calling Arbeidsliste API to fetch avvik data
 */

@JsonIgnoreProperties(ignoreUnknown = true)
data class KommuneParPage(
    val content: List<KommunePar>,
    val totalElements: Int,
    val totalPages: Int
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class KommunePar(
    val kommune1: KommuneInfo,
    val kommune2: KommuneInfo,
    val antallGrenserMedAvvik: Int,
    val antallPunkterMedAvvik: Int
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class KommuneInfo(
    val fylkesLokalID: String,
    val kommuneLokalID: String,
    val kommunenummer: String,
    val kommunenavn: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class AvvikDto(
    val id: Long,
    val grenseId: String,
    val status: String,
    val lokalId: String,
    val grensetype: String,
    val geometri: GeometriDto,
    val maalemetode: String,
    val noeyaktighet: Int,
    val antallKoordinater: Int,
    val antallKoordinaterMedAvvik: Int,
    val koordinaterMedAvvik: List<KoordinatAvvikDto>,
    val kommuner: List<KommuneInfo>
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GeometriDto(
    val type: String,
    val coordinates: List<List<Double>>
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class KoordinatAvvikDto(
    val nibasKoordinat: PointDto,
    val matrikkelKoordinat: PointDto,
    val distanseMellomKoordinater: Double
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class PointDto(
    val type: String,
    val coordinates: List<Double>
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class M22FeatureCollection(
    val type: String,
    val features: List<M22Feature>
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class M22Feature(
    val id: String,
    val type: String,
    val geometry: GeometriDto,
    val properties: M22Properties
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class M22Properties(
    val id: Long,
    val kommunenr1: String?,
    val kommunenr2: String?,
    val administrativgrensekodeId: Int?,
    val malemetodeId: Int?,
    val noyaktighet: Int?
)
