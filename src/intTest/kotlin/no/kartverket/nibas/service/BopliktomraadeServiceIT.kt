package no.kartverket.nibas.service

import com.ninjasquad.springmockk.MockkBean
import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.api.v1.common.geojson.FeatureCollection
import no.kartverket.nibas.api.v1.common.geojson.metadata.KodelisteEntry
import no.kartverket.nibas.api.v1.common.geojson.metadata.KommunalKretsgrenseMetadata
import no.kartverket.nibas.api.v1.common.geojson.metadata.Posisjonskvalitet
import no.kartverket.nibas.builders.BopliktomraadeRequestBuilder
import no.kartverket.nibas.builders.MaalemetodeKodeEnum
import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.common.ResponseException
import no.kartverket.nibas.domain.Bopliktomraade
import no.kartverket.nibas.domain.BopliktomraadeGrenser
import no.kartverket.nibas.domain.Endringstype
import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.domain.MaterieltVilkaar
import no.kartverket.nibas.kodelister.KodelisteService
import no.kartverket.nibas.testcontainer.TestWithDbContainer
import no.kartverket.nibas.testcontainer.assertGrenser
import no.kartverket.nibas.utils.GrenseHelper
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.locationtech.jts.geom.util.AffineTransformation
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.Month

@ActiveProfiles("localhost")
@Transactional
class BopliktomraadeServiceIT : TestWithDbContainer() {

    @MockkBean
    private lateinit var kodelisteServiceMock: KodelisteService

    private val october1In2022 = LocalDate.of(2022, Month.OCTOBER, 1)
    private val november28In2022 = LocalDate.of(2022, Month.NOVEMBER, 28)
    private val december1In2022 = LocalDate.of(2022, Month.DECEMBER, 1)
    private val jan1In2023 = LocalDate.of(2023, Month.JANUARY, 1)
    private val jan1In2024 = LocalDate.of(2024, Month.JANUARY, 1)

    private val bopliktomraadeLokalid = Lokalid("ac7e05cd-fb07")
    private val grenseLokallid = Lokalid("888")

    @Test
    fun `hent eksisterende bopliktomraade med dagens dato`() {
        val bopliktomraade = saveBopliktomraade(lokalid = bopliktomraadeLokalid)
        saveKommuneOgUnikLokalid(bopliktomraader = listOf(bopliktomraade))

        assertThat(bopliktomraadeRepository.findAllByLokalid(bopliktomraadeLokalid.value)).hasSize(1)

        // Later som 28.11.22 er dagens dato. Det fungerer så lenge den er senere enn 1.10.22
        assertThat(bopliktomraadeService.hentBopliktomraade(Identifikator(bopliktomraadeLokalid, november28In2022))).isNotNull
    }

    @Test
    fun `hent versjon av bopliktomraade fram i tid`() {
        saveBopliktomraadeMed1FramtidigVersjon(bopliktomraadeLokalid)
        assertThat(bopliktomraadeRepository.findAllByLokalid(bopliktomraadeLokalid.value)).hasSize(2)

        val framtidigBopliktomraade = bopliktomraadeService.hentBopliktomraade(Identifikator(bopliktomraadeLokalid, jan1In2023))!!
        assertThat(framtidigBopliktomraade.navn).isEqualTo("Nedre Singsaker")
    }

    @Test
    fun `hent antall framtidige versjoner`() {
        saveBopliktomraadeMed1FramtidigVersjon(bopliktomraadeLokalid)
        assertThat(bopliktomraadeService.hentAntallGyldigeVersjonerFramITidFor(Identifikator(bopliktomraadeLokalid, november28In2022))).isEqualTo(1)
    }

    @Test
    fun `hent grenser for bopliktomraade fram i tid`() {
        val identifikatorFor1Jan23 = Identifikator(bopliktomraadeLokalid, jan1In2023)

        val grense3 = saveGrense(id = "4002", lokalid = grenseLokallid, gyldigFra = jan1In2024, grensetype = Grensetype.Bopliktgrense)
        val grense2 = saveGrense(id = "4001", lokalid = grenseLokallid, gyldigFra = jan1In2023, gyldigTil = jan1In2024, grensetype = Grensetype.Bopliktgrense)
        val grense1 = saveGrense(id = "4000", lokalid = grenseLokallid, gyldigFra = october1In2022, gyldigTil = jan1In2023, grensetype = Grensetype.Bopliktgrense)

        val bopliktomraade1 = saveBopliktomraade(id = "301", lokalid = bopliktomraadeLokalid, gyldigFra = october1In2022, gyldigTil = december1In2022,
            grenser = listOf(grense1))
        val bopliktomraade2 = saveBopliktomraade(id = "302", lokalid = bopliktomraadeLokalid, gyldigFra = december1In2022, gyldigTil = jan1In2024,
            grenser = listOf(grense1, grense2))
        val bopliktomraade3 = saveBopliktomraade(id = "303", lokalid = bopliktomraadeLokalid, gyldigFra = jan1In2024, grenser = listOf(grense3))

        saveKommuneOgUnikLokalid(bopliktomraader = listOf(bopliktomraade1, bopliktomraade2, bopliktomraade3))

        val grensekoblinger = bopliktomraadeService.hentGrensekoblingerForBopliktomraade(identifikatorFor1Jan23)

        assertGrenser(
            grensekoblinger,
            grenseIGrensekoblingId = "4001",
            enhetKobletTilGrenseLokalid = bopliktomraadeLokalid)
    }

    @Test
    fun `hentFramtidigeVersjonerForBopliktomraade skal returnere en liste hvis det finnes nyere versjoner`() {
        saveBopliktomraade(id = "301", lokalid = bopliktomraadeLokalid, gyldigFra = october1In2022, gyldigTil = december1In2022)
        saveBopliktomraade(id = "302", lokalid = bopliktomraadeLokalid, gyldigFra = december1In2022, gyldigTil = jan1In2024)
        saveBopliktomraade(id = "303", lokalid = bopliktomraadeLokalid, gyldigFra = jan1In2024)

        assertThat(bopliktomraadeService.hentFramtidigeVersjonerForBopliktomraade(bopliktomraadeLokalid, december1In2022)).hasSize(2)
    }

    @Test
    fun `hentFramtidigeVersjonerForBopliktomraade skal returnere en liste med kun det gyldige elementet om det er ingen framtidige endringer`() {
        saveBopliktomraade(id = "301", lokalid = bopliktomraadeLokalid, gyldigFra = october1In2022, gyldigTil = december1In2022)
        saveBopliktomraade(id = "302", lokalid = bopliktomraadeLokalid, gyldigFra = december1In2022, gyldigTil = jan1In2024)

        assertThat(bopliktomraadeService.hentFramtidigeVersjonerForBopliktomraade(bopliktomraadeLokalid, december1In2022)).hasSize(1)
    }

    @Test
    fun `oppdaterBopliktomraade skal lage nytt bopliktomraade og sette gyldigtil for gammel krets`() {
        val bopliktomraadeRequest = BopliktomraadeRequestBuilder.DEFAULT_REQUEST
        assertThat(bopliktomraadeRequest.url).isNotNull()
        assertThat(bopliktomraadeRequest.forskriftsreferanse).isNotNull()
        assertThat(bopliktomraadeRequest.delvisBoplikt).isTrue()

        val identifikator = Identifikator(Lokalid(bopliktomraadeRequest.identifikasjon.lokalid), october1In2022)
        val lagredeGrenserForKrets =
            grenseRepository.saveAll(GrenseHelper.getGrenserForOestligKrets(october1In2022, Grensetype.Bopliktgrense))
        grenseRepository.flush()

        val opprinneligBopliktomraade = saveBopliktomraade(
            id = "2",
            lokalid = identifikator.lokalid,
            gyldigFra = october1In2022,
            grenser = lagredeGrenserForKrets
        )

        opprinneligBopliktomraade.bebygdEiendom = true
        opprinneligBopliktomraade.ubebygdTomt = true
        bopliktomraadeRepository.save(opprinneligBopliktomraade)

        val opprinneligeVilkaar = setOf(MaterieltVilkaar.BEBYGDEIENDOM, MaterieltVilkaar.UBEBYGDTOMT)
        assertThat(opprinneligBopliktomraade.materielleVilkaar).containsExactlyInAnyOrderElementsOf(opprinneligeVilkaar)

        saveKommuneOgUnikLokalid(kommunenummerUuid = "4227", bopliktomraader = listOf(opprinneligBopliktomraade))

        val now = LocalDate.now()
        assertThat (bopliktomraadeService.hentBopliktomraade(identifikator)).isSameAs(opprinneligBopliktomraade)

        bopliktomraadeService.oppdaterBopliktomraade(
            lokalId = identifikator.lokalid,
            oppdatertBopliktomraade = bopliktomraadeRequest,
            gyldigFra = now,
            endringstype = Endringstype.RETTING
        )

        val bopliktomraadeEtter = bopliktomraadeService.hentBopliktomraade(Identifikator(identifikator.lokalid))!!

        assertThat(bopliktomraadeEtter.nummer).isNotEqualTo(opprinneligBopliktomraade.nummer)
            .isEqualTo(bopliktomraadeRequest.nummer)
        assertThat(bopliktomraadeEtter.navn).isNotEqualTo(opprinneligBopliktomraade.navn)
            .isEqualTo(bopliktomraadeRequest.navn)
        assertThat(bopliktomraadeEtter.omraade).isEqualTo(opprinneligBopliktomraade.omraade)
        assertThat(bopliktomraadeEtter.posisjon).isEqualTo(opprinneligBopliktomraade.posisjon)
        assertThat(bopliktomraadeEtter.gyldighet.gyldigFra).isEqualTo(now)
        assertThat(bopliktomraadeEtter.url).isNotNull().isNotEqualTo(opprinneligBopliktomraade.url).isEqualTo(bopliktomraadeRequest.url)
        assertThat(bopliktomraadeEtter.forskriftsreferanse).isNotNull().isNotEqualTo(opprinneligBopliktomraade.forskriftsreferanse)
            .isEqualTo(bopliktomraadeRequest.forskriftsreferanse)
        assertThat(bopliktomraadeEtter.delvisBoplikt).isEqualTo(opprinneligBopliktomraade.delvisBoplikt)
        assertThat(bopliktomraadeEtter.usikkerAvgrensning).isTrue()

        assertThat(opprinneligBopliktomraade.gyldighet.gyldigFra).isEqualTo(october1In2022)
        assertThat(opprinneligBopliktomraade.gyldighet.gyldigTil).isEqualTo(now)

        val opprinnligeGrenser = opprinneligBopliktomraade.bopliktomraadeGrenser
            as? BopliktomraadeGrenser.DelvisBoplikt
            ?: throw IllegalStateException("Forventet delvis boplikt")

        assertThat(opprinneligBopliktomraade.materielleVilkaar)
            .containsExactlyInAnyOrderElementsOf(opprinneligeVilkaar)

        assertThat(bopliktomraadeEtter.materielleVilkaar)
            .allMatch { it in MaterieltVilkaar.entries }

        assertThat(bopliktomraadeEtter.bebygdEiendom)
            .isEqualTo(MaterieltVilkaar.BEBYGDEIENDOM in bopliktomraadeEtter.materielleVilkaar)
        assertThat(bopliktomraadeEtter.ikkeHelaarsBoligUnderOppforing)
            .isEqualTo(MaterieltVilkaar.IKKEHELAARSBOLIGUNDEROPPFORING in bopliktomraadeEtter.materielleVilkaar)
        assertThat(bopliktomraadeEtter.ubebygdTomt)
            .isEqualTo(MaterieltVilkaar.UBEBYGDTOMT in bopliktomraadeEtter.materielleVilkaar)
        assertThat(bopliktomraadeEtter.unntakFraSlektskapsunntak)
            .isEqualTo(MaterieltVilkaar.UNNTAKFRASLEKTSKAPSUNNTAK in bopliktomraadeEtter.materielleVilkaar)

        val etterGrenser = bopliktomraadeEtter.bopliktomraadeGrenser
            as? BopliktomraadeGrenser.DelvisBoplikt
            ?: throw IllegalStateException("Forventet delvis boplikt")

        val grenseObjIdsForOpprinneligBopliktomraade = opprinnligeGrenser.grenser.map { it.grense.id }
        val grenseObjIdsForNyBopliktomraade = etterGrenser.grenser.map { it.grense.id }

        assertThat(grenseObjIdsForOpprinneligBopliktomraade).containsExactlyInAnyOrderElementsOf(grenseObjIdsForNyBopliktomraade)

        val grenser = grenseRepository.findAllById(grenseObjIdsForOpprinneligBopliktomraade)
        assertThat(grenser).hasSize(4)
        grenser.forEach { grense ->
            assertThat(grense.bopliktomraader.map { it.bopliktomraade.id }).hasSize(2)
                .containsExactlyInAnyOrder(opprinneligBopliktomraade.id, bopliktomraadeEtter.id)
        }
    }

    @Test
    fun `oppdaterBopliktomraade skal kunne blanke ut felt`() {
        val bopliktomraadeRequest = BopliktomraadeRequestBuilder.DEFAULT_REQUEST.copy(url = null, forskriftsreferanse = null, informasjon = null)

        val identifikator = Identifikator(Lokalid(bopliktomraadeRequest.identifikasjon.lokalid), october1In2022)
        val lagredeGrenserForKrets = grenseRepository.saveAll(GrenseHelper.getGrenserForOestligKrets(october1In2022, Grensetype.Bopliktgrense))
        grenseRepository.flush()

        val bopliktomraade = saveBopliktomraade(
            id = "2",
            lokalid = identifikator.lokalid,
            gyldigFra = october1In2022,
            grenser = lagredeGrenserForKrets
        )
        assertThat(bopliktomraade.url).isNotNull()
        assertThat(bopliktomraade.forskriftsreferanse).isNotNull()
        assertThat(bopliktomraade.informasjon).isNotNull()

        saveKommuneOgUnikLokalid(kommunenummerUuid = "4227", bopliktomraader = listOf(bopliktomraade))

        val now = LocalDate.now()
        val opprinneligBopliktomraade = bopliktomraadeService.hentBopliktomraade(identifikator)!!

        bopliktomraadeService.oppdaterBopliktomraade(
            lokalId = identifikator.lokalid,
            oppdatertBopliktomraade = bopliktomraadeRequest,
            gyldigFra = now,
            endringstype = Endringstype.RETTING
        )

        val opprinneligBopliktomraadeEtter = bopliktomraadeService.hentBopliktomraade(identifikator)!!
        val bopliktomraadeEtter = bopliktomraadeService.hentBopliktomraade(Identifikator(identifikator.lokalid))!!

        assertThat(bopliktomraadeEtter.nummer).isNotEqualTo(bopliktomraade.nummer)
            .isEqualTo(bopliktomraadeRequest.nummer)
        assertThat(bopliktomraadeEtter.navn).isNotEqualTo(bopliktomraade.navn)
            .isEqualTo(bopliktomraadeRequest.navn)
        assertThat(bopliktomraadeEtter.omraade).isEqualTo(bopliktomraade.omraade)
        assertThat(bopliktomraadeEtter.posisjon).isEqualTo(bopliktomraade.posisjon)
        assertThat(bopliktomraadeEtter.gyldighet.gyldigFra).isEqualTo(now)
        assertThat(bopliktomraadeEtter.url).isNull()
        assertThat(bopliktomraadeEtter.forskriftsreferanse).isNull()
        assertThat(bopliktomraadeEtter.informasjon).isNull()

        assertThat(opprinneligBopliktomraadeEtter.gyldighet.gyldigFra).isEqualTo(october1In2022)
        assertThat(opprinneligBopliktomraadeEtter.gyldighet.gyldigTil).isEqualTo(now)
        assertThat(opprinneligBopliktomraadeEtter.url).isNotNull()
        assertThat(opprinneligBopliktomraadeEtter.forskriftsreferanse).isNotNull()


        val opprinnligeGrenser = opprinneligBopliktomraade.bopliktomraadeGrenser
            as? BopliktomraadeGrenser.DelvisBoplikt
            ?: throw IllegalStateException("Forventet delvis boplikt")

        val etterGrenser = bopliktomraadeEtter.bopliktomraadeGrenser
            as? BopliktomraadeGrenser.DelvisBoplikt
            ?: throw IllegalStateException("Forventet delvis boplikt")

        val grenseObjIdsForOpprinneligBopliktomraade = opprinnligeGrenser.grenser.map { it.grense.id }
        val grenseObjIdsForOpprinneligBopliktomraadeEtter = etterGrenser.grenser.map { it.grense.id }
        val grenseObjIdsForNyBopliktomraade = etterGrenser.grenser.map { it.grense.id }

        assertThat(grenseObjIdsForOpprinneligBopliktomraade).containsAll(grenseObjIdsForOpprinneligBopliktomraadeEtter)
        assertThat(grenseObjIdsForOpprinneligBopliktomraade).containsAll(grenseObjIdsForNyBopliktomraade)
        assertThat(grenseObjIdsForNyBopliktomraade).containsAll(grenseObjIdsForOpprinneligBopliktomraade)
        assertThat(grenseObjIdsForNyBopliktomraade).containsAll(grenseObjIdsForOpprinneligBopliktomraadeEtter)

        val grenser = grenseRepository.findAllById(grenseObjIdsForOpprinneligBopliktomraade)
        assertThat(grenser).hasSize(4)
        grenser.forEach { grense ->
            assertThat(grense.bopliktomraader.map { it.bopliktomraade.id }).hasSize(2).containsExactlyInAnyOrder(opprinneligBopliktomraade.id, bopliktomraadeEtter.id)
        }
    }

    @Test
    fun `oppdaterBopliktomraade skal kaste exception ved ugyldig kommune for bopliktomraadet`() {
        val bopliktomraadeRequest = BopliktomraadeRequestBuilder.DEFAULT_REQUEST

        val identifikator = Identifikator(Lokalid(bopliktomraadeRequest.identifikasjon.lokalid), october1In2022)
        val lagredeGrenserForKrets = grenseRepository.saveAll(GrenseHelper.getGrenserForOestligKrets(october1In2022, Grensetype.Bopliktgrense))
        grenseRepository.flush()

        val bopliktomraade = saveBopliktomraade(
            id = "2",
            lokalid = identifikator.lokalid,
            gyldigFra = october1In2022,
            grenser = lagredeGrenserForKrets
        )

        //Kommunen har gyldig fra etter bopliktområdets gyldig fra, og gyldig til før ønsket oppdateringsdato
        saveKommuneOgUnikLokalid(kommunenummerUuid = "4227", gyldigFra = november28In2022, gyldigTil = jan1In2023, bopliktomraader = listOf(bopliktomraade))
        val now = LocalDate.now()

        assertThatThrownBy {
            bopliktomraadeService.oppdaterBopliktomraade(
                lokalId = identifikator.lokalid,
                oppdatertBopliktomraade = bopliktomraadeRequest,
                gyldigFra = now,
                endringstype = Endringstype.RETTING
            )
        }.isInstanceOf(ResponseException.NotFoundException::class.java)
            .hasMessageContaining("Fant ikke kommune for bopliktområde med id ${bopliktomraade.id}")

        assertThatThrownBy {
            bopliktomraadeService.createNyVersjonAvBopliktomraade(bopliktomraade = bopliktomraade, gyldigFra = now, endringstype = Endringstype.RETTING)
        }.isInstanceOf(ResponseException.NotFoundException::class.java)
            .hasMessageContaining("Fant ikke kommune for bopliktområde med id ${bopliktomraade.id}")

    }

    @Test
    fun `endre metadata for grense skal lagres paa ny grense`() {
        val opprinneligDato = LocalDate.parse("2022-01-01")
        val endringsdato = LocalDate.parse("2022-02-01")

        val lagredeGrenserForKrets = grenseRepository.saveAll(GrenseHelper.getGrenserForOestligKrets(opprinneligDato, Grensetype.Bopliktgrense))
        grenseRepository.flush()

        val bopliktomraade = saveBopliktomraade(
            id = "2",
            lokalid = Lokalid("22"),
            gyldigFra = opprinneligDato,
            grenser = lagredeGrenserForKrets
        )
        saveKommuneOgUnikLokalid(kommunenummerUuid = "4227", bopliktomraader = listOf(bopliktomraade), gyldigFra = opprinneligDato)

        val featureForAnnenGrense = lagFeatureEndreYkoordinat(bopliktomraade, opprinneligDato, "grenseDeltLokalid")
        val kommunalKretsgrenseMetadata = featureForAnnenGrense.properties.metadata as KommunalKretsgrenseMetadata
        kommunalKretsgrenseMetadata.common.informasjon = "En eller annen informasjon"
        kommunalKretsgrenseMetadata.commonGrense.posisjonskvalitet =
            Posisjonskvalitet(maalemetode = KodelisteEntry(id = MaalemetodeKodeEnum.TERRENGMAALT.uuid, href = "maalemetodeHref"), noeyaktighet = 100)

        val flaterSomErEndret = grenseService.lagreGrenser(grenser = FeatureCollection(features = listOf(featureForAnnenGrense)),
            gyldighetsdato = endringsdato, endringstype = Endringstype.FASTSETTING
        )

        val opprinneligKrets = bopliktomraadeService.hentBopliktomraade(Identifikator(Lokalid(bopliktomraade.lokalid), opprinneligDato))!!
        assertThat(opprinneligKrets.gyldighet.gyldigTil).isEqualTo(endringsdato)

        val kretsEtterLagring = bopliktomraadeService.hentBopliktomraade(Identifikator(Lokalid(bopliktomraade.lokalid), endringsdato))!!
        assertThat(kretsEtterLagring.gyldighet.gyldigTil).isNull()
        assertThat(kretsEtterLagring.omraade).isNotEqualTo(opprinneligKrets.omraade)
        assertThat(kretsEtterLagring.version).isGreaterThan(opprinneligKrets.version)
        assertThat(kretsEtterLagring.auditInfo.typeEndring).isEqualTo(Endringstype.FASTSETTING)

        assertThat(flaterSomErEndret).containsExactly(kretsEtterLagring)
        assertAnnenGrenseEtterGrenseendring("grenseDelt", "grenseDeltLokalid", endringsdato, opprinneligDato,
            kretsEtterLagring, opprinneligKrets)

        val nyGrense = grenseService.hentGrense(Identifikator(Lokalid("grenseDeltLokalid"), endringsdato))!!
        assertThat(nyGrense.kvalitet!!.noeyaktighet).isEqualTo(100)
        assertThat(nyGrense.kvalitet!!.maalemetode).isEqualTo(MaalemetodeKodeEnum.TERRENGMAALT.uuid)
        assertThat(nyGrense.informasjon).isEqualTo("En eller annen informasjon")
        assertThat(nyGrense.auditInfo.typeEndring).isEqualTo(Endringstype.FASTSETTING)
    }

    @Test
    fun `endre to grenser for bopliktomraade skal opprette nye versjoner for grensene og deres bopliktomraade`() {
        val opprinneligDato = LocalDate.parse("2022-01-01")
        val endringsdato = LocalDate.parse("2022-02-01")
        val grenseLokalid = "grenseDeltLokalid"

        val lagredeGrenserForKrets = grenseRepository.saveAll(GrenseHelper.getGrenserForVestligKrets(opprinneligDato, Grensetype.Bopliktgrense))
        grenseRepository.flush()

        val bopliktomraade = saveBopliktomraade(
            id = "2",
            lokalid = Lokalid("22"),
            gyldigFra = opprinneligDato,
            grenser = lagredeGrenserForKrets
        )
        saveKommuneOgUnikLokalid(kommunenummerUuid = "4227", bopliktomraader = listOf(bopliktomraade), gyldigFra = opprinneligDato)

        val grense = grenseService.hentGrense(Identifikator(Lokalid(grenseLokalid), opprinneligDato))!!
        assertThat(grense.bopliktomraader).hasSize(1)
        assertThat(grense.bopliktomraader.map { it.bopliktomraade.id }).containsExactlyInAnyOrder(bopliktomraade.id)

        val featureForAnnenGrense = lagFeatureEndreYkoordinat(bopliktomraade, opprinneligDato, "grenseNordVestLokalid")
        val flaterSomErEndret = utfoerGrenseendring(kretsSomSkalHaGrenseendring = bopliktomraade, opprinneligDato = opprinneligDato,
            endringsdato = endringsdato, featureForAnnenGrense)

        val opprinneligKrets = bopliktomraadeService.hentBopliktomraade(Identifikator(Lokalid(bopliktomraade.lokalid), opprinneligDato))!!
        assertThat(opprinneligKrets.gyldighet.gyldigTil).isEqualTo(endringsdato)

        val kretsEtterLagring = bopliktomraadeService.hentBopliktomraade(Identifikator(Lokalid(bopliktomraade.lokalid), endringsdato))!!

        assertOmradeGyldighetVersjon(kretsEtterLagring, opprinneligKrets, endringsdato, Endringstype.VEDTATT_GRENSEJUSTERING)
        assertThat(flaterSomErEndret).containsExactly(kretsEtterLagring)

        val opprinneligGrense = grenseService.hentGrense(Identifikator(Lokalid(grenseLokalid), opprinneligDato))!!
        assertThat(opprinneligGrense.bopliktomraader).hasSize(1)
        assertThat(opprinneligGrense.bopliktomraader.map { it.bopliktomraade.id }).containsExactlyInAnyOrder(opprinneligKrets.id)

        assertAnnenGrenseEtterGrenseendring("grenseDelt", "grenseDeltLokalid", endringsdato, opprinneligDato,
            kretsEtterLagring, opprinneligKrets)
        assertAnnenGrenseEtterGrenseendring("grenseNordVest", "grenseNordVestLokalid", endringsdato, opprinneligDato,
            kretsEtterLagring, opprinneligKrets)
    }

    @Test
    fun `endre grense etter oppdatering av bopliktomraade skal opprette nye versjoner for grensa og dens bopliktomraade`() {
        val opprinneligDato = LocalDate.parse("2022-01-01")
        val datoForOppdatering = october1In2022
        val datoForGrenseendring = LocalDate.parse("2023-02-01")
        val grenseLokalid = "grenseDeltLokalid"

        val lagredeGrenserForKrets = grenseRepository.saveAll(GrenseHelper.getGrenserForVestligKrets(opprinneligDato, Grensetype.Bopliktgrense))
        grenseRepository.flush()

        val bopliktomraade = saveBopliktomraade(
            id = "2",
            lokalid = Lokalid("22"),
            gyldigFra = opprinneligDato,
            grenser = lagredeGrenserForKrets
        )
        saveKommuneOgUnikLokalid(kommunenummerUuid = "4227", bopliktomraader = listOf(bopliktomraade), gyldigFra = opprinneligDato)

        val bopliktomraadeEtterOppdatering = utfoerMetadataoppdatering(kretsSomSkalOppdateres = bopliktomraade, datoForOppdatering = datoForOppdatering)!!

        val grense = grenseService.hentGrense(Identifikator(Lokalid(grenseLokalid), datoForOppdatering))!!
        assertThat(grense.bopliktomraader).hasSize(2)
        assertThat(grense.bopliktomraader.map { it.bopliktomraade.id }).containsExactlyInAnyOrder(bopliktomraade.id, bopliktomraadeEtterOppdatering.id)

        val flaterSomErEndret = utfoerGrenseendring(kretsSomSkalHaGrenseendring = bopliktomraade, opprinneligDato = datoForOppdatering,
            endringsdato = datoForGrenseendring)

        val opprinneligKrets = bopliktomraadeService.hentBopliktomraade(Identifikator(Lokalid(bopliktomraade.lokalid), datoForOppdatering))!!
        assertThat(opprinneligKrets.gyldighet.gyldigTil).isEqualTo(datoForGrenseendring)

        val kretsEtterLagring = bopliktomraadeService.hentBopliktomraade(Identifikator(Lokalid(bopliktomraade.lokalid), datoForGrenseendring))!!
        assertOmradeGyldighetVersjon(kretsEtterLagring, bopliktomraadeEtterOppdatering, datoForGrenseendring, Endringstype.VEDTATT_GRENSEJUSTERING)
        assertThat(flaterSomErEndret).containsExactly(kretsEtterLagring)

        val opprinneligGrense = grenseService.hentGrense(Identifikator(Lokalid(grenseLokalid), opprinneligDato))!!

        //Sjekker at gammel versjon av grensa ikke har fått endra sine bopliktomraader
        assertThat(opprinneligGrense.bopliktomraader).hasSize(2)
        assertThat(opprinneligGrense.bopliktomraader.map { it.bopliktomraade.id })
            .containsExactlyInAnyOrder(bopliktomraade.id, bopliktomraadeEtterOppdatering.id)

        assertGrenseEtterGrenseendring("grenseDeltLokalid", datoForGrenseendring, opprinneligGrense, kretsEtterLagring, opprinneligKrets)
    }

    @Test
    fun `ikke tillat bopliktomraade utenfor kommune`() {
        val opprinneligDato = LocalDate.parse("2022-01-01")
        val kretsgrenser = grenseRepository.saveAll(GrenseHelper.getGrenserForVestligKrets(opprinneligDato, Grensetype.Kommunegrense))
        val bopliktgrenser = kretsgrenser.map { kretsgrense ->
            Grense(grensetype = Grensetype.Bopliktgrense).apply {
                grense = AffineTransformation.translationInstance(10.0, 0.0).transform(kretsgrense.grense) as org.locationtech.jts.geom.LineString
                gyldighet = kretsgrense.gyldighet
            }
        }
        val bopliktomraade = saveBopliktomraade(
            id = "2",
            lokalid = Lokalid("22"),
            gyldigFra = opprinneligDato,
            grenser = bopliktgrenser
        )
        val kommune = saveKommuneOgUnikLokalid(kommunenummerUuid = "4227", gyldigFra = opprinneligDato, bopliktomraader = listOf(bopliktomraade))
        assertThatThrownBy { kommune.oppdaterOmraadeOgPosisjon(kretsgrenser); entityManager.flush() }
            .message()
            .matches("Bopliktomraade.* er ikke dekket av kommune.*")
    }

    @Test
    fun `tillat bopliktomraade i kommune`() {
        val opprinneligDato = LocalDate.parse("2022-01-01")
        val kretsgrenser = grenseRepository.saveAll(GrenseHelper.getGrenserForVestligKrets(opprinneligDato, Grensetype.Kommunegrense))
        val bopliktgrenser = kretsgrenser.map { kretsgrense ->
            Grense(grensetype = Grensetype.Bopliktgrense).apply {
                grense = kretsgrense.grense.factory.createLineString(kretsgrense.grense.coordinates.copyOf())
                gyldighet = kretsgrense.gyldighet
            }
        }
        val bopliktomraade = saveBopliktomraade(
            id = "2",
            lokalid = Lokalid("22"),
            gyldigFra = opprinneligDato,
            grenser = bopliktgrenser
        )
        val kommune = saveKommuneOgUnikLokalid(kommunenummerUuid = "4227", gyldigFra = opprinneligDato, bopliktomraader = listOf(bopliktomraade))
        assertDoesNotThrow { kommune.oppdaterOmraadeOgPosisjon(kretsgrenser); entityManager.flush() }
    }

    private fun assertGrenseEtterGrenseendring(
        @Suppress("SameParameterValue") grenseLokalid: String, datoForGrenseendring: LocalDate, opprinneligGrense: Grense,
        kretsEtterLagring: Bopliktomraade, opprinneligKrets: Bopliktomraade
    ) {
        val grenseEtterGrenseendring = grenseService.hentGrense(Identifikator(Lokalid(grenseLokalid), datoForGrenseendring))!!
        assertThat(grenseEtterGrenseendring.bopliktomraader).hasSize(1)
        assertThat(grenseEtterGrenseendring.bopliktomraader.map { it.bopliktomraade.id }).containsExactlyInAnyOrder(kretsEtterLagring.id)

        assertThat(opprinneligGrense.gyldighet.gyldigTil).isEqualTo(datoForGrenseendring)
        assertThat(grenseEtterGrenseendring.gyldighet.gyldigTil).isNull()
        assertThat(opprinneligGrense.lokalid).isEqualTo(grenseEtterGrenseendring.lokalid)
        assertThat(opprinneligGrense.grense).isNotEqualTo(grenseEtterGrenseendring.grense)
        assertThat(grenseEtterGrenseendring.version).isGreaterThan(opprinneligGrense.version)

        val opprinnligeGrenser = opprinneligKrets.bopliktomraadeGrenser
            as? BopliktomraadeGrenser.DelvisBoplikt
            ?: throw IllegalStateException("Forventet delvis boplikt")

        val etterGrenser = kretsEtterLagring.bopliktomraadeGrenser
            as? BopliktomraadeGrenser.DelvisBoplikt
            ?: throw IllegalStateException("Forventet delvis boplikt")

        assertThat(opprinnligeGrenser.grenser.map { it.grense.id }).contains("grenseDelt")
        assertThat(etterGrenser.grenser.map { it.grense.id }).doesNotContain("grenseDelt")
        assertThat(etterGrenser.grenser.map { it.grense.lokalid }).contains(grenseLokalid)
    }

    private fun assertAnnenGrenseEtterGrenseendring(grenseId: String, grenseLokalid: String, datoForGrenseendring: LocalDate,
                                                    opprinneligDato: LocalDate, kretsEtterLagring: Bopliktomraade, opprinneligKrets: Bopliktomraade) {

        val opprinneligGrense = grenseService.hentGrense(Identifikator(Lokalid(grenseLokalid), opprinneligDato))!!
        assertThat(opprinneligGrense.bopliktomraader).hasSize(1)
        assertThat(opprinneligGrense.bopliktomraader.map { it.bopliktomraade.id }).contains(opprinneligKrets.id)

        val grenseEtterGrenseendring = grenseService.hentGrense(Identifikator(Lokalid(grenseLokalid), datoForGrenseendring))!!
        assertThat(grenseEtterGrenseendring.bopliktomraader).hasSize(1)
        assertThat(grenseEtterGrenseendring.bopliktomraader.map { it.bopliktomraade.id }).contains(kretsEtterLagring.id)

        assertThat(opprinneligGrense.gyldighet.gyldigTil).isEqualTo(datoForGrenseendring)
        assertThat(grenseEtterGrenseendring.gyldighet.gyldigTil).isNull()
        assertThat(opprinneligGrense.lokalid).isEqualTo(grenseEtterGrenseendring.lokalid)
        assertThat(opprinneligGrense.grense).isNotEqualTo(grenseEtterGrenseendring.grense)
        assertThat(grenseEtterGrenseendring.version).isGreaterThan(opprinneligGrense.version)

        val opprinnligeGrenser = opprinneligKrets.bopliktomraadeGrenser
            as? BopliktomraadeGrenser.DelvisBoplikt
            ?: throw IllegalStateException("Forventet delvis boplikt")

        val etterGrenser = kretsEtterLagring.bopliktomraadeGrenser
            as? BopliktomraadeGrenser.DelvisBoplikt
            ?: throw IllegalStateException("Forventet delvis boplikt")

        assertThat(opprinnligeGrenser.grenser.map { it.grense.id }).contains(grenseId)
        assertThat(etterGrenser.grenser.map { it.grense.id }).doesNotContain(grenseId)
        assertThat(etterGrenser.grenser.map { it.grense.lokalid }).contains(grenseLokalid)
    }

    private fun utfoerMetadataoppdatering(kretsSomSkalOppdateres: Bopliktomraade, datoForOppdatering: LocalDate): Bopliktomraade? {
        val bopliktomraadeRequest = BopliktomraadeRequestBuilder().withLokalId(kretsSomSkalOppdateres.lokalid).build()
        val identifikator = Identifikator(Lokalid(bopliktomraadeRequest.identifikasjon.lokalid), datoForOppdatering)

        bopliktomraadeService.oppdaterBopliktomraade(
            lokalId = identifikator.lokalid,
            oppdatertBopliktomraade = bopliktomraadeRequest,
            gyldigFra = datoForOppdatering,
            endringstype = Endringstype.RETTING
        )
        return bopliktomraadeService.hentBopliktomraade(identifikator)
    }

    private fun saveBopliktomraadeMed1FramtidigVersjon(bopliktomraadeLokalid: Lokalid) {
        val bopliktomraade = saveBopliktomraade(id = "9000", lokalid = bopliktomraadeLokalid, gyldigTil = jan1In2023)
        val bopliktomraade2 = saveBopliktomraade(id = "9001", lokalid = bopliktomraadeLokalid, bopliktomraadenavn = "Nedre Singsaker", gyldigFra = jan1In2023)

        saveKommuneOgUnikLokalid(bopliktomraader = listOf(bopliktomraade, bopliktomraade2))
    }

}

