package no.kartverket.nibas.domain.utkast

import com.vladmihalcea.hibernate.type.json.JsonBinaryType
import no.kartverket.nibas.api.v1.common.geojson.FeatureCollection
import no.kartverket.nibas.api.v1.request.FylkeRequest
import no.kartverket.nibas.api.v1.request.GrunnkretsRequest
import no.kartverket.nibas.api.v1.request.KommuneRequest
import no.kartverket.nibas.api.v1.request.NasjonRequest
import no.kartverket.nibas.api.v1.request.StemmekretsRequest
import no.kartverket.nibas.sporing.AuditInfo
import no.kartverket.nibas.sporing.Sporbar
import org.hibernate.annotations.Type
import org.hibernate.annotations.TypeDef
import org.hibernate.annotations.TypeDefs
import org.hibernate.envers.Audited
import java.io.Serializable
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.*
import javax.persistence.Column
import javax.persistence.Embedded
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table
import javax.persistence.Version

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
    val id: String = UUID.randomUUID().toString(),

    @Column(name = "navn")
    var navn: String,

    // Blir mappet av Converter
    var endringstype: UtkastEndringstype,

    @Type(type = "jsonb")
    @Column(columnDefinition = "jsonb", name = "utkast")
    val operasjoner: OperasjonerIUtkastet = OperasjonerIUtkastet()
) : Sporbar {

    @Version
    var version: Int = 1

    @Column(name = "opprettet_dato", updatable = false)
    val opprettetDato: ZonedDateTime = ZonedDateTime.now()

    @Embedded
    override var auditInfo: AuditInfo = AuditInfo()

    @Column(name = "gyldig_fra")
    var gyldigFra: LocalDate = LocalDate.now().plusYears(10)

    // Blir mappet av Converter
    var status: UtkastStatus = UtkastStatus.OPPRETTET

}

// TODO: Utvide med felter for sammenslåing og oppsplitting
// Typen er Serializable for persistering i jsonb-kolonne
data class OperasjonerIUtkastet(
    val metadataEndringer: MetadataEndringer = MetadataEndringer(),
    val grensejusteringer: Grensejusteringer = Grensejusteringer()
) : Serializable

data class MetadataEndringer(
    val nasjonsendringer: List<NasjonRequest> = emptyList(),
    val fylkesendringer: List<FylkeRequest> = emptyList(),
    val kommuneendringer: List<KommuneRequest> = emptyList(),
    val grunnkretsendringer: List<GrunnkretsRequest> = emptyList(),
    val stemmekretsendringer: List<StemmekretsRequest> = emptyList()
) : Serializable

data class Grensejusteringer(
    val featureCollections: List<FeatureCollection> = emptyList()
) : Serializable

class UtkastBuilder private constructor(val navn: String, private val endringstype: UtkastEndringstype = UtkastEndringstype.KVALITETSHEVING) {

    private var gyldigFra: LocalDate = LocalDate.now().plusYears(10)
    private var nasjonRequests: List<NasjonRequest> = listOf()
    private var fylkeRequests: List<FylkeRequest> = listOf()
    private var kommmuneRequests: List<KommuneRequest> = listOf()
    private var grunnkretsRequests: List<GrunnkretsRequest> = listOf()
    private var stemmekretsRequests: List<StemmekretsRequest> = listOf()
    private var endredeFeatures: List<FeatureCollection> = listOf()
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
            operasjoner = OperasjonerIUtkastet(
                metadataEndringer = MetadataEndringer(nasjonRequests, fylkeRequests, kommmuneRequests, grunnkretsRequests, stemmekretsRequests),
                grensejusteringer = Grensejusteringer(endredeFeatures)
            ))
        utkast.gyldigFra = this.gyldigFra
        auditInfo?.let { utkast.auditInfo = it }

        return utkast
    }

    fun withGyldigFra(gyldigFra: LocalDate): UtkastBuilder {
        this.gyldigFra = gyldigFra
        return this
    }

    fun withNasjonRequests(nasjonRequests: List<NasjonRequest>): UtkastBuilder {
        this.nasjonRequests += nasjonRequests
        return this
    }

    fun withFylkeRequests(fylkeRequests: List<FylkeRequest>): UtkastBuilder {
        this.fylkeRequests += fylkeRequests
        return this
    }

    fun withKommuneRequests(kommuneRequests: List<KommuneRequest>): UtkastBuilder {
        this.kommmuneRequests += kommuneRequests
        return this
    }

    fun withGrunnkretsRequests(grunnkretsRequests: List<GrunnkretsRequest>): UtkastBuilder {
        this.grunnkretsRequests += grunnkretsRequests
        return this
    }

    fun withStemmekretsRequests(stemmekretsRequests: List<StemmekretsRequest>): UtkastBuilder {
        this.stemmekretsRequests += stemmekretsRequests
        return this
    }

    fun withEndredeFeatures(endredeFeatures: List<FeatureCollection>): UtkastBuilder {
        this.endredeFeatures += endredeFeatures
        return this
    }

    fun withAuditInfo(auditInfo: AuditInfo): UtkastBuilder {
        this.auditInfo = auditInfo
        return this
    }

}
