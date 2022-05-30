package no.kartverket.nibas.domain

import no.kartverket.nibas.inndelinger.GrenseType
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
@Table(name = "fylkesgrense")
@Audited
@Entity
class Fylkesgrense(override val id: String = UUID.randomUUID().toString()) : AdministrativGrense() {

    override var objtype: String? = GrenseType.Fylkesgrense.value

    @OneToMany(mappedBy = "fylkesgrenseFk", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    override var dokumentasjonsreferanser: MutableSet<Dokumentasjonsreferanse> = mutableSetOf()

    @OneToMany(mappedBy = "fylkesgrenseFk", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    var informasjonselementer: MutableSet<FylkesgrenseInformasjon> = mutableSetOf()

    override fun getInfo(): String? = doGetInfoFromSingletonCollection(informasjonselementer)

    override fun setInfo(updatedInfo: String?) {
        doSetInfoInSingletonCollection(updatedInfo, informasjonselementer) { FylkesgrenseInformasjon(this) }
    }
}

@Table(name = "fylkesgrense_informasjon")
@Audited
@Entity
class FylkesgrenseInformasjon(fylkesgrense: Fylkesgrense) : Info {
    @Id
    @Column(name = "objid", nullable = false, updatable = false)
    override val id: String = UUID.randomUUID().toString()

    @ManyToOne
    @JoinColumn(name = "fylkesgrense_fk")
    var fylkesgrenseFk: Fylkesgrense? = fylkesgrense

    @Column(name = "informasjon")
    override var informasjon: String? = null
}
