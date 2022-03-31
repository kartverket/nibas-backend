package no.kartverket.nibas.builders

import no.kartverket.nibas.domain.Dokumentasjonsreferanse
import no.kartverket.nibas.domain.DokumentasjonsreferanseDokumentlink
import no.kartverket.nibas.domain.DokumentasjonsreferanseInternReferanseKartverket
import java.time.LocalDate

class DokumentasjonsreferanseBuilder {

    private var fastsettingsdato: LocalDate = LocalDate.now()
    private val lenker = mutableListOf<DokumentasjonsreferanseDokumentlink>()
    private val internRefs = mutableListOf<DokumentasjonsreferanseInternReferanseKartverket>()

    fun build(): Dokumentasjonsreferanse {
        val dokumentasjonsreferanse = Dokumentasjonsreferanse()
        dokumentasjonsreferanse.dokumentlenker.addAll(lenker)
        dokumentasjonsreferanse.fastsettingsdato = fastsettingsdato
        dokumentasjonsreferanse.fastsettingsmyndighet = MYNDIGHET
        dokumentasjonsreferanse.hjemmel = HJEMMEL_I_LOVEN
        dokumentasjonsreferanse.rettskildeid = RETTSKILDE_ID_1
        dokumentasjonsreferanse.rettskildetittel = TITTEL
        dokumentasjonsreferanse.internReferanserKartverket.addAll(internRefs)

        return dokumentasjonsreferanse
    }

    fun fastSettingAt(date: LocalDate): DokumentasjonsreferanseBuilder {
        this.fastsettingsdato = date
        return this
    }

    fun addLenker(vararg links: DokumentasjonsreferanseDokumentlink): DokumentasjonsreferanseBuilder {
        lenker.addAll(links)
        return this
    }

    fun addInternRefs(vararg refs: DokumentasjonsreferanseInternReferanseKartverket): DokumentasjonsreferanseBuilder {
        internRefs.addAll(refs)
        return this
    }

}
