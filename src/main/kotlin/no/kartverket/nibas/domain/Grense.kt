package no.kartverket.nibas.domain

import no.kartverket.nibas.api.v1.common.geojson.CommonGrenseMetadata
import no.kartverket.nibas.api.v1.common.geojson.Dokref
import no.kartverket.nibas.api.v1.common.geojson.toEmbeddable
import org.hibernate.envers.Audited
import org.locationtech.jts.geom.LineString
import java.util.*
import javax.persistence.Column
import javax.persistence.Embedded
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.MappedSuperclass

@MappedSuperclass
@Audited
abstract class Grense : CommonMetadataProperties {

    @Id
    @Column(name = "objid", nullable = false, updatable = false)
    val id: String = UUID.randomUUID().toString()

    @Column(name = "objtype")
    var objtype: String? = null

    @Column(name = "lokalid", nullable = false, updatable = false)
    override var lokalid: String = UUID.randomUUID().toString()

    @Column(name = "navnerom", nullable = false, updatable = false)
    override var navnerom: String = DEFAULT_NAVNEROM

    @Column(name = "grense", nullable = false)
    var grense: LineString? = null

    @Embedded
    var kvalitet: PosisjonskvalitetEmbeddable? = null

    @ManyToOne
    @JoinColumn(name = "grensestatus")
    var grensestatus: Grensestatus? = null

    @ManyToOne
    @JoinColumn(name = "fastsettingstype")
    var fastsettingstype: Fastsettingstype? = null

    abstract var dokumentasjonsreferanser: MutableSet<Dokumentasjonsreferanse>

    fun getGrenseMetadata(): CommonGrenseMetadata = CommonGrenseMetadata(
        posisjonskvalitet = this.kvalitet?.toKvalitet(),
        dokumentasjonsreferanser = this.dokumentasjonsreferanser.map { it.toDokRef() },
        fastsettingstype = this.fastsettingstype?.id,
        grensestatus = this.grensestatus?.id,
    )

    fun updateWithGrenseMetadata(commonGrenseMetadata: CommonGrenseMetadata) {
        this.kvalitet = commonGrenseMetadata.posisjonskvalitet?.toEmbeddable()
        this.updateDokumentasjonsreferanser(commonGrenseMetadata.dokumentasjonsreferanser)
        this.fastsettingstype?.id = commonGrenseMetadata.fastsettingstype
        this.grensestatus?.id = commonGrenseMetadata.grensestatus
    }

    private fun updateDokumentasjonsreferanser(dokRefList: List<Dokref>) {
        dokRefList.forEach { dokRef ->
            var dokumentasjonsreferanse = this.dokumentasjonsreferanser.find { dokumentasjonsreferanse -> dokumentasjonsreferanse.id == dokRef.id }
            if (dokumentasjonsreferanse == null) { // Legger til ny Dokumentasjonsreferanse hvis ulik id/id er null
                dokumentasjonsreferanse = Dokumentasjonsreferanse()
                this.dokumentasjonsreferanser.add(dokumentasjonsreferanse)
            }

            dokumentasjonsreferanse.fastsettingsdato = dokRef.fastsettingsdato
            dokumentasjonsreferanse.fastsettingsmyndighet = dokRef.fastsettingsmyndighet
            dokumentasjonsreferanse.hjemmel = dokRef.hjemmel
            dokumentasjonsreferanse.rettskildeid = dokRef.rettskildeId
            dokumentasjonsreferanse.rettskildetittel = dokRef.rettskildeTittel

            dokRef.dokumentlenker.forEach { dokumentLenkeHolder ->
                val dokumentlenker = dokumentasjonsreferanse.dokumentlenker
                var dokumentLink = dokumentlenker.find { dokumentLink -> dokumentLink.id == dokumentLenkeHolder.id }
                if (dokumentLink == null) { // Hvis ny instans, opprettes den
                    dokumentLink = DokumentasjonsreferanseDokumentlink()
                    dokumentlenker.add(dokumentLink)
                }
                dokumentLink.dokumentlink = dokumentLenkeHolder.beskrivelse
            }

            dokRef.internReferanserKartverket.forEach { internRefHolder ->
                val internReferanser = dokumentasjonsreferanse.internReferanserKartverket
                var internReferanse = internReferanser.find { internRef -> internRef.id == internRefHolder.id }
                if (internReferanse == null) { // Hvis ny instans, opprettes den
                    internReferanse = DokumentasjonsreferanseInternReferanseKartverket()
                    internReferanser.add(internReferanse)
                }
                internReferanse.internReferanseKartverket = internRefHolder.beskrivelse
            }
        }
    }
}
