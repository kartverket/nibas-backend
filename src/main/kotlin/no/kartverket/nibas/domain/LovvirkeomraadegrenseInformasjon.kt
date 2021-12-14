package no.kartverket.nibas.domain

import javax.persistence.*

@Table(name = "lovvirkeomraadegrense_informasjon")
@Entity
class LovvirkeomraadegrenseInformasjon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "objid", nullable = false)
    var id: Int? = null

    @ManyToOne
    @JoinColumn(name = "lovvirkeomraadegrense_fk")
    var lovvirkeomraadegrenseFk: Lovvirkeomraadegrense? = null

    @Column(name = "informasjon")
    var informasjon: String? = null
}