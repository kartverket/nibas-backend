package no.kartverket.nibas.domain.utkast

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType
import no.kartverket.nibas.api.v1.common.Flatetype
import no.kartverket.nibas.api.v1.common.KontekstEnhetType
import no.kartverket.nibas.api.v1.common.Operasjoner
import no.kartverket.nibas.api.v1.request.FylkeRequest
import no.kartverket.nibas.api.v1.request.GrunnkretsRequest
import no.kartverket.nibas.api.v1.request.KommuneRequest
import no.kartverket.nibas.api.v1.request.StemmekretsRequest
import no.kartverket.nibas.domain.Endringstype
import no.kartverket.nibas.sporing.AuditInfo
import no.kartverket.nibas.sporing.Laasbar
import no.kartverket.nibas.sporing.Sporbar
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.annotations.Type
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
    @Type(JsonBinaryType::class)
    @Column(columnDefinition = "jsonb", name = "utkast")
    var operasjoner: Operasjoner? = Operasjoner()
) : Sporbar, Laasbar {

    @Version
    override var version: Int = 1

    @Column(name = "opprettet_dato", updatable = false)
    val opprettetDato: ZonedDateTime = ZonedDateTime.now()

    @JdbcTypeCode(SqlTypes.JSON)
    @Type(JsonBinaryType::class)
    @Column(columnDefinition = "jsonb", name = "endrede_inndelinger")
    var endredeInndelinger: EndredeInndelinger? = null


    @Embedded
    override var auditInfo: AuditInfo = AuditInfo()

    // Blir mappet av Converter
    var status: UtkastStatus = UtkastStatus.OPPRETTET

    fun settPublisert() {
        endredeInndelinger = operasjoner?.toEndredeInndelinger()
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

    fun withStatus(status: UtkastStatus): UtkastBuilder {
        this.status = status
        return this
    }
}

data class EndredeInndelinger (
    val endredeStemmekretser: MutableSet<String> = mutableSetOf(),
    val endredeGrunnkretser: MutableSet<String> = mutableSetOf()
)

fun Operasjoner.toEndredeInndelinger(): EndredeInndelinger {
    val endredeInndelinger = EndredeInndelinger()

    metadataendringer.grunnkretsendringer.values.map {
        endredeInndelinger.endredeGrunnkretser.add(it.identifikasjon.lokalid)
    }
    metadataendringer.stemmekretsendringer.values.map {
        endredeInndelinger.endredeStemmekretser.add(it.identifikasjon.lokalid)
    }

    for (feature in grenseendringer.endredeFeatures){
        feature.properties.kontekstEgenskaper.forEach {
            it.id?.lokalid?.value?.let { id ->
                when(it.type){
                    KontekstEnhetType.GRUNNKRETS -> endredeInndelinger.endredeGrunnkretser.add(id)
                    KontekstEnhetType.STEMMEKRETS -> endredeInndelinger.endredeStemmekretser.add(id)
                }
            }
        }
    }

    stemmekretsSammenslaaingsendring?.viderefoertStemmekrets?.lokalId?.let { endredeInndelinger.endredeStemmekretser.add(it) }
    stemmekretsSammenslaaingsendring?.stemmekretserTilSammenslaaing?.map { it.lokalId }?.let { endredeInndelinger.endredeStemmekretser.addAll(it) }

    grunnkretsSammenslaaingsendring?.viderefoertGrunnkrets?.lokalId?.let { endredeInndelinger.endredeGrunnkretser.add(it) }
    grunnkretsSammenslaaingsendring?.grunnkretserTilSammenslaaing?.map { it.lokalId }?.let { endredeInndelinger.endredeGrunnkretser.addAll(it) }

    kretsDelingEndringer.forEach {
        if(it.flatetype === Flatetype.GRUNNKRETS){
            endredeInndelinger.endredeGrunnkretser.add(it.opprinneligKrets.lokalId)
        }
        else if ( it.flatetype === Flatetype.STEMMEKRETS){
            endredeInndelinger.endredeStemmekretser.add(it.opprinneligKrets.lokalId)
        }
    }

    return endredeInndelinger
}
