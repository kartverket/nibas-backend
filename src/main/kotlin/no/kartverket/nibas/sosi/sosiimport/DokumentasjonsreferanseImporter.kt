package no.kartverket.nibas.sosi.sosiimport

import no.kartverket.nibas.domain.Dokumentasjonsreferanse
import no.kartverket.nibas.domain.DokumentasjonsreferanseDokumentlink
import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.repository.DokumentasjonsreferanseDokumentlinkRepository
import no.kartverket.nibas.repository.DokumentasjonsreferanseRepository
import no.kartverket.nibas.sosi.model.SosiDokumentasjonsReferanse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class DokumentasjonsreferanseImporter {

    @Autowired
    lateinit var dokumentasjonsreferanseRepository: DokumentasjonsreferanseRepository

    @Autowired
    lateinit var dokumentasjonsreferanseDokumentlinkRepository: DokumentasjonsreferanseDokumentlinkRepository

    fun importerDokumentasjonsreferanse(sosiDokRef: SosiDokumentasjonsReferanse, grense: Grense) {
        var dokRef = Dokumentasjonsreferanse()
        dokRef.rettskildeid = sosiDokRef.getRettskildeId()
        dokRef.rettskildetittel = sosiDokRef.getRettskildeTittel()

        dokRef.fastsettingsdato = sosiDokRef.getFastsettingsdato()
        dokRef.grenseFk = grense

        dokRef = dokumentasjonsreferanseRepository.save(dokRef)

        val dokLink = DokumentasjonsreferanseDokumentlink()
        dokLink.dokumentlink = sosiDokRef.getDokumentLink()
        dokLink.dokumentasjonsreferanseFk = dokRef

        dokumentasjonsreferanseDokumentlinkRepository.save(dokLink)
    }
}
