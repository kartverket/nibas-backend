package no.kartverket.nibas.domain

import no.kartverket.nibas.api.v1.common.Flatetype
import no.kartverket.nibas.events.EventRad
import no.kartverket.nibas.events.EventType
import org.hibernate.annotations.SQLRestriction
import java.time.LocalDate
import java.util.*
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

/**
 * id overrides i konstruktør for å kunne overstyre verdi for test.
 */
@Table(name = "bopliktomraade")
@Entity
class Bopliktomraade(
    override val id: String = UUID.randomUUID().toString(),
    override val lokalid: String = UUID.randomUUID().toString()
) : InndelingMedDeltGeometri(Flatetype.BOPLIKTOMRAADE) {

    @Column(name = "informasjon")
    var informasjon: String? = null

    @Column(name = "delvis_boplikt")
    var delvisBoplikt: Boolean = false

    @Column(name = "forskriftsreferanse")
    var forskriftsreferanse: String? = null

    @Column(name = "url")
    var url: String? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kommunelokalid", referencedColumnName = "lokalid")
    @SQLRestriction("flatetype='KOMMUNE'")
    lateinit var kommuneLokalid: UnikLokalid

    override fun getInfo(): String? = informasjon
    override fun setInfo(updatedInfo: String?) { informasjon = updatedInfo }

    fun getKommune(gyldighetsdato: LocalDate = LocalDate.now()): Kommune? {
        return kommuneLokalid.kommuner.firstOrNull { it.gyldigPaaDato(gyldighetsdato) }
    }

    override fun getGrensekoblinger(gyldighetsdato: LocalDate): Set<GrenseKobling> = emptySet()

    override fun getFlateInformasjon(): FlateInformasjon =
        FlateInformasjon(type, "$id $lokalid")

    override fun toString(): String =
        "Bopliktomraade(id='$id', gyldighet: $gyldighet)"

    override fun toEventRad(eventType: EventType): EventRad =
        EventRad(
            uuid = this.id,
            eventType = eventType,
            flateType = Flatetype.BOPLIKTOMRAADE,
            lokalid = this.lokalid
        )
}
