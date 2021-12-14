package no.kartverket.nibas.domain

import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.OneToMany
import javax.persistence.Table

@Table(name = "kommunegrense")
@Entity
class Kommunegrense : AdministrativGrense() {
    override var objtype: String? = "Kommunegrense"

    @OneToMany(mappedBy = "kommunegrenseFk", fetch = FetchType.LAZY)
    var dokumentasjonsreferanse: MutableSet<Dokumentasjonsreferanse> = mutableSetOf()
}