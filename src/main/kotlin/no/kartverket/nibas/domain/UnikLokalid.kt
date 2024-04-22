package no.kartverket.nibas.domain

import no.kartverket.nibas.api.v1.common.Flatetype
import java.util.*
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToMany
import jakarta.persistence.Table

@Table(name = "uniklokalid")
@Entity
class UnikLokalid(
    @Id
    @Column(name = "lokalid", nullable = false, updatable = false)
    val lokalid: String = UUID.randomUUID().toString(),
    val flatetype: Flatetype
) {
    @OneToMany(fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    @JoinColumn(name = "lokalid", referencedColumnName = "lokalid")
    var nasjoner: MutableSet<Nasjon> = mutableSetOf()

    @OneToMany(fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    @JoinColumn(name = "lokalid", referencedColumnName = "lokalid")
    var fylker: MutableSet<Fylke> = mutableSetOf()

    @OneToMany(fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    @JoinColumn(name = "lokalid", referencedColumnName = "lokalid")
    var kommuner: MutableSet<Kommune> = mutableSetOf()

    @OneToMany(fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    @JoinColumn(name = "nasjonlokalid", referencedColumnName = "lokalid")
    var fylkerForNasjon: MutableSet<Fylke> = mutableSetOf()

    @OneToMany(fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    @JoinColumn(name = "fylkelokalid", referencedColumnName = "lokalid")
    var kommunerForFylke: MutableSet<Kommune> = mutableSetOf()

    @OneToMany(fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    @JoinColumn(name = "administrativenhetlokalid", referencedColumnName = "lokalid")
    var administrativenhetnavn: MutableSet<Administrativenhetnavn> = mutableSetOf()

    @OneToMany(fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    @JoinColumn(name = "kommunelokalid", referencedColumnName = "lokalid")
    var stemmekretser: MutableSet<Stemmekrets> = mutableSetOf()

    @OneToMany(fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    @JoinColumn(name = "kommunelokalid", referencedColumnName = "lokalid")
    var grunnkretser: MutableSet<Grunnkrets> = mutableSetOf()
}
