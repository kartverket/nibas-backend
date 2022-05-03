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

@Table(name = "delomraadegrense")
@Audited
@Entity
class Delomraadegrense(override val id: String = UUID.randomUUID().toString()) : StatistiskGrense() {

    override var objtype: String? = "Delområdegrense"

    @OneToMany(mappedBy = "delomraadegrenseFk", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    var informasjonselementer: MutableSet<DelomraadegrenseInformasjon> = mutableSetOf()

    override fun getInfo(): String? {
        return doGetInfoFromSingletonCollection(informasjonselementer)
    }

    override fun setInfo(updatedInfo: String?) {
        doSetInfoInSingletonCollection(updatedInfo, informasjonselementer) { DelomraadegrenseInformasjon(this) }
    }


}

@Table(name = "delomraadegrense_informasjon")
@Audited
@Entity
class DelomraadegrenseInformasjon(delomraadegrense: Delomraadegrense) : Info {
    @Id
    @Column(name = "objid", nullable = false, updatable = false)
    override val id: String = UUID.randomUUID().toString()

    @ManyToOne
    @JoinColumn(name = "delomraadegrense_fk")
    var delomraadegrenseFk: Delomraadegrense? = delomraadegrense

    @Column(name = "informasjon")
    override var informasjon: String? = null
}
