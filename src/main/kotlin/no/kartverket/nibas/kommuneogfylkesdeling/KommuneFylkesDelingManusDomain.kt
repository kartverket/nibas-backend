package no.kartverket.nibas.kommuneogfylkesdeling

import java.time.LocalDate

data class KommunedelingManus(
    val gammeltKommuneNummer: String,
    val fraDato: LocalDate,
    val nyeKommuner: List<NyKommuneManus>
)

data class NyKommuneManus(
    val kommunenummer: String,
    val navn: List<NavnManus>,
    val stemmekretser: List<StemmekretsManus>,
    val grunnkretser: List<GrunnkretsManus>,
)

data class NavnManus(
    val navn: String,
    val rekkefoelge: Int?,
    val spraak: String
)

data class StemmekretsManus(
    val stemmekretsId: String
)

data class GrunnkretsManus(
    val gammeltGrunnkretsNummer: String,
    val nyttGrunnkretsNummer: String
)

data class FylkesdelingManus(
    val gammeltFylkesNummer: String,
    val fraDato: LocalDate,
    val nyeFylker: List<NyttFylkeManus>
)

data class NyttFylkeManus(
    val fylkesNummer: String,
    val navn: List<NavnManus>,
    val kommuner: List<FylkesDelingKommuneManus>,
)

data class FylkesDelingKommuneManus(
    val gammeltKommunenummer: String,
    val nyttKommuneNummer: String,
    val grunnkretser: List<GrunnkretsManus>
)
