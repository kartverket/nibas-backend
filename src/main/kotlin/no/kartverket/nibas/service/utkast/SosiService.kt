package no.kartverket.nibas.service.utkast

import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.api.v1.common.geojson.Feature
import no.kartverket.nibas.api.v1.common.geojson.FeatureCollection
import no.kartverket.nibas.api.v1.common.geojson.FeatureProperties
import no.kartverket.nibas.api.v1.common.geojson.metadata.CommonGrenseMetadata
import no.kartverket.nibas.api.v1.common.geojson.metadata.CommonMetadata
import no.kartverket.nibas.api.v1.common.geojson.metadata.Identifikasjon
import no.kartverket.nibas.api.v1.common.geojson.metadata.Sporingsinformasjon
import no.kartverket.nibas.api.v1.common.geojson.metadata.getMetadataForGrensetype
import no.kartverket.nibas.common.geometry.avrundLineString
import no.kartverket.nibas.common.geometry.getGeometryFactory
import no.kartverket.nibas.extensions.geometry.geometryToDto
import no.kartverket.nibas.kodelister.KodelisteService
import no.kartverket.nibas.sosi.geometry.bueTilKoordinater
import no.kartverket.nibas.sosi.model.SosiBuep
import no.kartverket.nibas.sosi.model.SosiGeometri
import no.kartverket.nibas.sosi.model.SosiObjekt
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.LineString
import org.locationtech.proj4j.CRSFactory
import org.locationtech.proj4j.CoordinateReferenceSystem
import org.locationtech.proj4j.CoordinateTransform
import org.locationtech.proj4j.CoordinateTransformFactory
import org.locationtech.proj4j.ProjCoordinate
import org.springframework.stereotype.Service
import java.util.logging.Logger

@Service
class SosiGeometryService(
    val kodelisteService: KodelisteService
) {
    val logger = Logger.getLogger(this::class.java.name)

    fun getEPSGCodeForSosiKode(sosiKode: String?): String {
        return when (sosiKode) {
            "22" -> "EPSG:25832" // EUREF89 UTM sone 32
            "23" -> "EPSG:25833" // EUREF89 UTM sone 33
            "24" -> "EPSG:25834" // EUREF89 UTM sone 34
            "25" -> "EPSG:25835" // EUREF89 UTM sone 35
            "32" -> "EPSG:25832" // Alternative notation for UTM 32
            "33" -> "EPSG:25833" // Alternative notation for UTM 33
            "34" -> "EPSG:25834" // Alternative notation for UTM 34
            "35" -> "EPSG:25835" // Alternative notation for UTM 35
            "84" -> "EPSG:4326"  // WGS84
            "50" -> "EPSG:4258"  // ETRS89 geografisk
            else -> throw IllegalArgumentException("Ukjent SOSI koordinatsystemkode: '$sosiKode'")
        }
    }

    fun parseSosiVersjon(sosiString: String): String {
        return sosiString.take(3)
    }

    // OBS! Støtter kun å hente ut linestrings (kurver og buer) fra sosiobjektet. (TS-2004)
    fun getGeoJsonForSosiObjekt(sosiObjekt: SosiObjekt, inProjection: String): FeatureCollection {
        val sosiVersjonString = sosiObjekt.getHode()?.getSosiVersjon() ?: throw RuntimeException("SosiVersjon ble ikke funnet i filen")
        val sosiVersjon = parseSosiVersjon(sosiVersjonString)
        val illegalSOSIversionMessage = "Du kan kun laste opp sosifiler med versjon 4.5 eller 5.0, ikke $sosiVersjon"
        val isSupportedSOSIVersjon = listOf("4.5", "5.0").contains(sosiVersjon)
        if (!isSupportedSOSIVersjon) throw IllegalArgumentException(illegalSOSIversionMessage)
        val features = mutableListOf<Feature>()

        val linjer: List<SosiGeometri> = sosiObjekt.getKurver() + sosiObjekt.getBuep()
        val koordsys = getEPSGCodeForSosiKode(linjer.first().koordsys)
        val geometryFactory = getGeometryFactory(koordsys = koordsys)
        val crsFactory = CRSFactory()
        val transformFactory = CoordinateTransformFactory()

        val sourceCRS = crsFactory.createFromName(koordsys)
        val targetCRS = crsFactory.createFromName(inProjection)
        val transformer = transformFactory.createTransform(sourceCRS, targetCRS)

        for (kurve in linjer) {
            // Utkast godtar bare koordinater avrundet til nærmeste halve cm, og reprojisering gir lange desimaler.
            // Avrundingen kan slå sammen nabopunkter på korte buer, så like punkter etter hverandre fjernes.
            val lineString = convertSosiKurveToLineString(
                sosiKurve = kurve, geometryFactory = geometryFactory, sourceCRS = sourceCRS, targetCRS = targetCRS, transformer = transformer
            )?.let { avrundLineString(it.coordinates).fjernLikeNabopunkter() }
                ?.takeIf { it.size >= 2 }
                ?.let { geometryFactory.createLineString(it) }
            val grenseTypeString = getGrenseTypeForKurve(kurve = kurve)
                ?: throw IllegalArgumentException("Filen samsvarer ikke med oppgitt SOSI-versjon. Kontakt Kartverket hvis du mener dette er feil.")
            val formattedGrenseTypeString = grenseTypeString.lowercase().replaceFirstChar { it.uppercase() }
            val grenseType = try {
                Grensetype.fromValue(formattedGrenseTypeString)
            } catch (_: IllegalArgumentException) {
                throw IllegalArgumentException("Filen inneholder en eller flere grenser med grensetype som ikke støttes: '$formattedGrenseTypeString'")
            }
            val id = kurve.getIdent()?.getVerdiAvBarn("LOKALID") ?: kurve.getId()
            val posisjonskvalitet = kurve.getPosisjonskvalitet(kodelisteService = kodelisteService)?.toKvalitet()

            if (lineString != null && id != null) {
                val feature = Feature(
                    id = id,
                    properties = FeatureProperties(
                        type = grenseType.value,
                        srid = lineString.srid,
                        version = 1,
                        metadata = getMetadataForGrensetype(grensetype = grenseType, CommonMetadata(
                            identifikasjon = Identifikasjon(lokalid = id),
                            datafangstdato = kurve.getDatafangstDato(),
                            gyldigFra = kurve.getGyldigFra(),
                            gyldigTil = kurve.getGyldigTil(),
                            informasjon = kurve.getInformasjon(),
                            sporingsinformasjon = Sporingsinformasjon(),
                            opphav = kurve.getOpphav(),
                        ), CommonGrenseMetadata(
                            posisjonskvalitet = posisjonskvalitet
                        ))
                    ),
                    geometry = lineString.geometryToDto()
                )
                features.add(feature)
            }
        }

        return FeatureCollection(features = features)
    }

    // Avgrensningstype er en egenskap for noen grenser som inneholder grensetypen.
    // Hvis kuven ikke har egenskaper på grensen ligger grensetypen på OBJTYPE.
    fun getGrenseTypeForKurve(kurve: SosiGeometri): String? {
        return kurve.getAvgrensingstype() ?: kurve.getObjType()
    }

    fun convertSosiKurveToLineString(
        sosiKurve: SosiGeometri,
        geometryFactory: GeometryFactory,
        sourceCRS: CoordinateReferenceSystem,
        targetCRS: CoordinateReferenceSystem,
        transformer: CoordinateTransform,
    ): LineString? {
        val originale = sosiKurve.getAlleSosiNordOest().flatMap { nordOest ->
            nordOest.getKoordinater().map { koordinat -> koordinat.toCoordinate(nordOest.enhet) }
        }
        // Buen deles opp før reprojisering, siden den er en sirkelbue i filas koordinatsystem
        val koordinater = if (sosiKurve is SosiBuep && originale.size == 3) {
            bueTilKoordinater(originale[0], originale[1], originale[2])
        } else {
            originale
        }
        val coordinates = koordinater.map { original ->
            if (sourceCRS == targetCRS) {
                original
            } else {
                val src = ProjCoordinate(original.x, original.y)
                val transformedCoord = ProjCoordinate()
                transformer.transform(src, transformedCoord)
                Coordinate(transformedCoord.x, transformedCoord.y)
            }
        }

        return if (coordinates.isEmpty()) {
            null
        } else {
            geometryFactory.createLineString(coordinates.toTypedArray())
        }
    }
}

private fun Array<Coordinate>.fjernLikeNabopunkter(): Array<Coordinate> =
    filterIndexed { index, koordinat -> index == 0 || !koordinat.equals2D(this[index - 1]) }.toTypedArray()
