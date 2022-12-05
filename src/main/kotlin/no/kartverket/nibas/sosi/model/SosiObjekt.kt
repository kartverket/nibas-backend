package no.kartverket.nibas.sosi.model

import java.io.PrintStream
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

open class SosiObjekt(
    val type: String,
    val niva: Int,
    val verdier: MutableList<SosiVerdi> = ArrayList(),
    var barn: MutableList<SosiObjekt> = ArrayList(),
    var forelder: SosiObjekt? = null,
    var koordsys: String? = null,
    var enhet: Double = 1.0
) {

    companion object {
        val SOSI_DATOFORMAT = DateTimeFormatter.ofPattern("yyyyMMdd")
    }

    fun harBarn(): Boolean = barn.size > 0
    fun getVerdierAvBarn(type: String): List<String> = barn.filter { it.type == type }.flatMap { it.verdier }.map { it.verdi }
    fun getVerdiAvBarn(type: String): String? = barn.find { it.type == type }?.verdier?.getOrNull(0)?.verdi
    fun getKurver(): List<SosiKurve> = barn.filter { it is SosiKurve }.map { it as SosiKurve }
    fun getFlater(): List<SosiFlate> = barn.filter { it is SosiFlate }.map { it as SosiFlate }
    fun getBuep(): List<SosiBuep> = barn.filter { it is SosiBuep }.map { it as SosiBuep }
    fun getHode(): SosiHode? = barn.find { it is SosiHode } as? SosiHode

    fun getVerdiMedIndex(index: Int): String? {
        return when (verdier.size > index) {
            true -> verdier[index].verdi
            false -> null
        }
    }

    fun addBarn(next: SosiObjekt) {
        barn.add(next)
        next.forelder = this
    }

    fun fjernBarnUtenVerdierEllerBarn(): SosiObjekt {
        barn.forEach { it.fjernBarnUtenVerdierEllerBarn() }
        barn.removeIf { it.barn.isEmpty() && it.verdier.isEmpty() }
        return this
    }

    fun setKoordsysOgEnhetPaAlleBarn(koordsys: String?, enhet: Double): SosiObjekt {
        barn.forEach {
            it.koordsys = koordsys
            it.enhet = enhet
            it.setKoordsysOgEnhetPaAlleBarn(koordsys, enhet)
        }
        return this
    }

    fun debug(out: PrintStream) {
        when (niva) {
            0 -> out.print("")
            1 -> out.print("  ")
            2 -> out.print("    ")
            3 -> out.print("      ")
            else -> out.print("        ")
        }

        out.print("$niva - ${type} - ${verdier.map { v -> v.verdi }}\n")
        barn.forEach { it.debug(out) }
    }

    /**
     * Finner NIVÅ 1 - objekt med gitt id
     */
    fun getObjektMedId(objId: String): SosiGeometri? {
        return barn.find { it is SosiGeometri && it.getId() == objId } as? SosiGeometri
    }

    fun getSosiGeometriForReferanse(ref: SosiReferanse): SosiGeometri? {
        val geometri = barn.find { it is SosiGeometri && it.getId() == ref.getVerdiUtenRetningOgParentes() } as? SosiGeometri
        geometri?.erRetningMedKlokken = ref.erRetningMedKlokken()
        return geometri
    }

    protected fun stringAsDate(verdi: String?): LocalDate? = when (verdi) {
        null -> null
        else -> stringAsZonedDateTime(verdi)?.toLocalDate()
    }

    protected fun stringAsZonedDateTime(verdi: String?): ZonedDateTime? = when (verdi) {
        null -> null
        else -> ZonedDateTime.of(LocalDate.parse(verdi.substring(0, 8), SOSI_DATOFORMAT).atStartOfDay(), ZoneId.systemDefault())
    }
}
