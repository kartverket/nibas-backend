package no.kartverket.nibas.domain

import org.hibernate.envers.Audited
import javax.persistence.*

@Table(name = "lovvirkeomraadegrense_informasjon")
@Audited
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