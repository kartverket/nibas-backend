package no.kartverket.nibas.service

import no.kartverket.nibas.api.v1.common.geojson.KontekstEgenskaper
import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.domain.Endringstype
import no.kartverket.nibas.domain.Flate
import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.domain.GrenseKobling
import no.kartverket.nibas.domain.GrenseKoblingAttributter
import no.kartverket.nibas.domain.Grunnkrets
import no.kartverket.nibas.domain.GrunnkretsGrenseKobling
import no.kartverket.nibas.domain.Stemmekrets
import no.kartverket.nibas.domain.StemmekretsGrenseKobling
import no.kartverket.nibas.repository.GrunnkretsRepository
import no.kartverket.nibas.repository.StemmekretsRepository
import no.kartverket.nibas.repository.findGyldigObjektForIdentifikator
import no.kartverket.nibas.service.deltgeometri.DeltGeometriAdministrativeGrenserService
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.Polygon
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class GrenseKoblingService(
    private val grunnkretsService: GrunnkretsService,
    private val stemmekretsService: StemmekretsService,
    private val deltGeometriAdministrativeGrenserService: DeltGeometriAdministrativeGrenserService,
    private val grunnkretsRepository: GrunnkretsRepository,
    private val stemmekretsRepository: StemmekretsRepository,
) {

    data class TilhorighetEndringer<T: GrenseKobling>(
        val nye: List<KontekstEgenskaper>,
        val uendrede: List<T>,
        val frafallende: List<T>
    )

    fun <T: GrenseKobling>getTilhorighetEndringer(gamleGrenseKoblinger: List<T>, nyeKontekster: List<KontekstEgenskaper>, gyldighetsdato: LocalDate): TilhorighetEndringer<T> {
        // (setGamle + setNye) - (setGamle intersect setNye) er alle grunnkretsene som skal endres.
        // (setGamle intersect setNye) er alle uendrede

        val nye = nyeKontekster.filter { kontekts ->
            gamleGrenseKoblinger.none { grenseKobling ->
                when(grenseKobling){
                    is GrunnkretsGrenseKobling -> grenseKobling.grunnkrets.lokalid == kontekts.id?.lokalid?.value
                    is StemmekretsGrenseKobling -> grenseKobling.stemmekrets.lokalid == kontekts.id?.lokalid?.value
                    else -> throw RuntimeException("Støtter ikke noe annet enn grunnkretsgrensekobling eller stemmekretsgrensekobling")
                }
            }
        }

        val uendrede = gamleGrenseKoblinger.filter { gammelKobling ->
            nyeKontekster.any { kontekst ->
                when (gammelKobling) {
                    is GrunnkretsGrenseKobling -> kontekst.id?.lokalid?.value == gammelKobling.grunnkrets.lokalid
                    is StemmekretsGrenseKobling -> kontekst.id?.lokalid?.value == gammelKobling.stemmekrets.lokalid
                    else -> throw RuntimeException("Støtter ikke noe annet enn grunnkretsgrensekobling eller stemmekretsgrensekobling")
                }
            }
        }

        val frafallende = gamleGrenseKoblinger.filter {
            nyeKontekster.none {kontekst ->
                when(it){
                    is GrunnkretsGrenseKobling ->  kontekst.id?.lokalid?.value == it.grunnkrets.lokalid
                    is StemmekretsGrenseKobling -> kontekst.id?.lokalid?.value == it.stemmekrets.lokalid
                    else -> throw RuntimeException("Støtter ikke noe annet enn grunnkretsgrensekobling eller stemmekretsgrensekobling")
                }
            }
        }

        return TilhorighetEndringer(
            nye,
            uendrede,
            frafallende
        )
    }

    fun lagGrunnkretsGrenseKoblingerForKretserViaKontekstEgenskaper(
        nyeKontekster: List<KontekstEgenskaper>,
        grense: Grense,
        gyldighetsDato: LocalDate,
        flaterSomHarFaattNyVersjon: MutableMap<Lokalid, Flate>,
        endringstype: Endringstype
    ): List<GrunnkretsGrenseKobling> {
        return nyeKontekster.map { konteksEgenskaper ->
            if(konteksEgenskaper.id == null){
                throw RuntimeException("Kontekst peker ikke til en grunnkrets")
            }

            // Henter nyeste versjon av grunnkrets i dette utkastet hvis det finnes, ellers henter vi nyeste fra db og lager ny versjon med den.
            val nyVersjonAvGrunnkrets = flaterSomHarFaattNyVersjon[konteksEgenskaper.id.lokalid] ?:
            grunnkretsService.createNyVersjonAvGrunnkrets(
                grunnkretsRepository.findGyldigObjektForIdentifikator(Identifikator(konteksEgenskaper.id.lokalid, gyldighetsDato))
                    ?: throw RuntimeException("Fant ikke grunnkrets med lokalid:${konteksEgenskaper.id.lokalid}"),
                gyldighetsDato,
                endringstype
            )

            // Lager set med grenser for den nye versjonen av grunnkretsen.
            val grenserForNyVersjonAvGrunnkrets =
                grunnkretsService.hentGrenserForGrunnkrets(Identifikator(Lokalid(nyVersjonAvGrunnkrets.lokalid), gyldighetsDato)).map { it.grense } + grense

            // Det å legge til en ny grense på kretsen krever at man må regne ut nye grensekoblingattributter for alle grensene
            val nyeGrenseKoblingAttributter=
                dannGrenseKoblingAttributterForGrenser(grenserForNyVersjonAvGrunnkrets.toList())

            // Lager de nye GrunnkretsGrenseKoblingene
            val nyeGrunnkretsGrenseKoblinger = nyeGrenseKoblingAttributter.map {
                grenseOgAttributt ->
                GrunnkretsGrenseKobling(
                    nyVersjonAvGrunnkrets as Grunnkrets,
                    grenserForNyVersjonAvGrunnkrets.find { grense -> grense.lokalid == grenseOgAttributt.grense.lokalid }!!,
                    grenseOgAttributt.attributter
                )
            }

            // oppdaterer oppdatertGrunnkrets med nye grensekoblinger
            (nyVersjonAvGrunnkrets as Grunnkrets).grenser = nyeGrunnkretsGrenseKoblinger.toMutableSet()
            flaterSomHarFaattNyVersjon[konteksEgenskaper.id.lokalid] = nyVersjonAvGrunnkrets

            // returnerer den kun den spesifikke koblingen mellom grensen og grunnkretsen da det er dette som brukes senere
            nyeGrunnkretsGrenseKoblinger.find { it.grense.lokalid == grense.lokalid }
                ?: throw RuntimeException("Fant ikke ny GrunnkretsGrenseKobling for grensen")
        }
    }

    fun lagStemmekretsGrenseKoblingerForKretserViaKontekstEgenskaper(
        nyeKontekster: List<KontekstEgenskaper>,
        grense: Grense, gyldighetsDato: LocalDate,
        flaterSomHarFaattNyVersjon: MutableMap<Lokalid, Flate>,
        endringstype: Endringstype
    ): List<StemmekretsGrenseKobling> {
        return nyeKontekster.map { konteksEgenskaper ->
            if(konteksEgenskaper.id == null){
                throw RuntimeException("Kontekst peker ikke til en stemmekrets")
            }
            // Henter nyeste versjon av stemmekrets i dette utkastet hvis det finnes, ellers henter vi nyeste fra db og lager ny versjon med den.
            val nyVersjonAvStemmekrets = flaterSomHarFaattNyVersjon[konteksEgenskaper.id.lokalid]
                ?: stemmekretsService.createNyVersjonAvStemmekrets(stemmekretsRepository.findGyldigObjektForIdentifikator(
                    Identifikator(konteksEgenskaper.id.lokalid, gyldighetsDato))
                    ?: throw RuntimeException("Fant ikke stemmekrets med lokalid:${konteksEgenskaper.id.lokalid}"),
                    gyldighetsDato, endringstype)

            // Lager liste med grenser for den nye versjonen av stemmekretsen.
            val grenserForNyVersjonAvStemmekrets =
                (stemmekretsService.hentGrenserForStemmekrets(Identifikator(Lokalid(nyVersjonAvStemmekrets.lokalid), gyldighetsDato)).map { it.grense } + grense)

            // danner nye grensekoblingattributter for nye grensekoblinger
            val nyeGrenseKoblingAttributterForGrenser=
                dannGrenseKoblingAttributterForGrenser(grenserForNyVersjonAvStemmekrets.toList())

            // Lager de nye StemmekretsGrenseKoblingene som lagres via lagring av grunnkrets
            val nyeStemmekretsGrenseKoblinger = nyeGrenseKoblingAttributterForGrenser.map {
                grenseOgAttributt ->
                StemmekretsGrenseKobling(
                    nyVersjonAvStemmekrets as Stemmekrets,
                    grenserForNyVersjonAvStemmekrets.find { grense -> grense.lokalid == grenseOgAttributt.grense.lokalid }!!,
                    grenseOgAttributt.attributter)
            }.toMutableSet()

            // oppdaterer stemmekrets med nye grensekoblinger
            (nyVersjonAvStemmekrets as Stemmekrets).grenser = nyeStemmekretsGrenseKoblinger
            flaterSomHarFaattNyVersjon[konteksEgenskaper.id.lokalid] = nyVersjonAvStemmekrets

            // returnerer grensekoblingen for kretsen
            nyeStemmekretsGrenseKoblinger.find { it.grense.lokalid == grense.lokalid }
                ?: throw RuntimeException("Fant ikke ny StemmekretsGrenseKobling for grensen")
        }
    }

    fun <T: GrenseKobling>fjernGrenseFraKretserViaGrenseKobling(
        grense: Grense, grenseKoblinger: List<T>, gyldighetsDato: LocalDate, flaterSomHarFaattNyVersjon: MutableMap<Lokalid, Flate>, endringstype: Endringstype
    ) {
        grenseKoblinger.forEach { grenseKobling ->
            when(grenseKobling){
                is GrunnkretsGrenseKobling -> {
                    val lokalidForGrunnkrets = Lokalid(grenseKobling.grunnkrets.lokalid)
                    val nyVersjonAvGrunnkrets = flaterSomHarFaattNyVersjon[lokalidForGrunnkrets]
                        ?: grunnkretsService.createNyVersjonAvGrunnkrets(
                            grunnkretsRepository.findGyldigObjektForIdentifikator(Identifikator(lokalidForGrunnkrets, gyldighetsDato))
                                ?: throw RuntimeException("Fant ikke grunnkrets med lokalid:${lokalidForGrunnkrets}"),
                            gyldighetsDato,
                            endringstype
                        )
                    val oppdatertGrenseListe = (nyVersjonAvGrunnkrets as Grunnkrets).grenser.filter { gk -> gk.grense.lokalid != grense.lokalid }.map { it.grense }
                    val nyeGrenseKoblingAttributter = dannGrenseKoblingAttributterForGrenser(oppdatertGrenseListe)
                    val nyeGrunnkretsGrenseKoblinger = nyeGrenseKoblingAttributter.map { GrunnkretsGrenseKobling(nyVersjonAvGrunnkrets, it.grense, it.attributter) }
                    nyVersjonAvGrunnkrets.apply {
                        grenser = nyeGrunnkretsGrenseKoblinger.toMutableSet()
                    }
                    flaterSomHarFaattNyVersjon[lokalidForGrunnkrets] = nyVersjonAvGrunnkrets
                }
                is StemmekretsGrenseKobling -> {
                    val lokalidForStemmekrets = Lokalid(grenseKobling.stemmekrets.lokalid)
                    val nyVersjonAvStemmekrets = flaterSomHarFaattNyVersjon[lokalidForStemmekrets]
                        ?: stemmekretsService.createNyVersjonAvStemmekrets(
                            stemmekretsRepository.findGyldigObjektForIdentifikator(Identifikator(lokalidForStemmekrets, gyldighetsDato))
                                ?: throw RuntimeException("Fant ikke stemmekrets med lokalid:${lokalidForStemmekrets}"),
                            gyldighetsDato,
                            endringstype
                        )
                    val oppdatertGrenseListe = (nyVersjonAvStemmekrets as Stemmekrets).grenser.filter { gk -> gk.grense.lokalid != grense.lokalid }.map { it.grense }
                    val nyeGrenseKoblingAttributter = dannGrenseKoblingAttributterForGrenser(oppdatertGrenseListe)
                    val nyeStemmekretsGrenseKoblinger = nyeGrenseKoblingAttributter.map { StemmekretsGrenseKobling(nyVersjonAvStemmekrets, it.grense, it.attributter) }
                    nyVersjonAvStemmekrets.apply {
                        grenser = nyeStemmekretsGrenseKoblinger.toMutableSet()
                    }
                    flaterSomHarFaattNyVersjon[lokalidForStemmekrets] = nyVersjonAvStemmekrets
                }
                else -> throw RuntimeException("Støtter kun grunnkretser eller stemmekretser")
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


    // Hjelpeklasse for å håndtere grenser og koblinger grensen har til grunn- og stemmekretser.
    data class GrenseMedAttributter(
        val grense: Grense,
        val attributter: GrenseKoblingAttributter,
    )

    fun dannGrenseKoblingAttributterForGrenser(grenser: List<Grense>): List<GrenseMedAttributter> {
        // Lager polygon(er)
        val grenserLS = grenser.map { grense ->  grense.grense }

        // Får grensene delt inn i sammenhengende polygon(er)
        val polygonerLS = deltGeometriAdministrativeGrenserService.getGrenserIRiktigRekkefoelge(grenserLS)

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
                val hullPolygonLS = polygonTilLineStringsMap[hullPolygonP] ?: throw RuntimeException("Fant ikke tilbake til linestrings for hullpolygon: $hullPolygonP")
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
            val skallPolygonLS = polygonTilLineStringsMap[skallPolygonP] ?: throw RuntimeException("Fant ikke tilbake til linestrings for skallpolygon: $skallPolygonP")
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
