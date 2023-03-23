package no.kartverket.nibas.sosi.sosiimport

import no.kartverket.nibas.config.overlappedWindow
import no.kartverket.nibas.sosi.SosiImportService
import no.kartverket.nibas.sosi.common.SosiKoder
import no.kartverket.nibas.sosi.model.SosiFlate
import no.kartverket.nibas.sosi.model.SosiGeometri
import no.kartverket.nibas.sosi.model.SosiReferanse
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.linearref.LengthIndexedLine
import org.locationtech.jts.linearref.LengthLocationMap
import org.locationtech.jts.operation.distance.DistanceOp
import org.slf4j.LoggerFactory
import java.lang.Integer.min
import java.util.*

class GrenseMatcher {

    private val logger = LoggerFactory.getLogger(this.javaClass.name)

    var koordinaterSomHarBlittFlyttet = mutableMapOf<Coordinate, List<FlyttetTilKoordinat>>()
    var matchedeGrenser = mutableMapOf<String, List<SplitTilImport>>()

    companion object {
        fun skalMatches(grense: SosiGeometri) = when (grense.getObjType()) {
            SosiKoder.KOMMUNEGRENSE -> true
            SosiKoder.FYLKESGRENSE -> true
            SosiKoder.TERRITORIALGRENSE -> true
            SosiKoder.RIKSGRENSE -> true
            SosiKoder.AVTALT_AVGRENSNINGSLINJE -> true
            SosiKoder.LOV_VIRKEOMRADEGRENSE -> true
            SosiKoder.GRUNNLINJE -> true
            else -> false
        }
    }

    fun matchGrenser(kretsflater: List<SosiFlate>, kretsgrenser: List<SosiGeometri>, kommuneFlater: List<SosiFlate>) {
        kretsflater.forEach { flate ->
            flate.getSkallReferanser().forEach { ref ->
                //Her har vi en liste med grenser for en gitt grense fra kretsfila
                val grenseReferanse = ref.getVerdiUtenParentes()
                val motsattGrenseReferanse = motsattGrensereferanse(grenseReferanse)

                if (erNyGrensereferanse(grenseReferanse)) {
                    logger.debug("Ny grensereferanse: $grenseReferanse, må matches")
                    matchedeGrenser[grenseReferanse] = matchGrense(ref, kretsgrenser, kommuneFlater)
                }
                else {
                    if (matchedeGrenser.containsKey(motsattGrenseReferanse)) {
                        matchedeGrenser[grenseReferanse] = reverserteGrenser(grenseReferanse, motsattGrenseReferanse)
                    } else if (matchedeGrenser.containsKey(grenseReferanse)) {
                        logger.warn("Grense ${grenseReferanse} er allerede matchet. Dette betyr at to (eller flere) kretser refererer til samme krets med samme retning. Det skal ikke skje")
                    }
                }
            }

            flate.getHullReferanser().forEach { liste ->
                liste.forEach { ref ->
                    val grenseReferanse = ref.getVerdiUtenParentes()
                    val motsattGrenseReferanse = motsattGrensereferanse(grenseReferanse)

                    if (erNyGrensereferanse(grenseReferanse)) {
                        logger.debug("Ny grensereferanse (hull): $grenseReferanse, må matches")
                        matchedeGrenser[grenseReferanse] = matchGrense(ref, kretsgrenser, kommuneFlater)
                    }
                    else {
                        if (matchedeGrenser.containsKey(motsattGrenseReferanse)) {
                            matchedeGrenser[grenseReferanse] = reverserteGrenser(grenseReferanse, motsattGrenseReferanse)
                        } else if (matchedeGrenser.containsKey(grenseReferanse)) {
                            logger.warn("Grense ${grenseReferanse} (hull) er allerede matchet. Dette betyr at to (eller flere) kretser refererer til samme krets med samme retning")
                        }
                    }
                }
            }
        }
    }

    private fun matchGrense(
        kurveRef: SosiReferanse,
        kretsgrenser: List<SosiGeometri>,
        kommuneFlater: List<SosiFlate>
    ): List<SplitTilImport> {

        val referanse = kurveRef.getVerdiUtenRetningOgParentes()
        when (val grense: SosiGeometri? = kretsgrenser.find { it.getId() == referanse }) {
            null -> throw RuntimeException("Finner ikke kurve som refereres til. Feil i datagrunnlaget. Ref: $referanse")
            else -> {
                grense.erRetningMedKlokken = kurveRef.erRetningMedKlokken()

                if (listOf("14604", "75995").contains(referanse)) {
                    // Spesialhåndtering for Mostadmark:
                    // Disse to grensene danner et polygon, men begge matches av samme grense. Når man prøver å matche mot et enkelt grensestykke som biter seg selv i halen
                    // vil retningslogikken i matchingen feile og vi vil ende opp med bare en av grensene. Bruker derfor bare grensene fra grunnkrets-fila as is.
                    logger.info("Grense ${referanse} må håndteres spesifikt, returnerer den som den er")
                    return listOf(SplitTilImport(SplitRef(grense.asLineString(), grense, grense.erRetningMedKlokken), kurveRef, grense))
                }

                return when(skalMatches(grense)) {
                    true -> {
                        match(kurveRef, grense, kommuneFlater)
                    }
                    false -> {
                        // Grenser som ikke skal matches returneres bare as is. Da blir de lagt i mappet over grenser som skal importeres
                        logger.debug("Grense $referanse er ikke en administrativ grense, skal ikke matches")
                        listOf(SplitTilImport(SplitRef(grense.asLineString(), grense, grense.erRetningMedKlokken), kurveRef, grense))
                    }
                }
            }
        }
    }

    private fun match(kurveRef: SosiReferanse, grense: SosiGeometri, kommuneFlater: List<SosiFlate>): List<SplitTilImport> {
        val grenserSomDekker = finnGrenserSomDekker(kommuneFlater, grense)

        if(grenserSomDekker.isEmpty()) {
            logger.warn("Matching feilet for grense med lokalid: ${grense.getIdent()?.getLokalId()}")
            return emptyList()
        }

        if (kurveRef.getVerdiUtenRetningOgParentes() == "59090") {
            //Spesialhåndtering for å håndtere Mostadmark, den har en grense som dekkes av 2. Det håndterer ikke matchealgoritmen.
            logger.info("Grense 59090: Skal bare returnere de to grensene som dekker denne grensen")
            return grenserSomDekker
                .map { SplitRef(it.asLineString(), it, it.erRetningMedKlokken) }
                .map { SplitTilImport(it, kurveRef, grense) }
        }

        return splittGrenser(kurveRef, grense, grenserSomDekker)
    }

    private fun finnGrenserSomDekker(
        kommuneFlater: List<SosiFlate>,
        grense: SosiGeometri
    ): List<SosiGeometri> {
        kommuneFlater.forEach kommune@{ kommuneFlate ->
            val skalSjekkesMot = kommuneFlate.getSkallGeometri()
            logger.debug("Skal finne grenser som dekker for kommuneflate (skall): ${kommuneFlate.getId()} med ${skalSjekkesMot.size} grensereferanser")
            val dekketAvSkall = grenserSomDekkerNyGrense(skalSjekkesMot, grense)

            if (dekketAvSkall.isNotEmpty()) {
                return dekketAvSkall
            }

            kommuneFlate.getHullGeometri().forEach { hullGeometri ->
                logger.debug("Skal finne grenser som dekker for kommuneflate (hull): ${kommuneFlate.getId()} med ${hullGeometri.size} grensereferanser")
                val dekketAvHull = grenserSomDekkerNyGrense(hullGeometri, grense)
                if (dekketAvHull.isNotEmpty()) {
                    return dekketAvHull

                }
            }
        }
        return emptyList()
    }

    fun grenserSomDekkerNyGrense(skalSjekkesMot: List<SosiGeometri>, grense: SosiGeometri): List<SosiGeometri> {
        val grenseReferanse = grense.getVerdiMedIndex(0)

        var grenserSomDekker: List<SosiGeometri> = finnSammenhengendeGrenserSomDekker(skalSjekkesMot, grense, 1.0)

        if (grenserSomDekker.isEmpty()) {
            logger.debug("Grense: $grenseReferanse, ingen treff med buffer-størrelse 1.0 (antall grenser det sjekkes mot: ${skalSjekkesMot.size}), prøver derfor med buffer-størrelse på 5.0")
            grenserSomDekker = finnSammenhengendeGrenserSomDekker(skalSjekkesMot, grense, 5.0)
            if (grenserSomDekker.isEmpty()) {
                logger.debug("Grense: $grenseReferanse, fortsatt ingen treff etter å ha økt buffer-størrelse til 5.0")
            }
        }

        if (grenseReferanse == null) {
            throw RuntimeException("Grense uten referanse, skal aldri inntreffe")
        }

        //Spesialhåndtering for korte grenser som går mer enn 90 grader bakover og derfor blir matchet av feil grenser
        if (grenseReferanse.contains("14123")) {
            grenserSomDekker = finnKurve(skalSjekkesMot, "602991")
        } else if (grenseReferanse.contains("74309")) {
            grenserSomDekker = finnKurve(skalSjekkesMot, "588599")
        } else if (grenseReferanse.contains("75776")) {
            grenserSomDekker = finnKurve(skalSjekkesMot, "587013")
        } else if (grenseReferanse.contains("386033")) {
            grenserSomDekker = finnKurve(skalSjekkesMot, "500176")
        } else if (grenseReferanse.contains("412276")) {
            grenserSomDekker = finnKurve(skalSjekkesMot, "523828")
        } else if (grenseReferanse.contains("498216")) {
            grenserSomDekker = finnKurve(skalSjekkesMot, "602991")
        } else if (grenseReferanse.contains("393126")) {
            grenserSomDekker = finnKurve(skalSjekkesMot, "506988")
        } else if (grenseReferanse.contains("496993")) {
            grenserSomDekker = finnKurve(skalSjekkesMot, "602007")
        }

        return grenserSomDekker

    }

    private fun finnKurve(
        skalSjekkesMot: List<SosiGeometri>,
        kurveReferanse: String
    ): List<SosiGeometri> {
        val element = skalSjekkesMot.find { it.getVerdiMedIndex(0)?.contains(kurveReferanse) ?: false }
        return if (element == null) emptyList() else listOf(element)
    }

    private fun finnSammenhengendeGrenserSomDekker(
        skalSjekkesMot: List<SosiGeometri>,
        grense: SosiGeometri,
        bufferStr: Double
    ): List<SosiGeometri> {
        val nyGrense = grense.asLineString()

        val initialWindowSize = 1
        var maxWindowSize = min(30, skalSjekkesMot.size)
        val grenseReferanse = grense.getVerdiMedIndex(0)
        if (grenseReferanse != null && (grenseReferanse.contains("13897") || grenseReferanse.contains("379378"))) {
            logger.info("Grense $grenseReferanse må ha et større vindu for å å finne en match, setter det til 50.")
            maxWindowSize = 50
        }
        var currentWindowSize = initialWindowSize

        while (currentWindowSize <= maxWindowSize) {
            val windows: List<List<SosiGeometri>> = skalSjekkesMot.overlappedWindow(currentWindowSize)
            val grenserSomDekker: List<SosiGeometri>? = windows.find { grenserIVindu ->
                val grensesegmenter = grenserIVindu.fold(emptyList<LineString>()) { list, element ->
                    val grenseSegmentMedRiktigRetning = when (element.erRetningMedKlokken) {
                        true -> element.asLineString()
                        false -> element.asLineString().reverse()
                    }
                    list.plus(grenseSegmentMedRiktigRetning)
                }
                val segment = SosiGeometryFunctions.getGeometryFactory().createMultiLineString(grensesegmenter.toTypedArray())

                SosiGeometryFunctions.dekker(segment, nyGrense, bufferStr)
            }

            if (grenserSomDekker != null) {
                logger.debug("Grense $grenseReferanse dekkes av ${grenserSomDekker.size} grense(r)")
                grenserSomDekker.forEachIndexed {index, grenseIntern ->  logger.debug("\tGrense ($index) som dekker: ${grenseIntern.getVerdiMedIndex(0)}, retning med klokken: ${grense.erRetningMedKlokken}") }
                return grenserSomDekker
            }

            if (currentWindowSize >= 10 && currentWindowSize % 10 == 0) {
                logger.debug("Current window size: $currentWindowSize")
            }

            currentWindowSize += 1
        }

        return emptyList()
    }

    fun splittGrenser(ref: SosiReferanse, grense: SosiGeometri, grenserSomDekker: List<SosiGeometri>): List<SplitTilImport> {
        val startCoord = grense.startKoordinat(grense.erRetningMedKlokken)
        val sluttCoord = grense.sluttKoordinat(grense.erRetningMedKlokken)
        val referanse = ref.getVerdiUtenParentes()

        val splittetGrense = when (grenserSomDekker.size == 1) {
            true -> listOf(splittGrense(referanse, grenserSomDekker.first(), startCoord, sluttCoord)).map { SplitTilImport(it, ref, grense) }
            false -> {
                val grenseMedStartKoordinat = finnGrenseSegmentMedKoordinat(startCoord, grenserSomDekker) ?: grenserSomDekker.first()
                val grenseMedSluttKoordinat = finnGrenseSegmentMedKoordinat(sluttCoord, grenserSomDekker) ?: grenserSomDekker.last()
                if (grenserSomDekker.size >= 2 && grenseMedStartKoordinat == grenseMedSluttKoordinat) {
                    logger.warn("Samme grense dekker både start og slutt, men det er oppgitt at grensa skal dekkes av flere grenser")
                }
                val grenserSomSkalVareMedISinHelhet = grenserSomDekker
                    .minus(grenseMedStartKoordinat)
                    .minus(grenseMedSluttKoordinat)
                    .map { SplitRef(it.asLineString(), it, it.erRetningMedKlokken) }
                val startSplittet: SplitRef = splittGrense(referanse, grenseMedStartKoordinat, startCoord = startCoord)
                val sluttSplittet = splittGrense(referanse, grenseMedSluttKoordinat, startCoord = null, endCoord = sluttCoord)

                listOf(startSplittet).plus(grenserSomSkalVareMedISinHelhet).plus(sluttSplittet).map { SplitTilImport(it, ref, grense) }
            }
        }
        return splittetGrense
    }

    fun splittGrense(referanse: String, grenseSomSkalSplittes: SosiGeometri, startCoord: Coordinate?, endCoord: Coordinate? = null): SplitRef {
        return when (maSplittes(grenseSomSkalSplittes, startCoord, endCoord)) {
            true -> {
                val toSplit = grenseSomSkalSplittes.asLineString()

                val start = startCoord ?: grenseSomSkalSplittes.startKoordinat(grenseSomSkalSplittes.erRetningMedKlokken)
                val end = endCoord ?: grenseSomSkalSplittes.sluttKoordinat(grenseSomSkalSplittes.erRetningMedKlokken)

                // NearestPoints returnerer de to punktene som er nærmest hverandre på hhv linja og punktet det skal sjekkes mot
                val distanceOpStartPointToLine = DistanceOp(toSplit, SosiGeometryFunctions.getGeometryFactory().createPoint(start))
                val nearestActualStartOnLine = distanceOpStartPointToLine.nearestPoints().first()

                val distanceOpEndPointToLine = DistanceOp(toSplit, SosiGeometryFunctions.getGeometryFactory().createPoint(end))
                val nearestActualEndOnLine: Coordinate = distanceOpEndPointToLine.nearestPoints().first()

                val indexedLineToSplit = LengthIndexedLine(toSplit)
                val splitPointStartIndex = indexedLineToSplit.indexOf(nearestActualStartOnLine)
                val splitPointEndIndex = indexedLineToSplit.indexOf(nearestActualEndOnLine)
                val startLoc = LengthLocationMap.getLocation(toSplit, splitPointStartIndex)
                val endLoc = LengthLocationMap.getLocation(toSplit, splitPointEndIndex)

                // Hvis end er før start, reverseres output av extractLine under.
                // Derfor snur vi koordinatene for å beholde korrekt retning
                val (splitStart, splitEnd) = when (endLoc < startLoc) {
                    true -> Pair(splitPointEndIndex, splitPointStartIndex)
                    false -> Pair(splitPointStartIndex, splitPointEndIndex)
                }

                val theSplitLine = indexedLineToSplit.extractLine(splitStart, splitEnd) as LineString

                // Tar vare på informasjon om punkter som flyttes. En grensejustering kan føre til at gamle knutepunkt splittes opp og havner på forskjellige steder.
                // Vi vil derfor måtte ha en liste med flyttede koordinater tilknyttet ethvert koordinat som har blitt flyttet
                // Ved å tillegg ta med seg informasjon om nærmeste knutepunkt kan denne informasjonen bygges opp igjen senere når vi har gjennomført en komplett grensematching
                if (nearestActualStartOnLine != start) {
                    logger.debug("Kurve $referanse, start-koordinat har blitt flyttet fra $start til $nearestActualStartOnLine, avstand: ${distanceOpStartPointToLine.distance()}")
                    val flyttetTilKoordinaterForStartpunkt: List<FlyttetTilKoordinat> = koordinaterSomHarBlittFlyttet.getOrDefault(start, emptyList())
                    val naermesteKnutepunkt = finnNaermesteKnutepunkt(toSplit, nearestActualStartOnLine)

                    val flyttetStartKoordinat = when (grenseSomSkalSplittes.erRetningMedKlokken) {
                        true -> theSplitLine.startPoint.coordinate
                        false -> theSplitLine.endPoint.coordinate
                    }

                    //Legger til informasjon om (ny) flytting av koordinat inkludert info om nærmeste knutepunkt
                    koordinaterSomHarBlittFlyttet[start] = flyttetTilKoordinaterForStartpunkt.plus(FlyttetTilKoordinat(flyttetStartKoordinat, naermesteKnutepunkt.first, naermesteKnutepunkt.second, grenseSomSkalSplittes))
                }

                if (nearestActualEndOnLine != end) {
                    logger.debug("Kurve $referanse, slutt-koordinat har blitt flyttet fra $end til $nearestActualEndOnLine, avstand: ${distanceOpEndPointToLine.distance()}")
                    val flyttetTilKoordinaterForSluttpunkt = koordinaterSomHarBlittFlyttet.getOrDefault(end, mutableSetOf())
                    val naermesteKnutepunkt = finnNaermesteKnutepunkt(toSplit, nearestActualEndOnLine)

                    val flyttetSluttKoordinat = when (grenseSomSkalSplittes.erRetningMedKlokken) {
                        true -> theSplitLine.endPoint.coordinate
                        false -> theSplitLine.startPoint.coordinate
                    }

                    //Legger til informasjon om (ny) flytting av koordinat inkludert info om nærmeste knutepunkt
                    koordinaterSomHarBlittFlyttet[end] = flyttetTilKoordinaterForSluttpunkt.plus(FlyttetTilKoordinat(flyttetSluttKoordinat, naermesteKnutepunkt.first, naermesteKnutepunkt.second, grenseSomSkalSplittes))
                }

                if (referanse.contains("74080") || referanse.contains("518325")) {
                    return fiksSelfIntersectingGrense(theSplitLine, grenseSomSkalSplittes)
                }  else {
                    return SplitRef(theSplitLine, grenseSomSkalSplittes, grenseSomSkalSplittes.erRetningMedKlokken)
                }

            }
            false -> SplitRef(grenseSomSkalSplittes.asLineString(), grenseSomSkalSplittes, grenseSomSkalSplittes.erRetningMedKlokken)
        }
    }

    private fun fiksSelfIntersectingGrense(
        theSplitLine: LineString,
        grenseSomSkalSplittes: SosiGeometri
    ): SplitRef {
        val koordinater: MutableList<Coordinate> = theSplitLine.coordinateSequence.toCoordinateArray().toMutableList()
        if (koordinater[1].x == 283594.34 && koordinater[1].y == 7087087.25) {
            logger.info("Grense 74080 og 518325 ender opp med en self-intersection etter splitting. Må derfor fjerne et koordinat i den splitta grensa")
            koordinater.removeAt(1)
        }
        val splitLineForSelfIntersectingBorder = SosiGeometryFunctions.getGeometryFactory().createLineString(koordinater.toTypedArray())
        return SplitRef(splitLineForSelfIntersectingBorder, grenseSomSkalSplittes, grenseSomSkalSplittes.erRetningMedKlokken)
    }

    private fun reverserteGrenser(grenseReferanse: String, motsattGrenseReferanse: String): List<SplitTilImport> {
        logger.debug("Skal importere grense $grenseReferanse. Den er allerede importert via $motsattGrenseReferanse, legger inn et innslag med reversert liste og snudd retning på alle grensestykker")
        val splitTilImports = matchedeGrenser[motsattGrenseReferanse]

        return splitTilImports!!.reversed().map {
            SplitTilImport(SplitRef(it.splitRef.splittetGrense, it.splitRef.basertPaGrense, !it.splitRef.opprinneligRetningMedKlokken), it.originalRef, it.originalGrense)
        }
    }
    private fun erNyGrensereferanse(grenseReferanse: String): Boolean =
        !matchedeGrenser.containsKey(grenseReferanse) && !matchedeGrenser.containsKey(motsattGrensereferanse(grenseReferanse))
    private fun motsattGrensereferanse(grenseReferanse: String) = when (grenseReferanse.startsWith("-")) {
        true -> grenseReferanse.removePrefix("-")
        false -> "-$grenseReferanse"
    }

    fun finnErstatningskoordinater(): MutableMap<Coordinate, Erstatningskoordinat> {
        val erstatningskoordinater = mutableMapOf<Coordinate, Erstatningskoordinat>()

        koordinaterSomHarBlittFlyttet.forEach { (opprinneligKoordinat, koordinaterDenHarBlittFlyttetTil) ->
            val alleBeregnedePunkterErLike = koordinaterDenHarBlittFlyttetTil.all { koordinaterDenHarBlittFlyttetTil[0].beregnetKoordinatPaaSplitetGrense == it.beregnetKoordinatPaaSplitetGrense }
            if (alleBeregnedePunkterErLike) {
                logger.debug("Like beregnede punkter: Fant punkt ($opprinneligKoordinat) som har blitt flyttet (${koordinaterDenHarBlittFlyttetTil.size}) ganger, alle flyttingene går til samme beregnde punkt: ${koordinaterDenHarBlittFlyttetTil[0].beregnetKoordinatPaaSplitetGrense}")
                erstatningskoordinater[opprinneligKoordinat] = Erstatningskoordinat(nyttKoordinat = koordinaterDenHarBlittFlyttetTil[0].beregnetKoordinatPaaSplitetGrense)
            } else {
                val alleKnutepunkterErLike = koordinaterDenHarBlittFlyttetTil.all { koordinaterDenHarBlittFlyttetTil[0].naermesteKnutepunktISplittetGrense == it.naermesteKnutepunktISplittetGrense && it.avstandTilNaermesteKnutepunkt <= 5.0 }

                if (alleKnutepunkterErLike) {
                    logger.debug("Like knutepunkter: Fant punkt ($opprinneligKoordinat) som har blitt flyttet (${koordinaterDenHarBlittFlyttetTil.size}) ganger, alle flyttingene kommer fra samme knutepunkt: ${koordinaterDenHarBlittFlyttetTil[0].naermesteKnutepunktISplittetGrense}")
                    erstatningskoordinater[opprinneligKoordinat] = Erstatningskoordinat(true, koordinaterDenHarBlittFlyttetTil[0].naermesteKnutepunktISplittetGrense, koordinaterDenHarBlittFlyttetTil.map { it.beregnetKoordinatPaaSplitetGrense })
                } else {
                    logger.warn("IKKE Like knutepunker: Fant punkt ($opprinneligKoordinat) som har blitt flyttet (${koordinaterDenHarBlittFlyttetTil.size}) ganger, stammer ikke fra samme knutepunkt")
                }
            }

        }
        return erstatningskoordinater
    }

    fun finnGrenseSegmentMedKoordinat(koordinat: Coordinate, grenser: List<SosiGeometri>): SosiGeometri? {
        return when (val grenseSegmentMedKoordinat = grenser.find { SosiGeometryFunctions.dekker(it.asLineString(), SosiGeometryFunctions.getGeometryFactory().createPoint(koordinat), 1.0) }) {
            null -> {
                logger.debug("Fant ikke noe match med buffer 1.0, prøver med 5.0")
                val sosiGeometri = grenser.find { SosiGeometryFunctions.dekker(it.asLineString(), SosiGeometryFunctions.getGeometryFactory().createPoint(koordinat), 5.0) }
                if (sosiGeometri != null) {
                    logger.debug("Fant match med buffer 5.0")
                }
                sosiGeometri
            }
            else -> grenseSegmentMedKoordinat
        }
    }

    private fun finnNaermesteKnutepunkt(grenseSomSkalSplittes: LineString, naermesteKoordinat: Coordinate): Pair<Coordinate, Double> {
        val naermestePunkt = SosiGeometryFunctions.getGeometryFactory().createPoint(naermesteKoordinat)
        val startpunkt = grenseSomSkalSplittes.startPoint
        val avstandTilStartpunkt = DistanceOp(startpunkt, naermestePunkt).distance()
        val sluttpunkt = grenseSomSkalSplittes.endPoint
        val avstandTilSluttpunkt = DistanceOp(sluttpunkt, naermestePunkt).distance()

        return if(avstandTilStartpunkt < avstandTilSluttpunkt) {
            Pair(startpunkt.coordinate, avstandTilStartpunkt)
        } else {
            Pair(sluttpunkt.coordinate, avstandTilSluttpunkt)
        }
    }

    fun maSplittes(grense: SosiGeometri, start: Coordinate?, slutt: Coordinate? = null): Boolean {
        val koordinater = when (grense.erRetningMedKlokken) {
            true -> grense.koordinater()
            false -> grense.koordinater().reversed()
        }
        val koordinaterMatcher = when {
            start != null && slutt != null -> koordinater.first().equals2D(start) && koordinater.last().equals2D(slutt)
            start != null -> koordinater.first().equals2D(start)
            slutt != null -> koordinater.last().equals2D(slutt)
            else -> throw GrenseMatchingException(grense.asLineString(), "Trenger minimum ett koordinat for å kunne splitte grenser")
        }

        return !koordinaterMatcher
    }

}

class Erstatningskoordinat(
    val skalBrukeKnutepunkt: Boolean = false,
    val nyttKoordinat: Coordinate,
    var koordinaterSomErstattes: List<Coordinate> = emptyList()
)

class FlyttetTilKoordinat(
    val beregnetKoordinatPaaSplitetGrense: Coordinate,
    val naermesteKnutepunktISplittetGrense: Coordinate,
    val avstandTilNaermesteKnutepunkt: Double,
    val grenseSomFlytterKoordinatet: SosiGeometri
)


data class SplitRef(
    val splittetGrense: LineString,
    val basertPaGrense: SosiGeometri,

    //Denne må settes eksplisitt. Kan ikke bruke retningen fra basertPaGrense ettersom denne kan endres når man laster inn referanser på nytt for en annen kommune som refererer til samme grense
    val opprinneligRetningMedKlokken: Boolean
)
data class SplitTilImport(val splitRef: SplitRef, val originalRef: SosiReferanse, val originalGrense: SosiGeometri) {
    fun lokalId(): String {
        val lokalIdSplitTarget = splitRef.basertPaGrense.getIdent()?.getLokalId()!!
        val lokalIdOrigGrense = originalGrense.getIdent()?.getLokalId()!!
        return generateDeterministicUUIDFromSeed("${lokalIdSplitTarget}-${lokalIdOrigGrense}")
    }

    private fun generateDeterministicUUIDFromSeed(seed: String): String {
        val importTypePrefix = if (SosiImportService.importType == null) {
            "admEnhet"
        } else {
            SosiImportService.importType!!.name
        }
        return UUID.nameUUIDFromBytes((importTypePrefix + seed).toByteArray()).toString()
    }
}

data class GrenseMatchingException(val geometri: Geometry, override val message: String, override val cause: Throwable? = null): RuntimeException(message, cause)
