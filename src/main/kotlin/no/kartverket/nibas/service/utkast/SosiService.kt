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
import no.kartverket.nibas.common.geometry.getGeometryFactory
import no.kartverket.nibas.extensions.geometry.geometryToDto
import no.kartverket.nibas.kodelister.KodelisteService
import no.kartverket.nibas.sosi.model.SosiKurve
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

    // OBS! Støtter kun å hente ut linestrings (kurver) fra sosiobjektet. (TS-2004)
    fun getGeoJsonForSosiObjekt(sosiObjekt: SosiObjekt, inProjection: String): FeatureCollection {
        val sosiVersjonString = sosiObjekt.getHode()?.getSosiVersjon() ?: throw RuntimeException("SosiVersjon ble ikke funnet i filen")
        val sosiVersjon = parseSosiVersjon(sosiVersjonString)
        val illegalSOSIversionMessage = "Du kan kun laste opp sosifiler med versjon 4.5 eller 5.0, ikke $sosiVersjon"
        if(sosiVersjon != "4.5" && sosiVersjon != "5.0") throw IllegalArgumentException(illegalSOSIversionMessage)
        val features = mutableListOf<Feature>()

        val koordsys = getEPSGCodeForSosiKode(sosiObjekt.getKurver()[0].koordsys)
        val geometryFactory = getGeometryFactory(koordsys = koordsys)
        val crsFactory = CRSFactory()
        val transformFactory = CoordinateTransformFactory()

        val sourceCRS = crsFactory.createFromName(koordsys)
        val targetCRS = crsFactory.createFromName(inProjection)
        val transformer = transformFactory.createTransform(sourceCRS, targetCRS)

        for (kurve in sosiObjekt.getKurver()) {
            val lineString = convertSosiKurveToLineString(
                sosiKurve = kurve, geometryFactory = geometryFactory, sourceCRS = sourceCRS, targetCRS = targetCRS, transformer = transformer
            )
            val grenseTypeString = getGrenseTypeForKurve(kurve = kurve, sosiVersjon = sosiVersjon) ?: throw IllegalArgumentException(illegalSOSIversionMessage)
            val grenseType = try {
                Grensetype.fromValue(grenseTypeString)
            } catch(e: IllegalArgumentException){
                throw IllegalArgumentException("Filen inneholder en eller flere grenser med grensetype som ikke støttes: '$grenseTypeString'")
            }
            val id = kurve.getIdent()?.getVerdiAvBarn("LOKALID") ?: kurve.getId()
            val posisjonskvalitet = kurve.getPosisjonskvalitet(kodelisteService = kodelisteService, sosiVersjon = sosiVersjon)?.toKvalitet()

            if (lineString != null && id != null) {
                val feature = Feature(
                    id = id,
                    properties = FeatureProperties(
                        type = grenseType.toString(),
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
    
    fun getGrenseTypeForKurve(kurve: SosiKurve, sosiVersjon: String): String? {
        return when (sosiVersjon) {
            "4.5" -> kurve.getObjType()
            "5.0" -> kurve.getAvgrensingstype()
            else -> null
        }
    }

    fun convertSosiKurveToLineString(
        sosiKurve: SosiKurve,
        geometryFactory: GeometryFactory,
        sourceCRS: CoordinateReferenceSystem,
        targetCRS: CoordinateReferenceSystem,
        transformer: CoordinateTransform,
    ): LineString? {
        val coordinates = sosiKurve.getAlleSosiNordOest().flatMap { nordOest ->
            nordOest.getKoordinater().map { koordinat ->
                if (sourceCRS == targetCRS) {
                    koordinat.toCoordinate(nordOest.enhet)
                } else {
                    val original = koordinat.toCoordinate(nordOest.enhet)
                    val src = ProjCoordinate(original.x, original.y)
                    val transformedCoord = ProjCoordinate()
                    transformer.transform(src, transformedCoord)
                    Coordinate(transformedCoord.x, transformedCoord.y)
                }

            }
        }

        return if (coordinates.isEmpty()) {
            null
        } else {
            geometryFactory.createLineString(coordinates.toTypedArray())
        }
    }
}
