package no.kartverket.nibas.domain

import org.hibernate.envers.Audited
import java.util.*
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.OneToMany
import javax.persistence.Table

/**
 * id overrides i konstruktør for å kunne overstyre verdi for test.
 */
@Table(name = "fylkesgrense")
@Audited
@Entity
class Fylkesgrense(override val id: String = UUID.randomUUID().toString()) : AdministrativGrense() {

    override var objtype: String? = "Fylkesgrense"

    @OneToMany(mappedBy = "fylkesgrenseFk", fetch = FetchType.LAZY)
    var dokumentasjonsreferanse: MutableSet<Dokumentasjonsreferanse> = mutableSetOf()

    @OneToMany(mappedBy = "fylkesgrenseFk", fetch = FetchType.LAZY)
    var informasjonList: MutableSet<FylkesgrenseInformasjon> = mutableSetOf()

    override fun informasjon(): List<String> {
        return this.informasjonList.mapNotNull { it.informasjon }
    }

}
