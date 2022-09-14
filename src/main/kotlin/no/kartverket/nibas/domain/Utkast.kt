package no.kartverket.nibas.domain

import com.vladmihalcea.hibernate.type.json.JsonBinaryType
import no.kartverket.nibas.api.v1.common.UtkastEndringstype
import no.kartverket.nibas.api.v1.common.UtkastStatus
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
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.Id
import javax.persistence.Table
import javax.persistence.Version

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

    @Enumerated(EnumType.STRING)
    var endringstype: UtkastEndringstype = UtkastEndringstype.IKKE_DEFINERT

    @Enumerated(EnumType.STRING)
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

class UtkastBuilder private constructor(val navn: String) {

    private var endringstype: UtkastEndringstype = UtkastEndringstype.IKKE_DEFINERT
    private var gyldigFra: LocalDate = LocalDate.now().plusYears(10)
    private val nasjonRequests: List<NasjonRequest> = emptyList()
    private val fylkeRequests: List<FylkeRequest> = emptyList()
    private val kommmuneRequests: List<KommuneRequest> = emptyList()
    private val grunnkretsRequests: List<GrunnkretsRequest> = emptyList()
    private val stemmekretsRequests: List<StemmekretsRequest> = emptyList()
    private val endredeFeatures: List<FeatureCollection> = emptyList()

    companion object {
        fun builder(navn: String): UtkastBuilder {
            return UtkastBuilder(navn)
        }
    }

    fun build(): Utkast {
        val utkast = Utkast(
            navn = navn,
            operasjoner = OperasjonerIUtkastet(
                metadataEndringer = MetadataEndringer(nasjonRequests, fylkeRequests, kommmuneRequests, grunnkretsRequests, stemmekretsRequests),
                grensejusteringer = Grensejusteringer(endredeFeatures)
            ))
        utkast.gyldigFra = this.gyldigFra
        utkast.endringstype = this.endringstype
        return utkast
    }

    fun withGyldigFra(gyldigFra: LocalDate): UtkastBuilder {
        this.gyldigFra = gyldigFra
        return this
    }

    fun withEndringstype(endringstype: UtkastEndringstype): UtkastBuilder {
        this.endringstype = endringstype
        return this
    }

    fun withNasjonRequests(nasjonRequests: List<NasjonRequest>): UtkastBuilder {
        this.nasjonRequests.plus(nasjonRequests)
        return this
    }

    fun withFylkeRequests(fylkeRequests: List<FylkeRequest>): UtkastBuilder {
        this.fylkeRequests.plus(fylkeRequests)
        return this
    }

    fun withKommuneRequests(kommuneRequests: List<KommuneRequest>): UtkastBuilder {
        this.kommmuneRequests.plus(kommuneRequests)
        return this
    }

    fun withGrunnkretsRequests(grunnkretsRequests: List<GrunnkretsRequest>): UtkastBuilder {
        this.grunnkretsRequests.plus(grunnkretsRequests)
        return this
    }

    fun withStemmekretsRequests(stemmekretsRequests: List<StemmekretsRequest>): UtkastBuilder {
        this.stemmekretsRequests.plus(stemmekretsRequests)
        return this
    }

    fun withEndredeFeatures(endredeFeatures: List<FeatureCollection>): UtkastBuilder {
        this.endredeFeatures.plus(endredeFeatures)
        return this
    }

}
