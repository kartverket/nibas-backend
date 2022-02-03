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
@Table(name = "kommunegrense")
@Audited
@Entity
class Kommunegrense(override val id: String = UUID.randomUUID().toString()) : AdministrativGrense() {
    override var objtype: String? = "Kommunegrense"

    @OneToMany(mappedBy = "kommunegrenseFk", fetch = FetchType.LAZY)
    var dokumentasjonsreferanse: MutableSet<Dokumentasjonsreferanse> = mutableSetOf()
}
