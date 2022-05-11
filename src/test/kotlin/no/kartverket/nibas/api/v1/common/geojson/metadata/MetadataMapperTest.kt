package no.kartverket.nibas.api.v1.common.geojson.metadata

import no.kartverket.nibas.builders.DokumentasjonsreferanseBuilder
import no.kartverket.nibas.builders.FastsettingstypeEnum
import no.kartverket.nibas.builders.FylkesgrenseBuilder
import no.kartverket.nibas.builders.GrensestatusEnum
import no.kartverket.nibas.builders.HJEMMEL_I_LOVEN
import no.kartverket.nibas.builders.INFORMASJONTEKST
import no.kartverket.nibas.builders.LINK_TEKST_1
import no.kartverket.nibas.builders.LINK_TEKST_2
import no.kartverket.nibas.builders.LOKALID
import no.kartverket.nibas.builders.MYNDIGHET
import no.kartverket.nibas.builders.MaalemetodeKodeEnum
import no.kartverket.nibas.builders.NAVNEROM
import no.kartverket.nibas.builders.NoeyaktighetsklasseEnum
import no.kartverket.nibas.builders.REF_TEKST_1
import no.kartverket.nibas.builders.REF_TEKST_2
import no.kartverket.nibas.builders.RETTSKILDE_ID_1
import no.kartverket.nibas.builders.TITTEL
import no.kartverket.nibas.builders.TerrengdetaljEnum
import no.kartverket.nibas.builders.TerritorialgrenseBuilder
import no.kartverket.nibas.builders.VERSJON_33
import no.kartverket.nibas.builders.fastsettingstypeBestemteKoordinater
import no.kartverket.nibas.builders.noeyaktighetsklassekodeLite
import no.kartverket.nibas.builders.terrengdetaljkodeAnnetGjerde
import no.kartverket.nibas.domain.AnsvarligMyndighetHolder
import no.kartverket.nibas.kodelister.KodelisteService
import no.kartverket.nibas.repository.LandKodeRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import java.time.LocalDate

/**
 * Enhetstest av MetadataMapper.
 */
internal class MetadataMapperTest {

    @Mock
    private lateinit var kodelisteServiceMock: KodelisteService

    @Mock
    private lateinit var landKodeRepositoryMock: LandKodeRepository

    @Mock
    private lateinit var kodelisteHrefProviderMock: KodelisteHrefProviderI

    private lateinit var metadataMapper: MetadataMapper

    private val oppdDato: LocalDate = LocalDate.now()
    private val dataFanget = oppdDato.minusYears(1)
    private val gyldigFraTidspunkt = oppdDato.minusMonths(6)
    private val fastsettingsdato: LocalDate = oppdDato.minusMonths(5)
    private val beregningsdato: LocalDate = oppdDato.minusWeeks(17)
    private val transformasjonsdato: LocalDate = oppdDato.minusDays(19)

    private val link1 = dokLink(LINK_TEKST_1)
    private val link2 = dokLink(LINK_TEKST_2)
    private val referanse1 = internReferanse(REF_TEKST_1)
    private val referanse2 = internReferanse(REF_TEKST_2)

    private val dokumentasjonsreferanse = DokumentasjonsreferanseBuilder()
        .fastSettingAt(fastsettingsdato)
        .addLenker(link1, link2)
        .addInternRefs(referanse1, referanse2)
        .build()

    private val maalemetodeHref = "maalemetodeHref"
    private val terrengDetaljHref = "terrengHref"
    private val noeyaktighetHref = "href2"
    private val landkodeHref = "landkodeHref"
    private val fastsettingstypeHref = "fastHref"
    private val grensestatusHref = "grensestatusHref"
    private val kv = "Kartverket"
    private val hav = "Havarikommisjonen"
    private val ansvarligMyndighet1 = AnsvarligMyndighetHolder(ansvarligmyndighet = kv)
    private val ansvarligMyndighet2 = AnsvarligMyndighetHolder(ansvarligmyndighet = hav)

    @BeforeEach
    fun beforeEach() {
        MockitoAnnotations.openMocks(this)
        metadataMapper = MetadataMapper(kodelisteServiceMock, kodelisteHrefProviderMock)

        `when`(kodelisteHrefProviderMock.terrengDetaljHref()).thenReturn(terrengDetaljHref)
        `when`(kodelisteHrefProviderMock.noeyaktighetsklasseHref()).thenReturn(noeyaktighetHref)
        `when`(kodelisteHrefProviderMock.maalemetodeKodeHref()).thenReturn(maalemetodeHref)
        `when`(kodelisteHrefProviderMock.landkodeHref()).thenReturn(landkodeHref)
        `when`(kodelisteHrefProviderMock.fastsettingstypeKodeHref()).thenReturn(fastsettingstypeHref)
        `when`(kodelisteHrefProviderMock.grensestatusKodeHref()).thenReturn(grensestatusHref)
    }

    // Fra Fylkesgrense -> AdministrativGrenseMetadata
    @Test
    fun toAdministrativGrenseMetadata() {
        val fylkesgrense = FylkesgrenseBuilder()
            .withOppdateringsdato(oppdDato)
            .datafangstdato(dataFanget)
            .gyldigFra(gyldigFraTidspunkt)
            .dokRef(dokumentasjonsreferanse)
            .withFastsettingstype(fastsettingstypeBestemteKoordinater)
            .withFoelgerTerrengdetalj(terrengdetaljkodeAnnetGjerde)
            .withNoeyaktighetsklassekode(noeyaktighetsklassekodeLite)
            .build()

        val administrativGrenseMetadata = AdministrativGrenseMetadata(
            common = expectedCommonMetadata(),
            commonGrense = expectedCommonGrensedata(),
            dokumentasjonsreferanser = listOf(dokumentasjonsreferanse).map { it.toDokRef() },
            foelgerTerrengdetalj = KodelisteEntry(id = TerrengdetaljEnum.AnnetGjerde.code, href = terrengDetaljHref),
            noeyaktighetsklasse = KodelisteEntry(id = NoeyaktighetsklasseEnum.NK1.code, href = noeyaktighetHref),
            omtvistet = true
        )

        val metadata = metadataMapper.toMetadata(fylkesgrense)
        assertThat(metadata).isEqualTo(administrativGrenseMetadata)
    }

    // Får testet mapping for maritim grense
    // Territorialgrense -> TerritorialgrenseMetadata
    @Test
    fun toTerritorialgrenseMetadata() {
        mockRussland(landKodeRepositoryMock)

        val territorialgrense = TerritorialgrenseBuilder()
            .withOppdateringsdato(oppdDato)
            .datafangstdato(dataFanget)
            .gyldigFra(gyldigFraTidspunkt)
            .dokRef(dokumentasjonsreferanse)
            .transformasjonsdato(transformasjonsdato)
            .withAnsvarligeMyndigheter(ansvarligMyndighet1, ansvarligMyndighet2)
            .beregningsdato(beregningsdato)
            .build()

        val territorialgrenseMetadata = TerritorialgrenseMetadata(
            common = expectedCommonMetadata(),
            commonGrense = expectedCommonGrensedata(),
            maritimeGrenser = MaritimeGrenserMetadata(
                ansvarligeMyndigheter = listOf(
                    TekstHolder(territorialgrense.findIdForAnsvarligMyndighet(kv), kv),
                    TekstHolder(territorialgrense.findIdForAnsvarligMyndighet(hav), hav)
                ),
                gjelderOmraade = "Område over elva",
                transformasjonsdato = transformasjonsdato,
                virkeomraadenavn = "Virkeområde 33",
                land = Landkoder(
                    ids = listOf("ru"),
                    href = landkodeHref
                ),
            ),
            dokumentasjonsreferanser = listOf(dokumentasjonsreferanse).map { it.toDokRef() },
            beregningsdato = beregningsdato
        )

        val metadata = metadataMapper.toMetadata(territorialgrense)
        assertThat(metadata).isEqualTo(territorialgrenseMetadata)
    }

    private fun expectedCommonMetadata() = CommonMetadata(
        identifikasjon = Identifikasjon(LOKALID, NAVNEROM, VERSJON_33),
        datafangstdato = dataFanget,
        gyldigFra = gyldigFraTidspunkt,
        gyldigTil = null,
        sporingsinformasjon = Sporingsinformasjon(oppdateringsdato = oppdDato),
        opphav = "opphav3",
        informasjon = INFORMASJONTEKST
    )

    private fun expectedCommonGrensedata() = CommonGrenseMetadata(
        posisjonskvalitet = Posisjonskvalitet(
            maalemetode = KodelisteEntry(id = MaalemetodeKodeEnum.TERRENGMAALT.uuid, href = maalemetodeHref),
            noeyaktighet = 200
        ),
        fastsettingstype = KodelisteEntry(id = FastsettingstypeEnum.BestemteKoordinater.name, href = fastsettingstypeHref),
        grensestatus = KodelisteEntry(id = GrensestatusEnum.Traktat.name, href = grensestatusHref),
    )

    private fun dokRef(id: String): Dokref {
        return Dokref(
            id = id,
            dokumentlenker = listOf(
                TekstHolder(link1.id, LINK_TEKST_1),
                TekstHolder(link2.id, LINK_TEKST_2)
            ),
            fastsettingsdato = fastsettingsdato,
            fastsettingsmyndighet = MYNDIGHET,
            hjemmel = HJEMMEL_I_LOVEN,
            internReferanserKartverket = listOf(
                TekstHolder(referanse2.id, REF_TEKST_2),
                TekstHolder(referanse1.id, REF_TEKST_1)
            ),
            rettskildeId = RETTSKILDE_ID_1,
            rettskildeTittel = TITTEL
        )
    }

}
