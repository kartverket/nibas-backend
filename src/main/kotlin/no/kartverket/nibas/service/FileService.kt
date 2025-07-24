package no.kartverket.nibas.service

import no.kartverket.nibas.api.v1.common.geojson.FeatureCollection
import no.kartverket.nibas.service.utkast.SosiGeometryService
import no.kartverket.nibas.sosi.deserializer.NibasSosiDeserializer
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.BufferedInputStream

@Service
class FileService(
    val nibasSosiDeserializer: NibasSosiDeserializer,
    val sosiGeometryService: SosiGeometryService
) {

    fun convertSOSIFileToGeoJSON(sosiFile: MultipartFile, inProjection: String): FeatureCollection {
            return sosiGeometryService.getGeoJsonForSosiObjekt(sosiObjekt = nibasSosiDeserializer.deserialize(
                stream = BufferedInputStream(sosiFile.inputStream),
                filnavn = sosiFile.name
            ), inProjection = inProjection)
    }


}
