package no.kartverket.nibas.domain

import javax.persistence.*

@Table(name = "kommune_informasjon")
@Entity
class KommuneInformasjon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "objid", nullable = false)
    var id: Int? = null

    @ManyToOne
    @JoinColumn(name = "kommune_fk")
    var kommuneFk: Kommune? = null

    @Column(name = "informasjon")
    var informasjon: String? = null
}