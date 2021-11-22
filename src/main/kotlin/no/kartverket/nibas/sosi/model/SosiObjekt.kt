package no.kartverket.nibas.sosi.model

import no.kartverket.nibas.sosi.common.SosiKoder
import java.io.PrintStream
import java.time.format.DateTimeFormatter

open class SosiObjekt(
    val type: String,
    val niva: Int,
    val verdier: MutableList<SosiVerdi> = ArrayList(),
    var barn: MutableList<SosiObjekt> = ArrayList(),
    var forelder: SosiObjekt? = null,
) {

    companion object {
        val SOSI_DATOFORMAT = DateTimeFormatter.ofPattern("yyyyMMdd")
    }

    fun getVerdiAvBarn(type: String): String? = barn.find{ it.type == type}?.verdier?.getOrNull(0)?.verdi
    fun getKurver(): List<SosiKurve> = barn.filter { it is SosiKurve }.map { it as SosiKurve }
    fun getFlater(): List<SosiFlate> = barn.filter { it is SosiFlate }.map { it as SosiFlate }
    fun getBuep(): List<SosiBuep> = barn.filter { it is SosiBuep }.map { it as SosiBuep }
    fun getHode(): SosiHode? = barn.find { it is SosiHode } as? SosiHode

    fun addBarn(next: SosiObjekt) {
        barn.add(next)
        next.forelder = this
    }

    fun fjernBarnUtenVerdierEllerBarn(): SosiObjekt {
        barn.forEach  { it.fjernBarnUtenVerdierEllerBarn() }
        barn.removeIf { it.barn.isEmpty() && it.verdier.isEmpty()}
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
}