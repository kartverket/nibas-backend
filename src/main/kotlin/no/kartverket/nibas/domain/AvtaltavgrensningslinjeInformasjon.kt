package no.kartverket.nibas.domain

import org.hibernate.envers.Audited
import javax.persistence.*


@Table(name = "avtaltavgrensningslinje_informasjon")
@Audited
@Entity
class AvtaltavgrensningslinjeInformasjon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "objid", nullable = false)
    var id: Int? = null

    @ManyToOne
    @JoinColumn(name = "avtaltavgrensningslinje_fk")
    var avtaltavgrensningslinjeFk: Avtaltavgrensningslinje? = null

    
    @Column(name = "informasjon")
    var informasjon: String? = null
}