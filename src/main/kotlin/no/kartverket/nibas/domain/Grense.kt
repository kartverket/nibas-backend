package no.kartverket.nibas.domain

import org.locationtech.jts.geom.LineString
import java.time.LocalDate
import javax.persistence.*

@MappedSuperclass
abstract class Grense {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "objid", nullable = false)
    var id: Int? = null

    @Column(name = "objtype")
    var objtype: String? = null

    @Column(name = "grense", nullable = false)
    var grense: LineString? = null


}