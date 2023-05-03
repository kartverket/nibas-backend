package no.kartverket.nibas.service

import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.api.v1.common.KontekstEnhetType
import no.kartverket.nibas.api.v1.common.geojson.Feature
import no.kartverket.nibas.api.v1.common.geojson.FeatureCollection
import no.kartverket.nibas.api.v1.common.geojson.metadata.KodelisteEntry
import no.kartverket.nibas.api.v1.common.geojson.metadata.Posisjonskvalitet
import no.kartverket.nibas.api.v1.common.geojson.metadata.StatistiskgrenseMetadata
import no.kartverket.nibas.builders.FeatureBuilder
import no.kartverket.nibas.builders.GrenseBuilder
import no.kartverket.nibas.builders.GrunnkretsBuilder
import no.kartverket.nibas.builders.GrunnkretsRequestBuilder
import no.kartverket.nibas.builders.KommuneBuilder
import no.kartverket.nibas.builders.MaalemetodeKodeEnum
import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.common.ResponseException
import no.kartverket.nibas.domain.Endringstype
import no.kartverket.nibas.domain.Flate
import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.domain.Grunnkrets
import no.kartverket.nibas.domain.Kommune
import no.kartverket.nibas.repository.GrenseRepository
import no.kartverket.nibas.repository.GrunnkretsRepository
import no.kartverket.nibas.repository.KommuneRepository
import no.kartverket.nibas.repository.findAllByLokalidSortedByGyldigFra
import no.kartverket.nibas.utils.GrenseHelper
import no.kartverket.nibas.testcontainer.TestWithDbContainer
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.Month
import java.util.*

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

    @Autowired
    private lateinit var grenseService: GrenseService

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
        assertThat(framtidigGrunnkrets!!.grunnkretsnummer).isEqualTo("42270304")
        assertThat(framtidigGrunnkrets.grunnkretsnavn).isEqualTo("Måsvann")
    }

    @Test
    fun `finn grunnkrets med samme grunnkretsnummer og ulik lokalid`() {
        val grunnkretsLokalid = Lokalid("ac7e05cd-fb07-4270-ad95-e3c32c137ba6")
        saveGrunnkretsMed1FramtidigVersjon(grunnkretsLokalid)
        val grunnkrets = grunnkretsService.hentGrunnkrets(Identifikator(grunnkretsLokalid, jan1In2023))

        //Uten å ta hensyn til gyldighet og med vilkårlig lokalid skal det finnes to grunnkretser med gitt grunnkretsnummer
        var grunnkretser = grunnkretsRepository.findAllByGrunnkretsnummerAndLokalidIsNot(grunnkrets!!.grunnkretsnummer!!, "xx")
        assertThat(grunnkretser).hasSize(2)

        //Uten å ta hensyn til gyldighet og med gitt lokalid som ikke skal finnes skal det ikke finnes noen grunnkretser med gitt grunnkretsnummer
        //Dvs det finnes ikke noen ANDRE grunnkretser enn den som skal valideres med gitt grunnkretsnummer
        grunnkretser = grunnkretsRepository.findAllByGrunnkretsnummerAndLokalidIsNot(grunnkrets.grunnkretsnummer!!, grunnkrets.lokalid)
        assertThat(grunnkretser).isEmpty()
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

        val identifikator = Identifikator(Lokalid(grunnkretsRequest.identifikasjon.lokalid), october1In2022)
        val grunnkrets = lagGrenserForGrunnkrets(identifikator.lokalid, kommune)
        kommune.grunnkretser.add(grunnkrets)

        val now = LocalDate.now()
        val opprinneligGrunnkrets = grunnkretsService.hentGrunnkrets(identifikator)

        grunnkretsService.oppdaterGrunnkrets(
            lokalId = identifikator.lokalid,
            oppdatertGrunnkrets = grunnkretsRequest,
            gyldigFra = now,
            endringstype = Endringstype.RETTING
        )

        val opprinneligGrunnkretsEtter = grunnkretsService.hentGrunnkrets(identifikator)
        val grunnkretsEtter = grunnkretsService.hentGrunnkrets(Identifikator(identifikator.lokalid))

        assertThat(grunnkretsEtter!!.grunnkretsnummer).isNotEqualTo(grunnkrets.grunnkretsnummer)
            .isEqualTo(grunnkretsRequest.grunnkretsnummer)
        assertThat(grunnkretsEtter.grunnkretsnavn).isNotEqualTo(grunnkrets.grunnkretsnavn)
            .isEqualTo(grunnkretsRequest.navn)
        assertThat(grunnkretsEtter.omraade).isEqualTo(grunnkrets.omraade)
        assertThat(grunnkretsEtter.posisjon).isEqualTo(grunnkrets.posisjon)
        assertThat(grunnkretsEtter.gyldighet.gyldigFra).isEqualTo(now)

        assertThat(opprinneligGrunnkretsEtter!!.gyldighet.gyldigFra).isEqualTo(october1In2022)
        assertThat(opprinneligGrunnkretsEtter.gyldighet.gyldigTil).isEqualTo(now)

        val grenseObjIdsForOpprinneligGrunnkrets = opprinneligGrunnkrets!!.grenser.map { it.grense.id }
        val grenseObjIdsForOpprinneligGrunnkretsEtter = opprinneligGrunnkretsEtter.grenser.map { it.grense.id }
        val grenseObjIdsForNyGrunnkrets = grunnkretsEtter.grenser.map { it.grense.id }

        assertThat(grenseObjIdsForOpprinneligGrunnkrets).containsAll(grenseObjIdsForOpprinneligGrunnkretsEtter)
        assertThat(grenseObjIdsForOpprinneligGrunnkrets).containsAll(grenseObjIdsForNyGrunnkrets)
        assertThat(grenseObjIdsForNyGrunnkrets).containsAll(grenseObjIdsForOpprinneligGrunnkrets)
        assertThat(grenseObjIdsForNyGrunnkrets).containsAll(grenseObjIdsForOpprinneligGrunnkretsEtter)

        val grenser = grenseRepository.findAllById(grenseObjIdsForOpprinneligGrunnkrets)
        assertThat(grenser).hasSize(3)
        grenser.forEach { grense ->
            assertThat(grense.grunnkretser.map { it.grunnkrets.id }).hasSize(2).containsExactlyInAnyOrder(opprinneligGrunnkrets.id, grunnkretsEtter.id)
        }
    }

    @Test
    fun `oppdatering av grunnkrets til et allerede brukt grunnkretsnummer skal gi feil`() {
        val opprinneligDato = LocalDate.parse("2022-01-01")
        val datoForOppdatering = october1In2022
        val (vestreKrets, oestreKrets) = saveNaboGrunnkretser(gyldigFra = opprinneligDato)

        val grunnkretsRequest = GrunnkretsRequestBuilder().withLokalId(oestreKrets.lokalid).withGrunnkretsnummer(vestreKrets.grunnkretsnummer!!).build()
        val identifikator = Identifikator(Lokalid(grunnkretsRequest.identifikasjon.lokalid), datoForOppdatering)

        Assertions.assertThatThrownBy {
            grunnkretsService.oppdaterGrunnkrets(
                lokalId = identifikator.lokalid,
                oppdatertGrunnkrets = grunnkretsRequest,
                gyldigFra = datoForOppdatering,
                endringstype = Endringstype.RETTING
            )
        }.isInstanceOf(ResponseException.BadRequestException::class.java)
            .hasMessageContaining("Det finnes en annen grunnkrets med grunnkretsnummer " + vestreKrets.grunnkretsnummer +
                " innen gyldighet " + datoForOppdatering + " og framover")

    }
        @Test
    fun `endre grense for grunnkrets skal opprette nye versjoner for grensa og dens grunnkretser`() {
        val opprinneligDato = LocalDate.parse("2022-01-01")
        val endringsdato = LocalDate.parse("2022-02-01")
        val deltGrenseLokalid = "grenseDeltLokalid"
        val (vestreKrets, oestreKrets) = saveNaboGrunnkretser(gyldigFra = opprinneligDato)

        val deltGrense = grenseService.hentGrense(Identifikator(Lokalid(deltGrenseLokalid), opprinneligDato))
        assertThat(deltGrense!!.grunnkretser).hasSize(2)
        assertThat(deltGrense.grunnkretser.map { it.grunnkrets.id }).containsExactlyInAnyOrder(vestreKrets.id, oestreKrets.id)

        val flaterSomErEndret = utfoerGrenseendring(kretsSomSkalHaGrenseendring = vestreKrets, opprinneligDato = opprinneligDato, endringsdato = endringsdato)
        val opprinneligVestreKrets = grunnkretsService.hentGrunnkrets(Identifikator(Lokalid(vestreKrets.lokalid), opprinneligDato))
        val opprinneligOestreKrets = grunnkretsService.hentGrunnkrets(Identifikator(Lokalid(oestreKrets.lokalid), opprinneligDato))
        assertThat(opprinneligVestreKrets!!.gyldighet.gyldigTil).isEqualTo(endringsdato)
        assertThat(opprinneligOestreKrets!!.gyldighet.gyldigTil).isEqualTo(endringsdato)

        val vestreKretsEtterLagring = grunnkretsService.hentGrunnkrets(Identifikator(Lokalid(vestreKrets.lokalid), endringsdato))
        val oestreKretsEtterLagring = grunnkretsService.hentGrunnkrets(Identifikator(Lokalid(oestreKrets.lokalid), endringsdato))
        assertOmradeGyldighetVersjon(vestreKretsEtterLagring!!, oestreKretsEtterLagring!!, opprinneligVestreKrets, opprinneligOestreKrets)
        assertThat(flaterSomErEndret).containsExactlyInAnyOrder(vestreKretsEtterLagring, oestreKretsEtterLagring)

        val opprinneligDeltGrense = grenseService.hentGrense(Identifikator(Lokalid(deltGrenseLokalid), opprinneligDato))
        assertThat(opprinneligDeltGrense!!.grunnkretser).hasSize(2)
        assertThat(opprinneligDeltGrense.grunnkretser.map { it.grunnkrets.id })
            .containsExactlyInAnyOrder(opprinneligVestreKrets.id, opprinneligOestreKrets.id)

        assertDeltGrenseEtterGrenseendring(deltGrenseLokalid, endringsdato, opprinneligDeltGrense,
            vestreKretsEtterLagring, oestreKretsEtterLagring, opprinneligVestreKrets, opprinneligOestreKrets)
    }

    @Test
    fun `endre to grenser for grunnkrets skal opprette nye versjoner for grensene og deres grunnkretser`() {
        val opprinneligDato = LocalDate.parse("2022-01-01")
        val endringsdato = LocalDate.parse("2022-02-01")
        val deltGrenseLokalid = "grenseDeltLokalid"
        val (vestreKrets, oestreKrets) = saveNaboGrunnkretser(gyldigFra = opprinneligDato)

        val deltGrense = grenseService.hentGrense(Identifikator(Lokalid(deltGrenseLokalid), opprinneligDato))
        assertThat(deltGrense!!.grunnkretser).hasSize(2)
        assertThat(deltGrense.grunnkretser.map { it.grunnkrets.id }).containsExactlyInAnyOrder(vestreKrets.id, oestreKrets.id)

        val featureForAnnenGrense = lagFeatureEndreYkoordinat(vestreKrets, opprinneligDato, endringsdato, "grenseNordVestLokalid")
        val flaterSomErEndret = utfoerGrenseendring(kretsSomSkalHaGrenseendring = vestreKrets, opprinneligDato = opprinneligDato,
            endringsdato = endringsdato, featureForAnnenGrense)

        val opprinneligVestreKrets = grunnkretsService.hentGrunnkrets(Identifikator(Lokalid(vestreKrets.lokalid), opprinneligDato))
        val opprinneligOestreKrets = grunnkretsService.hentGrunnkrets(Identifikator(Lokalid(oestreKrets.lokalid), opprinneligDato))
        assertThat(opprinneligVestreKrets!!.gyldighet.gyldigTil).isEqualTo(endringsdato)
        assertThat(opprinneligOestreKrets!!.gyldighet.gyldigTil).isEqualTo(endringsdato)

        val vestreKretsEtterLagring = grunnkretsService.hentGrunnkrets(Identifikator(Lokalid(vestreKrets.lokalid), endringsdato))
        val oestreKretsEtterLagring = grunnkretsService.hentGrunnkrets(Identifikator(Lokalid(oestreKrets.lokalid), endringsdato))
        assertOmradeGyldighetVersjon(vestreKretsEtterLagring!!, oestreKretsEtterLagring!!, opprinneligVestreKrets, opprinneligOestreKrets)
        assertThat(flaterSomErEndret).containsExactlyInAnyOrder(vestreKretsEtterLagring, oestreKretsEtterLagring)

        val opprinneligDeltGrense = grenseService.hentGrense(Identifikator(Lokalid(deltGrenseLokalid), opprinneligDato))
        assertThat(opprinneligDeltGrense!!.grunnkretser).hasSize(2)
        assertThat(opprinneligDeltGrense.grunnkretser.map { it.grunnkrets.id })
            .containsExactlyInAnyOrder(opprinneligVestreKrets.id, opprinneligOestreKrets.id)

        assertDeltGrenseEtterGrenseendring(deltGrenseLokalid, endringsdato, opprinneligDeltGrense,
            vestreKretsEtterLagring, oestreKretsEtterLagring, opprinneligVestreKrets, opprinneligOestreKrets)

        assertAnnenGrenseEtterGrenseendring("grenseNordVest", "grenseNordVestLokalid", endringsdato, opprinneligDato,
            vestreKretsEtterLagring, opprinneligVestreKrets)
    }

    @Test
    fun `endre to grenser for to grunnkrets skal opprette nye versjoner for grensene og deres grunnkretser`() {
        val opprinneligDato = LocalDate.parse("2022-01-01")
        val endringsdato = LocalDate.parse("2022-02-01")
        val deltGrenseLokalid = "grenseDeltLokalid"
        val (vestreKrets, oestreKrets) = saveNaboGrunnkretser(gyldigFra = opprinneligDato)

        val deltGrense = grenseService.hentGrense(Identifikator(Lokalid(deltGrenseLokalid), opprinneligDato))
        assertThat(deltGrense!!.grunnkretser).hasSize(2)
        assertThat(deltGrense.grunnkretser.map { it.grunnkrets.id }).containsExactlyInAnyOrder(vestreKrets.id, oestreKrets.id)

        val featureForAnnenGrense = lagFeatureEndreYkoordinat(oestreKrets, opprinneligDato, endringsdato, "grenseNordØstLokalid")
        val flaterSomErEndret = utfoerGrenseendring(kretsSomSkalHaGrenseendring = vestreKrets, opprinneligDato = opprinneligDato,
            endringsdato = endringsdato, featureForAnnenGrense)

        val opprinneligVestreKrets = grunnkretsService.hentGrunnkrets(Identifikator(Lokalid(vestreKrets.lokalid), opprinneligDato))
        val opprinneligOestreKrets = grunnkretsService.hentGrunnkrets(Identifikator(Lokalid(oestreKrets.lokalid), opprinneligDato))
        assertThat(opprinneligVestreKrets!!.gyldighet.gyldigTil).isEqualTo(endringsdato)
        assertThat(opprinneligOestreKrets!!.gyldighet.gyldigTil).isEqualTo(endringsdato)

        val vestreKretsEtterLagring = grunnkretsService.hentGrunnkrets(Identifikator(Lokalid(vestreKrets.lokalid), endringsdato))
        val oestreKretsEtterLagring = grunnkretsService.hentGrunnkrets(Identifikator(Lokalid(oestreKrets.lokalid), endringsdato))
        assertOmradeGyldighetVersjon(vestreKretsEtterLagring!!, oestreKretsEtterLagring!!, opprinneligVestreKrets, opprinneligOestreKrets)
        assertThat(flaterSomErEndret).containsExactlyInAnyOrder(vestreKretsEtterLagring, oestreKretsEtterLagring)

        val opprinneligDeltGrense = grenseService.hentGrense(Identifikator(Lokalid(deltGrenseLokalid), opprinneligDato))
        assertThat(opprinneligDeltGrense!!.grunnkretser).hasSize(2)
        assertThat(opprinneligDeltGrense.grunnkretser.map { it.grunnkrets.id })
            .containsExactlyInAnyOrder(opprinneligVestreKrets.id, opprinneligOestreKrets.id)

        assertDeltGrenseEtterGrenseendring(deltGrenseLokalid, endringsdato, opprinneligDeltGrense,
            vestreKretsEtterLagring, oestreKretsEtterLagring, opprinneligVestreKrets, opprinneligOestreKrets)

        assertAnnenGrenseEtterGrenseendring("grenseNordØst", "grenseNordØstLokalid", endringsdato, opprinneligDato,
            oestreKretsEtterLagring, opprinneligOestreKrets)
    }

    @Test
    fun `endre delt grense etter oppdatering av grunnkrets skal opprette nye versjoner for grensa og dens grunnkretser`() {
        val opprinneligDato = LocalDate.parse("2022-01-01")
        val datoForOppdatering = october1In2022
        val datoForGrenseendring = LocalDate.parse("2023-02-01")
        val deltGrenseLokalid = "grenseDeltLokalid"
        val (vestreKrets, oestreKrets) = saveNaboGrunnkretser(gyldigFra = opprinneligDato)

        val vestreGrunnkretsEtterOppdatering = utfoerMetadataoppdatering(kretsSomSkalOppdateres = vestreKrets, datoForOppdatering = datoForOppdatering)

        val deltGrense = grenseService.hentGrense(Identifikator(Lokalid(deltGrenseLokalid), datoForOppdatering))
        assertThat(deltGrense!!.grunnkretser).hasSize(3)
        assertThat(deltGrense.grunnkretser.map { it.grunnkrets.id })
            .containsExactlyInAnyOrder(vestreKrets.id, oestreKrets.id, vestreGrunnkretsEtterOppdatering!!.id)

        val flaterSomErEndret = utfoerGrenseendring(kretsSomSkalHaGrenseendring = vestreKrets, opprinneligDato = datoForOppdatering,
            endringsdato = datoForGrenseendring)

        val opprinneligVestreKrets = grunnkretsService.hentGrunnkrets(Identifikator(Lokalid(vestreKrets.lokalid), datoForOppdatering))
        val opprinneligOestreKrets = grunnkretsService.hentGrunnkrets(Identifikator(Lokalid(oestreKrets.lokalid), datoForOppdatering))
        assertThat(opprinneligVestreKrets!!.gyldighet.gyldigTil).isEqualTo(datoForGrenseendring)
        assertThat(opprinneligOestreKrets!!.gyldighet.gyldigTil).isEqualTo(datoForGrenseendring)

        val vestreKretsEtterLagring = grunnkretsService.hentGrunnkrets(Identifikator(Lokalid(vestreKrets.lokalid), datoForGrenseendring))
        val oestreKretsEtterLagring = grunnkretsService.hentGrunnkrets(Identifikator(Lokalid(oestreKrets.lokalid), datoForGrenseendring))
        assertOmradeGyldighetVersjon(vestreKretsEtterLagring!!, oestreKretsEtterLagring!!, opprinneligVestreKrets, opprinneligOestreKrets)
        assertThat(flaterSomErEndret).containsExactlyInAnyOrder(vestreKretsEtterLagring, oestreKretsEtterLagring)

        val opprinneligDeltGrense = grenseService.hentGrense(Identifikator(Lokalid(deltGrenseLokalid), opprinneligDato))

        //Sjekker at gammel versjon av grensa ikke har fått endra sine grunnkretser
        assertThat(opprinneligDeltGrense!!.grunnkretser).hasSize(3)
        assertThat(opprinneligDeltGrense.grunnkretser.map { it.grunnkrets.id })
            .containsExactlyInAnyOrder(vestreKrets.id, oestreKrets.id, vestreGrunnkretsEtterOppdatering.id)

        assertDeltGrenseEtterGrenseendring(deltGrenseLokalid, datoForGrenseendring, opprinneligDeltGrense,
            vestreKretsEtterLagring, oestreKretsEtterLagring, opprinneligVestreKrets, opprinneligOestreKrets)
    }

    @Test
    fun `endre delt grense etter oppdatering av grunnkrets paa andre sida skal opprette nye versjoner for grensa og dens grunnkretser`() {
        val opprinneligDato = LocalDate.parse("2022-01-01")
        val datoForOppdatering = october1In2022
        val datoForGrenseendring = LocalDate.parse("2023-02-01")
        val deltGrenseLokalid = "grenseDeltLokalid"
        val (vestreKrets, oestreKrets) = saveNaboGrunnkretser(gyldigFra = opprinneligDato)

        val oestreGrunnkretsEtterOppdatering = utfoerMetadataoppdatering(kretsSomSkalOppdateres = oestreKrets, datoForOppdatering = datoForOppdatering)

        val deltGrense = grenseService.hentGrense(Identifikator(Lokalid(deltGrenseLokalid), datoForOppdatering))
        assertThat(deltGrense!!.grunnkretser).hasSize(3)
        assertThat(deltGrense.grunnkretser.map { it.grunnkrets.id })
            .containsExactlyInAnyOrder(vestreKrets.id, oestreKrets.id, oestreGrunnkretsEtterOppdatering!!.id)

        val flaterSomErEndret = utfoerGrenseendring(kretsSomSkalHaGrenseendring = vestreKrets, opprinneligDato = datoForOppdatering,
            endringsdato = datoForGrenseendring)

        val opprinneligVestreKrets = grunnkretsService.hentGrunnkrets(Identifikator(Lokalid(vestreKrets.lokalid), datoForOppdatering))
        val opprinneligOestreKrets = grunnkretsService.hentGrunnkrets(Identifikator(Lokalid(oestreKrets.lokalid), datoForOppdatering))
        assertThat(opprinneligVestreKrets!!.gyldighet.gyldigTil).isEqualTo(datoForGrenseendring)
        assertThat(opprinneligOestreKrets!!.gyldighet.gyldigTil).isEqualTo(datoForGrenseendring)

        val vestreKretsEtterLagring = grunnkretsService.hentGrunnkrets(Identifikator(Lokalid(vestreKrets.lokalid), datoForGrenseendring))
        val oestreKretsEtterLagring = grunnkretsService.hentGrunnkrets(Identifikator(Lokalid(oestreKrets.lokalid), datoForGrenseendring))
        assertOmradeGyldighetVersjon(vestreKretsEtterLagring!!, oestreKretsEtterLagring!!, opprinneligVestreKrets, opprinneligOestreKrets)
        assertThat(flaterSomErEndret).containsExactlyInAnyOrder(vestreKretsEtterLagring, oestreKretsEtterLagring)

        val opprinneligDeltGrense = grenseService.hentGrense(Identifikator(Lokalid(deltGrenseLokalid), opprinneligDato))

        //Sjekker at gammel versjon av grensa ikke har fått endra sine grunnkretser
        assertThat(opprinneligDeltGrense!!.grunnkretser).hasSize(3)
        assertThat(opprinneligDeltGrense.grunnkretser.map { it.grunnkrets.id })
            .containsExactlyInAnyOrder(vestreKrets.id, oestreKrets.id, oestreGrunnkretsEtterOppdatering.id)

        assertDeltGrenseEtterGrenseendring(deltGrenseLokalid, datoForGrenseendring, opprinneligDeltGrense,
            vestreKretsEtterLagring, oestreKretsEtterLagring, opprinneligVestreKrets, opprinneligOestreKrets)
    }

    @Test
    fun `endre metadata for grense skal lagres paa ny grense`() {
        val opprinneligDato = LocalDate.parse("2022-01-01")
        val endringsdato = LocalDate.parse("2022-02-01")
        val (_, oestreKrets) = saveNaboGrunnkretser(gyldigFra = opprinneligDato)

        val featureForAnnenGrense = lagFeatureEndreYkoordinat(oestreKrets, opprinneligDato, endringsdato, "grenseNordØstLokalid")
        val statistiskgrenseMetadata = featureForAnnenGrense.properties.metadata as StatistiskgrenseMetadata
        statistiskgrenseMetadata.common.informasjon = "En eller annen informasjon"
        statistiskgrenseMetadata.commonGrense.posisjonskvalitet =
            Posisjonskvalitet(maalemetode = KodelisteEntry(id = MaalemetodeKodeEnum.TERRENGMAALT.uuid, href = "maalemetodeHref"), noeyaktighet = 100)

        val flaterSomErEndret = grenseService.lagreGrenser(grenser = FeatureCollection(features = listOf(featureForAnnenGrense)),
            gyldighetsdato = endringsdato, endringstype = Endringstype.FASTSETTING
        )

        val opprinneligOestreKrets = grunnkretsService.hentGrunnkrets(Identifikator(Lokalid(oestreKrets.lokalid), opprinneligDato))
        assertThat(opprinneligOestreKrets!!.gyldighet.gyldigTil).isEqualTo(endringsdato)

        val oestreKretsEtterLagring = grunnkretsService.hentGrunnkrets(Identifikator(Lokalid(oestreKrets.lokalid), endringsdato))
        assertThat(oestreKretsEtterLagring!!.gyldighet.gyldigTil).isNull()
        assertThat(oestreKretsEtterLagring.omraade).isNotEqualTo(opprinneligOestreKrets.omraade)
        assertThat(oestreKretsEtterLagring.version).isGreaterThan(opprinneligOestreKrets.version)
        assertThat(oestreKretsEtterLagring.auditInfo.typeEndring).isEqualTo(Endringstype.FASTSETTING)

        assertThat(flaterSomErEndret).containsExactly(oestreKretsEtterLagring)
        assertAnnenGrenseEtterGrenseendring("grenseNordØst", "grenseNordØstLokalid", endringsdato, opprinneligDato,
            oestreKretsEtterLagring, opprinneligOestreKrets)

        val nyGrense = grenseService.hentGrense(Identifikator(Lokalid("grenseNordØstLokalid"), endringsdato))
        assertThat(nyGrense!!.kvalitet!!.noeyaktighet).isEqualTo(100)
        assertThat(nyGrense.kvalitet!!.maalemetode).isEqualTo(MaalemetodeKodeEnum.TERRENGMAALT.uuid)
        assertThat(nyGrense.informasjonstekst).isEqualTo("En eller annen informasjon")
        assertThat(nyGrense.auditInfo.typeEndring).isEqualTo(Endringstype.FASTSETTING)
    }

    private fun assertOmradeGyldighetVersjon(vestreKretsEtterLagring: Grunnkrets, oestreKretsEtterLagring: Grunnkrets,
                                             opprinneligVestreKrets: Grunnkrets, opprinneligOestreKrets: Grunnkrets) {
        assertThat(vestreKretsEtterLagring.gyldighet.gyldigTil).isNull()
        assertThat(oestreKretsEtterLagring.gyldighet.gyldigTil).isNull()
        assertThat(vestreKretsEtterLagring.omraade).isNotEqualTo(opprinneligVestreKrets.omraade)
        assertThat(oestreKretsEtterLagring.omraade).isNotEqualTo(opprinneligOestreKrets.omraade)
        assertThat(vestreKretsEtterLagring.version).isGreaterThan(opprinneligVestreKrets.version)
        assertThat(oestreKretsEtterLagring.version).isGreaterThan(opprinneligOestreKrets.version)
        assertThat(Endringstype.VEDTATT_GRENSEJUSTERING).isEqualTo(vestreKretsEtterLagring.auditInfo.typeEndring)
        assertThat(Endringstype.VEDTATT_GRENSEJUSTERING).isEqualTo(oestreKretsEtterLagring.auditInfo.typeEndring)
    }

    private fun assertDeltGrenseEtterGrenseendring(
        @Suppress("SameParameterValue") deltGrenseLokalid: String, datoForGrenseendring: LocalDate, opprinneligGrense: Grense,
        vestreKretsEtterLagring: Grunnkrets, oestreKretsEtterLagring: Grunnkrets, opprinneligVestreKrets: Grunnkrets, opprinneligOestreKrets: Grunnkrets) {

        val deltGrenseEtterGrenseendring = grenseService.hentGrense(Identifikator(Lokalid(deltGrenseLokalid), datoForGrenseendring))
        assertThat(deltGrenseEtterGrenseendring!!.grunnkretser).hasSize(2)
        assertThat(deltGrenseEtterGrenseendring.grunnkretser.map { it.grunnkrets.id })
            .containsExactlyInAnyOrder(vestreKretsEtterLagring.id, oestreKretsEtterLagring.id)

        assertThat(opprinneligGrense.gyldighet.gyldigTil).isEqualTo(datoForGrenseendring)
        assertThat(deltGrenseEtterGrenseendring.gyldighet.gyldigTil).isNull()
        assertThat(opprinneligGrense.lokalid).isEqualTo(deltGrenseEtterGrenseendring.lokalid)
        assertThat(opprinneligGrense.grense).isNotEqualTo(deltGrenseEtterGrenseendring.grense)
        assertThat(deltGrenseEtterGrenseendring.version).isGreaterThan(opprinneligGrense.version)

        assertThat(opprinneligVestreKrets.grenser.map { it.grense.id }).contains("grenseDelt")
        assertThat(opprinneligOestreKrets.grenser.map { it.grense.id }).contains("grenseDelt")
        assertThat(vestreKretsEtterLagring.grenser.map { it.grense.id }).doesNotContain("grenseDelt")
        assertThat(oestreKretsEtterLagring.grenser.map { it.grense.id }).doesNotContain("grenseDelt")
        assertThat(vestreKretsEtterLagring.grenser.map { it.grense.lokalid }).contains(deltGrenseLokalid)
        assertThat(oestreKretsEtterLagring.grenser.map { it.grense.lokalid }).contains(deltGrenseLokalid)
    }

    private fun assertAnnenGrenseEtterGrenseendring(annenGrenseId: String, annenGrenseLokalid: String, datoForGrenseendring: LocalDate,
                                                    opprinneligDato: LocalDate, kretsEtterLagring: Grunnkrets, opprinneligKrets: Grunnkrets) {
        val opprinneligGrense = grenseService.hentGrense(Identifikator(Lokalid(annenGrenseLokalid), opprinneligDato))
        assertThat(opprinneligGrense!!.grunnkretser).hasSize(1)
        assertThat(opprinneligGrense.grunnkretser.map { it.grunnkrets.id }).containsExactlyInAnyOrder(opprinneligKrets.id)

        val annenGrenseEtterGrenseendring = grenseService.hentGrense(Identifikator(Lokalid(annenGrenseLokalid), datoForGrenseendring))
        assertThat(annenGrenseEtterGrenseendring!!.grunnkretser).hasSize(1)
        assertThat(annenGrenseEtterGrenseendring.grunnkretser.map { it.grunnkrets.id }).containsExactlyInAnyOrder(kretsEtterLagring.id)

        assertThat(opprinneligGrense.gyldighet.gyldigTil).isEqualTo(datoForGrenseendring)
        assertThat(annenGrenseEtterGrenseendring.gyldighet.gyldigTil).isNull()
        assertThat(opprinneligGrense.lokalid).isEqualTo(annenGrenseEtterGrenseendring.lokalid)
        assertThat(opprinneligGrense.grense).isNotEqualTo(annenGrenseEtterGrenseendring.grense)
        assertThat(annenGrenseEtterGrenseendring.version).isGreaterThan(opprinneligGrense.version)

        assertThat(opprinneligKrets.grenser.map { it.grense.id }).contains(annenGrenseId)
        assertThat(kretsEtterLagring.grenser.map { it.grense.id }).doesNotContain(annenGrenseId)
        assertThat(kretsEtterLagring.grenser.map { it.grense.lokalid }).contains(annenGrenseLokalid)
    }

    private fun utfoerMetadataoppdatering(kretsSomSkalOppdateres: Grunnkrets, datoForOppdatering: LocalDate): Grunnkrets? {
        val grunnkretsRequest = GrunnkretsRequestBuilder().withLokalId(kretsSomSkalOppdateres.lokalid).build()
        val identifikator = Identifikator(Lokalid(grunnkretsRequest.identifikasjon.lokalid), datoForOppdatering)

        grunnkretsService.oppdaterGrunnkrets(
            lokalId = identifikator.lokalid,
            oppdatertGrunnkrets = grunnkretsRequest,
            gyldigFra = datoForOppdatering,
            endringstype = Endringstype.RETTING
        )
        return grunnkretsService.hentGrunnkrets(identifikator)
    }

    private fun utfoerGrenseendring(kretsSomSkalHaGrenseendring: Grunnkrets, opprinneligDato: LocalDate, endringsdato: LocalDate,
                                    featureAnnenOppdatertGrense: Feature? = null): Set<Flate> {
        val grenseKobling = kretsSomSkalHaGrenseendring.getGrenser(gyldighetsdato = opprinneligDato)
            .first { it.grense.lokalid == "grenseDeltLokalid" }

        val oppdatertGeometri = GeometryFactory().createLineString(arrayOf(
            Coordinate(10.0, 10.0),
            Coordinate(5.0, 5.0),
            Coordinate(10.0, 0.0)
        ))

        val oppdatertGrensefeature = FeatureBuilder(id = "grenseDeltLokalid")
            .withKontekstType(KontekstEnhetType.GRUNNKRETS)
            .withGrenseversjon(grenseKobling.grense.version)
            .withKontekstId(
                kontekstId = kretsSomSkalHaGrenseendring.lokalid,
                gyldighetsdato = endringsdato)
            .withGrensetype(Grensetype.Grunnkretsgrense)
            .withGeometry(oppdatertGeometri)
            .withRekkefoelge(grenseKobling.grenseKoblingAttributter.rekkefoelge)
            .withRetning(grenseKobling.grenseKoblingAttributter.retningMedKlokken)
            .build()

        val features = if (featureAnnenOppdatertGrense != null) listOf(oppdatertGrensefeature, featureAnnenOppdatertGrense) else listOf(oppdatertGrensefeature)
        return grenseService.lagreGrenser(
            grenser = FeatureCollection(features = features),
            gyldighetsdato = endringsdato,
            endringstype = Endringstype.VEDTATT_GRENSEJUSTERING
        )
    }

    private fun lagFeatureEndreYkoordinat(kretsSomSkalHaGrenseendring: Grunnkrets, opprinneligDato: LocalDate, endringsdato: LocalDate,
                                          lokalidGrenseSomSkalEndres: String): Feature {
        val grenseKobling = kretsSomSkalHaGrenseendring.getGrenser(gyldighetsdato = opprinneligDato)
            .first { it.grense.lokalid == lokalidGrenseSomSkalEndres }
        val grense = grenseKobling.grense.grense
        val oppdatertGeometri = GeometryFactory().createLineString(arrayOf(
            grense.getCoordinateN(0).copy(),
            grense.getCoordinateN(1).copy().also { it.setY(it.getY() + 4) },
            grense.getCoordinateN(2).copy()
        ))

        return FeatureBuilder(id = lokalidGrenseSomSkalEndres)
            .withKontekstType(KontekstEnhetType.GRUNNKRETS)
            .withGrenseversjon(grenseKobling.grense.version)
            .withKontekstId(kontekstId = kretsSomSkalHaGrenseendring.lokalid, gyldighetsdato = endringsdato)
            .withGrensetype(Grensetype.Grunnkretsgrense)
            .withGeometry(oppdatertGeometri)
            .withRekkefoelge(grenseKobling.grenseKoblingAttributter.rekkefoelge)
            .withRetning(grenseKobling.grenseKoblingAttributter.retningMedKlokken)
            .build()
    }

    private fun lagGrenserForGrunnkrets(grunnkretsLokalid: Lokalid, kommune: Kommune = KommuneBuilder.DEFAULT_KOMMUNE): Grunnkrets {
        val x1 = 23857.0
        val y1 = 675635.0

        val y2 = 675635.0
        val x2 = 23867.0

        val x3 = 23862.0
        val y3 = 675645.0

        val grense1 = grenseRepository.save(GrenseBuilder(id = "1", lokalId = Lokalid("850"), gyldigFra = october1In2022)
            .withGrenseGeometri(listOf(Coordinate(x1, y1), Coordinate(x2, y2)))
            .buildGrunnkretsgrense())

        val grense2 = grenseRepository.save(GrenseBuilder(id = "2", lokalId = Lokalid("851"), gyldigFra = october1In2022)
            .withGrenseGeometri(listOf(Coordinate(x2, y2), Coordinate(x3, y3)))
            .buildGrunnkretsgrense())

        val grense3 = grenseRepository.save(GrenseBuilder(id = "3", lokalId = Lokalid("852"), gyldigFra = october1In2022)
            .withGrenseGeometri(listOf(Coordinate(x3, y3), Coordinate(x1, y1)))
            .buildGrunnkretsgrense())

        grenseRepository.flush()

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
                               grunnkretsnummer: String = "42270304",
                               grunnkretsnavn: String = "Dåsvatn",
                               kommune: Kommune = KommuneBuilder.DEFAULT_KOMMUNE,
                               grenser: List<Grense> = emptyList()): Grunnkrets {
        return grunnkretsRepository.save(GrunnkretsBuilder(
            id = id,
            lokalId = lokalid,
            gyldigFra = gyldigFra,
            gyldigTil = gyldigTil,
            grunnkretsnummer = grunnkretsnummer,
            grunnkretsnavn = grunnkretsnavn,
            kommune = kommune,
            grenser = grenser
        ).build())
    }

    private fun saveGrunnkretsMed1FramtidigVersjon(grunnkretsLokalid: Lokalid) {
        val grunnkrets = saveGrunnkrets(
            id = "9000",
            lokalid = grunnkretsLokalid,
            gyldigFra = october1In2022,
            gyldigTil = jan1In2023
        )
        val grunnkretsFramtidigVersjon = saveGrunnkrets(
            id = "9001",
            lokalid = grunnkretsLokalid,
            gyldigFra = jan1In2023,
            grunnkretsnavn = "Måsvann",
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

    private fun saveNaboGrunnkretser(gyldigFra: LocalDate, medHull: Boolean = false): List<Grunnkrets> {
        val grenserForVest = if (medHull) {
            GrenseHelper.getGrenserForVestligKrets(gyldigFra, Grensetype.Grunnkretsgrense) +
                GrenseHelper.getHullVestligKrets(gyldigFra, Grensetype.Grunnkretsgrense)
        } else {
            GrenseHelper.getGrenserForVestligKrets(gyldigFra, Grensetype.Grunnkretsgrense)
        }

        //Lagrer grensene først, for å være sikker på at de er tilgjengelige for å knyttes til kretsene
        val lagredeGrenserForVest = grenseRepository.saveAll(grenserForVest)
        val lagredeGrenserForOest = grenseRepository.saveAll(GrenseHelper.getGrenserForOestligKrets(gyldigFra, Grensetype.Grunnkretsgrense))
        grenseRepository.flush()

        val kretsVest = saveGrunnkrets(
            id = "1",
            lokalid = Lokalid("123"),
            gyldigFra = gyldigFra,
            grunnkretsnummer = "03010020",
            grunnkretsnavn = "Vestre krets",
            grenser = lagredeGrenserForVest
        )

        val kretsOest = saveGrunnkrets(
            id = "2",
            lokalid = Lokalid("234"),
            gyldigFra = gyldigFra,
            grunnkretsnummer = "03010021",
            grunnkretsnavn = "Østre krets",
            grenser = lagredeGrenserForOest
        )

        saveKommune(grunnkretser = listOf(kretsVest, kretsOest))
        grenseRepository.flush()
        return listOf(kretsVest, kretsOest)
    }

}
