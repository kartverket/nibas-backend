package no.kartverket.nibas.domain.utkast

import com.vladmihalcea.hibernate.type.json.JsonBinaryType
import no.kartverket.nibas.api.v1.common.Operasjoner
import no.kartverket.nibas.api.v1.request.FylkeRequest
import no.kartverket.nibas.api.v1.request.KommuneRequest
import no.kartverket.nibas.sporing.AuditInfo
import no.kartverket.nibas.sporing.Laasbar
import no.kartverket.nibas.sporing.Sporbar
import org.hibernate.annotations.Type
import org.hibernate.annotations.TypeDef
import org.hibernate.annotations.TypeDefs
import org.hibernate.envers.Audited
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.*
import javax.persistence.Column
import javax.persistence.Embedded
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table
import javax.persistence.Version

private const val DEFAULT_GYLDIG_FRA_YEARS: Long = 10

/**
 * Navn og endringstype settes i konstruktør da de er påkrevd ved opprettelse av utkastet.
 */
@Table(name = "utkast")
@Audited
@TypeDefs(TypeDef(name = "jsonb", typeClass = JsonBinaryType::class))
@Entity
class Utkast(
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    override val id: String = UUID.randomUUID().toString(),

    @Column(name = "navn")
    var navn: String,

    // Blir mappet av Converter
    var endringstype: UtkastEndringstype,

    @Type(type = "jsonb")
    @Column(columnDefinition = "jsonb", name = "utkast")
    var operasjoner: Operasjoner? = Operasjoner()
) : Sporbar, Laasbar {

    @Version
    override var version: Int = 1

    @Column(name = "opprettet_dato", updatable = false)
    val opprettetDato: ZonedDateTime = ZonedDateTime.now()

    @Embedded
    override var auditInfo: AuditInfo = AuditInfo()

    @Column(name = "gyldig_fra")
    var gyldigFra: LocalDate = LocalDate.now().plusYears(DEFAULT_GYLDIG_FRA_YEARS)

    // Blir mappet av Converter
    var status: UtkastStatus = UtkastStatus.OPPRETTET

}

class UtkastBuilder private constructor(val navn: String, private val endringstype: UtkastEndringstype = UtkastEndringstype.KVALITETSHEVING) {

    private var status: UtkastStatus = UtkastStatus.OPPRETTET
    private var operasjoner: Operasjoner = Operasjoner()
    private var gyldigFra: LocalDate = LocalDate.now().plusYears(DEFAULT_GYLDIG_FRA_YEARS)
    private var auditInfo: AuditInfo? = null

    companion object {
        fun builder(navn: String, endringstype: UtkastEndringstype = UtkastEndringstype.KVALITETSHEVING): UtkastBuilder {
            return UtkastBuilder(navn, endringstype)
        }
    }

    fun build(): Utkast {
        val utkast = Utkast(
            navn = navn,
            endringstype = this.endringstype,
            operasjoner = this.operasjoner
        )
        utkast.gyldigFra = this.gyldigFra
        auditInfo?.let { utkast.auditInfo = it }
        utkast.status = status

        return utkast
    }

    fun withGyldigFra(gyldigFra: LocalDate): UtkastBuilder {
        this.gyldigFra = gyldigFra
        return this
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

    fun withStatus(status: UtkastStatus): UtkastBuilder {
        this.status = status
        return this
    }
}
