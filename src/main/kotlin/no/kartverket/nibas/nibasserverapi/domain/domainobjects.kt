package no.kartverket.nibas.nibasserverapi.domain

import org.locationtech.jts.geom.Polygon
import javax.persistence.Entity
import javax.persistence.Id

@Entity
class Kommune(
    @Id val id: Int,
    val name: String,
    val omrade: Polygon
)