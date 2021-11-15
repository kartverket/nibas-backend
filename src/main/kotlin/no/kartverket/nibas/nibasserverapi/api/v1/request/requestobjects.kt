package no.kartverket.nibas.nibasserverapi.api.v1.request

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import io.swagger.v3.oas.annotations.media.Schema
import no.kartverket.nibas.nibasserverapi.api.v1.common.AdministrativEnhetType
import org.locationtech.jts.geom.LineString
import org.locationtech.spatial4j.io.jackson.GeometryAsGeoJSONSerializer
import org.locationtech.spatial4j.io.jackson.GeometryDeserializer
import javax.validation.constraints.NotBlank
import javax.validation.constraints.Size

@Schema(description = "Representasjon av en administrativ enhet")
class AdministrativEnhet(

    @Schema(description = "Type administrativ enhet")
    @NotBlank
    val type: AdministrativEnhetType,

    @Schema(description = "Navnet til den administrative enheten")
    @NotBlank
    @Size(min = 0)
    val navn: String,

    @Schema(description = "Nummeret som identifiserer den administrative enheten")
    @NotBlank
    val nummer: String
)