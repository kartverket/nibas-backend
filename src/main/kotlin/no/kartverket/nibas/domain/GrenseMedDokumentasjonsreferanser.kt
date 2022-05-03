package no.kartverket.nibas.domain

import no.kartverket.nibas.api.v1.common.geojson.metadata.Dokref


abstract class GrenseMedDokumentasjonsreferanser : Grense() {

    abstract var dokumentasjonsreferanser: MutableSet<Dokumentasjonsreferanse>

    fun updateDokumentasjonsreferanser(dokRefList: List<Dokref>) {
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
