package no.kartverket.nibas.domain

import javax.persistence.*

@Table(name = "nasjon_informasjon")
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