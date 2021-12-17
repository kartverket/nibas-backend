package no.kartverket.nibas.domain

import org.hibernate.envers.Audited
import javax.persistence.*

@Table(name = "nasjon_informasjon")
@Audited
@Entity
class NasjonInformasjon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "objid", nullable = false)
    var id: Int? = null

    @ManyToOne
    @JoinColumn(name = "nasjon_fk")
    var nasjonFk: Nasjon? = null

    @Column(name = "informasjon")
    var informasjon: String? = null
}