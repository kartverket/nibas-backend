package no.kartverket.nibas.domain

import no.kartverket.nibas.api.v1.common.AdministrativEnhetType
import org.locationtech.jts.geom.Point
import org.locationtech.jts.geom.Polygon
import java.time.LocalDate
import javax.persistence.*

@Table(name = "nasjon")
@Entity
class Nasjon : AdministrativEnhet(AdministrativEnhetType.NASJON) {

    @OneToMany(mappedBy = "nasjonFk", fetch = FetchType.LAZY)
    override var administrativenhetnavn: MutableSet<Administrativenhetnavn> = mutableSetOf()

    @ManyToMany
    @JoinTable(
        schema = "nibas",
        name = "nasjon_avtaltavgrensningslinje",
        joinColumns = arrayOf(JoinColumn(name = "nasjon_fk")),
        inverseJoinColumns = arrayOf(JoinColumn(name = "avtaltavgrensningslinje_fk"))
    )
    var avtaltavgrensningslinjer: MutableSet<Avtaltavgrensningslinje> = mutableSetOf()

    @ManyToMany
    @JoinTable(
        schema = "nibas",
        name = "nasjon_territorialgrense",
        joinColumns = arrayOf(JoinColumn(name = "nasjon_fk")),
        inverseJoinColumns = arrayOf(JoinColumn(name = "territorialgrense_fk"))
    )
    var territorialgrenser: MutableSet<Territorialgrense> = mutableSetOf()

    @ManyToMany
    @JoinTable(
        schema = "nibas",
        name = "nasjon_riksgrense",
        joinColumns = arrayOf(JoinColumn(name = "nasjon_fk")),
        inverseJoinColumns = arrayOf(JoinColumn(name = "riksgrense_fk"))
    )
    var riksgrenser: MutableSet<Riksgrense> = mutableSetOf()

}