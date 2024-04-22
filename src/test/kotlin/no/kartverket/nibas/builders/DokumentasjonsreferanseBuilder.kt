package no.kartverket.nibas.builders

import no.kartverket.nibas.domain.Dokumentasjonsreferanse
import no.kartverket.nibas.domain.DokumentasjonsreferanseDokumentlink
import no.kartverket.nibas.domain.DokumentasjonsreferanseInternReferanseKartverket
import no.kartverket.nibas.sporing.Gyldighet
import java.time.LocalDate

class DokumentasjonsreferanseBuilder {

    private var fastsettingsdato: LocalDate = LocalDate.now()
    private val lenker = mutableListOf<DokumentasjonsreferanseDokumentlink>()
    private val internRefs = mutableListOf<DokumentasjonsreferanseInternReferanseKartverket>()
    private var gyldighet : Gyldighet? = null
    fun build(): Dokumentasjonsreferanse {
        val dokumentasjonsreferanse = Dokumentasjonsreferanse()
        dokumentasjonsreferanse.dokumentlenker.addAll(lenker)
        dokumentasjonsreferanse.fastsettingsdato = fastsettingsdato
        dokumentasjonsreferanse.fastsettingsmyndighet = MYNDIGHET
        dokumentasjonsreferanse.hjemmel = HJEMMEL_I_LOVEN
        dokumentasjonsreferanse.rettskildeid = RETTSKILDE_ID_1
        dokumentasjonsreferanse.rettskildetittel = TITTEL
        dokumentasjonsreferanse.internReferanserKartverket.addAll(internRefs)
        gyldighet?.let { dokumentasjonsreferanse.gyldighet = it }
        return dokumentasjonsreferanse
    }

    fun addGyldighet(gyldighet: Gyldighet) : DokumentasjonsreferanseBuilder {
        this.gyldighet = gyldighet
        return this
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
