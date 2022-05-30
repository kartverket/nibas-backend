package no.kartverket.nibas.domain

import no.kartverket.nibas.inndelinger.GrenseType
import org.hibernate.envers.Audited
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.*
import javax.persistence.*

@Entity
@Audited
@Table(name = "stemmekretsgrense")
class Stemmekretsgrense () : KommunalKretsgrense() {

    override var objtype: String? = GrenseType.Stemmekretsgrense.value

    @OneToMany(mappedBy = "stemmekretsgrenseFk", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    var informasjonselementer: MutableSet<StemmekretsgrenseInformasjon> = mutableSetOf()

    override fun getInfo(): String? {
        return doGetInfoFromSingletonCollection(informasjonselementer)
    }

    override fun setInfo(updatedInfo: String?) {
        doSetInfoInSingletonCollection(updatedInfo, informasjonselementer) { StemmekretsgrenseInformasjon(this) }
    }
}

@Table(name = "stemmekretsgrense_informasjon")
@Audited
@Entity
class StemmekretsgrenseInformasjon(stemmekretsgrense: Stemmekretsgrense) : Info {
    @Id
    @Column(name = "objid", nullable = false, updatable = false)
    override val id: String = UUID.randomUUID().toString()

    @ManyToOne
    @JoinColumn(name = "stemmekretsgrense_fk")
    var stemmekretsgrenseFk: Stemmekretsgrense? = stemmekretsgrense

    @Column(name = "informasjon")
    override var informasjon: String? = null
}
