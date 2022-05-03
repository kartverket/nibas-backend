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

@Table(name = "grunnkretsgrense")
@Audited
@Entity
class Grunnkretsgrense(override val id: String = UUID.randomUUID().toString()) : StatistiskGrense() {

    override var objtype: String? = "Grunnkretsgrense"

    @OneToMany(mappedBy = "grunnkretsgrenseFk", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    var informasjonselementer: MutableSet<GrunnkretsgrenseInformasjon> = mutableSetOf()

    override fun getInfo(): String? {
        return doGetInfoFromSingletonCollection(informasjonselementer)
    }

    override fun setInfo(updatedInfo: String?) {
        doSetInfoInSingletonCollection(updatedInfo, informasjonselementer) { GrunnkretsgrenseInformasjon(this) }
    }

}

@Table(name = "grunnkretsgrense_informasjon")
@Audited
@Entity
class GrunnkretsgrenseInformasjon(grunnkretsgrense: Grunnkretsgrense) : Info {
    @Id
    @Column(name = "objid", nullable = false, updatable = false)
    override val id: String = UUID.randomUUID().toString()

    @ManyToOne
    @JoinColumn(name = "grunnkretsgrense_fk")
    var grunnkretsgrenseFk: Grunnkretsgrense? = grunnkretsgrense

    @Column(name = "informasjon")
    override var informasjon: String? = null
}
