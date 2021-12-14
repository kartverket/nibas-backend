package no.kartverket.nibas.domain

import javax.persistence.*

@Table(name = "grunnlinje_informasjon")
@Entity
class GrunnlinjeInformasjon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "objid", nullable = false)
    var id: Int? = null

    @ManyToOne
    @JoinColumn(name = "grunnlinje_fk")
    var grunnlinjeFk: Grunnlinje? = null

    @Column(name = "informasjon")
    var informasjon: String? = null
}