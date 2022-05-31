package no.kartverket.nibas.domain

import no.kartverket.nibas.api.v1.common.geojson.metadata.Dokref
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import java.util.logging.Logger


abstract class GrenseMedDokumentasjonsreferanser : Grense() {

    private val logger: Logger = Logger.getLogger(this::class.java.name)
    abstract var dokumentasjonsreferanser: MutableSet<Dokumentasjonsreferanse>
    abstract fun setDokumentasjonsreferanseTilbakePeker(dokumentasjonsreferanse: Dokumentasjonsreferanse)

    fun updateDokumentasjonsreferanser(dokRefList: List<Dokref>) {
        dokRefList.forEach { dokRef ->
            var dokumentasjonsreferanse = this.dokumentasjonsreferanser.find { dokumentasjonsreferanse -> dokumentasjonsreferanse.id == dokRef.id }
            if (dokumentasjonsreferanse == null) { // Legger til ny Dokumentasjonsreferanse hvis id er null
                if (dokRef.id != null) { // Har sendt inn ikke-eksisterende id: Kaster HTTP 400
                    logger.warning("Invalid request - non-existing Dokumentasjonsreferanse-id.")
                    throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Non-existing Dokumentasjonsreferanse-id")
                }
                dokumentasjonsreferanse = Dokumentasjonsreferanse()
                this.dokumentasjonsreferanser.add(dokumentasjonsreferanse)
                setDokumentasjonsreferanseTilbakePeker(dokumentasjonsreferanse)
            }

            dokumentasjonsreferanse.fastsettingsdato = dokRef.fastsettingsdato
            dokumentasjonsreferanse.fastsettingsmyndighet = dokRef.fastsettingsmyndighet
            dokumentasjonsreferanse.hjemmel = dokRef.hjemmel
            dokumentasjonsreferanse.rettskildeid = dokRef.rettskildeId
            dokumentasjonsreferanse.rettskildetittel = dokRef.rettskildeTittel

            val dokumentlenker = dokumentasjonsreferanse.dokumentlenker
            dokRef.dokumentlenker.forEach { dokumentLenkeHolder ->
                val dokumentLenkeHolderId = dokumentLenkeHolder.id
                var dokumentLink = dokumentlenker.find { dokumentLink -> dokumentLink.id == dokumentLenkeHolderId }
                if (dokumentLink == null) { // Hvis ny instans, opprettes den
                    if (dokumentLenkeHolderId != null) { // Har sendt inn ikke-eksisterende id: Kaster HTTP 400
                        logger.warning("Invalid request - non-existing DokumentasjonsreferanseDokumentlink-id.")
                        throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Non-existing DokumentasjonsreferanseDokumentlink-id")
                    }
                    dokumentLink = dokumentasjonsreferanse.addDokumentLenke()
                }
                dokumentLink.dokumentlink = dokumentLenkeHolder.beskrivelse
            }

            val internReferanser = dokumentasjonsreferanse.internReferanserKartverket
            dokRef.internReferanserKartverket.forEach { internRefHolder ->
                val internRefHolderId = internRefHolder.id
                var internReferanse = internReferanser.find { internRef -> internRef.id == internRefHolderId }
                if (internReferanse == null) { // Hvis ny instans, opprettes den
                    if (internRefHolderId != null) { // Har sendt inn ikke-eksisterende id: Kaster HTTP 400
                        logger.warning("Invalid request - non-existing DokumentasjonsreferanseInternReferanseKartverket-id.")
                        throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Non-existing DokumentasjonsreferanseInternReferanseKartverket-id")
                    }
                    internReferanse = dokumentasjonsreferanse.addInternReferanse()
                }
                internReferanse.internReferanseKartverket = internRefHolder.beskrivelse
            }
        }
    }

}
