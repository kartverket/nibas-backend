package no.kartverket.nibas.domain

import javax.persistence.*

@Table(name = "fylke_informasjon")
@Entity
class FylkeInformasjon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "objid", nullable = false)
    var id: Int? = null

    @ManyToOne
    @JoinColumn(name = "fylke_fk")
    var fylkeFk: Fylke? = null

    
    @Column(name = "informasjon")
    var informasjon: String? = null
}