package no.kartverket.nibas.service

import no.kartverket.nibas.api.v1.common.Flatetype
import no.kartverket.nibas.api.v1.common.geojson.KontekstEgenskaper
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.common.ResponseException
import no.kartverket.nibas.common.geometry.erFlateHullIEtPolygon
import no.kartverket.nibas.common.geometry.getGrenserIRiktigRekkefoelge
import no.kartverket.nibas.common.geometry.konverterLineStringsTilPolygon
import no.kartverket.nibas.domain.Endringstype
import no.kartverket.nibas.domain.Flate
import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.domain.GrenseKobling
import no.kartverket.nibas.domain.GrenseKoblingAttributter
import no.kartverket.nibas.domain.Grunnkrets
import no.kartverket.nibas.domain.GrunnkretsGrenseKobling
import no.kartverket.nibas.domain.Stemmekrets
import no.kartverket.nibas.domain.StemmekretsGrenseKobling
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.Polygon
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class GrenseKoblingService {

    data class TilhorighetEndringer<T : GrenseKobling>(
        val nye: List<KontekstEgenskaper>,
        val uendrede: List<T>,
        val frafallende: List<T>
    )

    fun <T : GrenseKobling> getTilhorighetEndringer(gamleGrenseKoblinger: Set<T>, nyeKontekster: List<KontekstEgenskaper>, gyldighetsdato: LocalDate): TilhorighetEndringer<T> {
        // (setGamle + setNye) - (setGamle intersect setNye) er alle grunnkretsene som skal endres.
        // (setGamle intersect setNye) er alle uendrede

        val nye = nyeKontekster.filter { kontekts ->
            gamleGrenseKoblinger.none { grenseKobling ->
                when (grenseKobling) {
                    is GrunnkretsGrenseKobling -> grenseKobling.grunnkrets.lokalid == kontekts.id?.lokalid?.value
                    is StemmekretsGrenseKobling -> grenseKobling.stemmekrets.lokalid == kontekts.id?.lokalid?.value
                    else -> throw ResponseException.BadRequestException("Støtter ikke noe annet enn grunnkretsgrensekobling eller stemmekretsgrensekobling")
                }
            }
        }

        val uendrede = gamleGrenseKoblinger.filter { gammelKobling ->
            nyeKontekster.any { kontekst ->
                when (gammelKobling) {
                    is GrunnkretsGrenseKobling -> kontekst.id?.lokalid?.value == gammelKobling.grunnkrets.lokalid
                    is StemmekretsGrenseKobling -> kontekst.id?.lokalid?.value == gammelKobling.stemmekrets.lokalid
                    else -> throw ResponseException.BadRequestException("Støtter ikke noe annet enn grunnkretsgrensekobling eller stemmekretsgrensekobling")
                }
            }
        }

        val frafallende = gamleGrenseKoblinger.filter {
            nyeKontekster.none { kontekst ->
                when (it) {
                    is GrunnkretsGrenseKobling -> kontekst.id?.lokalid?.value == it.grunnkrets.lokalid
                    is StemmekretsGrenseKobling -> kontekst.id?.lokalid?.value == it.stemmekrets.lokalid
                    else -> throw ResponseException.BadRequestException("Støtter ikke noe annet enn grunnkretsgrensekobling eller stemmekretsgrensekobling")
                }
            }
        }

        return TilhorighetEndringer(
            nye,
            uendrede,
            frafallende
        )
    }

    fun <T : GrenseKobling> fjernGrenseFraKretserViaGrenseKobling(
        grense: Grense,
        grenseKoblinger: List<T>,
        gyldighetsDato: LocalDate,
        flaterSomHarFaattNyVersjon: Map<Lokalid, Flate>,
        endringstype: Endringstype
    ): Map<Lokalid, Flate> {
        //Når grensa tidligere hørte til kretsen er det allerede laget ny versjon av kretsen
        val nyeVersjonerAvFlater = mutableMapOf<Lokalid, Flate>()

        grenseKoblinger.forEach { grenseKobling ->
            when (grenseKobling) {
                is GrunnkretsGrenseKobling -> {
                    val lokalidForGrunnkrets = Lokalid(grenseKobling.grunnkrets.lokalid)
                    val nyVersjonAvGrunnkrets = (flaterSomHarFaattNyVersjon[lokalidForGrunnkrets]
                        ?: throw ResponseException.NotFoundException("Fant ikke grunnkrets med lokalid: $lokalidForGrunnkrets")) as Grunnkrets

                    nyVersjonAvGrunnkrets.removeGrensekobling(grense)
                    nyeVersjonerAvFlater[lokalidForGrunnkrets] = nyVersjonAvGrunnkrets
                }

                is StemmekretsGrenseKobling -> {
                    val lokalidForStemmekrets = Lokalid(grenseKobling.stemmekrets.lokalid)
                    val nyVersjonAvStemmekrets = (flaterSomHarFaattNyVersjon[lokalidForStemmekrets]
                        ?: throw ResponseException.NotFoundException("Fant ikke stemmekrets med lokalid: $lokalidForStemmekrets")) as Stemmekrets

                    nyVersjonAvStemmekrets.removeGrensekobling(grense)
                    nyeVersjonerAvFlater[lokalidForStemmekrets] = nyVersjonAvStemmekrets
                }

                else -> throw ResponseException.BadRequestException("Kan kun setter endre tilhørighet på grunnkretsgrenser eller stemmekretsgrenser")
            }
        }

        return nyeVersjonerAvFlater
    }

    fun oppdaterGrenseKoblingAttributterForFlater(flater: Set<Flate>, gyldighetsDato: LocalDate) {
        flater
            .filter { (it.type == Flatetype.GRUNNKRETS || it.type == Flatetype.STEMMEKRETS) }
            .forEach { flate ->
                val oppdatertGrenseKoblingListe = when (flate.type) {
                    Flatetype.GRUNNKRETS -> (flate as Grunnkrets).getGrensekoblinger(gyldighetsDato)
                    Flatetype.STEMMEKRETS -> (flate as Stemmekrets).getGrensekoblinger(gyldighetsDato)
                    else -> throw RuntimeException("Det gir ikke mening å oppdatere grensekoblingattributter for flatetypen: ${flate.type}. Dette tyder på en implementasjonsfeil")
                }

                val gma = dannGrenseKoblingAttributterForGrenser(oppdatertGrenseKoblingListe.map { it.grense })
                oppdatertGrenseKoblingListe.forEach {
                    it.oppdaterAttributter(gma.first { gma -> gma.grense.lokalid == it.grense.lokalid }.attributter)
                }
            }
    }

    // Hjelpeklasse for å håndtere grenser og koblinger grensen har til grunn- og stemmekretser.
    data class GrenseMedAttributter(
        val grense: Grense,
        val attributter: GrenseKoblingAttributter,
    )

    fun dannGrenseKoblingAttributterForGrenser(grenser: List<Grense>): List<GrenseMedAttributter> {
        // Lager polygon(er)
        val grenserLS = grenser.map { grense -> grense.grense }

        // Får grensene delt inn i sammenhengende polygon(er)
        val polygonerLS = getGrenserIRiktigRekkefoelge(grenserLS)

        // Holder oversikt over hvilket polygon som tilsvarer hvilke line strings
        val polygonTilLineStringsMap = mutableMapOf<Polygon, List<LineString>>()

        // En liste for flatene transformert til JTS Polygon
        val polygonerP = polygonerLS.map { lineStrings ->
            val polygon = konverterLineStringsTilPolygon(lineStrings)
            polygonTilLineStringsMap[polygon] = lineStrings
            polygon
        }

        // Splitter listen inn i hull og skall for å kunne gå gjennom de separat
        val (hull, skall) = polygonerP.partition { erFlateHullIEtPolygon(it, polygonerP) }

        // Opprett nye GrenseKoblingAttributter ved å gå hierarkisk gjennom skall og hull
        val grenserMedAttributter = skall.flatMapIndexed { flateIndeks, skallPolygonP ->

            val hullISkall = hull.filter { it.within(skallPolygonP) }.flatMapIndexed { hullIndeks, hullPolygonP ->
                val hullPolygonLS = polygonTilLineStringsMap[hullPolygonP]
                    ?: throw RuntimeException("Fant ikke tilbake til linestrings for hullpolygon: $hullPolygonP under danning av nye grensekoblingattributter")
                hullPolygonLS
                    .map { finnGrenseMedLineString(it, grenser) }
                    .mapIndexed { index, hullGrenseForLineString ->
                        val forrigeLineString = if (index == 0) hullPolygonLS.last() else hullPolygonLS[index - 1]
                        mapLineStringTilGrenseMedAttributter(
                            grense = hullGrenseForLineString,
                            rekkefolge = index,
                            retningMedKlokken = getRetningMedKlokken(hullGrenseForLineString, finnGrenseMedLineString(forrigeLineString, grenser), hullPolygonLS.size, index),
                            flateIndeks = flateIndeks,
                            hullIndeks = hullIndeks
                        )
                    }
            }
            val skallPolygonLS = polygonTilLineStringsMap[skallPolygonP]
                ?: throw RuntimeException("Fant ikke tilbake til linestrings for skallpolygon: $skallPolygonP under danning av nye grensekoblingattributter")
            skallPolygonLS
                .map { finnGrenseMedLineString(it, grenser) }
                .mapIndexed { index, skallGrenseForLineString ->
                    val forrigeLineString = if (index == 0) skallPolygonLS.last() else skallPolygonLS[index - 1]
                    mapLineStringTilGrenseMedAttributter(
                        grense = skallGrenseForLineString,
                        rekkefolge = index,
                        retningMedKlokken = getRetningMedKlokken(skallGrenseForLineString, finnGrenseMedLineString(forrigeLineString, grenser), skallPolygonLS.size, index),
                        flateIndeks = flateIndeks,
                        hullIndeks = null
                    )
                } + hullISkall// legger til skall-polygonet sine hull sine GrenseMedAttributter
        }

        return grenserMedAttributter
    }

    private fun getRetningMedKlokken(grense: Grense, forrigeGrense: Grense, antallGrenser: Int, grenseIndex: Int): Boolean {
        val grenseSittStartPunktToucherForrigeGrense = grense.grense.startPoint == forrigeGrense.grense.startPoint || grense.grense.startPoint == forrigeGrense.grense.endPoint
        val grenseSittEndePunktToucherForrigeGrense = grense.grense.endPoint == forrigeGrense.grense.startPoint || grense.grense.endPoint == forrigeGrense.grense.endPoint

        if (antallGrenser == 1) {
            return true
        }

        if (antallGrenser == 2) {
            if (grenseIndex == 0) {
                return true
            }
            return grense.grense.startPoint == forrigeGrense.grense.endPoint
        }

        if (!grenseSittEndePunktToucherForrigeGrense && !grenseSittStartPunktToucherForrigeGrense) {
            throw RuntimeException("Den nye grensen rører ikke forrige grense i hverken startpunkt eller endepunkt. Dette tyder på noe feil med algoritmen som bygger opp grensene.")
        }

        return grenseSittStartPunktToucherForrigeGrense
    }

    private fun finnGrenseMedLineString(linestring: LineString, grenser: List<Grense>): Grense {
        return grenser.find { it.grense.equalsTopo(linestring) }
            ?: throw RuntimeException("Klarte ikke finne igjen grensen for linestringen. Dette tyder på feil i flatedanningsalgoritmen.")
    }

    private fun mapLineStringTilGrenseMedAttributter(
        grense: Grense, rekkefolge: Int, retningMedKlokken: Boolean, flateIndeks: Int, hullIndeks: Int?
    ): GrenseMedAttributter {
        val grenseKoblingAttributter = GrenseKoblingAttributter(
            rekkefoelge = rekkefolge,
            retningMedKlokken = retningMedKlokken,
            flateIndeks = flateIndeks,
            hullIndeks = hullIndeks,
        )

        return GrenseMedAttributter(
            grense,
            grenseKoblingAttributter
        )
    }
}
