package no.kartverket.nibas.kommuneogfylkesdeling

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.core.io.ClassPathResource
import java.time.LocalDate

object ManusReader {
    private val kommunefil = "fylkes-komunedeling-filer/kommuner.json"
    private val fylkefil = "fylkes-komunedeling-filer/fylker.json"
    private val stemmekretsfil = "fylkes-komunedeling-filer/stemmekretser.json"
    private val grunnkretsfil = "fylkes-komunedeling-filer/grunnkretser.json"
    private val fylkerNavnFil = "fylkes-komunedeling-filer/fylker-navn.json"
    private val kommunerNavnFil = "fylkes-komunedeling-filer/kommuner-navn.json"
    private val objectMapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    fun getFylkeEndringer(): List<AdministrativEnhetEndring> {
        return readAndParseFile(fylkefil).codeChanges
    }

    fun getKommuneEndringer(): List<AdministrativEnhetEndring> {
        return readAndParseFile(kommunefil).codeChanges
    }

    fun getGrunnkretsEndringer(): List<AdministrativEnhetEndring> {
        return readAndParseFile(grunnkretsfil).codeChanges
    }

    fun getStemmekretsEndringer(): List<StemmekretsEndring> {
        val resource = ClassPathResource(stemmekretsfil)
        val file = resource.inputStream.reader().readText()
        return objectMapper.readValue(file)
    }

    fun getFylkesnavn(): List<AdmEnhetNavn> {
        val resource = ClassPathResource(fylkerNavnFil)
        val file = resource.inputStream.reader().readText()
        return objectMapper.readValue(file)
    }

    fun getKommunenavn(): List<AdmEnhetNavn> {
        val resource = ClassPathResource(kommunerNavnFil)
        val file = resource.inputStream.reader().readText()
        return objectMapper.readValue(file)
    }

    private fun readAndParseFile(fileName: String): AdministrativEnhetEndringWrapper {
        val resource = ClassPathResource(fileName)
        val file = resource.inputStream.reader().readText()
        return objectMapper.readValue(file)
    }
}

object ManusMapper {
    fun mapToFylkesDelingerManus(fylkesendringer: List<AdministrativEnhetEndring>, nyeNavn: List<AdmEnhetNavn>, kommuneendringer: List<AdministrativEnhetEndring>, grunnkretsEndringer: List<AdministrativEnhetEndring>): List<FylkesdelingManus> {
        return fylkesendringer
            .groupBy { it.oldCode }
            .map { (key, nyeFylker) ->
                FylkesdelingManus(
                    gammeltFylkesNummer = key,
                    fraDato = nyeFylker.first().changeOccurred,
                    nyeFylker = nyeFylker.map {
                        NyttFylkeManus(
                            fylkesNummer = it.newCode,
                            navn = getNavnForKode(it.newCode, nyeNavn),
                            kommuner = getKommunerForNyttFylke(it.newCode, kommuneendringer, grunnkretsEndringer)
                        )
                    }
                )
            }
    }

    fun mapToKommuneDelingerManus(kommuneendringer: List<AdministrativEnhetEndring>, nyeKommuneNavn: List<AdmEnhetNavn>, grunnkretserMedEndringer: List<AdministrativEnhetEndring>, stemmekretserMedEndringer: List<StemmekretsEndring>): List<KommunedelingManus> {
        return kommuneendringer
            .groupBy { it.oldCode }
            .filter { (_, nyeKommuner) -> nyeKommuner.size >= 2 }
            .map { (key, nyeKommuner) ->
                KommunedelingManus(
                    gammeltKommuneNummer = key,
                    fraDato = nyeKommuner.first().changeOccurred,
                    nyeKommuner = nyeKommuner.map {
                        NyKommuneManus(
                            kommunenummer = it.newCode,
                            navn = getNavnForKode(it.newCode, nyeKommuneNavn),
                            stemmekretser = getStemmekretserForNyKommune(it.newCode, stemmekretserMedEndringer),
                            grunnkretser = getGrunnkretserForNyKommune(it.newCode, grunnkretserMedEndringer)
                        )
                    }
                )
            }
    }

    private fun getNavnForKode(kode: String, admEnhetNavn: List<AdmEnhetNavn>): List<NavnManus> =
        admEnhetNavn
            .find { it.kode == kode }
            ?.navn?.map {
                NavnManus(
                    navn = it.navn,
                    spraak = it.spraak,
                    rekkefoelge = it.rekkefoelge
                )
            } ?: emptyList()

    private fun getKommunerForNyttFylke(fylkesnummer: String, kommuneendringer: List<AdministrativEnhetEndring>, grunnkretserMedEndringer: List<AdministrativEnhetEndring>): List<FylkesDelingKommuneManus> =
        kommuneendringer
            .filter { it.newCode.startsWith(fylkesnummer) }
            .map {
                FylkesDelingKommuneManus(
                    gammeltKommunenummer = it.oldCode,
                    nyttKommuneNummer = it.newCode,
                    grunnkretser = getGrunnkretserForNyKommune(it.newCode, grunnkretserMedEndringer)
                )
            }
    private fun getGrunnkretserForNyKommune(nyttKommuneNummer: String, administrativEnhetEndringer: List<AdministrativEnhetEndring>): List<GrunnkretsManus> =
        administrativEnhetEndringer
            .filter { it.newCode.startsWith((nyttKommuneNummer)) }
            .map {
                GrunnkretsManus(
                    gammeltGrunnkretsNummer = it.oldCode,
                    nyttGrunnkretsNummer = it.newCode
                )
            }

    private fun getStemmekretserForNyKommune(nyttKommuneNummer: String, stemmekretsEndringer: List<StemmekretsEndring>): List<StemmekretsManus> =
        stemmekretsEndringer
            .filter { it.nyttKommuneNummer == nyttKommuneNummer }
            .map { StemmekretsManus(it.stemmekretsnummer) }
}

data class AdministrativEnhetEndringWrapper (
    val codeChanges: List<AdministrativEnhetEndring>
)

data class AdministrativEnhetEndring(
    val oldCode: String,
    val newCode: String,
    val changeOccurred: LocalDate
)

data class StemmekretsEndring(
    val gammeltKommuneNummer: String,
    val nyttKommuneNummer: String,
    val stemmekretsnummer: String
)

data class AdmEnhetNavn(
    val kode: String,
    val navn: List<NavnPaaSpraak>
)
data class NavnPaaSpraak(
    val navn: String,
    val spraak: String,
    val rekkefoelge: Int? = null
)
