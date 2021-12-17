package no.kartverket.nibas.service

import no.kartverket.nibas.api.v1.common.AdministrativEnhetType
import no.kartverket.nibas.api.v1.common.geojson.Feature
import no.kartverket.nibas.api.v1.common.geojson.FeatureProperties
import no.kartverket.nibas.api.v1.request.AdministrativEnhet
import no.kartverket.nibas.api.v1.request.AdministrativEnhetNavn as AdministrativEnhetNavnRequest
import no.kartverket.nibas.api.v1.response.AdministrativEnhetNavn as AdministrativEnhetNavnResponse
import no.kartverket.nibas.api.v1.response.AdministrativEnhet as AdministrativEnhetResponse
import no.kartverket.nibas.domain.Fylke
import no.kartverket.nibas.domain.Fylkesgrense
import no.kartverket.nibas.domain.Kommune
import no.kartverket.nibas.domain.Kommunegrense
import no.kartverket.nibas.kodelister.KodelisteService
import no.kartverket.nibas.repository.*
import org.junit.jupiter.api.Test
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import java.time.LocalDate
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
    lateinit var kodelisteService: KodelisteService

//    @Test
//    fun thatOppdaterKommunegrenserOppdatererLikeGrenserKunEnGang() {
//        val gammelGrenseForRingerikeOgHole = GeometryFactory().createLineString(
//            arrayOf(
//                Coordinate(999.0, 999.0),
//                Coordinate(1099.0, 1099.0)
//            )
//        )
//        val gammelGrenseKunForHole = GeometryFactory().createLineString(
//            arrayOf(
//                Coordinate(1099.0, 1099.0),
//                Coordinate(1199.0, 1199.0)
//            )
//        )
//
//        val nyGrenseForRingerikeOgHole = GeometryFactory().createLineString(
//            arrayOf(
//                Coordinate(1000.0, 1000.0),
//                Coordinate(1100.0, 1100.0)
//            )
//        )
//        val nyGrenseKunForHole = GeometryFactory().createLineString(
//            arrayOf(
//                Coordinate(1100.0, 1100.0),
//                Coordinate(1200.0, 1200.0)
//            )
//        )
//        val nyeFeatures = mutableListOf(
//            Feature(
//                properties = FeatureProperties(
//                    1,
//                    "Grense1",
//                    AdministrativEnhetType.KOMMUNE,
//                    AdministrativEnhetResponse(
//                        AdministrativEnhetType.KOMMUNE,
//                        1,
//                        listOf(AdministrativEnhetNavnResponse("Ringerike", "nor"))
//                    )),
//                geometry = nyGrenseForRingerikeOgHole
//            ),
//            Feature(
//                properties = FeatureProperties(
//                    1,
//                    "Grense1",
//                    AdministrativEnhetType.KOMMUNE,
//                    AdministrativEnhetResponse(
//                        AdministrativEnhetType.KOMMUNE,
//                        2,
//                        listOf(AdministrativEnhetNavnResponse("Hole", "nor"))
//                    )),
//                geometry = nyGrenseForRingerikeOgHole
//            ),
//            Feature(
//                properties = FeatureProperties(
//                    2,
//                    "Grense2",
//                    AdministrativEnhetType.KOMMUNE,
//                    AdministrativEnhetResponse(
//                        AdministrativEnhetType.KOMMUNE,
//                        2,
//                        listOf(AdministrativEnhetNavnResponse("Hole", "nor"))
//                    )),
//                geometry = nyGrenseKunForHole
//            )
//        )
//
//        val gammelKommunegrense1 = Kommunegrense()
//        gammelKommunegrense1.id = 1
//        gammelKommunegrense1.grense = gammelGrenseForRingerikeOgHole
//
//        val gammelKommunegrense2 = Kommunegrense()
//        gammelKommunegrense2.id = 2
//        gammelKommunegrense2.grense = gammelGrenseKunForHole
//
//        val gamleKommunegrenser = setOf(gammelKommunegrense1, gammelKommunegrense2)
//        val forventetKommunegrense1 = Kommunegrense()
//        forventetKommunegrense1.id = gammelKommunegrense1.id
//        forventetKommunegrense1.grense = nyGrenseForRingerikeOgHole
//
//        val forventetKommunegrense2 = Kommunegrense()
//        forventetKommunegrense2.id = gammelKommunegrense2.id
//        forventetKommunegrense2.grense = nyGrenseKunForHole
//
//        Mockito.`when`(kommunegrenseRepository.findAllById(setOf(1,2))).thenReturn(gamleKommunegrenser)
//
//        administrativenhetService.oppdaterGrenser(AdministrativEnhetType.KOMMUNE, nyeFeatures)
//        Mockito.verify(kommunegrenseRepository).saveAll(setOf(forventetKommunegrense1, forventetKommunegrense2))
//    }
//
//    @Test
//    fun thatOppdaterFylkesgrenserOppdatererLikeGrenserKunEnGang() {
//        val gammelGrenseForVikenOgInnlandet = GeometryFactory().createLineString(
//            arrayOf(
//                Coordinate(999.0, 999.0),
//                Coordinate(1099.0, 1099.0)
//            )
//        )
//        val gammelGrenseKunForViken = GeometryFactory().createLineString(
//            arrayOf(
//                Coordinate(1099.0, 1099.0),
//                Coordinate(1199.0, 1199.0)
//            )
//        )
//
//        val nyGrenseForVikenOgInnlandet = GeometryFactory().createLineString(
//            arrayOf(
//                Coordinate(1000.0, 1000.0),
//                Coordinate(1100.0, 1100.0)
//            )
//        )
//        val nyGrenseKunForViken = GeometryFactory().createLineString(
//            arrayOf(
//                Coordinate(1100.0, 1100.0),
//                Coordinate(1200.0, 1200.0)
//            )
//        )
//        val nyeFeatures = mutableListOf(
//            Feature(
//                properties = FeatureProperties(
//                    1,
//                    "Grense1",
//                    AdministrativEnhetType.FYLKE,
//                    AdministrativEnhetResponse(
//                        AdministrativEnhetType.FYLKE,
//                        1,
//                        listOf(AdministrativEnhetNavnResponse("Innlandet", "nor"))
//                    )),
//                geometry = nyGrenseForVikenOgInnlandet
//            ),
//            Feature(
//                properties = FeatureProperties(
//                    1,
//                    "Grense1",
//                    AdministrativEnhetType.FYLKE,
//                    AdministrativEnhetResponse(
//                        AdministrativEnhetType.FYLKE,
//                        1,
//                        listOf(AdministrativEnhetNavnResponse("Viken", "nor"))
//                    )),
//                geometry = nyGrenseForVikenOgInnlandet
//            ),
//            Feature(
//                properties = FeatureProperties(
//                    2,
//                    "Grense2",
//                    AdministrativEnhetType.FYLKE,
//                    AdministrativEnhetResponse(
//                        AdministrativEnhetType.FYLKE,
//                        1,
//                        listOf(AdministrativEnhetNavnResponse("Viken", "nor"))
//                    )),
//                geometry = nyGrenseKunForViken
//            )
//        )
//
//        val gammelFylkesgrense1 = Fylkesgrense()
//        gammelFylkesgrense1.id = 1
//        gammelFylkesgrense1.grense = gammelGrenseForVikenOgInnlandet
//
//        val gammelFylkesgrense2 = Fylkesgrense()
//        gammelFylkesgrense2.id = 2
//        gammelFylkesgrense2.grense = gammelGrenseKunForViken
//
//        val gamleFylkesgrenser = setOf(gammelFylkesgrense1, gammelFylkesgrense2)
//        val forventetFylkesgrense1 = Fylkesgrense()
//        forventetFylkesgrense1.id = gammelFylkesgrense1.id
//        forventetFylkesgrense1.grense = nyGrenseForVikenOgInnlandet
//
//        val forventetFylkesgrense2 = Fylkesgrense()
//        forventetFylkesgrense2.id = gammelFylkesgrense2.id
//        forventetFylkesgrense2.grense = nyGrenseKunForViken
//
//        Mockito.`when`(fylkesgrenseRepository.findAllById(setOf(1,2))).thenReturn(gamleFylkesgrenser)
//
//        administrativenhetService.oppdaterGrenser(AdministrativEnhetType.FYLKE, nyeFeatures)
//        Mockito.verify(fylkesgrenseRepository).saveAll(setOf(forventetFylkesgrense1, forventetFylkesgrense2))
//    }
//
//    @Test
//    fun thatOppdaterKommuneOppdateresKorrekt() {
//        val kommuneIdSomSkalOppdateres = 1
//
//        val administrativEnhetRequest = AdministrativEnhet(
//            AdministrativEnhetType.KOMMUNE,
//            listOf(AdministrativEnhetNavnRequest("Ringerike - ny", "nor")),
//            "3008")
//        val kommuneFraDB = Kommune()
//        kommuneFraDB.id = kommuneIdSomSkalOppdateres
//        kommuneFraDB.navn = "Ringerike"
//
//        val forventetAdministrativEnhetTilLagring = Kommune()
//        forventetAdministrativEnhetTilLagring.id = kommuneIdSomSkalOppdateres
//        forventetAdministrativEnhetTilLagring.navn = "Ringerike - ny"
//
//        Mockito.`when`(kommuneRepository.findById(kommuneIdSomSkalOppdateres)).thenReturn(Optional.of(kommuneFraDB))
//        Mockito.`when`(kommuneRepository.save(ArgumentMatchers.any())).thenReturn(forventetAdministrativEnhetTilLagring)
//
//        administrativenhetService.oppdaterAdministrativEnhet(kommuneIdSomSkalOppdateres, administrativEnhetRequest)
//        Mockito.verify(kommuneRepository).save(forventetAdministrativEnhetTilLagring)
//    }
//
//    @Test
//    fun thatOppdaterFylkeOppdateresKorrekt() {
//        val fylkeIdSomSkalOppdateres = 1
//
//        val administrativEnhetRequest = AdministrativEnhet(
//            AdministrativEnhetType.FYLKE,
//            listOf(AdministrativEnhetNavnRequest("Viken - ny", "nor")),
//            "34")
//
//        val fylkeFraDB = Fylke()
//        fylkeFraDB.id = fylkeIdSomSkalOppdateres
//        fylkeFraDB.navn = "Viken"
//
//        val forventetAdministrativEnhetTilLagring = Fylke()
//        forventetAdministrativEnhetTilLagring.id = fylkeIdSomSkalOppdateres
//        forventetAdministrativEnhetTilLagring.navn = "Viken - ny"
//
//        Mockito.`when`(fylkeRepository.findById(1)).thenReturn(Optional.of(fylkeFraDB))
//        Mockito.`when`(fylkeRepository.save(ArgumentMatchers.any())).thenReturn(forventetAdministrativEnhetTilLagring)
//
//        administrativenhetService.oppdaterAdministrativEnhet(1, administrativEnhetRequest)
//        Mockito.verify(fylkeRepository).save(forventetAdministrativEnhetTilLagring)
//    }

}