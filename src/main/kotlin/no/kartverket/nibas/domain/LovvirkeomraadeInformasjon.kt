package no.kartverket.nibas.domain

import javax.persistence.*

@Table(name = "lovvirkeomraade_informasjon")
@Entity
class LovvirkeomraadeInformasjon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "objid", nullable = false)
    var id: Int? = null

    @ManyToOne
    @JoinColumn(name = "lovvirkeomraade_fk")
    var lovvirkeomraadeFk: Lovvirkeomraade? = null

    @Column(name = "informasjon")
    var informasjon: String? = null
}