package no.kartverket.nibas.nibasserverapi.api.v1.response

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import io.swagger.v3.oas.annotations.media.Schema
import no.kartverket.nibas.nibasserverapi.api.v1.common.AdministrativEnhetType
import org.locationtech.jts.geom.LineString
import org.locationtech.spatial4j.io.jackson.GeometryAsGeoJSONSerializer
import org.locationtech.spatial4j.io.jackson.GeometryDeserializer

@Schema(description = "Representasjon av en administrativ enhet")
class AdministrativEnhet(
    val type: AdministrativEnhetType,

    @Schema(description = "Unik id for den administrative enheten")
    val id: Int,

    @Schema(description = "Navnet på den administrative enheten")
    val navn: String,

    @Schema(description = "Nummeret som identifiserer den administrative enheten")
    val nummer: String,
)
