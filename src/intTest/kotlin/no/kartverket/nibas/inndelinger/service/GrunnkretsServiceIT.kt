package no.kartverket.nibas.inndelinger.service

import no.kartverket.nibas.builders.GrenseBuilder
import no.kartverket.nibas.builders.GrunnkretsBuilder
import no.kartverket.nibas.builders.GrunnkretsRequestBuilder
import no.kartverket.nibas.builders.KommuneBuilder
import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.domain.Endringstype
import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.domain.Grunnkrets
import no.kartverket.nibas.domain.Kommune
import no.kartverket.nibas.repository.GrenseRepository
import no.kartverket.nibas.repository.GrunnkretsRepository
import no.kartverket.nibas.repository.KommuneRepository
import no.kartverket.nibas.repository.findAllByLokalidSortedByGyldigFra
import no.kartverket.nibas.service.GrunnkretsRequestWrapper
import no.kartverket.nibas.service.GrunnkretsService
import no.kartverket.nibas.testcontainer.TestWithDbContainer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.locationtech.jts.geom.Coordinate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.Month

@ActiveProfiles(profiles = ["security-off", "eventsDisabled"])
@Transactional
class GrunnkretsServiceIT : TestWithDbContainer() {

    @Autowired
    private lateinit var grunnkretsService: GrunnkretsService

    @Autowired
    private lateinit var kommuneRepository: KommuneRepository

    @Autowired
    private lateinit var grunnkretsRepository: GrunnkretsRepository

    @Autowired
    private lateinit var grenseRepository: GrenseRepository

    private val lokalidOfGrunnkretsInDataset = Lokalid("c54fb55e-15d0-4528-8b88-f58d6c74bd87")
    private val earlyDate = LocalDate.of(2000, 1, 1)
    private val october1In2022 = LocalDate.of(2022, Month.OCTOBER, 1)
    private val november28In2022 = LocalDate.of(2022, Month.NOVEMBER, 28) // "Dagens dato"
    private val december1In2022 = LocalDate.of(2022, Month.DECEMBER, 1)
    private val jan1In2023 = LocalDate.of(2023, Month.JANUARY, 1)
    private val jan1In2024 = LocalDate.of(2024, Month.JANUARY, 1)

    private val grenseLokallid = Lokalid("888")
    private val grunnkretsLokalid = Lokalid("1001")

    @Test
    fun hentEksisterendeGrunnkretsMedDagensDato() {
        val grunnkrets = grunnkretsService.hentGrunnkrets(Identifikator(lokalidOfGrunnkretsInDataset, november28In2022))
        assertThat(grunnkrets).isNotNull
    }

    @Test
    fun hentEksisterendeGrunnkretsMedTidligereDato() {
        assertThat(grunnkretsService.hentGrunnkrets(Identifikator(lokalidOfGrunnkretsInDataset, earlyDate))).isNull()
    }

    @Test
    fun hentVersjonAvGrunnkretsFramITid() {
        val grunnkretsLokalid = Lokalid("ac7e05cd-fb07-4270-ad95-e3c32c137ba6")
        saveGrunnkretsMed1FramtidigVersjon(grunnkretsLokalid)
        assertThat(grunnkretsRepository.findAllByLokalid(grunnkretsLokalid.value).size).isEqualTo(2)
        assertThat(grunnkretsRepository.findAllByLokalidSortedByGyldigFra(grunnkretsLokalid).size).isEqualTo(2)

        val framtidigGrunnkrets = grunnkretsService.hentGrunnkrets(Identifikator(grunnkretsLokalid, jan1In2023))
        assertThat(framtidigGrunnkrets).isNotNull
        assertThat(framtidigGrunnkrets!!.grunnkretsnummer).isEqualTo("569")
    }

    @Test
    fun hentGrenserForEksisterendeGrunnkretsMedDagensDato() {
        val identifikator = Identifikator(Lokalid("1001"), november28In2022)
        lagGrenserForGrunnkrets(identifikator.lokalid)

        val grensekoblinger = grunnkretsService.hentGrenserForGrunnkrets(identifikator)
        assertThat(grensekoblinger).isNotEmpty
    }

    @Test
    fun hentGrenserForEksisterendeGrunnkretsMedTidligereDato() {
        val grenserForGrunnkrets = grunnkretsService.hentGrenserForGrunnkrets(Identifikator(lokalidOfGrunnkretsInDataset, earlyDate))
        assertThat(grenserForGrunnkrets).isEmpty()
    }

    @Test
    fun hentGrenserForGrunnkretsFramITid() {
        val identifikatorFor1Jan23 = Identifikator(grunnkretsLokalid, jan1In2023)

        val grense3 = saveGrenseAvTypeKommunegrense(id = "3002", lokalid = grenseLokallid, gyldigFra = jan1In2024)
        val grense2 = saveGrenseAvTypeKommunegrense(id = "3001", lokalid = grenseLokallid, nesteVersjon = grense3, gyldigFra = jan1In2023, gyldigTil = jan1In2024)
        val grense1 = saveGrenseAvTypeKommunegrense(id = "3000", lokalid = grenseLokallid, nesteVersjon = grense2, gyldigFra = october1In2022, gyldigTil = jan1In2023)

        val grunnkrets1 = saveGrunnkrets(id = "1301", lokalid = grunnkretsLokalid, gyldigFra = october1In2022, gyldigTil = december1In2022, grenser = listOf(grense1))
        val grunnkrets2 = saveGrunnkrets(id = "1302", lokalid = grunnkretsLokalid, gyldigFra = december1In2022, gyldigTil = jan1In2024, grenser = listOf(grense1))
        val grunnkrets3 = saveGrunnkrets(id = "1303", lokalid = grunnkretsLokalid, gyldigFra = jan1In2024, grenser = listOf(grense1))

        saveKommune(grunnkretser = listOf(grunnkrets1, grunnkrets2, grunnkrets3))

        val grensekoblinger = grunnkretsService.hentGrenserForGrunnkrets(identifikatorFor1Jan23)
        assertGrenser(grensekoblinger,
            gyldighetsdato = identifikatorFor1Jan23.gyldighetsdato,
            grenseIGrensekoblingId = "3000",
            relevantGrenseId = "3001",
            enhetKobletTilGrenseLokalid = grunnkretsLokalid
        )
    }

    @Test
    fun hentAntallFramtidigeVersjoner() {
        val grunnkretsLokalid = Lokalid("ac7e05cd-fb07-4270-ad95-e3c32c137ba6")
        saveGrunnkretsMed1FramtidigVersjon(grunnkretsLokalid)
        assertThat(grunnkretsService.hentAntallGyldigeVersjonerFramITidFor(Identifikator(grunnkretsLokalid, november28In2022))).isEqualTo(1)
    }

    @Test
    fun oppdaterGrunnkretsMedDagensDato() {
        val grunnkretsRequest = GrunnkretsRequestBuilder.DEFAULT_REQUEST
        val kommune = saveKommune(kommunenummer = "4227")

        val identifikator = Identifikator(Lokalid(grunnkretsRequest.identifikasjon.lokalid), november28In2022)
        val grunnkrets = lagGrenserForGrunnkrets(identifikator.lokalid, kommune)
        kommune.grunnkretser.add(grunnkrets)

        val now = LocalDate.now()
        val grunnkretsRequestWrapper = GrunnkretsRequestWrapper(
            grunnkretsRequest = grunnkretsRequest,
            gyldigFra = now,
            endringstype = Endringstype.RETTING)

        grunnkretsService.oppdaterGrunnkrets(identifikator.lokalid, grunnkretsRequestWrapper)
        val grunnkretsEtter = grunnkretsService.hentGrunnkrets(Identifikator(identifikator.lokalid))

        assertThat(grunnkretsEtter!!.grunnkretsnummer).isNotEqualTo(grunnkrets.grunnkretsnummer).isEqualTo(grunnkretsRequest.grunnkretsnummer)
        assertThat(grunnkretsEtter.grunnkretsnavn).isNotEqualTo(grunnkrets.grunnkretsnavn).isEqualTo(grunnkretsRequest.navn)
        assertThat(grunnkretsEtter.omraade).isEqualTo(grunnkrets.omraade)
        assertThat(grunnkretsEtter.gyldighet.gyldigFra).isEqualTo(now)
    }

    private fun lagGrenserForGrunnkrets(grunnkretsLokalid: Lokalid, kommune: Kommune? = null): Grunnkrets {
        val x1 = 23857.0
        val y1 = 675635.0

        val y2 = 675635.0
        val x2 = 23867.0

        val x3 = 23862.0
        val y3 = 675645.0

        val grense1 = GrenseBuilder(id = "1", lokalId = Lokalid("850"), gyldigFra = october1In2022)
            .withGrenseGeometri(listOf(Coordinate(x1, y1), Coordinate(x2, y2)))
            .buildGrunnkretsgrense()

        val grense2 = GrenseBuilder(id = "2", lokalId = Lokalid("851"), gyldigFra = october1In2022)
            .withGrenseGeometri(listOf(Coordinate(x2, y2), Coordinate(x3, y3)))
            .buildGrunnkretsgrense()

        val grense3 = GrenseBuilder(id = "3", lokalId = Lokalid("852"), gyldigFra = october1In2022)
            .withGrenseGeometri(listOf(Coordinate(x3, y3), Coordinate(x1, y1)))
            .buildGrunnkretsgrense()

        grenseRepository.save(grense1)
        grenseRepository.save(grense2)
        grenseRepository.save(grense3)

        return saveGrunnkrets(lokalid = grunnkretsLokalid, kommune = kommune, grenser = listOf(grense1, grense2, grense3))
    }

    fun saveGrenseAvTypeKommunegrense(id: String = "12000",
                                      lokalid: Lokalid = Lokalid("ac7e05cd-fb07-4277-ad95-63c32c1370d0"),
                                      gyldigFra: LocalDate = october1In2022,
                                      gyldigTil: LocalDate? = null,
                                      opphav: String = "Eidsvoll",
                                      nesteVersjon: Grense? = null): Grense {
        return grenseRepository.save(GrenseBuilder(
            id = id,
            lokalId = lokalid,
            gyldigFra = gyldigFra,
            gyldigTil = gyldigTil,
            opphav = opphav,
            nesteVersjon = nesteVersjon
        ).buildKommunegrense())
    }

    private fun saveGrunnkrets(id: String = "9000",
                               lokalid: Lokalid = Lokalid("1001"),
                               gyldigFra: LocalDate = october1In2022,
                               gyldigTil: LocalDate? = null,
                               grunnkrnr: String = "42270304",
                               kommune: Kommune? = null,
                               grenser: List<Grense> = emptyList()): Grunnkrets {
        return grunnkretsRepository.save(GrunnkretsBuilder(
            id = id,
            lokalId = lokalid,
            gyldigFra = gyldigFra,
            gyldigTil = gyldigTil,
            grunnkretsnummer = grunnkrnr,
            kommune = kommune,
            grenser = grenser
        ).build())
    }

    private fun saveGrunnkretsMed1FramtidigVersjon(grunnkretsLokalid: Lokalid) {
        val grunnkrets = saveGrunnkrets(
            id = "9000",
            lokalid = grunnkretsLokalid,
            gyldigFra = october1In2022,
            gyldigTil = jan1In2023,
            grunnkrnr = "568",
        )
        val grunnkretsFramtidigVersjon = saveGrunnkrets(
            id = "9001",
            lokalid = grunnkretsLokalid,
            gyldigFra = jan1In2023,
            grunnkrnr = "569",
        )
        saveKommune(grunnkretser = listOf(grunnkrets, grunnkretsFramtidigVersjon))
    }

    private fun saveKommune(grunnkretser: List<Grunnkrets> = emptyList(), kommunenummer: String = "0301"): Kommune {
        return kommuneRepository.save(KommuneBuilder(
            id = "9000",
            lokalId = Lokalid("abc-123"),
            gyldigFra = october1In2022,
            kommunenummer = kommunenummer,
            grunnkretser = grunnkretser
        ).buildWithoutAdministrativenhetnavn())
    }
}
