package no.kartverket.nibas.domain

import no.kartverket.nibas.api.v1.common.AdministrativEnhetType
import org.locationtech.jts.geom.LineString
import javax.persistence.*

@MappedSuperclass
abstract class AdministrativEnhet(
    @Transient
    val type: AdministrativEnhetType,

    @Id
    @Column(name = "objid")
    val id: Int,

    @Column(name = "navn")
    val navn: String,

    @Transient
    val nummer: String,

    @Transient
    val grenser: Set<Grense> = emptySet()
)

@Entity
@Table(schema = "nibas")
data class Kommune(
    override val id: Int,
    override val navn: String,
    @Column(name = "kommunenummer")
    override val nummer: String,

    @ManyToMany
    @JoinTable(
        schema = "nibas",
        name = "kommune_kommunegrense",
        joinColumns = arrayOf(JoinColumn(name = "kommune_fk")),
        inverseJoinColumns = arrayOf(JoinColumn(name = "kommunegrense_fk"))
    )
    override val grenser: Set<Kommunegrense> = emptySet()
) : AdministrativEnhet(AdministrativEnhetType.KOMMUNE, id, navn, nummer)

@Entity
@Table(schema = "nibas")
data class Fylke(
    override val id: Int,
    override val navn: String,

    @Column(name = "fylkesnummer")
    override val nummer: String,

    @ManyToMany
    @JoinTable(
        schema = "nibas",
        name = "fylke_fylkesgrense",
        joinColumns = arrayOf(JoinColumn(name = "fylke_fk")),
        inverseJoinColumns = arrayOf(JoinColumn(name = "fylkesgrense_fk"))
    )
    override val grenser: Set<Fylkesgrense> = emptySet()
) : AdministrativEnhet(AdministrativEnhetType.FYLKE, id, navn, nummer)

@MappedSuperclass
abstract class Grense (

    @Transient
    val type: AdministrativEnhetType?,

    @Id
    @Column(name = "objid")
    val id: Int,
    @Column(name = "lokalid")
    val navn: String,

    @Column(name = "grense")
    val kurve: LineString,
)

@Entity
@Table(schema = "nibas")
data class Kommunegrense(
    override val id: Int,
    override val navn: String,
    override val kurve: LineString,
) : Grense(AdministrativEnhetType.KOMMUNE, id,  navn, kurve)

@Entity
@Table(schema = "nibas")
data class Fylkesgrense(
    override val id: Int,
    override val navn: String,
    override val kurve: LineString,
) : Grense(AdministrativEnhetType.FYLKE, id,  navn, kurve)