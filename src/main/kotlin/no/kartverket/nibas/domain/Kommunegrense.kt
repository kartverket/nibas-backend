package no.kartverket.nibas.domain

import org.hibernate.envers.Audited
import java.util.*
import javax.persistence.CascadeType
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.Table

/**
 * id overrides i konstruktør for å kunne overstyre verdi for test.
 */
@Table(name = "kommunegrense")
@Audited
@Entity
class Kommunegrense(override val id: String = UUID.randomUUID().toString()) : AdministrativGrense() {
    override var objtype: String? = "Kommunegrense"

    @OneToMany(mappedBy = "kommunegrenseFk", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    override var dokumentasjonsreferanser: MutableSet<Dokumentasjonsreferanse> = mutableSetOf()

    @OneToMany(mappedBy = "kommunegrenseFk", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    var informasjonselementer: MutableSet<KommunegrenseInformasjon> = mutableSetOf()

    override fun getInfo(): String? {
        return doGetInfoFromSingletonCollection(informasjonselementer)
    }

    override fun setInfo(updatedInfo: String?) {
        doSetInfoInSingletonCollection(updatedInfo, informasjonselementer) { KommunegrenseInformasjon(this) }
    }
}

@Table(name = "kommunegrense_informasjon")
@Audited
@Entity
class KommunegrenseInformasjon(kommunegrense: Kommunegrense) : Info {

    @Id
    @Column(name = "objid", nullable = false, updatable = false)
    override val id: String = UUID.randomUUID().toString()

    @ManyToOne
    @JoinColumn(name = "kommunegrense_fk")
    var kommunegrenseFk: Kommunegrense? = kommunegrense

    @Column(name = "informasjon")
    override var informasjon: String? = null
}
