package no.kartverket.nibas.domain

import no.kartverket.nibas.sporing.AuditInfo
import no.kartverket.nibas.sporing.Gyldig
import no.kartverket.nibas.sporing.Gyldighet
import no.kartverket.nibas.sporing.Laasbar
import no.kartverket.nibas.sporing.Sporbar
import java.time.LocalDate
import java.util.*
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.Version

@Table(name = "administrativenhetnavn")
@Entity
class Administrativenhetnavn(
    @Id
    @Column(name = "objid", nullable = false, updatable = false)
    override val id: String = UUID.randomUUID().toString(),

    @Column(name = "lokalid", nullable = false, updatable = false)
    val lokalid: String = UUID.randomUUID().toString()
): Laasbar, Sporbar, Gyldig {

    @Version
    override var version: Int = 1

    @Column(name = "navn", nullable = false)
    var navn: String? = null

    @Embedded
    override var gyldighet: Gyldighet = Gyldighet()

    @Column(name = "foerstedigitaliseringsdato")
    var foerstedigitaliseringsdato: LocalDate = LocalDate.now()

    @Embedded
    override var auditInfo: AuditInfo = AuditInfo()

    @ManyToOne(optional = false)
    @JoinColumn(name = "spraak", nullable = false)
    var spraak: Spraakkode? = null

    @Column(name = "rekkefoelge")
    var rekkefoelge: Int? = null

    @ManyToOne(fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    @JoinColumn(name = "administrativenhetlokalid", referencedColumnName = "lokalid")
    lateinit var administrativenhetLokalid : UnikLokalid

    /**
     * Henter navnet sitt fylke på en gitt dato. Hvis man gir en dato hvor fylket med gitt lokalid ikke er gyldig, returneres null.
     * Hvis man kaller denne for en nasjon eller kommune, returneres også null.
     */
    fun getFylke(gyldighetsdato: LocalDate = LocalDate.now()): Fylke? {
        return administrativenhetLokalid.fylker.filter { it.gyldigPaaDato(gyldighetsdato) }.firstOrNull()
    }

    /**
     * Henter navnet sin kommune på en gitt dato. Hvis man gir en dato hvor kommunen med gitt lokalid ikke er gyldig, returneres null.
     * Hvis man kaller denne for en nasjon eller fylke, returneres også null.
     */
    fun getKommune(gyldighetsdato: LocalDate = LocalDate.now()): Kommune? {
        return administrativenhetLokalid.kommuner.filter { it.gyldigPaaDato(gyldighetsdato) }.firstOrNull()
    }
    
    /**
     * Henter navnet sin nasjon på en gitt dato. Hvis man gir en dato hvor nasjonen med gitt lokalid ikke er gyldig, returneres null.
     * Hvis man kaller denne for et fylke eller kommune, returneres også null.
     */
    fun getNasjon(gyldighetsdato: LocalDate = LocalDate.now()): Nasjon? {
        return administrativenhetLokalid.nasjoner.filter { it.gyldigPaaDato(gyldighetsdato) }.firstOrNull()
    }

}
