package no.kartverket.nibas.controller.klient

import no.kartverket.nibas.api.v1.common.geojson.FeatureCollection
import no.kartverket.nibas.api.v1.klient.KartlagApi
import no.kartverket.nibas.service.FileService
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController("FileController")
class KartlagController(
    private val fileService: FileService,
): KartlagApi {

    override fun convertSOSIToGeoJSON(file: MultipartFile): FeatureCollection {
        val regex = Regex("\\.([^.]+)$")
        val match = regex.find(file.originalFilename.toString())
        val filetype = match?.groupValues?.get(1)?.lowercase()
        if(filetype != "sos") throw IllegalArgumentException("Kun filer av typen .sos støttes. $filetype støttes ikke.")
        return fileService.convertSOSIFileToGeoJSON(file, "EPSG:25833")
    }
}
