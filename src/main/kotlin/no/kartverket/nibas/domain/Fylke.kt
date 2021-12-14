package no.kartverket.nibas.domain

import no.kartverket.nibas.api.v1.common.AdministrativEnhetType
import javax.persistence.*

@Table(name = "fylke")
@Entity
class Fylke : AdministrativEnhet(AdministrativEnhetType.FYLKE) {

    @ManyToOne(optional = false)
    @JoinColumn(name = "fylkesnummer", nullable = false)
    var fylkesnummer: Fylkesnummer? = null

    @Column(name = "samiskforvaltningsomraade")
    var samiskforvaltningsomraade: Boolean? = null

    @ManyToOne
    @JoinColumn(name = "nasjon_fk")
    var nasjonFk: Nasjon? = null

    @ManyToMany
    @JoinTable(
        schema = "nibas",
        name = "fylke_fylkesgrense",
        joinColumns = arrayOf(JoinColumn(name = "fylke_fk")),
        inverseJoinColumns = arrayOf(JoinColumn(name = "fylkesgrense_fk"))
    )
    var fylkesgrenser: MutableSet<Fylkesgrense> = mutableSetOf()

    @ManyToMany
    @JoinTable(
        schema = "nibas",
        name = "fylke_avtaltavgrensningslinje",
        joinColumns = arrayOf(JoinColumn(name = "fylke_fk")),
        inverseJoinColumns = arrayOf(JoinColumn(name = "avtaltavgrensningslinje_fk"))
    )
    var avtaltavgrensningslinjer: MutableSet<Avtaltavgrensningslinje> = mutableSetOf()

    @ManyToMany
    @JoinTable(
        schema = "nibas",
        name = "fylke_territorialgrense",
        joinColumns = arrayOf(JoinColumn(name = "fylke_fk")),
        inverseJoinColumns = arrayOf(JoinColumn(name = "territorialgrense_fk"))
    )
    var territorialgrenser: MutableSet<Territorialgrense> = mutableSetOf()

    @ManyToMany
    @JoinTable(
        schema = "nibas",
        name = "fylke_riksgrense",
        joinColumns = arrayOf(JoinColumn(name = "fylke_fk")),
        inverseJoinColumns = arrayOf(JoinColumn(name = "riksgrense_fk"))
    )
    var riksgrenser: MutableSet<Riksgrense> = mutableSetOf()

    @OneToMany(mappedBy = "fylkeFk", fetch = FetchType.LAZY)
    var informasjon: MutableSet<FylkeInformasjon> = mutableSetOf()

    @OneToMany(mappedBy = "fylkeFk", fetch = FetchType.LAZY)
    override var administrativenhetnavn: MutableSet<Administrativenhetnavn> = mutableSetOf()
}