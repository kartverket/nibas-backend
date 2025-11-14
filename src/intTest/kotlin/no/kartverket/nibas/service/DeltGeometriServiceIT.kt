package no.kartverket.nibas.service

import no.kartverket.nibas.api.v1.common.Flatetype
import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.builders.GrenseBuilder
import no.kartverket.nibas.builders.MaalemetodeKodeEnum
import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.domain.Flate
import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.domain.Kommune
import no.kartverket.nibas.events.EventRad
import no.kartverket.nibas.repository.findGyldigKommuneForKommunenummer
import no.kartverket.nibas.testcontainer.TestWithDbContainer
import no.kartverket.nibas.unwrap
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.locationtech.jts.geom.Coordinate
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.Month

@ActiveProfiles("localhost")
@Transactional
class DeltGeometriServiceIT : TestWithDbContainer() {

    private val october1In2022 = LocalDate.of(2022, Month.OCTOBER, 1)

    private val punktA = Coordinate(0.0, 200.0)
    private val punktB = Coordinate(100.0, 200.0)
    private val punktC = Coordinate(200.0, 200.0)
    private val punktD = Coordinate(0.0, 100.0)
    private val punktE = Coordinate(100.0, 100.0)
    private val punktF = Coordinate(200.0, 100.0)
    private val punktG = Coordinate(0.0, 0.0)
    private val punktH = Coordinate(100.0, 0.0)
    private val punktI = Coordinate(200.0, 0.0)
    private val punktJ = Coordinate(300.0, 200.0)
    private val punktK = Coordinate(300.0, 100.0)
    private val punktL = Coordinate(300.0, 0.0)
    private val punktM = Coordinate(400.0, 200.0)
    private val punktO = Coordinate(400.0, 0.0)
    private val punktQ = Coordinate(100.0, -100.0)
    private val punktR = Coordinate(200.0, -100.0)
    private val punktS = Coordinate(300.0, -100.0)
    private val punktT = Coordinate(400.0, -100.0)
    private val punktU = Coordinate(0.0, -200.0)
    private val punktV = Coordinate(100.0, -200.0)
    private val punktW = Coordinate(200.0, -200.0)
    private val punktY = Coordinate(400.0, -200.0)

    /**
     * Lager en kommune som ser slik ut:
     *
     *      A ------ B ------ C ------ J
     *      |        |        |        |
     *      D ------ E ------ F ------ K
     *      |        |        |        |
     *      G ------ H ------ I ------ L
     *
     * Det ytre skallet er en kommunegrense.
     * B - E - H er kun en stemmekretsgrense, hvor punktene B og H ikke finnes på grunnkretsgrensene.
     * C - F - I er kun en stemmekretsgrense, hvor punktene C og I ikke finnes på grunnkretsgrensene.
     * D - E - F - K er kun en grunnkretsgrense.
     *
     * Totalt har denne kommunen da 2 grunnkretser og 3 stemmekretser.
     * Grunnkretsene er horisontale, og stemmekretsene er vertikale
     */
    private fun lagreKommuneMedToGrunnkretserOgTreStemmekretser(medKurvepunktFraStemmekrets: Boolean): List<Flate> {
        val grunnkretsKommunegrenseDABCJK = grenseRepository.save(GrenseBuilder(id = "DABCJK", lokalId = Lokalid("DABCJK"),
            gyldigFra = october1In2022, grensetype = Grensetype.Kommunegrense)
            .withGrenseGeometri(if (medKurvepunktFraStemmekrets) listOf(punktD, punktA, punktB, punktC, punktJ, punktK)
            else listOf(punktD, punktA, punktJ, punktK))
            .withKvalitet(MaalemetodeKodeEnum.TERRENGMAALT, 20).build())
        val grunnkretsKommunegrenseKLIHGD = grenseRepository.save(GrenseBuilder(id = "KLIHGD", lokalId = Lokalid("KLIHGD"),
            gyldigFra = october1In2022, grensetype = Grensetype.Kommunegrense)
            .withGrenseGeometri(if (medKurvepunktFraStemmekrets) listOf(punktK, punktL, punktI, punktH, punktG, punktD)
            else listOf(punktK, punktL, punktG, punktD))
            .withKvalitet(MaalemetodeKodeEnum.TERRENGMAALT, 40).build())
        val grunnkretsgrenseDEFK = grenseRepository.save(GrenseBuilder(id = "DEFK", lokalId = Lokalid("DEFK"),
            gyldigFra = october1In2022, grensetype = Grensetype.Grunnkretsgrense)
            .withGrenseGeometri(listOf(punktD, punktE, punktF, punktK)).build())

        val stemmekretsKommunegrenseDAB = grenseRepository.save(GrenseBuilder(id = "DAB", lokalId = Lokalid("DAB"),
            gyldigFra = october1In2022, grensetype = Grensetype.Kommunegrense)
            .withGrenseGeometri(listOf(punktD, punktA, punktB)).build())
        val stemmekretsKommunegrenseBC = grenseRepository.save(GrenseBuilder(id = "BC", lokalId = Lokalid("BC"),
            gyldigFra = october1In2022, grensetype = Grensetype.Kommunegrense)
            .withGrenseGeometri(listOf(punktB, punktC)).build())
        val stemmekretsKommunegrenseCJK = grenseRepository.save(GrenseBuilder(id = "CJK", lokalId = Lokalid("CJK"),
            gyldigFra = october1In2022, grensetype = Grensetype.Kommunegrense)
            .withGrenseGeometri(listOf(punktC, punktJ, punktK)).build())
        val stemmekretsKommunegrenseKLI = grenseRepository.save(GrenseBuilder(id = "KLI", lokalId = Lokalid("KLI"),
            gyldigFra = october1In2022, grensetype = Grensetype.Kommunegrense)
            .withGrenseGeometri(listOf(punktK, punktL, punktI)).build())
        val stemmekretsKommunegrenseIH = grenseRepository.save(GrenseBuilder(id = "IH", lokalId = Lokalid("IH"),
            gyldigFra = october1In2022, grensetype = Grensetype.Kommunegrense)
            .withGrenseGeometri(listOf(punktI, punktH)).build())
        val stemmekretsKommunegrenseHGD = grenseRepository.save(GrenseBuilder(id = "HGD", lokalId = Lokalid("HGD"),
            gyldigFra = october1In2022, grensetype = Grensetype.Kommunegrense)
            .withGrenseGeometri(listOf(punktH, punktG, punktD)).build())
        val stemmekretsGrenseBEH = grenseRepository.save(GrenseBuilder(id = "BEH", lokalId = Lokalid("BEH"),
            gyldigFra = october1In2022, grensetype = Grensetype.Stemmekretsgrense)
            .withGrenseGeometri(listOf(punktB, punktE, punktH)).build())
        val stemmekretsGrenseIFC = grenseRepository.save(GrenseBuilder(id = "IFC", lokalId = Lokalid("IFC"),
            gyldigFra = october1In2022, grensetype = Grensetype.Stemmekretsgrense)
            .withGrenseGeometri(listOf(punktI, punktF, punktC)).build())
        grenseRepository.flush()

        val grunnkretsOppe = saveGrunnkrets(lokalid = Lokalid("grunnkretsOppe"), id = "grunnkretsOppe",
            grunnkretsnummer = "45270101", grenser = listOf(grunnkretsKommunegrenseDABCJK, grunnkretsgrenseDEFK))
        val grunnkretsNede = saveGrunnkrets(lokalid = Lokalid("grunnkretsNede"), id = "grunnkretsNede",
            grunnkretsnummer = "45270102", grenser = listOf(grunnkretsgrenseDEFK, grunnkretsKommunegrenseKLIHGD))
        val stemmekretsTilVenstre = saveStemmekrets(lokalid = Lokalid("stemmekretsTilVenstre"), id = "stemmekretsTilVenstre",
            stemmekretsnummer = "1", grenser = listOf(stemmekretsKommunegrenseDAB, stemmekretsGrenseBEH, stemmekretsKommunegrenseHGD))
        val stemmekretsIMidten = saveStemmekrets(lokalid = Lokalid("stemmekretsIMidten"), id = "stemmekretsIMidten",
            stemmekretsnummer = "2", grenser = listOf(stemmekretsKommunegrenseBC, stemmekretsGrenseIFC, stemmekretsKommunegrenseIH, stemmekretsGrenseBEH))
        val stemmekretsTilHoyre = saveStemmekrets(lokalid = Lokalid("stemmekretsTilHoyre"), id = "stemmekretsTilHoyre",
            stemmekretsnummer = "3", grenser = listOf(stemmekretsKommunegrenseCJK, stemmekretsKommunegrenseKLI, stemmekretsGrenseIFC))

        val kommune = saveKommuneOgUnikLokalid(kommunenummerUuid = "4527", kommunenummer = "4527", gyldigFra = october1In2022,
            grunnkretser = listOf(grunnkretsNede, grunnkretsOppe), stemmekretser = listOf(stemmekretsTilHoyre, stemmekretsTilVenstre, stemmekretsIMidten))
        saveFylkeOgUnikLokalid(fylkesnummerUuid = "45", fylkesnummer = "45", gyldigFra = october1In2022, kommuner = listOf(kommune))
        grenseRepository.flush()

        flateService.oppdaterFlaterFor(setOf(kommune))
        grenseRepository.flush()

        return listOf(kommune, grunnkretsNede, grunnkretsOppe, stemmekretsTilVenstre, stemmekretsIMidten, stemmekretsTilHoyre)
    }

    /**
     * Lager en kommune som ser slik ut:
     *
     *      A ------ B ------ C
     *      |        |        |
     *      D ------ E ------ F
     *      |        |        |
     *      G ------ H ------ I
     *
     * Det ytre skallet er en kommunegrense.
     * B - E - H er kun en stemmekretsgrense, hvor punktene B og H ikke finnes på grunnkretsgrensene.
     * D - E - F er en grunnkretsgrense.
     *
     * Totalt har denne kommunen da 2 grunnkretser og 4 stemmekretser.
     * Grunnkretsene er horisontale og stemmekretsene er hvert rute
     */
    private fun lagreKommuneMedToGrunnkretserOgFireStemmekretser(medKurvepunktFraStemmekrets: Boolean = true): List<Flate> {
        val grunnkretsKommunegrenseDABCF = grenseRepository.save(GrenseBuilder(id = "DABCF", lokalId = Lokalid("DABCF"),
            gyldigFra = october1In2022, grensetype = Grensetype.Kommunegrense)
            .withGrenseGeometri(if (medKurvepunktFraStemmekrets) listOf(punktD, punktA, punktB, punktC, punktF)
            else listOf(punktD, punktA, punktC, punktF))
            .withKvalitet(MaalemetodeKodeEnum.TERRENGMAALT, 20).build())
        val grunnkretsKommunegrenseFIHGD = grenseRepository.save(GrenseBuilder(id = "FIHGD", lokalId = Lokalid("FIHGD"),
            gyldigFra = october1In2022, grensetype = Grensetype.Kommunegrense)
            .withGrenseGeometri(if (medKurvepunktFraStemmekrets) listOf(punktF, punktI, punktH, punktG, punktD)
            else listOf(punktF, punktI, punktG, punktD))
            .withKvalitet(MaalemetodeKodeEnum.TERRENGMAALT, 40).build())
        val grunnkretsgrenseDEF = grenseRepository.save(GrenseBuilder(id = "DEF", lokalId = Lokalid("DEF"),
            gyldigFra = october1In2022, grensetype = Grensetype.Grunnkretsgrense)
            .withGrenseGeometri(listOf(punktD, punktE, punktF)).build())

        val stemmekretsKommunegrenseBAD = grenseRepository.save(GrenseBuilder(id = "BAD", lokalId = Lokalid("BAD"),
            gyldigFra = october1In2022, grensetype = Grensetype.Kommunegrense)
            .withGrenseGeometri(listOf(punktB, punktA, punktD)).build())
        val stemmekretsKommunegrenseBCF = grenseRepository.save(GrenseBuilder(id = "BCF", lokalId = Lokalid("BCF"),
            gyldigFra = october1In2022, grensetype = Grensetype.Kommunegrense)
            .withGrenseGeometri(listOf(punktB, punktC, punktF)).build())
        val stemmekretsKommunegrenseFIH = grenseRepository.save(GrenseBuilder(id = "FIH", lokalId = Lokalid("FIH"),
            gyldigFra = october1In2022, grensetype = Grensetype.Kommunegrense)
            .withGrenseGeometri(listOf(punktF, punktI, punktH)).build())
        val stemmekretsKommunegrenseHGD = grenseRepository.save(GrenseBuilder(id = "HGD", lokalId = Lokalid("HGD"),
            gyldigFra = october1In2022, grensetype = Grensetype.Kommunegrense)
            .withGrenseGeometri(listOf(punktH, punktG, punktD)).build())
        val stemmekretsGrenseBE = grenseRepository.save(GrenseBuilder(id = "BE", lokalId = Lokalid("BE"),
            gyldigFra = october1In2022, grensetype = Grensetype.Stemmekretsgrense)
            .withGrenseGeometri(listOf(punktB, punktE)).build())
        val stemmekretsGrenseEH = grenseRepository.save(GrenseBuilder(id = "EH", lokalId = Lokalid("EH"),
            gyldigFra = october1In2022, grensetype = Grensetype.Stemmekretsgrense)
            .withGrenseGeometri(listOf(punktE, punktH)).build())
        val stemmekretsGrenseDE = grenseRepository.save(GrenseBuilder(id = "DE", lokalId = Lokalid("DE"),
            gyldigFra = october1In2022, grensetype = Grensetype.Stemmekretsgrense)
            .withGrenseGeometri(listOf(punktD, punktE)).build())
        val stemmekretsGrenseEF = grenseRepository.save(GrenseBuilder(id = "EF", lokalId = Lokalid("EF"),
            gyldigFra = october1In2022, grensetype = Grensetype.Stemmekretsgrense)
            .withGrenseGeometri(listOf(punktE, punktF)).build())
        grenseRepository.flush()

        val grunnkretsOppe = saveGrunnkrets(lokalid = Lokalid("grunnkretsOppe"), id = "grunnkretsOppe",
            grunnkretsnummer = "45270101", grenser = listOf(grunnkretsKommunegrenseDABCF, grunnkretsgrenseDEF))
        val grunnkretsNede = saveGrunnkrets(lokalid = Lokalid("grunnkretsNede"), id = "grunnkretsNede",
            grunnkretsnummer = "45270102", grenser = listOf(grunnkretsgrenseDEF, grunnkretsKommunegrenseFIHGD))
        val stemmekretsOppeVenstre = saveStemmekrets(lokalid = Lokalid("stemmekretsOppeVenstre"), id = "stemmekretsOppeVenstre",
            stemmekretsnummer = "1", grenser = listOf(stemmekretsKommunegrenseBAD, stemmekretsGrenseBE, stemmekretsGrenseDE))
        val stemmekretsOppeHoyre = saveStemmekrets(lokalid = Lokalid("stemmekretsOppeHoyre"), id = "stemmekretsOppeHoyre",
            stemmekretsnummer = "2", grenser = listOf(stemmekretsKommunegrenseBCF, stemmekretsGrenseEF, stemmekretsGrenseBE))
        val stemmekretsNedeVenstre = saveStemmekrets(lokalid = Lokalid("stemmekretsNedeVenstre"), id = "stemmekretsNedeVenstre",
            stemmekretsnummer = "3", grenser = listOf(stemmekretsKommunegrenseHGD, stemmekretsGrenseDE, stemmekretsGrenseEH))
        val stemmekretsNedeHoyre = saveStemmekrets(lokalid = Lokalid("stemmekretsNedeHoyre"), id = "stemmekretsNedeHoyre",
            stemmekretsnummer = "4", grenser = listOf(stemmekretsKommunegrenseFIH, stemmekretsGrenseEH, stemmekretsGrenseEF))

        val kommune = saveKommuneOgUnikLokalid(kommunenummerUuid = "4527", kommunenummer = "4527", gyldigFra = october1In2022,
            grunnkretser = listOf(grunnkretsNede, grunnkretsOppe),
            stemmekretser = listOf(stemmekretsNedeVenstre, stemmekretsNedeHoyre, stemmekretsOppeVenstre, stemmekretsOppeHoyre))
        saveFylkeOgUnikLokalid(fylkesnummerUuid = "45", fylkesnummer = "45", gyldigFra = october1In2022, kommuner = listOf(kommune))
        grenseRepository.flush()

        flateService.oppdaterFlaterFor(setOf(kommune))
        grenseRepository.flush()

        return listOf(kommune, grunnkretsNede, grunnkretsOppe, stemmekretsOppeVenstre, stemmekretsOppeHoyre, stemmekretsNedeVenstre, stemmekretsNedeHoyre)
    }

    /**
     * Lager en kommune som ser slik ut:
     *
     *      A ------ B ------ C
     *      |                 |
     *      D                 F
     *      |                 |
     *      G ------ H ------ I
     *
     *  Skallet er en kommunegrense. Kommunen har 1 grunnkrets og 1 stemmekrets.
     */
    private fun lagreKommuneMedEnGrunnkretsOgEnStemmekrets(medKurvepunktFraStemmekrets: Boolean = true): List<Flate> {
        val grunnkretsKommunegrenseDABCF = grenseRepository.save(GrenseBuilder(id = "DABCF", lokalId = Lokalid("DABCF"),
            gyldigFra = october1In2022, grensetype = Grensetype.Kommunegrense)
            .withGrenseGeometri(if (medKurvepunktFraStemmekrets) listOf(punktD, punktA, punktB, punktC, punktF)
            else listOf(punktD, punktA, punktC, punktF))
            .withKvalitet(MaalemetodeKodeEnum.TERRENGMAALT, 20).build())
        val grunnkretsKommunegrenseFIHGD = grenseRepository.save(GrenseBuilder(id = "FIHGD", lokalId = Lokalid("FIHGD"),
            gyldigFra = october1In2022, grensetype = Grensetype.Kommunegrense)
            .withGrenseGeometri(if (medKurvepunktFraStemmekrets) listOf(punktF, punktI, punktH, punktG, punktD)
            else listOf(punktF, punktI, punktG, punktD))
            .withKvalitet(MaalemetodeKodeEnum.TERRENGMAALT, 40).build())

        val stemmekretsKommunegrenseGDA = grenseRepository.save(GrenseBuilder(id = "GDA", lokalId = Lokalid("GDA"),
            gyldigFra = october1In2022, grensetype = Grensetype.Kommunegrense)
            .withGrenseGeometri(listOf(punktG, punktD, punktA)).build())
        val stemmekretsKommunegrenseBA = grenseRepository.save(GrenseBuilder(id = "BA", lokalId = Lokalid("BA"),
            gyldigFra = october1In2022, grensetype = Grensetype.Kommunegrense)
            .withGrenseGeometri(listOf(punktB, punktA)).build())
        val stemmekretsKommunegrenseBCF = grenseRepository.save(GrenseBuilder(id = "BCF", lokalId = Lokalid("BCF"),
            gyldigFra = october1In2022, grensetype = Grensetype.Kommunegrense)
            .withGrenseGeometri(listOf(punktB, punktC, punktF)).build())
        val stemmekretsKommunegrenseFIH = grenseRepository.save(GrenseBuilder(id = "FIH", lokalId = Lokalid("FIH"),
            gyldigFra = october1In2022, grensetype = Grensetype.Kommunegrense)
            .withGrenseGeometri(listOf(punktF, punktI, punktH)).build())
        val stemmekretsKommunegrenseHG = grenseRepository.save(GrenseBuilder(id = "HG", lokalId = Lokalid("HG"),
            gyldigFra = october1In2022, grensetype = Grensetype.Kommunegrense)
            .withGrenseGeometri(listOf(punktH, punktG)).build())
        grenseRepository.flush()

        val grunnkrets = saveGrunnkrets(lokalid = Lokalid("grunnkrets"), id = "grunnkrets",
            grunnkretsnummer = "45270101", grenser = listOf(grunnkretsKommunegrenseDABCF, grunnkretsKommunegrenseFIHGD))
        val stemmekrets = saveStemmekrets(lokalid = Lokalid("stemmekrets"), id = "stemmekrets",
            stemmekretsnummer = "1", grenser = listOf(stemmekretsKommunegrenseGDA, stemmekretsKommunegrenseBA, stemmekretsKommunegrenseBCF,
            stemmekretsKommunegrenseFIH, stemmekretsKommunegrenseHG))

        val kommune = saveKommuneOgUnikLokalid(kommunenummerUuid = "4527", kommunenummer = "4527", gyldigFra = october1In2022,
            grunnkretser = listOf(grunnkrets), stemmekretser = listOf(stemmekrets))
        saveFylkeOgUnikLokalid(fylkesnummerUuid = "45", fylkesnummer = "45", gyldigFra = october1In2022, kommuner = listOf(kommune))
        grenseRepository.flush()

        flateService.oppdaterFlaterFor(setOf(kommune))
        grenseRepository.flush()

        return listOf(kommune, grunnkrets, stemmekrets)
    }

    /**
     * Lager to kommuner som ser slik ut:
     *
     *      A ------ B ------------------------ M
     *      |        |                          |
     *      |        |        F ------ K        |
     *      |        |        |        |        |
     *      G ------ H ------ I ------ L        |
     *      |        |        |                 |
     *      |        Q ------ R --------------- T
     *      |                 |
     *      U --------------- W
     *
     * Kommune 1 har grensene A-B-M-T-R-I-L-K-F-I-H-G-A. Kommune 2 har I-F-K-L-I-R-W-U-G-I.
     */
    private fun lagreToKommunerMedEnklaveSomBeroererIEttPunkt(medKurvepunktFraStemmekrets: Boolean = true): List<Flate> {
        val grunnkretsKommunegrenseIH = grenseRepository.save(GrenseBuilder(id = "IH", lokalId = Lokalid("IH"),
            gyldigFra = october1In2022, grensetype = Grensetype.Kommunegrense)
            .withGrenseGeometri(listOf(punktI, punktH))
            .withKvalitet(MaalemetodeKodeEnum.TERRENGMAALT, 20).build())
        val grunnkretsKommunegrenseGH = grenseRepository.save(GrenseBuilder(id = "GH", lokalId = Lokalid("GH"),
            gyldigFra = october1In2022, grensetype = Grensetype.Kommunegrense)
            .withGrenseGeometri(listOf(punktG, punktH))
            .withKvalitet(MaalemetodeKodeEnum.TERRENGMAALT, 20).build())
        val grunnkretsKommunegrenseGA = grenseRepository.save(GrenseBuilder(id = "GA", lokalId = Lokalid("GA"),
            gyldigFra = october1In2022, grensetype = Grensetype.Kommunegrense)
            .withGrenseGeometri(listOf(punktG, punktA))
            .withKvalitet(MaalemetodeKodeEnum.TERRENGMAALT, 25).build())
        val grunnkretsKommunegrenseBA = grenseRepository.save(GrenseBuilder(id = "BA", lokalId = Lokalid("BA"),
            gyldigFra = october1In2022, grensetype = Grensetype.Kommunegrense)
            .withGrenseGeometri(listOf(punktB, punktA))
            .withKvalitet(MaalemetodeKodeEnum.TERRENGMAALT, 25).build())
        val grunnkretsKommunegrenseBM = grenseRepository.save(GrenseBuilder(id = "BM", lokalId = Lokalid("BM"),
            gyldigFra = october1In2022, grensetype = Grensetype.Kommunegrense)
            .withGrenseGeometri(listOf(punktB, punktM))
            .withKvalitet(MaalemetodeKodeEnum.TERRENGMAALT, 27).build())
        val grunnkretsKommunegrenseMT = grenseRepository.save(GrenseBuilder(id = "MT", lokalId = Lokalid("MT"),
            gyldigFra = october1In2022, grensetype = Grensetype.Kommunegrense)
            .withGrenseGeometri(listOf(punktM, punktT))
            .withKvalitet(MaalemetodeKodeEnum.TERRENGMAALT, 27).build())
        val grunnkretsKommunegrenseIF = grenseRepository.save(GrenseBuilder(id = "IF", lokalId = Lokalid("IF"),
            gyldigFra = october1In2022, grensetype = Grensetype.Kommunegrense)
            .withGrenseGeometri(listOf(punktI, punktF))
            .withKvalitet(MaalemetodeKodeEnum.TERRENGMAALT, 25).build())
        val grunnkretsKommunegrenseFKL = grenseRepository.save(GrenseBuilder(id = "FKL", lokalId = Lokalid("FKL"),
            gyldigFra = october1In2022, grensetype = Grensetype.Kommunegrense)
            .withGrenseGeometri(listOf(punktF, punktK, punktL))
            .withKvalitet(MaalemetodeKodeEnum.TERRENGMAALT, 35).build())
        val grunnkretsKommunegrenseIL = grenseRepository.save(GrenseBuilder(id = "IL", lokalId = Lokalid("IL"),
            gyldigFra = october1In2022, grensetype = Grensetype.Kommunegrense)
            .withGrenseGeometri(listOf(punktI, punktL))
            .withKvalitet(MaalemetodeKodeEnum.TERRENGMAALT, 25).build())
        val grunnkretsKommunegrenseIR = grenseRepository.save(GrenseBuilder(id = "IR", lokalId = Lokalid("IR"),
            gyldigFra = october1In2022, grensetype = Grensetype.Kommunegrense)
            .withGrenseGeometri(listOf(punktI, punktR))
            .withKvalitet(MaalemetodeKodeEnum.TERRENGMAALT, 200).build())
        val grunnkretsKommunegrenseTR = grenseRepository.save(GrenseBuilder(id = "TR", lokalId = Lokalid("TR"),
            gyldigFra = october1In2022, grensetype = Grensetype.Kommunegrense)
            .withGrenseGeometri(listOf(punktT, punktR))
            .withKvalitet(MaalemetodeKodeEnum.TERRENGMAALT, 200).build())
        val grunnkretsKommunegrenseRWUG = grenseRepository.save(GrenseBuilder(id = "RWUG", lokalId = Lokalid("RWUG"),
            gyldigFra = october1In2022, grensetype = Grensetype.Kommunegrense)
            .withGrenseGeometri(listOf(punktR, punktW, punktU, punktG))
            .withKvalitet(MaalemetodeKodeEnum.TERRENGMAALT, 200).build())
        val grunnkretsGrenseBH = grenseRepository.save(GrenseBuilder(id = "BH", lokalId = Lokalid("BH"),
            gyldigFra = october1In2022, grensetype = Grensetype.Grunnkretsgrense)
            .withGrenseGeometri(listOf(punktB, punktH))
            .withKvalitet(MaalemetodeKodeEnum.TERRENGMAALT, 30).build())
        val grunnkretsGrenseRQH = grenseRepository.save(GrenseBuilder(id = "RQH", lokalId = Lokalid("RQH"),
            gyldigFra = october1In2022, grensetype = Grensetype.Grunnkretsgrense)
            .withGrenseGeometri(listOf(punktR, punktQ, punktH))
            .withKvalitet(MaalemetodeKodeEnum.TERRENGMAALT, 40).build())

        val stemmekretsKommunegrenseIHG = grenseRepository.save(GrenseBuilder(id = "IHG", lokalId = Lokalid("IHG"),
            gyldigFra = october1In2022, grensetype = Grensetype.Kommunegrense)
            .withGrenseGeometri(listOf(punktI, punktG)).build())
        val stemmekretsKommunegrenseGA = grenseRepository.save(GrenseBuilder(id = "GDA", lokalId = Lokalid("GDA"),
            gyldigFra = october1In2022, grensetype = Grensetype.Kommunegrense)
            .withGrenseGeometri(listOf(punktG, punktA)).build())
        val stemmekretsKommunegrenseABM = grenseRepository.save(GrenseBuilder(id = "ABM", lokalId = Lokalid("ABM"),
            gyldigFra = october1In2022, grensetype = Grensetype.Kommunegrense)
            .withGrenseGeometri(listOf(punktA, punktM)).build())
        val stemmekretsKommunegrenseMTR = grenseRepository.save(GrenseBuilder(id = "MTR", lokalId = Lokalid("MTR"),
            gyldigFra = october1In2022, grensetype = Grensetype.Kommunegrense)
            .withGrenseGeometri(listOf(punktM, punktT, punktR))
            .withKvalitet(MaalemetodeKodeEnum.TERRENGMAALT, 5).build())
        val stemmekretsKommunegrenseRI = grenseRepository.save(GrenseBuilder(id = "RI", lokalId = Lokalid("RI"),
            gyldigFra = october1In2022, grensetype = Grensetype.Kommunegrense)
            .withGrenseGeometri(listOf(punktR, punktI)).build())
        val stemmekretsKommunegrenseGUWR = grenseRepository.save(GrenseBuilder(id = "GUWR", lokalId = Lokalid("GUWR"),
            gyldigFra = october1In2022, grensetype = Grensetype.Kommunegrense)
            .withGrenseGeometri(listOf(punktG, punktU, punktW, punktR)).build())

        val listeForHull: List<Grense>
        if (medKurvepunktFraStemmekrets) {
            val stemmekretsKommunegrenseIFK = grenseRepository.save(GrenseBuilder(id = "IFK", lokalId = Lokalid("IFK"),
                gyldigFra = october1In2022, grensetype = Grensetype.Kommunegrense)
                .withGrenseGeometri(listOf(punktI, punktF, punktK)).build())
            val stemmekretsKommunegrenseKLI = grenseRepository.save(GrenseBuilder(id = "KLI", lokalId = Lokalid("KLI"),
                gyldigFra = october1In2022, grensetype = Grensetype.Kommunegrense)
                .withGrenseGeometri(listOf(punktK, punktL, punktI)).build())
            listeForHull = listOf(stemmekretsKommunegrenseIFK, stemmekretsKommunegrenseKLI)
        } else {
            val stemmekretsKommunegrenseIFKLI = grenseRepository.save(GrenseBuilder(id = "IFKLI", lokalId = Lokalid("IFKLI"),
                gyldigFra = october1In2022, grensetype = Grensetype.Kommunegrense)
                .withGrenseGeometri(listOf(punktI, punktF, punktK, punktL, punktI)).build())
            listeForHull = listOf(stemmekretsKommunegrenseIFKLI)
        }
        grenseRepository.flush()

        val grunnkretsABHG = saveGrunnkrets(lokalid = Lokalid("grunnkretsABHG"), id = "grunnkretsABHG", grunnkretsnummer = "45270101",
            grenser = listOf(grunnkretsKommunegrenseBA, grunnkretsGrenseBH, grunnkretsKommunegrenseGH, grunnkretsKommunegrenseGA))
        val grunnkretsIHBMTRogHull = saveGrunnkrets(lokalid = Lokalid("grunnkretsIHBMTRogEnklave"), id = "grunnkretsIHBMTRogEnklave",
            grunnkretsnummer = "45270102",
            grenser = listOf(grunnkretsKommunegrenseIH, grunnkretsGrenseBH, grunnkretsKommunegrenseBM, grunnkretsKommunegrenseMT, grunnkretsKommunegrenseTR,
                grunnkretsKommunegrenseIR, grunnkretsKommunegrenseIL, grunnkretsKommunegrenseFKL, grunnkretsKommunegrenseIF))
        val grunnkretsQKmedToPolygoner = saveGrunnkrets(lokalid = Lokalid("grunnkretsQK"), id = "grunnkretsQK", grunnkretsnummer = "45280102",
            grenser = listOf(grunnkretsKommunegrenseIH, grunnkretsGrenseRQH, grunnkretsKommunegrenseIR,
                grunnkretsKommunegrenseIL, grunnkretsKommunegrenseFKL, grunnkretsKommunegrenseIF))
        val grunnkretsGUW = saveGrunnkrets(lokalid = Lokalid("grunnkretsGUW"), id = "grunnkretsGUW", grunnkretsnummer = "45280103",
            grenser = listOf(grunnkretsKommunegrenseRWUG, grunnkretsKommunegrenseGH, grunnkretsGrenseRQH))

        val stemmekretsGAMTogHull = saveStemmekrets(lokalid = Lokalid("stemmekretsGAMTogEnklave"), id = "stemmekretsGAMTogEnklave",
            stemmekretsnummer = "1", grenser = listOf(stemmekretsKommunegrenseIHG, stemmekretsKommunegrenseGA, stemmekretsKommunegrenseABM,
            stemmekretsKommunegrenseMTR, stemmekretsKommunegrenseRI) + listeForHull)
        val stemmekretsUKmedToPolygoner = saveStemmekrets(lokalid = Lokalid("stemmekretsUK"), id = "stemmekretsUK", stemmekretsnummer = "1",
            grenser = listOf(stemmekretsKommunegrenseIHG, stemmekretsKommunegrenseGUWR, stemmekretsKommunegrenseRI) + listeForHull)

        val kommune4527 = saveKommuneOgUnikLokalid(kommunenummerUuid = "4527", kommunenummer = "4527", id = "4527", lokalid = "4527",
            gyldigFra = october1In2022, grunnkretser = listOf(grunnkretsABHG, grunnkretsIHBMTRogHull), stemmekretser = listOf(stemmekretsGAMTogHull))
        val kommune4528 = saveKommuneOgUnikLokalid(kommunenummerUuid = "4528", kommunenummer = "4528", id = "4528", lokalid = "4528",
            gyldigFra = october1In2022, grunnkretser = listOf(grunnkretsQKmedToPolygoner, grunnkretsGUW), stemmekretser = listOf(stemmekretsUKmedToPolygoner))

        saveFylkeOgUnikLokalid(fylkesnummerUuid = "45", fylkesnummer = "45", gyldigFra = october1In2022, kommuner = listOf(kommune4527, kommune4528))
        grenseRepository.flush()

        flateService.oppdaterFlaterFor(setOf(kommune4527, kommune4528))
        grenseRepository.flush()

        return listOf(kommune4527, kommune4528, grunnkretsABHG, grunnkretsIHBMTRogHull, grunnkretsQKmedToPolygoner, grunnkretsGUW, stemmekretsGAMTogHull, stemmekretsUKmedToPolygoner)
    }

    /**
     * Lager to kommuner som ser slik ut:
     *
     *      A ------ B --------------- J
     *      |        |                 |
     *      D ------ E --------------- K
     *      |        |                 |
     *      G ------ H ------ I ------ L ------ O
     *      |        |        |        |        |
     *      |        |        |        S ------ T
     *      |        |        |                 |
     *      U ------ V ------ W --------------- Y
     *
     * Kommune 1 har har hde horisontale polygonene øverst og nederst. Kommune 2 har polygonet i midten
     */
    private fun lagreToKommunerHvorEnHarToPolygoner(): List<Flate> {
        val grunnkretsKommunegrenseBA = grenseRepository.save(GrenseBuilder(id = "BA", lokalId = Lokalid("BA"),
            gyldigFra = october1In2022, grensetype = Grensetype.Kommunegrense)
            .withGrenseGeometri(listOf(punktB, punktA))
            .withKvalitet(MaalemetodeKodeEnum.TERRENGMAALT, 25).build())
        val grunnkretsKommunegrenseBJ = grenseRepository.save(GrenseBuilder(id = "BJ", lokalId = Lokalid("BJ"),
            gyldigFra = october1In2022, grensetype = Grensetype.Kommunegrense)
            .withGrenseGeometri(listOf(punktB, punktJ))
            .withKvalitet(MaalemetodeKodeEnum.TERRENGMAALT, 27).build())
        val grunnkretsKommunegrenseAD = grenseRepository.save(GrenseBuilder(id = "AD", lokalId = Lokalid("AD"),
            gyldigFra = october1In2022, grensetype = Grensetype.Kommunegrense)
            .withGrenseGeometri(listOf(punktA, punktD))
            .withKvalitet(MaalemetodeKodeEnum.TERRENGMAALT, 25).build())
        val grunnkretsGrenseBE = grenseRepository.save(GrenseBuilder(id = "BE", lokalId = Lokalid("BE"),
            gyldigFra = october1In2022, grensetype = Grensetype.Grunnkretsgrense)
            .withGrenseGeometri(listOf(punktB, punktE))
            .withKvalitet(MaalemetodeKodeEnum.TERRENGMAALT, 25).build())
        val grunnkretsKommunegrenseJK = grenseRepository.save(GrenseBuilder(id = "JK", lokalId = Lokalid("JK"),
            gyldigFra = october1In2022, grensetype = Grensetype.Kommunegrense)
            .withGrenseGeometri(listOf(punktJ, punktK))
            .withKvalitet(MaalemetodeKodeEnum.TERRENGMAALT, 25).build())
        val grunnkretsKommunegrenseDE = grenseRepository.save(GrenseBuilder(id = "DE", lokalId = Lokalid("DE"),
            gyldigFra = october1In2022, grensetype = Grensetype.Kommunegrense)
            .withGrenseGeometri(listOf(punktD, punktE))
            .withKvalitet(MaalemetodeKodeEnum.TERRENGMAALT, 25).build())
        val grunnkretsKommunegrenseKE = grenseRepository.save(GrenseBuilder(id = "KE", lokalId = Lokalid("KE"),
            gyldigFra = october1In2022, grensetype = Grensetype.Kommunegrense)
            .withGrenseGeometri(listOf(punktK, punktE))
            .withKvalitet(MaalemetodeKodeEnum.TERRENGMAALT, 25).build())
        val grunnkretsKommunegrenseGD = grenseRepository.save(GrenseBuilder(id = "GD", lokalId = Lokalid("GD"),
            gyldigFra = october1In2022, grensetype = Grensetype.Kommunegrense)
            .withGrenseGeometri(listOf(punktG, punktD))
            .withKvalitet(MaalemetodeKodeEnum.TERRENGMAALT, 25).build())
        val grunnkretsGrenseEH = grenseRepository.save(GrenseBuilder(id = "EH", lokalId = Lokalid("EH"),
            gyldigFra = october1In2022, grensetype = Grensetype.Grunnkretsgrense)
            .withGrenseGeometri(listOf(punktE, punktH))
            .withKvalitet(MaalemetodeKodeEnum.TERRENGMAALT, 30).build())
        val grunnkretsKommunegrenseKL = grenseRepository.save(GrenseBuilder(id = "KL", lokalId = Lokalid("KL"),
            gyldigFra = october1In2022, grensetype = Grensetype.Kommunegrense)
            .withGrenseGeometri(listOf(punktK, punktL))
            .withKvalitet(MaalemetodeKodeEnum.TERRENGMAALT, 35).build())
        val grunnkretsKommunegrenseGH = grenseRepository.save(GrenseBuilder(id = "GH", lokalId = Lokalid("GH"),
            gyldigFra = october1In2022, grensetype = Grensetype.Kommunegrense)
            .withGrenseGeometri(listOf(punktG, punktH))
            .withKvalitet(MaalemetodeKodeEnum.TERRENGMAALT, 20).build())
        val grunnkretsKommunegrenseLH = grenseRepository.save(GrenseBuilder(id = "LH", lokalId = Lokalid("LH"),
            gyldigFra = october1In2022, grensetype = Grensetype.Kommunegrense)
            .withGrenseGeometri(listOf(punktL, punktH))
            .withKvalitet(MaalemetodeKodeEnum.TERRENGMAALT, 20).build())
        val grunnkretsKommunegrenseOL = grenseRepository.save(GrenseBuilder(id = "OL", lokalId = Lokalid("OL"),
            gyldigFra = october1In2022, grensetype = Grensetype.Kommunegrense)
            .withGrenseGeometri(listOf(punktO, punktL))
            .withKvalitet(MaalemetodeKodeEnum.TERRENGMAALT, 27).build())
        val grunnkretsKommunegrenseVUG = grenseRepository.save(GrenseBuilder(id = "VUG", lokalId = Lokalid("VUG"),
            gyldigFra = october1In2022, grensetype = Grensetype.Kommunegrense)
            .withGrenseGeometri(listOf(punktV, punktU, punktG))
            .withKvalitet(MaalemetodeKodeEnum.TERRENGMAALT, 200).build())
        val grunnkretsGrenseHV = grenseRepository.save(GrenseBuilder(id = "HV", lokalId = Lokalid("HV"),
            gyldigFra = october1In2022, grensetype = Grensetype.Grunnkretsgrense)
            .withGrenseGeometri(listOf(punktH, punktV))
            .withKvalitet(MaalemetodeKodeEnum.TERRENGMAALT, 200).build())
        val grunnkretsKommunegrenseOT = grenseRepository.save(GrenseBuilder(id = "OT", lokalId = Lokalid("OT"),
            gyldigFra = october1In2022, grensetype = Grensetype.Kommunegrense)
            .withGrenseGeometri(listOf(punktO, punktT))
            .withKvalitet(MaalemetodeKodeEnum.TERRENGMAALT, 27).build())
        val grunnkretsKommunegrenseTY = grenseRepository.save(GrenseBuilder(id = "TY", lokalId = Lokalid("TY"),
            gyldigFra = october1In2022, grensetype = Grensetype.Kommunegrense)
            .withGrenseGeometri(listOf(punktT, punktY))
            .withKvalitet(MaalemetodeKodeEnum.TERRENGMAALT, 200).build())
        val grunnkretsGrenseTSL = grenseRepository.save(GrenseBuilder(id = "TSL", lokalId = Lokalid("TSL"),
            gyldigFra = october1In2022, grensetype = Grensetype.Grunnkretsgrense)
            .withGrenseGeometri(listOf(punktT, punktS, punktL))
            .withKvalitet(MaalemetodeKodeEnum.TERRENGMAALT, 40).build())
        val grunnkretsKommunegrenseYV = grenseRepository.save(GrenseBuilder(id = "YV", lokalId = Lokalid("YV"),
            gyldigFra = october1In2022, grensetype = Grensetype.Kommunegrense)
            .withGrenseGeometri(listOf(punktY, punktV))
            .withKvalitet(MaalemetodeKodeEnum.TERRENGMAALT, 25).build())

        val stemmekretsKommunegrenseABJ = grenseRepository.save(GrenseBuilder(id = "ABJ", lokalId = Lokalid("ABJ"),
            gyldigFra = october1In2022, grensetype = Grensetype.Kommunegrense)
            .withGrenseGeometri(listOf(punktA, punktJ)).build())
        val stemmekretsKommunegrenseDA = grenseRepository.save(GrenseBuilder(id = "DA", lokalId = Lokalid("DA"),
            gyldigFra = october1In2022, grensetype = Grensetype.Kommunegrense)
            .withGrenseGeometri(listOf(punktD, punktA)).build())
        val stemmekretsKommunegrenseJK = grenseRepository.save(GrenseBuilder(id = "JK", lokalId = Lokalid("JK"),
            gyldigFra = october1In2022, grensetype = Grensetype.Kommunegrense)
            .withGrenseGeometri(listOf(punktJ, punktK)).build())
        val stemmekretsKommunegrenseDEK = grenseRepository.save(GrenseBuilder(id = "DEK", lokalId = Lokalid("DEK"),
            gyldigFra = october1In2022, grensetype = Grensetype.Kommunegrense)
            .withGrenseGeometri(listOf(punktD, punktK)).build())
        val stemmekretsKommunegrenseDG = grenseRepository.save(GrenseBuilder(id = "DG", lokalId = Lokalid("DG"),
            gyldigFra = october1In2022, grensetype = Grensetype.Kommunegrense)
            .withGrenseGeometri(listOf(punktD, punktG)).build())
        val stemmekretsKommunegrenseKL = grenseRepository.save(GrenseBuilder(id = "KL", lokalId = Lokalid("KL"),
            gyldigFra = october1In2022, grensetype = Grensetype.Kommunegrense)
            .withGrenseGeometri(listOf(punktK, punktL)).build())
        val stemmekretsKommunegrenseIHG = grenseRepository.save(GrenseBuilder(id = "IHG", lokalId = Lokalid("IHG"),
            gyldigFra = october1In2022, grensetype = Grensetype.Kommunegrense)
            .withGrenseGeometri(listOf(punktI, punktG)).build())
        val stemmekretsKommunegrenseIL = grenseRepository.save(GrenseBuilder(id = "IL", lokalId = Lokalid("IL"),
            gyldigFra = october1In2022, grensetype = Grensetype.Kommunegrense)
            .withGrenseGeometri(listOf(punktI, punktL)).build())
        val stemmekretsKommunegrenseLO = grenseRepository.save(GrenseBuilder(id = "LO", lokalId = Lokalid("LO"),
            gyldigFra = october1In2022, grensetype = Grensetype.Kommunegrense)
            .withGrenseGeometri(listOf(punktL, punktO)).build())
        val stemmekretsKommunegrenseGU = grenseRepository.save(GrenseBuilder(id = "GU", lokalId = Lokalid("GU"),
            gyldigFra = october1In2022, grensetype = Grensetype.Kommunegrense)
            .withGrenseGeometri(listOf(punktG, punktU)).build())
        val stemmekretsGrenseIW = grenseRepository.save(GrenseBuilder(id = "IW", lokalId = Lokalid("IW"),
            gyldigFra = october1In2022, grensetype = Grensetype.Stemmekretsgrense)
            .withGrenseGeometri(listOf(punktI, punktW)).build())
        val stemmekretsKommunegrenseOTY = grenseRepository.save(GrenseBuilder(id = "OTY", lokalId = Lokalid("OTY"),
            gyldigFra = october1In2022, grensetype = Grensetype.Kommunegrense)
            .withGrenseGeometri(listOf(punktO, punktY))
            .withKvalitet(MaalemetodeKodeEnum.TERRENGMAALT, 5).build())
        val stemmekretsKommunegrenseUW = grenseRepository.save(GrenseBuilder(id = "UW", lokalId = Lokalid("UW"),
            gyldigFra = october1In2022, grensetype = Grensetype.Kommunegrense)
            .withGrenseGeometri(listOf(punktU, punktW))
            .withKvalitet(MaalemetodeKodeEnum.TERRENGMAALT, 5).build())
        val stemmekretsKommunegrenseYW = grenseRepository.save(GrenseBuilder(id = "YW", lokalId = Lokalid("YW"),
            gyldigFra = october1In2022, grensetype = Grensetype.Kommunegrense)
            .withGrenseGeometri(listOf(punktY, punktW)).build())

        grenseRepository.flush()

        val grunnkretsABED = saveGrunnkrets(lokalid = Lokalid("grunnkretsABED"), id = "grunnkretsABED", grunnkretsnummer = "45270101",
            grenser = listOf(grunnkretsKommunegrenseBA, grunnkretsKommunegrenseAD, grunnkretsKommunegrenseDE, grunnkretsGrenseBE))
        val grunnkretsBEKJ = saveGrunnkrets(lokalid = Lokalid("grunnkretsBEKJ"), id = "grunnkretsBEKJ", grunnkretsnummer = "45270102",
            grenser = listOf(grunnkretsKommunegrenseBJ, grunnkretsKommunegrenseJK, grunnkretsKommunegrenseKE, grunnkretsGrenseBE))

        val grunnkretsDEHG = saveGrunnkrets(lokalid = Lokalid("grunnkretsDEHG"), id = "grunnkretsDEHG", grunnkretsnummer = "45280102",
            grenser = listOf(grunnkretsKommunegrenseGD, grunnkretsKommunegrenseDE, grunnkretsGrenseEH, grunnkretsKommunegrenseGH))
        val grunnkretsEHLK = saveGrunnkrets(lokalid = Lokalid("grunnkretsEHLK"), id = "grunnkretsEHLK", grunnkretsnummer = "45280103",
            grenser = listOf(grunnkretsKommunegrenseKE, grunnkretsKommunegrenseKL, grunnkretsKommunegrenseLH, grunnkretsGrenseEH))

        val grunnkretsGHVU = saveGrunnkrets(lokalid = Lokalid("grunnkretsGHVU"), id = "grunnkretsGHVU", grunnkretsnummer = "45271102",
            grenser = listOf(grunnkretsKommunegrenseGH, grunnkretsGrenseHV, grunnkretsKommunegrenseVUG))
        val grunnkretsHLTYV = saveGrunnkrets(lokalid = Lokalid("grunnkretsHLTYV"), id = "grunnkretsHLTYV", grunnkretsnummer = "45271103",
            grenser = listOf(grunnkretsKommunegrenseLH, grunnkretsGrenseTSL, grunnkretsKommunegrenseTY, grunnkretsKommunegrenseYV, grunnkretsGrenseHV))
        val grunnkretsLOTS = saveGrunnkrets(lokalid = Lokalid("grunnkretsLOTS"), id = "grunnkretsLOTS", grunnkretsnummer = "45271104",
            grenser = listOf(grunnkretsKommunegrenseOL, grunnkretsGrenseTSL, grunnkretsKommunegrenseOT))

        val stemmekretsDAJK = saveStemmekrets(lokalid = Lokalid("stemmekretsDAJK"), id = "stemmekretsDAJK", stemmekretsnummer = "1",
            grenser = listOf(stemmekretsKommunegrenseDA, stemmekretsKommunegrenseABJ, stemmekretsKommunegrenseJK, stemmekretsKommunegrenseDEK))
        val stemmekretsDGLK = saveStemmekrets(lokalid = Lokalid("stemmekretsDGLK"), id = "stemmekretsDGLK", stemmekretsnummer = "1",
            grenser = listOf(stemmekretsKommunegrenseDG, stemmekretsKommunegrenseDEK, stemmekretsKommunegrenseKL, stemmekretsKommunegrenseIL, stemmekretsKommunegrenseIHG))
        val stemmekretsGUWI = saveStemmekrets(lokalid = Lokalid("stemmekretsGUWI"), id = "stemmekretsGUWI", stemmekretsnummer = "12",
            grenser = listOf(stemmekretsKommunegrenseGU, stemmekretsKommunegrenseUW, stemmekretsGrenseIW, stemmekretsKommunegrenseIHG))
        val stemmekretsIWYO = saveStemmekrets(lokalid = Lokalid("stemmekretsIWYO"), id = "stemmekretsIWYO", stemmekretsnummer = "13",
            grenser = listOf(stemmekretsGrenseIW, stemmekretsKommunegrenseYW, stemmekretsKommunegrenseOTY, stemmekretsKommunegrenseLO, stemmekretsKommunegrenseIL))

        val kommune4527 = saveKommuneOgUnikLokalid(kommunenummerUuid = "4527", kommunenummer = "4527", id = "4527", lokalid = "4527",
            gyldigFra = october1In2022, grunnkretser = listOf(grunnkretsABED, grunnkretsBEKJ, grunnkretsGHVU, grunnkretsHLTYV, grunnkretsLOTS),
            stemmekretser = listOf(stemmekretsDAJK, stemmekretsGUWI, stemmekretsIWYO))
        val kommune4528 = saveKommuneOgUnikLokalid(kommunenummerUuid = "4528", kommunenummer = "4528", id = "4528", lokalid = "4528",
            gyldigFra = october1In2022, grunnkretser = listOf(grunnkretsDEHG, grunnkretsEHLK), stemmekretser = listOf(stemmekretsDGLK))

        saveFylkeOgUnikLokalid(fylkesnummerUuid = "45", fylkesnummer = "45", gyldigFra = october1In2022, kommuner = listOf(kommune4527, kommune4528))
        grenseRepository.flush()

        flateService.oppdaterFlaterFor(setOf(kommune4527, kommune4528))
        grenseRepository.flush()

        return listOf(kommune4527, kommune4528, stemmekretsDAJK, stemmekretsGUWI, stemmekretsIWYO, stemmekretsDGLK,
            grunnkretsABED, grunnkretsBEKJ, grunnkretsGHVU, grunnkretsHLTYV, grunnkretsLOTS, grunnkretsDEHG, grunnkretsEHLK)
    }

    /**
     * Lager to kommuner som ser slik ut:
     *
     *      A ------ B ------ C ------ J
     *      |        |        |        |
     *      D ------ E ------ F ------ K
     *      |        |        |        |
     *      G ------ H ------ I ------ L
     *
     * Kommune 1 har grensene A-B-C-F-I-H-G-D-A. Kommune 2 har C-J-K-L-I-F-C.
     * Grunnkretsene er vertikale og hver rute er en stemmekrets.
     * Punktene D, F og K finnes ikke på grunnkretsgrensene.
     * Kommune 1 har 2 grunnkretser og 4 stemmekretser, kommune 2 har 1 grunnkrets og 2 stemmekretser
     */
    private fun lagreToKommuner(): List<Flate> {
        val grunnkretsKommunegrenseHGAB = grenseRepository.save(GrenseBuilder(id = "HGAB", lokalId = Lokalid("HGAB"),
            gyldigFra = october1In2022, grensetype = Grensetype.Kommunegrense)
            .withGrenseGeometri(listOf(punktH, punktG, punktA, punktB))
            .withKvalitet(MaalemetodeKodeEnum.TERRENGMAALT, 20).build())
        val grunnkretsKommunegrenseCB = grenseRepository.save(GrenseBuilder(id = "CB", lokalId = Lokalid("CB"),
            gyldigFra = october1In2022, grensetype = Grensetype.Kommunegrense, opphav = null)
            .withGrenseGeometri(listOf(punktC, punktB))
            .withKvalitet(MaalemetodeKodeEnum.TERRENGMAALT, 25).build())
        val grunnkretsKommunegrenseCJ = grenseRepository.save(GrenseBuilder(id = "CJ", lokalId = Lokalid("CJ"),
            gyldigFra = october1In2022, grensetype = Grensetype.Kommunegrense)
            .withGrenseGeometri(listOf(punktC, punktJ))
            .withKvalitet(MaalemetodeKodeEnum.TERRENGMAALT, 25).build())
        val grunnkretsKommunegrenseCI = grenseRepository.save(GrenseBuilder(id = "CI", lokalId = Lokalid("CI"),
            gyldigFra = october1In2022, grensetype = Grensetype.Kommunegrense, opphav = null)
            .withGrenseGeometri(listOf(punktC, punktI))
            .withKvalitet(MaalemetodeKodeEnum.TERRENGMAALT, 35).build())
        val grunnkretsKommunegrenseJL = grenseRepository.save(GrenseBuilder(id = "JL", lokalId = Lokalid("JL"),
            gyldigFra = october1In2022, grensetype = Grensetype.Kommunegrense)
            .withGrenseGeometri(listOf(punktJ, punktL))
            .withKvalitet(MaalemetodeKodeEnum.TERRENGMAALT, 40).build())
        val grunnkretsKommunegrenseLI = grenseRepository.save(GrenseBuilder(id = "LI", lokalId = Lokalid("LI"),
            gyldigFra = october1In2022, grensetype = Grensetype.Kommunegrense)
            .withGrenseGeometri(listOf(punktL, punktI))
            .withKvalitet(MaalemetodeKodeEnum.TERRENGMAALT, 40).build())
        val grunnkretsKommunegrenseHI = grenseRepository.save(GrenseBuilder(id = "HI", lokalId = Lokalid("HI"),
            gyldigFra = october1In2022, grensetype = Grensetype.Kommunegrense)
            .withGrenseGeometri(listOf(punktH, punktI))
            .withKvalitet(MaalemetodeKodeEnum.TERRENGMAALT, 40).build())
        val grunnkretsgrenseBH = grenseRepository.save(GrenseBuilder(id = "BH", lokalId = Lokalid("BH"),
            gyldigFra = october1In2022, grensetype = Grensetype.Grunnkretsgrense)
            .withGrenseGeometri(listOf(punktB, punktH)).build())

        val stemmekretsKommunegrenseDAB = grenseRepository.save(GrenseBuilder(id = "DAB", lokalId = Lokalid("DAB"),
            gyldigFra = october1In2022, grensetype = Grensetype.Kommunegrense)
            .withGrenseGeometri(listOf(punktD, punktA, punktB)).build())
        val stemmekretsKommunegrenseBC = grenseRepository.save(GrenseBuilder(id = "BC", lokalId = Lokalid("BC"),
            gyldigFra = october1In2022, grensetype = Grensetype.Kommunegrense)
            .withGrenseGeometri(listOf(punktB, punktC)).build())
        val stemmekretsKommunegrenseCJK = grenseRepository.save(GrenseBuilder(id = "CJK", lokalId = Lokalid("CJK"),
            gyldigFra = october1In2022, grensetype = Grensetype.Kommunegrense)
            .withGrenseGeometri(listOf(punktC, punktJ, punktK)).build())
        val stemmekretsKommunegrenseCF = grenseRepository.save(GrenseBuilder(id = "CF", lokalId = Lokalid("CF"),
            gyldigFra = october1In2022, grensetype = Grensetype.Kommunegrense, opphav = null)
            .withGrenseGeometri(listOf(punktC, punktF)).build())
        val stemmekretsKommunegrenseKLI = grenseRepository.save(GrenseBuilder(id = "KLI", lokalId = Lokalid("KLI"),
            gyldigFra = october1In2022, grensetype = Grensetype.Kommunegrense)
            .withGrenseGeometri(listOf(punktK, punktL, punktI)).build())
        val stemmekretsKommunegrenseIH = grenseRepository.save(GrenseBuilder(id = "IH", lokalId = Lokalid("IH"),
            gyldigFra = october1In2022, grensetype = Grensetype.Kommunegrense)
            .withGrenseGeometri(listOf(punktI, punktH)).build())
        val stemmekretsKommunegrenseIF = grenseRepository.save(GrenseBuilder(id = "IF", lokalId = Lokalid("IF"),
            gyldigFra = october1In2022, grensetype = Grensetype.Kommunegrense)
            .withGrenseGeometri(listOf(punktI, punktF)).build())
        val stemmekretsKommunegrenseHGD = grenseRepository.save(GrenseBuilder(id = "HGD", lokalId = Lokalid("HGD"),
            gyldigFra = october1In2022, grensetype = Grensetype.Kommunegrense)
            .withGrenseGeometri(listOf(punktH, punktG, punktD)).build())
        val stemmekretsGrenseBE = grenseRepository.save(GrenseBuilder(id = "BE", lokalId = Lokalid("BE"),
            gyldigFra = october1In2022, grensetype = Grensetype.Stemmekretsgrense)
            .withGrenseGeometri(listOf(punktB, punktE)).build())
        val stemmekretsGrenseED = grenseRepository.save(GrenseBuilder(id = "ED", lokalId = Lokalid("ED"),
            gyldigFra = october1In2022, grensetype = Grensetype.Stemmekretsgrense)
            .withGrenseGeometri(listOf(punktE, punktD)).build())
        val stemmekretsGrenseEF = grenseRepository.save(GrenseBuilder(id = "EF", lokalId = Lokalid("EF"),
            gyldigFra = october1In2022, grensetype = Grensetype.Stemmekretsgrense)
            .withGrenseGeometri(listOf(punktE, punktF)).build())
        val stemmekretsGrenseEH = grenseRepository.save(GrenseBuilder(id = "EH", lokalId = Lokalid("EH"),
            gyldigFra = october1In2022, grensetype = Grensetype.Stemmekretsgrense)
            .withGrenseGeometri(listOf(punktE, punktH)).build())
        val stemmekretsGrenseFK = grenseRepository.save(GrenseBuilder(id = "FK", lokalId = Lokalid("FK"),
            gyldigFra = october1In2022, grensetype = Grensetype.Stemmekretsgrense)
            .withGrenseGeometri(listOf(punktF, punktK)).build())
        grenseRepository.flush()

        val grunnkretsTilVenstre = saveGrunnkrets(lokalid = Lokalid("grunnkretsTilVenstre"), id = "grunnkretsTilVenstre",
            grunnkretsnummer = "45270101", grenser = listOf(grunnkretsKommunegrenseHGAB, grunnkretsgrenseBH))
        val grunnkretsIMidten = saveGrunnkrets(lokalid = Lokalid("grunnkretsIMidten"), id = "grunnkretsIMidten",
            grunnkretsnummer = "45270102", grenser = listOf(grunnkretsgrenseBH, grunnkretsKommunegrenseCB, grunnkretsKommunegrenseCI, grunnkretsKommunegrenseHI))
        val grunnkretsITilHoyre = saveGrunnkrets(lokalid = Lokalid("grunnkretsITilHoyre"), id = "grunnkretsITilHoyre",
            grunnkretsnummer = "45280101", grenser = listOf(grunnkretsKommunegrenseCJ,
            grunnkretsKommunegrenseJL, grunnkretsKommunegrenseLI, grunnkretsKommunegrenseCI))
        val stemmekretsABED = saveStemmekrets(lokalid = Lokalid("stemmekretsABED"), id = "stemmekretsABED",
            stemmekretsnummer = "1", grenser = listOf(stemmekretsKommunegrenseDAB, stemmekretsGrenseBE, stemmekretsGrenseED))
        val stemmekretsDEHG = saveStemmekrets(lokalid = Lokalid("stemmekretsDEHG"), id = "stemmekretsDEHG",
            stemmekretsnummer = "2", grenser = listOf(stemmekretsGrenseED, stemmekretsGrenseEH, stemmekretsKommunegrenseHGD))
        val stemmekretsBCFE = saveStemmekrets(lokalid = Lokalid("stemmekretsBCFE"), id = "stemmekretsBCFE",
            stemmekretsnummer = "3", grenser = listOf(stemmekretsKommunegrenseBC, stemmekretsKommunegrenseCF, stemmekretsGrenseEF, stemmekretsGrenseBE))
        val stemmekretsEFIH = saveStemmekrets(lokalid = Lokalid("stemmekretsEFIH"), id = "stemmekretsEFIH",
            stemmekretsnummer = "4", grenser = listOf(stemmekretsGrenseEF, stemmekretsKommunegrenseIF, stemmekretsKommunegrenseIH, stemmekretsGrenseEH))
        val stemmekretsCJKF = saveStemmekrets(lokalid = Lokalid("stemmekretsCJKF"), id = "stemmekretsCJKF",
            stemmekretsnummer = "1", grenser = listOf(stemmekretsKommunegrenseCJK, stemmekretsGrenseFK, stemmekretsKommunegrenseCF))
        val stemmekretsFKLI = saveStemmekrets(lokalid = Lokalid("stemmekretsFKLI"), id = "stemmekretsFKLI",
            stemmekretsnummer = "2", grenser = listOf(stemmekretsGrenseFK, stemmekretsKommunegrenseKLI, stemmekretsKommunegrenseIF))

        val kommune4527 = saveKommuneOgUnikLokalid(kommunenummerUuid = "4527", kommunenummer = "4527", id = "4527", lokalid = "4527",
            gyldigFra = october1In2022, grunnkretser = listOf(grunnkretsIMidten, grunnkretsTilVenstre),
            stemmekretser = listOf(stemmekretsABED, stemmekretsBCFE, stemmekretsEFIH, stemmekretsDEHG))
        val kommune4528 = saveKommuneOgUnikLokalid(kommunenummerUuid = "4528", kommunenummer = "4528", id = "4528", lokalid = "4528",
            gyldigFra = october1In2022, grunnkretser = listOf(grunnkretsITilHoyre), stemmekretser = listOf(stemmekretsCJKF, stemmekretsFKLI))
        saveFylkeOgUnikLokalid(fylkesnummerUuid = "45", fylkesnummer = "45", gyldigFra = october1In2022, kommuner = listOf(kommune4527, kommune4528))
        grenseRepository.flush()

        flateService.oppdaterFlaterFor(setOf(kommune4527, kommune4528))
        grenseRepository.flush()

        return listOf(kommune4527, kommune4528, grunnkretsIMidten, grunnkretsTilVenstre, grunnkretsITilHoyre,
            stemmekretsABED, stemmekretsBCFE, stemmekretsEFIH, stemmekretsDEHG, stemmekretsCJKF, stemmekretsFKLI)
    }

    /**
     * Testcaset har tre kommunegrenser for stemmekrets som skal splitte ei kommunegrense for grunnkrets.
     * En av stemmekretsgrensene har verken startpunkt eller sluttpunkt som faller sammen med startpunkt eller sluttpunkt for grunnkretsgrense.
     */
    @Test
    fun `lag delt geometri hvor tre stemmekretskommunegrenser skal splitte ei grunnkretskommunegrense`() {
        val flateList = lagreKommuneMedToGrunnkretserOgTreStemmekretser(medKurvepunktFraStemmekrets = false)
        dannDeltGeometriOgAssertKommuneMedToGrunnkretserOgTreStemmekretser(flateList)
    }

    /**
     * Testcaset har tre kommunegrenser for stemmekrets som skal splitte ei kommunegrense for grunnkrets.
     * Grunnkretsgrensene har kurvepunkt som er de samme som for stemmekrets
     */
    @Test
    fun `lag delt geometri hvor grunnkretskommunegrense har to kurvepunkt som stemmekretskommunegrenser`() {
        val flateList = lagreKommuneMedToGrunnkretserOgTreStemmekretser(medKurvepunktFraStemmekrets = true)
        dannDeltGeometriOgAssertKommuneMedToGrunnkretserOgTreStemmekretser(flateList)
    }

    /**
     * Testcaset har to kommunegrenser for stemmekrets som skal splitte ei kommunegrense for grunnkrets.
     * Stemmekretsgrensene har startpunkt eller sluttpunkt som faller sammen med startpunkt eller sluttpunkt for grunnkretsgrense.
     */
    @Test
    fun `lag delt geometri hvor to stemmekretskommunegrenser skal splitte ei grunnkretskommunegrense`() {
        val flateList = lagreKommuneMedToGrunnkretserOgFireStemmekretser(medKurvepunktFraStemmekrets = false)
        dannDeltGeometriOgAssertKommuneMedToGrunnkretserOgFireStemmekretser(flateList)
    }

    /**
     * Testcaset har to kommunegrenser for stemmekrets som skal splitte ei kommunegrense for grunnkrets.
     * Grunnkretsgrensene har kurvepunkt som er de samme som for stemmekrets
     */
    @Test
    fun `lag delt geometri hvor grunnkretskommunegrense har ett kurvepunkt som stemmekretskommunegrenser`() {
        val flateList = lagreKommuneMedToGrunnkretserOgFireStemmekretser(medKurvepunktFraStemmekrets = true)
        dannDeltGeometriOgAssertKommuneMedToGrunnkretserOgFireStemmekretser(flateList)
    }

    /**
     * Testcaset har to kommuner hvor den ene har en enklave i den andre, som berører i ett punkt.
     */
    @Test
    fun `lag delt geometri med to kommuner med enklave hvor alle grunnkretsgrensene beholdes`() {
        val flateList = lagreToKommunerMedEnklaveSomBeroererIEttPunkt(medKurvepunktFraStemmekrets = false)
        val gyldighetsDato = LocalDate.now()
        val eventRaderForAlleredeOppdaterteFlater = mutableMapOf<String, EventRad>()

        //4527 har hull, 4528 har to polygoner
        val kommune4527 = kommuneRepository.findGyldigKommuneForKommunenummer("4527", gyldighetsDato)!!
        val kommune4528 = kommuneRepository.findGyldigKommuneForKommunenummer("4528", gyldighetsDato)!!

        val (administrativeGrenserFor4527, evtNyeEventRaderFor4527) =
            deltGeometriAdministrativeGrenserService.dannDeltGeometriForKommune(kommune4527.lokalid, eventRaderForAlleredeOppdaterteFlater, gyldighetsDato)

        assertThat(administrativeGrenserFor4527).hasSize(11)
        assertThat(evtNyeEventRaderFor4527).hasSize(2)

        evtNyeEventRaderFor4527.forEach { (lokalid, eventRad) ->
            assertNyVersjonMotGammelVersjonAvFlate(flateList, lokalid, eventRad, gyldighetsDato)
        }

        eventRaderForAlleredeOppdaterteFlater.putAll(evtNyeEventRaderFor4527)
        val (administrativeGrenserFor4528, evtNyeEventRaderFor4528) =
            deltGeometriAdministrativeGrenserService.dannDeltGeometriForKommune(kommune4528.lokalid, eventRaderForAlleredeOppdaterteFlater, gyldighetsDato)

        assertThat(administrativeGrenserFor4528).hasSize(7)
        assertThat(evtNyeEventRaderFor4528).hasSize(1) //Yttergrense for stemmekretsen

        evtNyeEventRaderFor4528.forEach { (lokalid, eventRad) ->
            assertNyVersjonMotGammelVersjonAvFlate(flateList, lokalid, eventRad, gyldighetsDato)
        }

        val alleGyldigeAdmgrenser = grenseRepository.findAllById((administrativeGrenserFor4527+administrativeGrenserFor4528).map { it.id })
        alleGyldigeAdmgrenser.forEach {
            assertThat(it.gyldighet.gyldigFra).isEqualTo(october1In2022)
            assertThat(it.stemmekretser(gyldighetsDato)).isNotEmpty()
            assertThat(it.grunnkretser(gyldighetsDato)).isNotEmpty()
        }
    }

    /**
     * Testcaset har to kommuner hvor den ene har en enklave i den andre, som berører i ett punkt.
     */
    @Test
    fun `lag delt geometri med to kommuner med enklave hvor grunnkretsgrenser rundt enklaven skal endres`() {
        val flateList = lagreToKommunerMedEnklaveSomBeroererIEttPunkt(medKurvepunktFraStemmekrets = true)
        val gyldighetsDato = LocalDate.now()
        val eventRaderForAlleredeOppdaterteFlater = mutableMapOf<String, EventRad>()

        //4527 har hull, 4528 har to polygoner
        val kommune4527 = kommuneRepository.findGyldigKommuneForKommunenummer("4527", gyldighetsDato)!!
        val kommune4528 = kommuneRepository.findGyldigKommuneForKommunenummer("4528", gyldighetsDato)!!

        val (administrativeGrenserFor4527, evtNyeEventRaderFor4527) =
            deltGeometriAdministrativeGrenserService.dannDeltGeometriForKommune(kommune4527.lokalid, eventRaderForAlleredeOppdaterteFlater, gyldighetsDato)

        assertThat(administrativeGrenserFor4527).hasSize(12)
        assertThat(evtNyeEventRaderFor4527).hasSize(6)

        evtNyeEventRaderFor4527.forEach { (lokalid, eventRad) ->
            assertNyVersjonMotGammelVersjonAvFlate(flateList, lokalid, eventRad, gyldighetsDato)
        }

        eventRaderForAlleredeOppdaterteFlater.putAll(evtNyeEventRaderFor4527)
        val (administrativeGrenserFor4528, evtNyeEventRaderFor4528) =
            deltGeometriAdministrativeGrenserService.dannDeltGeometriForKommune(kommune4528.lokalid, eventRaderForAlleredeOppdaterteFlater, gyldighetsDato)

        assertThat(administrativeGrenserFor4528).hasSize(8)
        assertThat(evtNyeEventRaderFor4528).hasSize(1) //Yttergrense for stemmekretsen

        evtNyeEventRaderFor4528.forEach { (lokalid, eventRad) ->
            assertNyVersjonMotGammelVersjonAvFlate(flateList, lokalid, eventRad, gyldighetsDato)
        }

        val alleGyldigeAdmgrenser = grenseRepository.findAllById((administrativeGrenserFor4527+administrativeGrenserFor4528).map { it.id })
        alleGyldigeAdmgrenser.forEach {
            assertThat(it.stemmekretser(gyldighetsDato)).isNotEmpty()
            assertThat(it.grunnkretser(gyldighetsDato)).isNotEmpty()
        }
    }

    /**
     * Testcaset har to kommuner hvor den ene har to adskilte polygoner.
     */
    @Test
    fun `lag delt geometri med to kommuner hvor en har to adskilte polygoner`() {
        val flateList = lagreToKommunerHvorEnHarToPolygoner()
        val gyldighetsDato = LocalDate.now()
        val eventRaderForAlleredeOppdaterteFlater = mutableMapOf<String, EventRad>()

        //4527 har to polygoner, 4528 er mellom polygonene
        val kommune4527 = kommuneRepository.findGyldigKommuneForKommunenummer("4527", gyldighetsDato)!!
        val kommune4528 = kommuneRepository.findGyldigKommuneForKommunenummer("4528", gyldighetsDato)!!

        val (administrativeGrenserFor4527, evtNyeEventRaderFor4527) =
            deltGeometriAdministrativeGrenserService.dannDeltGeometriForKommune(kommune4527.lokalid, eventRaderForAlleredeOppdaterteFlater, gyldighetsDato)

        assertThat(administrativeGrenserFor4527).hasSize(16)
        assertThat(evtNyeEventRaderFor4527).hasSize(9)  //Stemmekretsene i kommunen og i nabokommunen. Ingen endring på grunnkretsene

        evtNyeEventRaderFor4527.forEach { (lokalid, eventRad) ->
            assertNyVersjonMotGammelVersjonAvFlate(flateList, lokalid, eventRad, gyldighetsDato)
        }

        eventRaderForAlleredeOppdaterteFlater.putAll(evtNyeEventRaderFor4527)
        val (administrativeGrenserFor4528, evtNyeEventRaderFor4528) =
            deltGeometriAdministrativeGrenserService.dannDeltGeometriForKommune(kommune4528.lokalid, eventRaderForAlleredeOppdaterteFlater, gyldighetsDato)

        assertThat(administrativeGrenserFor4528).hasSize(7)
        assertThat(evtNyeEventRaderFor4528).hasSize(1) //Yttergrense for stemmekretsen

        evtNyeEventRaderFor4528.forEach { (lokalid, eventRad) ->
            assertNyVersjonMotGammelVersjonAvFlate(flateList, lokalid, eventRad, gyldighetsDato)
        }

        val alleGyldigeAdmgrenser = grenseRepository.findAllById((administrativeGrenserFor4527+administrativeGrenserFor4528).map { it.id })
        alleGyldigeAdmgrenser.forEach {
            assertThat(it.stemmekretser(gyldighetsDato)).isNotEmpty()
            assertThat(it.grunnkretser(gyldighetsDato)).isNotEmpty()
        }
    }

    /**
     * Testcaset har ei kommunegrense for stemmekrets som skal matche mot to kommunegrenser for grunnkrets.
     * Stemmekretsgrensene har startpunkt eller sluttpunkt som faller sammen med endepunkt eller kurvepunkt for grunnkretsgrense.
     */
    @Test
    fun `lag delt geometri med en grunnkrets og en stemmekrets`() {
        val flateList = lagreKommuneMedEnGrunnkretsOgEnStemmekrets(medKurvepunktFraStemmekrets = false)
        dannDeltGeometriOgAssertKommuneMedEnGrunnkretsOgEnStemmekrets(flateList)
    }

    /**
     * Testcaset har ei kommunegrense for stemmekrets som skal matche mot to kommunegrenser for grunnkrets.
     * Grunnkretsgrensene har kurvepunkt som er de samme som for stemmekrets
     */
    @Test
    fun `lag delt geometri med en grunnkrets med kurvepunkt og en stemmekrets`() {
        val flateList = lagreKommuneMedEnGrunnkretsOgEnStemmekrets(medKurvepunktFraStemmekrets = true)
        dannDeltGeometriOgAssertKommuneMedEnGrunnkretsOgEnStemmekrets(flateList)
    }

    /**
     * Testcaset har to kommuner med tilsammen tre grunnkretser og ni stemmekretser.
     * Noen av grensene fra grunnkretsene skal gjenbrukes.
     */
    @Test
    fun `lag delt geometri for to kommuner stoerste foerst`() {
        val flateList = lagreToKommuner()
        val gyldighetsDato = LocalDate.now()
        val eventRaderForAlleredeOppdaterteFlater = mutableMapOf<String, EventRad>()

        val kommune4527 = kommuneRepository.findGyldigKommuneForKommunenummer("4527", gyldighetsDato)!!
        val kommune4528 = kommuneRepository.findGyldigKommuneForKommunenummer("4528", gyldighetsDato)!!

        val (administrativeGrenserFor4527, evtNyeEventRaderFor4527) =
            deltGeometriAdministrativeGrenserService.dannDeltGeometriForKommune(kommune4527.lokalid, eventRaderForAlleredeOppdaterteFlater, gyldighetsDato)

        assertThat(administrativeGrenserFor4527).hasSize(6)
        assertThat(evtNyeEventRaderFor4527).hasSize(11)  //Kretsene i kommunen og i nabokommunen pluss begge kommunene

        evtNyeEventRaderFor4527.forEach { (lokalid, eventRad) ->
            assertNyVersjonMotGammelVersjonAvFlate(flateList, lokalid, eventRad, gyldighetsDato)
        }

        eventRaderForAlleredeOppdaterteFlater.putAll(evtNyeEventRaderFor4527)
        val (administrativeGrenserFor4528, evtNyeEventRaderFor4528) =
            deltGeometriAdministrativeGrenserService.dannDeltGeometriForKommune(kommune4528.lokalid, eventRaderForAlleredeOppdaterteFlater, gyldighetsDato)

        assertThat(administrativeGrenserFor4528).hasSize(6)
        assertThat(evtNyeEventRaderFor4528).hasSize(4) //Kun kretsene i kommunen og kommunen selv

        evtNyeEventRaderFor4528.forEach { (lokalid, eventRad) ->
            assertNyVersjonMotGammelVersjonAvFlate(flateList, lokalid, eventRad, gyldighetsDato)
        }

        assertKommunegrenserForToKommuner(gyldighetsDato, administrativeGrenserFor4527 + administrativeGrenserFor4528)
    }

    /**
     * Testcaset har to kommuner med tilsammen tre grunnkretser og ni stemmekretser.
     * Noen av grensene fra grunnkretsene skal gjenbrukes.
     */
    @Test
    fun `lag delt geometri for to kommuner minste foerst`() {
        val flateList = lagreToKommuner()
        val gyldighetsDato = LocalDate.now()
        val eventRaderForAlleredeOppdaterteFlater = mutableMapOf<String, EventRad>()

        val kommune4527 = kommuneRepository.findGyldigKommuneForKommunenummer("4527", gyldighetsDato)!!
        val kommune4528 = kommuneRepository.findGyldigKommuneForKommunenummer("4528", gyldighetsDato)!!

        val (administrativeGrenserFor4528, evtNyeEventRaderFor4528) =
            deltGeometriAdministrativeGrenserService.dannDeltGeometriForKommune(kommune4528.lokalid, eventRaderForAlleredeOppdaterteFlater, gyldighetsDato)

        assertThat(administrativeGrenserFor4528).hasSize(6)
        assertThat(evtNyeEventRaderFor4528).hasSize(8) //Kretsene i kommunen og kretsene rett over kommunegrensa, pluss begge kommunene

        evtNyeEventRaderFor4528.forEach { (lokalid, eventRad) ->
            assertNyVersjonMotGammelVersjonAvFlate(flateList, lokalid, eventRad, gyldighetsDato)
        }
        eventRaderForAlleredeOppdaterteFlater.putAll(evtNyeEventRaderFor4528)

        val (administrativeGrenserFor4527, evtNyeEventRaderFor4527) =
            deltGeometriAdministrativeGrenserService.dannDeltGeometriForKommune(kommune4527.lokalid, eventRaderForAlleredeOppdaterteFlater, gyldighetsDato)

        assertThat(administrativeGrenserFor4527).hasSize(6)
        assertThat(evtNyeEventRaderFor4527).hasSize(6)  //Stemmekretsene i kommunen, grunnkrets til venstre og kommunen

        evtNyeEventRaderFor4527.forEach { (lokalid, eventRad) ->
            assertNyVersjonMotGammelVersjonAvFlate(flateList, lokalid, eventRad, gyldighetsDato)
        }

        assertKommunegrenserForToKommuner(gyldighetsDato, administrativeGrenserFor4527 + administrativeGrenserFor4528)
    }

    private fun assertKommunegrenserForToKommuner(
        gyldighetsDato: LocalDate,
        administrativeGrenser: List<Grense>
    ) {
        assertOpprinneligGrenseMotNyGrense(onsketGrenseLokalid = "HGAB", gyldighetsDato = gyldighetsDato,
            nyeAdministrativeGrenser = administrativeGrenser, onsketStartpunkt = punktH, onsketEndpunkt = punktD)
        assertOpprinneligGrenseMotNyGrense(onsketGrenseLokalid = "HGAB", gyldighetsDato = gyldighetsDato,
            nyeAdministrativeGrenser = administrativeGrenser, onsketStartpunkt = punktD, onsketEndpunkt = punktB)
        assertOpprinneligGrenseMotNyGrense(onsketGrenseLokalid = "CB", gyldighetsDato = gyldighetsDato,
            nyeAdministrativeGrenser = administrativeGrenser, onsketStartpunkt = punktC, onsketEndpunkt = punktB, uendret = true)
        assertOpprinneligGrenseMotNyGrense(onsketGrenseLokalid = "HI", gyldighetsDato = gyldighetsDato,
            nyeAdministrativeGrenser = administrativeGrenser, onsketStartpunkt = punktH, onsketEndpunkt = punktI, uendret = true)

        assertOpprinneligGrenseMotNyGrense(onsketGrenseLokalid = "CI", gyldighetsDato = gyldighetsDato,
            nyeAdministrativeGrenser = administrativeGrenser, onsketStartpunkt = punktC, onsketEndpunkt = punktF)
        assertOpprinneligGrenseMotNyGrense(onsketGrenseLokalid = "CI", gyldighetsDato = gyldighetsDato,
            nyeAdministrativeGrenser = administrativeGrenser, onsketStartpunkt = punktF, onsketEndpunkt = punktI)

        assertOpprinneligGrenseMotNyGrense(onsketGrenseLokalid = "CJ", gyldighetsDato = gyldighetsDato,
            nyeAdministrativeGrenser = administrativeGrenser, onsketStartpunkt = punktC, onsketEndpunkt = punktJ, uendret = true)
        assertOpprinneligGrenseMotNyGrense(onsketGrenseLokalid = "JL", gyldighetsDato = gyldighetsDato,
            nyeAdministrativeGrenser = administrativeGrenser, onsketStartpunkt = punktJ, onsketEndpunkt = punktK)
        assertOpprinneligGrenseMotNyGrense(onsketGrenseLokalid = "JL", gyldighetsDato = gyldighetsDato,
            nyeAdministrativeGrenser = administrativeGrenser, onsketStartpunkt = punktK, onsketEndpunkt = punktL)
        assertOpprinneligGrenseMotNyGrense(onsketGrenseLokalid = "LI", gyldighetsDato = gyldighetsDato,
            nyeAdministrativeGrenser = administrativeGrenser, onsketStartpunkt = punktL, onsketEndpunkt = punktI, uendret = true)

        val alleGyldigeAdmgrenser = grenseRepository.findAllById(administrativeGrenser.map { it.id })
        alleGyldigeAdmgrenser.forEach {
            assertThat(it.stemmekretser(gyldighetsDato)).isNotEmpty()
            assertThat(it.grunnkretser(gyldighetsDato)).isNotEmpty()
        }

    }

    private fun dannDeltGeometriOgAssertKommuneMedToGrunnkretserOgTreStemmekretser(flateList: List<Flate>) {
        val kommune = flateList.first { it is Kommune } as Kommune

        val gyldighetsDato = LocalDate.now()
        val (administrativeGrenser, evtNyeEventRader) =
            deltGeometriAdministrativeGrenserService.dannDeltGeometriForKommune(kommune.lokalid, mapOf(), gyldighetsDato)

        assertThat(administrativeGrenser).hasSize(6)
        assertThat(evtNyeEventRader).hasSize(6)

        evtNyeEventRader.forEach { (lokalid, eventRad) ->
            assertNyVersjonMotGammelVersjonAvFlate(flateList, lokalid, eventRad, gyldighetsDato)
        }

        assertOpprinneligGrenseMotNyGrense(onsketGrenseLokalid = "DABCJK", gyldighetsDato = gyldighetsDato,
            nyeAdministrativeGrenser = administrativeGrenser, onsketStartpunkt = punktD, onsketEndpunkt = punktB)
        assertOpprinneligGrenseMotNyGrense(onsketGrenseLokalid = "DABCJK", gyldighetsDato = gyldighetsDato,
            nyeAdministrativeGrenser = administrativeGrenser, onsketStartpunkt = punktB, onsketEndpunkt = punktC)
        assertOpprinneligGrenseMotNyGrense(onsketGrenseLokalid = "DABCJK", gyldighetsDato = gyldighetsDato,
            nyeAdministrativeGrenser = administrativeGrenser, onsketStartpunkt = punktC, onsketEndpunkt = punktK)

        assertOpprinneligGrenseMotNyGrense(onsketGrenseLokalid = "KLIHGD", gyldighetsDato = gyldighetsDato,
            nyeAdministrativeGrenser = administrativeGrenser, onsketStartpunkt = punktK, onsketEndpunkt = punktI)
        assertOpprinneligGrenseMotNyGrense(onsketGrenseLokalid = "KLIHGD", gyldighetsDato = gyldighetsDato,
            nyeAdministrativeGrenser = administrativeGrenser, onsketStartpunkt = punktI, onsketEndpunkt = punktH)
        assertOpprinneligGrenseMotNyGrense(onsketGrenseLokalid = "KLIHGD", gyldighetsDato = gyldighetsDato,
            nyeAdministrativeGrenser = administrativeGrenser, onsketStartpunkt = punktH, onsketEndpunkt = punktD)

        val alleGyldigeAdmgrenser = grenseRepository.findAllById(administrativeGrenser.map { it.id })
        alleGyldigeAdmgrenser.forEach {
            assertThat(it.stemmekretser(gyldighetsDato)).isNotEmpty()
            assertThat(it.grunnkretser(gyldighetsDato)).isNotEmpty()
        }
    }

    private fun dannDeltGeometriOgAssertKommuneMedToGrunnkretserOgFireStemmekretser(flateList: List<Flate>) {
        val kommune = flateList.first { it is Kommune } as Kommune

        val gyldighetsDato = LocalDate.now()
        val (administrativeGrenser, evtNyeEventRader) =
            deltGeometriAdministrativeGrenserService.dannDeltGeometriForKommune(kommune.lokalid, mapOf(), gyldighetsDato)

        assertThat(administrativeGrenser).hasSize(4)
        assertThat(evtNyeEventRader).hasSize(7)

        evtNyeEventRader.forEach { (lokalid, eventRad) ->
            assertNyVersjonMotGammelVersjonAvFlate(flateList, lokalid, eventRad, gyldighetsDato)
        }

        assertOpprinneligGrenseMotNyGrense(onsketGrenseLokalid = "DABCF", gyldighetsDato = gyldighetsDato,
            nyeAdministrativeGrenser = administrativeGrenser, onsketStartpunkt = punktD, onsketEndpunkt = punktB)
        assertOpprinneligGrenseMotNyGrense(onsketGrenseLokalid = "DABCF", gyldighetsDato = gyldighetsDato,
            nyeAdministrativeGrenser = administrativeGrenser, onsketStartpunkt = punktB, onsketEndpunkt = punktF)

        assertOpprinneligGrenseMotNyGrense(onsketGrenseLokalid = "FIHGD", gyldighetsDato = gyldighetsDato,
            nyeAdministrativeGrenser = administrativeGrenser, onsketStartpunkt = punktF, onsketEndpunkt = punktH)
        assertOpprinneligGrenseMotNyGrense(onsketGrenseLokalid = "FIHGD", gyldighetsDato = gyldighetsDato,
            nyeAdministrativeGrenser = administrativeGrenser, onsketStartpunkt = punktH, onsketEndpunkt = punktD)

        val alleGyldigeAdmgrenser = grenseRepository.findAllById(administrativeGrenser.map { it.id })
        alleGyldigeAdmgrenser.forEach {
            assertThat(it.stemmekretser(gyldighetsDato)).isNotEmpty()
            assertThat(it.grunnkretser(gyldighetsDato)).isNotEmpty()
        }
    }

    private fun dannDeltGeometriOgAssertKommuneMedEnGrunnkretsOgEnStemmekrets(flateList: List<Flate>) {
        val kommune = flateList.first { it is Kommune } as Kommune

        val gyldighetsDato = LocalDate.now()
        val (administrativeGrenser, evtNyeEventRader) =
            deltGeometriAdministrativeGrenserService.dannDeltGeometriForKommune(kommune.lokalid, mapOf(), gyldighetsDato)

        assertThat(administrativeGrenser).hasSize(6)
        assertThat(evtNyeEventRader).hasSize(3)

        evtNyeEventRader.forEach { (lokalid, eventRad) ->
            assertNyVersjonMotGammelVersjonAvFlate(flateList, lokalid, eventRad, gyldighetsDato)
        }

        assertOpprinneligGrenseMotNyGrense(onsketGrenseLokalid = "DABCF", gyldighetsDato = gyldighetsDato,
            nyeAdministrativeGrenser = administrativeGrenser, onsketStartpunkt = punktD, onsketEndpunkt = punktA)
        assertOpprinneligGrenseMotNyGrense(onsketGrenseLokalid = "DABCF", gyldighetsDato = gyldighetsDato,
            nyeAdministrativeGrenser = administrativeGrenser, onsketStartpunkt = punktA, onsketEndpunkt = punktB)
        assertOpprinneligGrenseMotNyGrense(onsketGrenseLokalid = "DABCF", gyldighetsDato = gyldighetsDato,
            nyeAdministrativeGrenser = administrativeGrenser, onsketStartpunkt = punktB, onsketEndpunkt = punktF)

        assertOpprinneligGrenseMotNyGrense(onsketGrenseLokalid = "FIHGD", gyldighetsDato = gyldighetsDato,
            nyeAdministrativeGrenser = administrativeGrenser, onsketStartpunkt = punktF, onsketEndpunkt = punktH)
        assertOpprinneligGrenseMotNyGrense(onsketGrenseLokalid = "FIHGD", gyldighetsDato = gyldighetsDato,
            nyeAdministrativeGrenser = administrativeGrenser, onsketStartpunkt = punktH, onsketEndpunkt = punktG)
        assertOpprinneligGrenseMotNyGrense(onsketGrenseLokalid = "FIHGD", gyldighetsDato = gyldighetsDato,
            nyeAdministrativeGrenser = administrativeGrenser, onsketStartpunkt = punktG, onsketEndpunkt = punktD)

        val alleGyldigeAdmgrenser = grenseRepository.findAllById(administrativeGrenser.map { it.id })
        alleGyldigeAdmgrenser.forEach {
            assertThat(it.stemmekretser(gyldighetsDato)).isNotEmpty()
            assertThat(it.grunnkretser(gyldighetsDato)).isNotEmpty()
        }

        //Kjører deling en gang til, skal få grensene, men ingen nye flateversjoner
        val (administrativeGrenser2, evtNyeEventRader2) =
            deltGeometriAdministrativeGrenserService.dannDeltGeometriForKommune(kommune.lokalid, mapOf(), gyldighetsDato)
        assertThat(administrativeGrenser2).containsExactlyInAnyOrderElementsOf(administrativeGrenser)
        assertThat(evtNyeEventRader2).isEmpty()
    }

    private fun assertNyVersjonMotGammelVersjonAvFlate(flateList: List<Flate>, oensketLokalid: String, eventRad: EventRad, gyldighetsDato: LocalDate) {
        val nyVersjonAvFlate: Flate? = when (eventRad.flateType) {
            Flatetype.GRUNNKRETS -> grunnkretsRepository.findById(eventRad.uuid).unwrap()
            Flatetype.STEMMEKRETS -> stemmekretsRepository.findById(eventRad.uuid).unwrap()
            Flatetype.KOMMUNE -> kommuneRepository.findById(eventRad.uuid).unwrap()
            Flatetype.FYLKE -> fylkeRepository.findById(eventRad.uuid).unwrap()
            Flatetype.NASJON -> nasjonRepository.findById(eventRad.uuid).unwrap()
            else -> null
        }
        assertThat(nyVersjonAvFlate).isNotNull
        val gammelVersjonAvFlate = flateList.first { flate -> flate.lokalid == oensketLokalid }

        assertThat(nyVersjonAvFlate!!.id).isNotEqualTo(gammelVersjonAvFlate.id)
        assertThat(nyVersjonAvFlate.gyldighet.gyldigFra).isEqualTo(gammelVersjonAvFlate.gyldighet.gyldigTil).isEqualTo(gyldighetsDato)
        assertThat(nyVersjonAvFlate.gyldighet.slettet).isFalse()
        assertTopologi(nyVersjonAvFlate.omraade, gammelVersjonAvFlate.omraade, true)
    }

    private fun assertOpprinneligGrenseMotNyGrense(
        onsketGrenseLokalid: String,
        gyldighetsDato: LocalDate,
        nyeAdministrativeGrenser: List<Grense>,
        onsketStartpunkt: Coordinate,
        onsketEndpunkt: Coordinate,
        uendret: Boolean = false
    ) {
        val opprinneligKommunegrense = grenseService.hentGrense(Identifikator(lokalid = Lokalid(onsketGrenseLokalid), october1In2022))!!
        assertThat(opprinneligKommunegrense.gyldighet.slettet).isFalse()

        var nyGrensebit = nyeAdministrativeGrenser.firstOrNull {
            it.grense.startPoint.coordinate.equals(onsketStartpunkt) && it.grense.endPoint.coordinate.equals(onsketEndpunkt)
        }

        if (uendret) {
            assertThat(opprinneligKommunegrense.id).isEqualTo(nyGrensebit!!.id)
            assertThat(opprinneligKommunegrense.gyldighet.gyldigTil).isNull()
        } else {
            if (nyGrensebit == null) {
                //Fant ikke grense fra start til slutt, ser om det finnes en fra slutt til start
                nyGrensebit = nyeAdministrativeGrenser.first {
                    it.grense.startPoint.coordinate.equals(onsketEndpunkt) && it.grense.endPoint.coordinate.equals(onsketStartpunkt)
                }
            }
            assertThat(opprinneligKommunegrense.gyldighet.gyldigTil).isEqualTo(gyldighetsDato)

            assertThat(nyGrensebit.kvalitet).isEqualTo(opprinneligKommunegrense.kvalitet)
            assertThat(nyGrensebit.opphav).isEqualTo(opprinneligKommunegrense.opphav)

            assertThat(nyGrensebit.lokalid).isNotEqualTo(opprinneligKommunegrense.lokalid)
            assertThat(nyGrensebit.gyldighet.gyldigFra).isEqualTo(gyldighetsDato)
            assertThat(nyGrensebit.gyldighet.gyldigTil).isNull()
            assertThat(nyGrensebit.gyldighet.slettet).isFalse()
        }
    }
}
