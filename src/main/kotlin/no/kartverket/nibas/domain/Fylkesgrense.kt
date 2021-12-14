package no.kartverket.nibas.domain

import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.OneToMany
import javax.persistence.Table

@Table(name = "fylkesgrense")
@Entity
class Fylkesgrense : AdministrativGrense() {
    override var objtype: String? = "Fylkesgrense"

    @OneToMany(mappedBy = "fylkesgrenseFk", fetch = FetchType.LAZY)
    var dokumentasjonsreferanse: MutableSet<Dokumentasjonsreferanse> = mutableSetOf()
}