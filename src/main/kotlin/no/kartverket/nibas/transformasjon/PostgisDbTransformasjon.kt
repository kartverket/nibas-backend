package no.kartverket.nibas.transformasjon

import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.PrecisionModel
import org.locationtech.jts.io.WKTReader
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.queryForObject
import org.springframework.stereotype.Service

@Service
class PostgisDbTransformasjon
    @Autowired constructor(val jdbcTemplate: JdbcTemplate)
    : TransformasjonService {

    private val transformQuery = "select st_astext(st_transform(st_geomfromtext(?, ?), ?));"

    override fun transformer(geometri: Geometry, tilSrid: Int): Geometry {
        val wktReader = createWktReader(tilSrid)

        val fraSrid = geometri.srid
        val transformermertWkt = jdbcTemplate.queryForObject<String>(transformQuery, arrayOf(geometri.toText(), fraSrid, tilSrid))
        val transformertGeometri = wktReader.read(transformermertWkt)

        return transformertGeometri
    }

    private fun createWktReader(srid: Int): WKTReader {
        val scale: Double = when(srid) {
            4258 -> 1000000000.toDouble()
            else -> 1000.toDouble()
        }

        val precisionModel = PrecisionModel(scale)
        val geometryFactory = GeometryFactory(precisionModel, srid)

        return WKTReader(geometryFactory)
    }
}
