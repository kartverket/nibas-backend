package no.kartverket.nibas.nibasserverapi.api.v1.common.geojson

import io.swagger.v3.oas.annotations.media.Schema
import no.kartverket.nibas.nibasserverapi.api.v1.common.AdministrativEnhetType
import no.kartverket.nibas.nibasserverapi.api.v1.response.AdministrativEnhet

@Schema(description = "Egenskaper som beskriver en gitt feature (properties-objektet i geojson-strukturen)")
class FeatureProperties (
    @Schema(description = "Id til feature")
    val id: Int,

    @Schema(description = "Navn som beskriver feature - kun i en initiell fase for å forenkle representasjonen")
    val navn: String,

    @Schema(description = "Type som definerer hvilken administrativ enhet vi beskriver i featuren (Kommune, fylke, etc)")
    val type: AdministrativEnhetType,

    @Schema(description = "Den administrative enheten som \"eier\" featuren. Mulig denne ikke er nødvendig på sikt")
    val administrativEnhet: AdministrativEnhet
)

