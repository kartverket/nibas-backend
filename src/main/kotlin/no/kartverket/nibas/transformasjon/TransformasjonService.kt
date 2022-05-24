package no.kartverket.nibas.transformasjon

import org.locationtech.jts.geom.Geometry

interface TransformasjonService {

    fun transformer(geometri: Geometry, tilSrid: Int): Geometry
}
