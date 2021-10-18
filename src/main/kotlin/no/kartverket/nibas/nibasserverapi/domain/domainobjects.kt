package no.kartverket.nibas.nibasserverapi.domain

import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.Polygon
import javax.persistence.*

@Entity
@Table(schema = "nibas")
class Kommune(
    @Id
    @Column(name = "objid")
    val id: Int,
    val navn: String,
    val kommunenummer: Int,

    @ManyToMany
    @JoinTable(
        schema = "nibas",
        name = "kommune_kommunegrense",
        joinColumns = arrayOf(JoinColumn(name = "kommune_fk")),
        inverseJoinColumns = arrayOf(JoinColumn(name = "kommunegrense_fk"))
    )
    val grenser: List<Kommunegrense> = emptyList()
)

@Entity
@Table(schema = "nibas")
class Kommunegrense(
    @Id
    @Column(name = "objid")
    val id: Int,
    @Column(name = "lokalid")
    val navn: String = "",
    val grense: LineString,
    @ManyToMany(mappedBy = "grenser")
    val kommuner: List<Kommune> = emptyList()
)
