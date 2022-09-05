package no.kartverket.nibas.domain

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
    private val id: String = UUID.randomUUID().toString()
) : Sporbar {

    @Version
    var version: Int = 1

    @Column(name = "opprettet_dato", updatable = false)
    private val opprettetDato: ZonedDateTime = ZonedDateTime.now()

    @Embedded
    override var auditInfo: AuditInfo = AuditInfo()

    @Column(name = "navn")
    var navn: String = "Utkast_" + UUID.randomUUID().toString().substring(IntRange(0, 6))

    @Column(name = "gyldig_fra")
    var gyldigFra: LocalDate = LocalDate.now().plusYears(100)

    @Enumerated(EnumType.STRING)
    var endringstype: Endringstype = Endringstype.IKKE_DEFINERT

    @Enumerated(EnumType.STRING)
    var status: UtkastStatus = UtkastStatus.OPPRETTET

    @Type(type = "jsonb")
    @Column(columnDefinition = "jsonb")
    var utkast: UtkastStruktur = UtkastStruktur(id)

}

enum class Endringstype { IKKE_DEFINERT, VEDTATT_GRENSEENDRING, VEDTATT_SAMMENSLAAING }
enum class UtkastStatus { OPPRETTET, PUBLISERT, FORKASTET }

// TODO: Utvide med felter for sammenslåing og oppsplitting
data class UtkastStruktur(
    val id: String,
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
