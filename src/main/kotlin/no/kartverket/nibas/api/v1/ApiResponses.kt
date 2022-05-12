package no.kartverket.nibas.api.v1

import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import no.kartverket.nibas.api.v1.common.geojson.Feature
import no.kartverket.nibas.api.v1.common.geojson.FeatureCollection
import no.kartverket.nibas.api.v1.response.RevisjonRef


@ApiResponses(
    ApiResponse(
        responseCode = "200",
        description = "Successful operation",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = Feature::class))]
    ),
    ApiResponse(
        responseCode = "404",
        description = "Not Found"
    )
)
annotation class DefaultFeatureApiResponses

@ApiResponses(
    ApiResponse(
        responseCode = "200",
        description = "Successful operation",
        content = [Content(
            mediaType = "application/json",
            array = ArraySchema(schema = Schema(implementation = RevisjonRef::class))
        )]
    )
)
annotation class DefaultRevisjonRefListApiResponses

@ApiResponses(
    ApiResponse(
        responseCode = "200",
        description = "Successful operation",
        content = [Content(
            mediaType = "application/json",
            schema = Schema(implementation = FeatureCollection::class)
        )]
    ),
    ApiResponse(
        responseCode = "404",
        description = "Not Found"
    )
)
annotation class DefaultFeatureCollectionApiResponses
