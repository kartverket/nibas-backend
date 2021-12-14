package no.kartverket.nibas.domain

import no.kartverket.nibas.api.v1.common.AdministrativEnhetType
import org.hibernate.Hibernate
import org.locationtech.jts.geom.Point
import org.locationtech.jts.geom.Polygon
import java.time.LocalDate
import java.time.OffsetDateTime
import javax.persistence.*

@Table(name = "kommune")
@Entity
class Kommune : AdministrativEnhet(AdministrativEnhetType.KOMMUNE) {

    @ManyToOne(optional = false)
    @JoinColumn(name = "kommunenummer", nullable = false)
    var kommunenummer: Kommunenummer? = null

    @Column(name = "samiskforvaltningsomraade", nullable = false)
    var samiskforvaltningsomraade: Boolean? = false

    @ManyToOne
    @JoinColumn(name = "fylke_fk")
    var fylkeFk: Fylke? = null

    @ManyToMany
    @JoinTable(
        schema = "nibas",
        name = "kommune_kommunegrense",
        joinColumns = arrayOf(JoinColumn(name = "kommune_fk")),
        inverseJoinColumns = arrayOf(JoinColumn(name = "kommunegrense_fk"))
    )
    var kommunegrenser: MutableSet<Kommunegrense> = mutableSetOf()

    @ManyToMany
    @JoinTable(
        schema = "nibas",
        name = "kommune_fylkesgrense",
        joinColumns = arrayOf(JoinColumn(name = "kommune_fk")),
        inverseJoinColumns = arrayOf(JoinColumn(name = "fylkesgrense_fk"))
    )
    var fylkesgrenser: MutableSet<Fylkesgrense> = mutableSetOf()

    @ManyToMany
    @JoinTable(
        schema = "nibas",
        name = "kommune_avtaltavgrensningslinje",
        joinColumns = arrayOf(JoinColumn(name = "kommune_fk")),
        inverseJoinColumns = arrayOf(JoinColumn(name = "avtaltavgrensningslinje_fk"))
    )
    var avtaltavgrensningslinjer: MutableSet<Avtaltavgrensningslinje> = mutableSetOf()

    @ManyToMany
    @JoinTable(
        schema = "nibas",
        name = "kommune_territorialgrense",
        joinColumns = arrayOf(JoinColumn(name = "kommune_fk")),
        inverseJoinColumns = arrayOf(JoinColumn(name = "territorialgrense_fk"))
    )
    var territorialgrenser: MutableSet<Territorialgrense> = mutableSetOf()

    @ManyToMany
    @JoinTable(
        schema = "nibas",
        name = "kommune_riksgrense",
        joinColumns = arrayOf(JoinColumn(name = "kommune_fk")),
        inverseJoinColumns = arrayOf(JoinColumn(name = "riksgrense_fk"))
    )
    var riksgrenser: MutableSet<Riksgrense> = mutableSetOf()

    @OneToMany(mappedBy = "kommuneFk", fetch = FetchType.LAZY)
    override var administrativenhetnavn: MutableSet<Administrativenhetnavn> = mutableSetOf()

}