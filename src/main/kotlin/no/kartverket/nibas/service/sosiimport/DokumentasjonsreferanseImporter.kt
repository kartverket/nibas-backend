package no.kartverket.nibas.service.sosiimport

import no.kartverket.nibas.domain.*
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
        when(grense) {
            is Fylkesgrense -> dokRef.fylkesgrenseFk = grense
            is Kommunegrense -> dokRef.kommunegrenseFk = grense
            is Avtaltavgrensningslinje -> dokRef.avtaltavgrensningslinjeFk = grense
            is Territorialgrense -> dokRef.territorialgrenseFk = grense
            is Grunnlinje -> dokRef.grunnlinjeFk = grense
            is Riksgrense -> dokRef.riksgrenseFk = grense
            is Lovvirkeomraadegrense -> dokRef.lovvirkeomraadegrenseFk = grense
        }

        dokRef = dokumentasjonsreferanseRepository.save(dokRef)

        val dokLink = DokumentasjonsreferanseDokumentlink()
        dokLink.dokumentlink = sosiDokRef.getDokumentLink()
        dokLink.dokumentasjonsreferanseFk = dokRef

        dokumentasjonsreferanseDokumentlinkRepository.save(dokLink)
    }
}