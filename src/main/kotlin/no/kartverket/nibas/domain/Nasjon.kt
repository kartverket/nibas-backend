package no.kartverket.nibas.domain

import no.kartverket.nibas.api.v1.common.AdministrativEnhetType
import org.hibernate.envers.Audited
import java.util.*
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.JoinColumn
import javax.persistence.JoinTable
import javax.persistence.ManyToMany
import javax.persistence.OneToMany
import javax.persistence.Table

/**
 * id overrides i konstruktør for å kunne overstyre verdi for test.
 */
@Table(name = "nasjon")
@Audited
@Entity
class Nasjon(override val id: String = UUID.randomUUID().toString()) :
    AdministrativEnhet(AdministrativEnhetType.NASJON) {

    @OneToMany(mappedBy = "nasjonFk", fetch = FetchType.LAZY)
    override var administrativenhetnavn: MutableSet<Administrativenhetnavn> = mutableSetOf()

    @ManyToMany
    @JoinTable(
        schema = "nibas",
        name = "nasjon_avtaltavgrensningslinje",
        joinColumns = arrayOf(JoinColumn(name = "nasjon_fk")),
        inverseJoinColumns = arrayOf(JoinColumn(name = "avtaltavgrensningslinje_fk"))
    )
    override var avtaltavgrensningslinjer: MutableSet<Avtaltavgrensningslinje> = mutableSetOf()

    @ManyToMany
    @JoinTable(
        schema = "nibas",
        name = "nasjon_territorialgrense",
        joinColumns = arrayOf(JoinColumn(name = "nasjon_fk")),
        inverseJoinColumns = arrayOf(JoinColumn(name = "territorialgrense_fk"))
    )
    override var territorialgrenser: MutableSet<Territorialgrense> = mutableSetOf()

    @ManyToMany
    @JoinTable(
        schema = "nibas",
        name = "nasjon_riksgrense",
        joinColumns = arrayOf(JoinColumn(name = "nasjon_fk")),
        inverseJoinColumns = arrayOf(JoinColumn(name = "riksgrense_fk"))
    )
    override var riksgrenser: MutableSet<Riksgrense> = mutableSetOf()

}
