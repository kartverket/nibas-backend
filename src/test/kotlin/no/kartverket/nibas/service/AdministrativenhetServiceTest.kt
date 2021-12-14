package no.kartverket.nibas.service

import no.kartverket.nibas.api.v1.common.AdministrativEnhetType
import no.kartverket.nibas.api.v1.common.geojson.Feature
import no.kartverket.nibas.api.v1.common.geojson.FeatureProperties
import no.kartverket.nibas.api.v1.request.AdministrativEnhet
import no.kartverket.nibas.api.v1.response.AdministrativEnhetNavn
import no.kartverket.nibas.api.v1.response.AdministrativEnhet as AdministrativEnhetResponse
import no.kartverket.nibas.domain.Fylke
import no.kartverket.nibas.domain.Kommune
import no.kartverket.nibas.domain.Kommunegrense
import no.kartverket.nibas.repository.*
import org.junit.jupiter.api.Test
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import java.util.*

@SpringBootTest(classes = [AdministrativEnhetService::class])
class AdministrativEnhetServiceTest {

    @Autowired lateinit var administrativenhetService: AdministrativEnhetService

    @MockBean
    private lateinit var kommunegrenseRepository: KommunegrenseRepository

    @MockBean
    private lateinit var kommuneRepository: KommuneRepository

    @MockBean
    private lateinit var fylkeRepository: FylkeRepository

    @MockBean
    private lateinit var nasjonRepository: NasjonRepository

    @MockBean
    private lateinit var fylkesgrenseRepository: FylkesgrenseRepository

    @MockBean
    lateinit var riksgrenseRepository: RiksgrenseRepository

    @MockBean
    lateinit var spraakkodeRepository: SpraakkodeRepository

    @MockBean
    lateinit var kommunenummerRepository: KommunenummerRepository

    @MockBean
    lateinit var fylkesnummerRepository: FylkesnummerRepository

//    @Test
//    fun thatOppdaterKommunegrenserOppdatererLikeGrenserKunEnGang() {
//        val koordinater = arrayOf(Coordinate(1000.0, 1000.0), Coordinate(1100.0, 1100.0) )
//        val koordinater2 = arrayOf(Coordinate(1100.0, 1100.0), Coordinate(1200.0, 1200.0) )
//        val grenseString = GeometryFactory().createLineString(koordinater)
//        val grenseString2 = GeometryFactory().createLineString(koordinater2)
//        val nyeFeatures = mutableListOf(
//            Feature(
//                properties = FeatureProperties(
//                    1,
//                    "Grense1",
//                    AdministrativEnhetType.KOMMUNE,
//                    AdministrativEnhetResponse(
//                        AdministrativEnhetType.KOMMUNE,
//                        1,
//                        listOf(AdministrativEnhetNavn("Ringerike", "nor"))
//                    )),
//                geometry = grenseString
//            ),
//            Feature(
//                properties = FeatureProperties(
//                    1,
//                    "Grense1",
//                    AdministrativEnhetType.KOMMUNE,
//                    AdministrativEnhetResponse(
//                        AdministrativEnhetType.KOMMUNE,
//                        1,
//                        listOf(AdministrativEnhetNavn("Hole", "nor"))
//                    )),
//                geometry = grenseString
//            ),
//            Feature(
//                properties = FeatureProperties(
//                    2,
//                    "Grense2",
//                    AdministrativEnhetType.KOMMUNE,
//                    AdministrativEnhetResponse(
//                        AdministrativEnhetType.KOMMUNE,
//                        1,
//                        listOf(AdministrativEnhetNavn("Hole", "nor"))
//                    )),
//                geometry = grenseString2
//            )
//        )
//
//        val kommunegrense1 = Kommunegrense()
//        kommunegrense1.id = 1
//
//        val kommunegrense2 = Kommunegrense()
//        kommunegrense2.id = 1
//
//        val nyeKommunegrenser = setOf(kommunegrense1, kommunegrense2)
//
//        administrativenhetService.oppdaterGrenser(AdministrativEnhetType.KOMMUNE, nyeFeatures)
//        Mockito.verify(kommunegrenseRepository).saveAll(nyeKommunegrenser)
//    }

//    @Test
//    fun thatOppdaterKommuneOppdateresKorrekt() {
//        val administrativEnhetRequest = AdministrativEnhet(AdministrativEnhetType.KOMMUNE, "Ringerike - ny", "3008")
//        val forventetAdministrativEnhetTilLagring = Kommune(1, "Ringerike - ny", "3008")
//
//        val kommuneFraDB = Kommune(1, "Ringerike", "3007")
//        Mockito.`when`(kommuneRepository.findById(1)).thenReturn(Optional.of(kommuneFraDB))
//        Mockito.`when`(kommuneRepository.save(ArgumentMatchers.any())).thenReturn(forventetAdministrativEnhetTilLagring)
//
//        administrativenhetService.oppdaterAdministrativEnhet(1, administrativEnhetRequest)
//        Mockito.verify(kommuneRepository).save(forventetAdministrativEnhetTilLagring)
//    }
//
//    @Test
//    fun thatOppdaterFylkeOppdateresKorrekt() {
//        val administrativEnhetRequest = AdministrativEnhet(AdministrativEnhetType.FYLKE, "Viken - ny", "134")
//        val forventetAdministrativEnhetTilLagring = Fylke(1, "Viken - ny", "134")
//
//        val fylkeFraDB = Fylke(1, "Viken", "34")
//        Mockito.`when`(fylkesRepository.findById(1)).thenReturn(Optional.of(fylkeFraDB))
//        Mockito.`when`(fylkesRepository.save(ArgumentMatchers.any())).thenReturn(forventetAdministrativEnhetTilLagring)
//
//        administrativenhetService.oppdaterAdministrativEnhet(1, administrativEnhetRequest)
//        Mockito.verify(fylkesRepository).save(forventetAdministrativEnhetTilLagring)
//    }

}