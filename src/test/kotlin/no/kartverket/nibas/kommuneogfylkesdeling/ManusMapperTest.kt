package no.kartverket.nibas.kommuneogfylkesdeling

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class ManusMapperTest {
    @Test
    fun `mapToKommuneDelingerManus skal lage en kommunedeling fra kommuneEndringer`() {
        val kommuneEndringer = listOf(
            AdministrativEnhetEndring(
                oldCode = "1212",
                newCode = "1313",
                changeOccurred = LocalDate.now()
            ),
            AdministrativEnhetEndring(
                oldCode = "1212",
                newCode = "1415",
                changeOccurred = LocalDate.now()
            )
        )

        val kommuneDelinger = ManusMapper.mapToKommuneDelingerManus(kommuneEndringer, emptyList(), emptyList(), emptyList())

        assertThat(kommuneDelinger).hasSize(1)
        assertThat(kommuneDelinger.firstOrNull()?.nyeKommuner).hasSize(2)
    }

    @Test
    fun `mapToKommuneDelingerManus skal ikke inneholde omnummereringer som kommer av fylkesdeling`() {
        val kommuneEndringer = listOf(
            AdministrativEnhetEndring(
                oldCode = "1212",
                newCode = "1313",
                changeOccurred = LocalDate.now()
            ),
            AdministrativEnhetEndring(
                oldCode = "1212",
                newCode = "1415",
                changeOccurred = LocalDate.now()
            ),
            AdministrativEnhetEndring(
                oldCode = "1567",
                newCode = "1587",
                changeOccurred = LocalDate.now()
            )
        )

        val kommuneDelinger = ManusMapper.mapToKommuneDelingerManus(kommuneEndringer, emptyList(), emptyList(), emptyList())

        assertThat(kommuneDelinger).hasSize(1)
    }

    @Test
    fun `mapToKommuneDelingerManus skal sette riktig navn paa nye kommuner`() {
        val kommuneEndringer = listOf(
            AdministrativEnhetEndring(
                oldCode = "1212",
                newCode = "1313",
                changeOccurred = LocalDate.now()
            ),
            AdministrativEnhetEndring(
                oldCode = "1212",
                newCode = "1415",
                changeOccurred = LocalDate.now()
            )
        )

        val nyeNavn: List<AdmEnhetNavn> = listOf(
            AdmEnhetNavn(
                kode = "1313",
                navn = listOf(
                    NavnPaaSpraak(
                        navn = "nytt navn 1313",
                        spraak = "nor"
                    )
                )
            ),
            AdmEnhetNavn(
                kode = "1415",
                navn = listOf(
                    NavnPaaSpraak(
                        navn = "nytt navn 1415",
                        spraak = "nor"
                    )
                )
            )
        )

        val kommuneDelinger = ManusMapper.mapToKommuneDelingerManus(kommuneEndringer, nyeNavn, emptyList(), emptyList())
        val nyeKommunerSortert = kommuneDelinger.first().nyeKommuner.sortedBy { it.kommunenummer }

        assertThat(nyeKommunerSortert[0].navn).hasSize(1)
        assertThat(nyeKommunerSortert[1].navn).hasSize(1)
        assertThat(nyeKommunerSortert[0].navn.first().navn).isEqualTo("nytt navn 1313")
        assertThat(nyeKommunerSortert[1].navn.first().navn).isEqualTo("nytt navn 1415")
    }

    @Test
    fun `mapToKommuneDelingerManus skal sette grunnkretser paa de nye kommunene`() {
        val kommuneEndringer = listOf(
            AdministrativEnhetEndring(
                oldCode = "1212",
                newCode = "1313",
                changeOccurred = LocalDate.now()
            ),
            AdministrativEnhetEndring(
                oldCode = "1212",
                newCode = "1415",
                changeOccurred = LocalDate.now()
            )
        )

        val grunnkretsTildelinger = listOf(
            AdministrativEnhetEndring(
                oldCode = "12121313",
                newCode = "13131313",
                changeOccurred = LocalDate.now()
            ),
            AdministrativEnhetEndring(
                oldCode = "12121313",
                newCode = "14151313",
                changeOccurred = LocalDate.now()
            )
        )

        val kommuneDelinger = ManusMapper.mapToKommuneDelingerManus(
            kommuneendringer = kommuneEndringer,
            nyeKommuneNavn = emptyList(),
            grunnkretserMedEndringer = grunnkretsTildelinger,
            stemmekretserMedEndringer = emptyList()
        )

        val nyeKommunerSortert = kommuneDelinger.first().nyeKommuner.sortedBy { it.kommunenummer }

        assertThat(nyeKommunerSortert[0].grunnkretser).hasSize(1)
        assertThat(nyeKommunerSortert[1].grunnkretser).hasSize(1)
        assertThat(nyeKommunerSortert[0].grunnkretser.first().nyttGrunnkretsNummer).isEqualTo("13131313")
        assertThat(nyeKommunerSortert[1].grunnkretser.first().nyttGrunnkretsNummer).isEqualTo("14151313")
    }

    @Test
    fun `mapToKommuneDelingerManus skal sette stemmekretser paa de nye kommunene`() {
        val kommuneEndringer = listOf(
            AdministrativEnhetEndring(
                oldCode = "1212",
                newCode = "1313",
                changeOccurred = LocalDate.now()
            ),
            AdministrativEnhetEndring(
                oldCode = "1212",
                newCode = "1415",
                changeOccurred = LocalDate.now()
            )
        )

        val stemmekretsTildelinger = listOf(
            StemmekretsEndring(
                gammeltKommuneNummer = "1212",
                nyttKommuneNummer = "1313",
                stemmekretsnummer = "1"
            ),
            StemmekretsEndring(
                gammeltKommuneNummer = "1212",
                nyttKommuneNummer = "1415",
                stemmekretsnummer = "2"
            )
        )

        val kommuneDelinger = ManusMapper.mapToKommuneDelingerManus(
            kommuneendringer = kommuneEndringer,
            nyeKommuneNavn = emptyList(),
            grunnkretserMedEndringer = emptyList(),
            stemmekretserMedEndringer = stemmekretsTildelinger
        )

        val nyeKommunerSortert = kommuneDelinger.first().nyeKommuner.sortedBy { it.kommunenummer }

        assertThat(nyeKommunerSortert[0].stemmekretser).hasSize(1)
        assertThat(nyeKommunerSortert[1].stemmekretser).hasSize(1)
        assertThat(nyeKommunerSortert[0].stemmekretser.first().stemmekretsId).isEqualTo("1")
        assertThat(nyeKommunerSortert[1].stemmekretser.first().stemmekretsId).isEqualTo("2")
    }

    @Test
    fun `mapToFylkesDelingerManus skal lage en fylkesdeling fra fylkesendringer`() {
        val fylkesEndringer = listOf(
            AdministrativEnhetEndring(
                oldCode = "30",
                newCode = "31",
                changeOccurred = LocalDate.now()
            ),
            AdministrativEnhetEndring(
                oldCode = "30",
                newCode = "32",
                changeOccurred = LocalDate.now()
            )
        )

        val kommuneDelinger = ManusMapper.mapToFylkesDelingerManus(fylkesEndringer, emptyList(), emptyList(), emptyList())

        assertThat(kommuneDelinger).hasSize(1)
        assertThat(kommuneDelinger.firstOrNull()?.nyeFylker).hasSize(2)
    }

    @Test
    fun `mapToFylkesDelingerManus skal sette riktig navn paa nye fylker`() {
        val fylkesEndringer = listOf(
            AdministrativEnhetEndring(
                oldCode = "30",
                newCode = "31",
                changeOccurred = LocalDate.now()
            ),
            AdministrativEnhetEndring(
                oldCode = "30",
                newCode = "32",
                changeOccurred = LocalDate.now()
            )
        )

        val nyeNavn: List<AdmEnhetNavn> = listOf(
            AdmEnhetNavn(
                kode = "1313",
                navn = listOf(
                    NavnPaaSpraak(
                        navn = "nytt navn 1313",
                        spraak = "nor"
                    )
                )
            ),
            AdmEnhetNavn(
                kode = "1415",
                navn = listOf(
                    NavnPaaSpraak(
                        navn = "nytt navn 1415",
                        spraak = "nor"
                    )
                )
            )
        )

        val kommuneDelinger = ManusMapper.mapToFylkesDelingerManus(fylkesEndringer, nyeNavn, emptyList(), emptyList())

        assertThat(kommuneDelinger).hasSize(1)
        assertThat(kommuneDelinger.firstOrNull()?.nyeFylker).hasSize(2)
    }

    @Test
    fun `mapToFylkesDelingerManus skal sette hvilke kommuner som horer til de nye fylkene`() {
        val fylkesEndringer = listOf(
            AdministrativEnhetEndring(
                oldCode = "30",
                newCode = "31",
                changeOccurred = LocalDate.now()
            ),
            AdministrativEnhetEndring(
                oldCode = "30",
                newCode = "32",
                changeOccurred = LocalDate.now()
            )
        )

        val kommuneEndringer = listOf(
            AdministrativEnhetEndring(
                oldCode = "3001",
                newCode = "3101",
                changeOccurred = LocalDate.now()
            ),
            AdministrativEnhetEndring(
                oldCode = "3003",
                newCode = "3103",
                changeOccurred = LocalDate.now()
            ),
            AdministrativEnhetEndring(
                oldCode = "3002",
                newCode = "3203",
                changeOccurred = LocalDate.now()
            )
        )

        val fylkesdelinger = ManusMapper.mapToFylkesDelingerManus(
            fylkesendringer = fylkesEndringer,
            nyeNavn = emptyList(),
            kommuneendringer = kommuneEndringer,
            grunnkretsEndringer = emptyList()
        )

        val nyeFylkerSortert = fylkesdelinger.first().nyeFylker.sortedBy { it.fylkesNummer }

        assertThat(nyeFylkerSortert[0].kommuner).hasSize(2)
        assertThat(nyeFylkerSortert[1].kommuner).hasSize(1)
        assertThat(nyeFylkerSortert[0].kommuner.first().nyttKommuneNummer).isEqualTo("3101")
        assertThat(nyeFylkerSortert[1].kommuner.first().nyttKommuneNummer).isEqualTo("3203")
    }

    @Test
    fun `mapToFylkesDelingerManus skal vise hvilke grunnkretser som skal omnummereres`() {
        val fylkesEndringer = listOf(
            AdministrativEnhetEndring(
                oldCode = "30",
                newCode = "31",
                changeOccurred = LocalDate.now()
            ),
            AdministrativEnhetEndring(
                oldCode = "30",
                newCode = "32",
                changeOccurred = LocalDate.now()
            )
        )

        val kommuneEndringer = listOf(
            AdministrativEnhetEndring(
                oldCode = "3001",
                newCode = "3101",
                changeOccurred = LocalDate.now()
            ),
            AdministrativEnhetEndring(
                oldCode = "3003",
                newCode = "3203",
                changeOccurred = LocalDate.now()
            ),
        )

        val grunnkretsEndringer = listOf(
            AdministrativEnhetEndring(
                oldCode = "30011111",
                newCode = "31011111",
                changeOccurred = LocalDate.now()
            ),
            AdministrativEnhetEndring(
                oldCode = "30032222",
                newCode = "32032222",
                changeOccurred = LocalDate.now()
            ),
        )

        val fylkesdelinger = ManusMapper.mapToFylkesDelingerManus(
            fylkesendringer = fylkesEndringer,
            nyeNavn = emptyList(),
            kommuneendringer = kommuneEndringer,
            grunnkretsEndringer = grunnkretsEndringer
        )

        val nyeFylkerSortert = fylkesdelinger.first().nyeFylker.sortedBy { it.fylkesNummer }

        val grunnkretserFylke1 = nyeFylkerSortert[0].kommuner.first().grunnkretser
        val grunnkretserFylke2 = nyeFylkerSortert[1].kommuner.first().grunnkretser

        assertThat(grunnkretserFylke1).hasSize(1)
        assertThat(grunnkretserFylke2).hasSize(1)
        assertThat(grunnkretserFylke1.map { it.nyttGrunnkretsNummer }).contains("31011111")
        assertThat(grunnkretserFylke2.map { it.nyttGrunnkretsNummer }).contains("32032222")
    }
}
