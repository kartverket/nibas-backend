package no.kartverket.nibas.domain.utkast

import no.kartverket.nibas.api.v1.common.Flatetype
import no.kartverket.nibas.api.v1.common.Operasjoner
import no.kartverket.nibas.api.v1.request.BopliktomraadeRequest
import no.kartverket.nibas.api.v1.request.FylkeRequest
import no.kartverket.nibas.api.v1.request.GrunnkretsRequest
import no.kartverket.nibas.api.v1.request.KommuneRequest
import no.kartverket.nibas.api.v1.request.StemmekretsRequest
import no.kartverket.nibas.domain.Endringstype
import no.kartverket.nibas.events.EventRad
import no.kartverket.nibas.sporing.AuditInfo
import no.kartverket.nibas.sporing.Laasbar
import no.kartverket.nibas.sporing.Sporbar
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.*
import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version

/**
 * Navn og endringstype settes i konstruktør da de er påkrevd ved opprettelse av utkastet.
 */
@Table(name = "utkast")
@Entity
class Utkast(
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    override val id: String = UUID.randomUUID().toString(),

    @Column(name = "navn")
    var navn: String,

    @Column(name = "gyldig_fra", nullable = false)
    var gyldigFra: LocalDate,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", name = "utkast")
    var operasjoner: Operasjoner? = Operasjoner()
) : Sporbar, Laasbar {

    @Version
    override var version: Int = 1

    @Column(name = "opprettet_dato", updatable = false)
    val opprettetDato: ZonedDateTime = ZonedDateTime.now()

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", name = "endrede_inndelinger")
    var endredeInndelinger: EndredeInndelinger? = null


    @Embedded
    override var auditInfo: AuditInfo = AuditInfo()

    // Blir mappet av Converter
    var status: UtkastStatus = UtkastStatus.OPPRETTET

    fun settPublisert(flateEndringer: List<EventRad>) {
        endredeInndelinger = flateEndringer.toEndredeInndelinger()
        operasjoner = null
        status = UtkastStatus.PUBLISERT
    }
}

class UtkastBuilder private constructor(val navn: String, private val endringstype: Endringstype = Endringstype.KVALITETSHEVING, val gyldigFra: LocalDate) {

    private var status: UtkastStatus = UtkastStatus.OPPRETTET
    private var operasjoner: Operasjoner = Operasjoner()
    private var auditInfo: AuditInfo? = null

    companion object {
        fun builder(navn: String, endringstype: Endringstype = Endringstype.KVALITETSHEVING, gyldigFra: LocalDate): UtkastBuilder {
            return UtkastBuilder(navn, endringstype, gyldigFra)
        }
    }

    fun build(): Utkast {
        val utkast = Utkast(
            navn = navn,
            operasjoner = this.operasjoner,
            gyldigFra = gyldigFra
        )
        utkast.auditInfo.typeEndring = this.endringstype
        auditInfo?.let { utkast.auditInfo = it }
        utkast.status = status

        return utkast
    }

    fun withAuditInfo(auditInfo: AuditInfo): UtkastBuilder {
        this.auditInfo = auditInfo
        return this
    }

    fun withOperasjoner(operasjoner: Operasjoner): UtkastBuilder {
        this.operasjoner = operasjoner
        return this
    }

    fun withKommuneRequests(kommuneRequests: Map<String, KommuneRequest>): UtkastBuilder {
        val metadataEndringerCopy = operasjoner.metadataendringer.copy(kommuneendringer = kommuneRequests)
        operasjoner = operasjoner.copy(metadataendringer = metadataEndringerCopy)
        return this
    }

    fun withFylkeRequests(fylkeRequests: Map<String, FylkeRequest>): UtkastBuilder {
        val metadataEndringerCopy = operasjoner.metadataendringer.copy(fylkesendringer = fylkeRequests)
        operasjoner = operasjoner.copy(metadataendringer = metadataEndringerCopy)
        return this
    }

    fun withGrunnkretsRequests(grunnkretsRequests: Map<String, GrunnkretsRequest>): UtkastBuilder {
        val metadataEndringerCopy = operasjoner.metadataendringer.copy(grunnkretsendringer = grunnkretsRequests)
        operasjoner = operasjoner.copy(metadataendringer = metadataEndringerCopy)
        return this
    }

    fun withStemmekretsRequests(stemmekretsRequest: Map<String, StemmekretsRequest>): UtkastBuilder {
        val metadataEndringerCopy = operasjoner.metadataendringer.copy(stemmekretsendringer = stemmekretsRequest)
        operasjoner = operasjoner.copy(metadataendringer = metadataEndringerCopy)
        return this
    }

    fun withBopliktomraadeRequests(bopliktomraadeRequest: Map<String, BopliktomraadeRequest>): UtkastBuilder {
        val metadataEndringerCopy = operasjoner.metadataendringer.copy(bopliktomraadeendringer = bopliktomraadeRequest)
        operasjoner = operasjoner.copy(metadataendringer = metadataEndringerCopy)
        return this
    }

    fun withStatus(status: UtkastStatus): UtkastBuilder {
        this.status = status
        return this
    }
}

data class EndredeInndelinger(
    val endredeStemmekretser: MutableSet<String> = mutableSetOf(),
    val endredeGrunnkretser: MutableSet<String> = mutableSetOf(),
    val endredeKommuner: MutableSet<String> = mutableSetOf(),
    val endredeFylker: MutableSet<String> = mutableSetOf(),
    val endredeNasjoner: MutableSet<String> = mutableSetOf()
)

fun List<EventRad>.toEndredeInndelinger(): EndredeInndelinger {
    val endredeInndelinger = EndredeInndelinger()

    filter { Flatetype.GRUNNKRETS == it.flateType }.forEach { endredeInndelinger.endredeGrunnkretser.add(it.lokalid) }
    filter { Flatetype.STEMMEKRETS == it.flateType }.forEach { endredeInndelinger.endredeStemmekretser.add(it.lokalid) }
    filter { Flatetype.KOMMUNE == it.flateType }.forEach { endredeInndelinger.endredeKommuner.add(it.lokalid) }
    filter { Flatetype.FYLKE == it.flateType }.forEach { endredeInndelinger.endredeFylker.add(it.lokalid) }
    filter { Flatetype.NASJON == it.flateType }.forEach { endredeInndelinger.endredeNasjoner.add(it.lokalid) }

    return endredeInndelinger
}
