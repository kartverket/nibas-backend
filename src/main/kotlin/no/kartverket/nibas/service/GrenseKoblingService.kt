package no.kartverket.nibas.service

import no.kartverket.nibas.api.v1.common.Flatetype
import no.kartverket.nibas.api.v1.common.geojson.KontekstEgenskaper
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.common.ResponseException
import no.kartverket.nibas.common.getGrenserIRiktigRekkefoelge
import no.kartverket.nibas.domain.Endringstype
import no.kartverket.nibas.domain.Flate
import no.kartverket.nibas.domain.Fylke
import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.domain.GrenseKobling
import no.kartverket.nibas.domain.GrenseKoblingAttributter
import no.kartverket.nibas.domain.Grunnkrets
import no.kartverket.nibas.domain.GrunnkretsGrenseKobling
import no.kartverket.nibas.domain.Kommune
import no.kartverket.nibas.domain.Nasjon
import no.kartverket.nibas.domain.Stemmekrets
import no.kartverket.nibas.domain.StemmekretsGrenseKobling
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.Polygon
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class GrenseKoblingService {

    data class TilhorighetEndringer<T: GrenseKobling>(
        val nye: List<KontekstEgenskaper>,
        val uendrede: List<T>,
        val frafallende: List<T>
    )

    fun <T: GrenseKobling>getTilhorighetEndringer(gamleGrenseKoblinger: Set<T>, nyeKontekster: List<KontekstEgenskaper>, gyldighetsdato: LocalDate): TilhorighetEndringer<T> {
        // (setGamle + setNye) - (setGamle intersect setNye) er alle grunnkretsene som skal endres.
        // (setGamle intersect setNye) er alle uendrede

        val nye = nyeKontekster.filter { kontekts ->
            gamleGrenseKoblinger.none { grenseKobling ->
                when(grenseKobling){
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
            nyeKontekster.none {kontekst ->
                when(it){
                    is GrunnkretsGrenseKobling ->  kontekst.id?.lokalid?.value == it.grunnkrets.lokalid
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
        flaterSomHarFaattNyVersjon: MutableMap<Lokalid, Flate>,
        endringstype: Endringstype
    ) {
        //Når grensa tidligere hørte til kretsen er det allerede laget ny versjon av kretsen
        grenseKoblinger.forEach { grenseKobling ->
            when(grenseKobling){
                is GrunnkretsGrenseKobling -> {
                    val lokalidForGrunnkrets = Lokalid(grenseKobling.grunnkrets.lokalid)
                    val nyVersjonAvGrunnkrets = (flaterSomHarFaattNyVersjon[lokalidForGrunnkrets]
                        ?: throw ResponseException.NotFoundException("Fant ikke grunnkrets med lokalid: $lokalidForGrunnkrets")) as Grunnkrets

                    nyVersjonAvGrunnkrets.removeGrensekobling(grense)
                }
                is StemmekretsGrenseKobling -> {
                    val lokalidForStemmekrets = Lokalid(grenseKobling.stemmekrets.lokalid)
                    val nyVersjonAvStemmekrets = (flaterSomHarFaattNyVersjon[lokalidForStemmekrets]
                        ?: throw ResponseException.NotFoundException("Fant ikke stemmekrets med lokalid: $lokalidForStemmekrets")) as Stemmekrets

                    nyVersjonAvStemmekrets.removeGrensekobling(grense)
                }
                else -> throw ResponseException.BadRequestException("Kan kun setter endre tilhørighet på grunnkretsgrenser eller stemmekretsgrenser")
            }
        }
    }

    fun getRetningMedKlokken(grense: LineString, forrigeGrense: LineString?): Boolean {
        if (grense.startPoint == forrigeGrense?.startPoint || grense.startPoint == forrigeGrense?.endPoint) {
            return true
        }
        if (grense.endPoint == forrigeGrense?.startPoint || grense.endPoint == forrigeGrense?.endPoint) {
            return false
        }
        throw RuntimeException("Grensen rører hverken startpunkt eller endepunktet til forrige grense. Er rekkefølgen riktig?")
    }

    fun oppdaterGrenseKoblingAttributterForFlater(flater: Set<Flate>, gyldighetsDato: LocalDate){
        flater.forEach { flate ->
            val oppdatertGrenseKoblingListe = when (flate.type) {
                Flatetype.GRUNNKRETS -> (flate as Grunnkrets).getGrensekoblinger(gyldighetsDato)
                Flatetype.STEMMEKRETS -> (flate as Stemmekrets).getGrensekoblinger(gyldighetsDato)
                Flatetype.FYLKE -> (flate as Fylke).getGrensekoblinger(gyldighetsDato)
                Flatetype.KOMMUNE -> (flate as Kommune).getGrensekoblinger(gyldighetsDato)
                Flatetype.NASJON -> (flate as Nasjon).getGrensekoblinger(gyldighetsDato)
                Flatetype.SKOLEKRETS -> throw ResponseException.BadRequestException("Støtter ikke oppdatering av grensekoblinger for skolekretser")
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
        val grenserLS = grenser.map { grense ->  grense.grense }

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
        val grenserMedAttributter =  skall.flatMapIndexed {flateIndeks, skallPolygonP ->

            val hullISkall = hull.filter { it.within(skallPolygonP) }.flatMapIndexed {hullIndeks, hullPolygonP ->
                val hullPolygonLS = polygonTilLineStringsMap[hullPolygonP]
                    ?: throw RuntimeException("Fant ikke tilbake til linestrings for hullpolygon: $hullPolygonP under danning av nye grensekoblingattributter")
                hullPolygonLS.mapIndexed { index, lineString ->
                    val hullGrenseForLineString = grenser.first { it.grense.equals(lineString) }.apply { grense = lineString }
                    mapLineStringTilGrenseMedAttributter(
                        hullGrenseForLineString,
                        index,
                        getRetningMedKlokken(lineString, if(index-1 < 0) hullPolygonLS.last() else hullPolygonLS[index-1]),
                        flateIndeks,
                        hullIndeks
                    )
                }
            }
            val skallPolygonLS = polygonTilLineStringsMap[skallPolygonP]
                ?: throw RuntimeException("Fant ikke tilbake til linestrings for skallpolygon: $skallPolygonP under danning av nye grensekoblingattributter")
            skallPolygonLS.mapIndexed { index, lineString ->
                val skallGrenseForLineString = grenser.first { it.grense.equals(lineString) }.apply { grense = lineString }
                mapLineStringTilGrenseMedAttributter(
                    skallGrenseForLineString,
                    index,
                    getRetningMedKlokken(lineString, if(index-1 < 0) skallPolygonLS.last() else skallPolygonLS[index-1]),
                    flateIndeks,
                    null
                )
            }+hullISkall// legger til skall-polygonet sine hull sine GrenseMedAttributter
        }

        return grenserMedAttributter
    }

    private fun erFlateHullIEtPolygon(flate: Geometry, flater: List<Geometry>): Boolean {
        return flater.any { currentFlate -> currentFlate != flate && flate.within(currentFlate) }
    }

    private fun mapLineStringTilGrenseMedAttributter(
       grense: Grense, rekkefolge: Int, retningMedKlokken: Boolean, flateIndeks: Int, hullIndeks: Int?
    ): GrenseMedAttributter{
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

    fun konverterLineStringsTilPolygon(lineStrings: List<LineString>): Polygon {
        val geometryFactory = GeometryFactory()

        // Tar kordinatene til alle linestrings i en liste
        val coordinates = lineStrings.flatMap { it.coordinates.toList() }.toTypedArray()

        // lager en linear ring av kordinatene
        val linearRing = geometryFactory.createLinearRing(coordinates)

        // med linear ring lager vi et JTS Polygon
        return geometryFactory.createPolygon(linearRing)
    }
}
