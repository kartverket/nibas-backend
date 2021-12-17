package no.kartverket.nibas.domain

import org.hibernate.envers.Audited
import javax.persistence.*

@Table(name = "lovvirkeomraade_informasjon")
@Audited
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