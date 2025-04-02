package no.kartverket.nibas.api.v1.ekstern.response

import io.swagger.v3.oas.annotations.media.Schema
import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.api.v1.common.GyldighetResponse
import no.kartverket.nibas.common.getInSpraak
import no.kartverket.nibas.domain.Endringstype
import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.extensions.geometry.geometryToDto
import java.time.LocalDate
import java.time.ZonedDateTime
import no.kartverket.nibas.api.v1.common.geojson.Geometry as GeometryDto

@Schema(description = "Representasjon av en grense")
data class GrenseResponse(
    @Schema(description = "Unik ID for grensen")
    val id: String,

    @Schema(description = "Lokal ID for grensen")
    val lokalid: String,

    @Schema(description = "Type grense")
    val grensetype: Grensetype,

    @Schema(description = "Geometri for grensen (GeoJSON format)")
    val geometri: GeometryDto,

    @Schema(description = "Gyldighetsperiode for grensen")
    val gyldighet: GyldighetResponse,

    @Schema(description = "Dato for datafangst", nullable = true)
    val datafangstdato: ZonedDateTime?,

    @Schema(description = "Dato for første digitalisering")
    val foerstedigitaliseringsdato: ZonedDateTime,

    @Schema(description = "Opphav/kilde til grensen", nullable = true)
    val opphav: String?,

    @Schema(description = "Tilleggsinformasjon om grensen", nullable = true)
    val informasjon: String?,

    @Schema(description = "Brukeren som sist endret grensen")
    val endretAv: String,

    @Schema(description = "Dato for siste endring")
    val endretDato: ZonedDateTime,

    @Schema(description = "Type endring som ble gjort")
    val typeEndring: Endringstype,

    @Schema(description = "Metode for måling", nullable = true)
    val maalemetode: String?,

    @Schema(description = "Punktstandardavviket i grunnriss for punkter samt tverravvik for linjer (oppgitt i cm)", nullable = true)
    val noeyaktighet: Int?,

    @Schema(description = "Kommuner på grensen med side-informasjon")
    val kommuner: List<KommuneInfoDTO>
) {
    companion object {
        /**
         * Konverterer et Grense-domene-objekt til et GrenseResponse-objekt
         *
         * @param grense Grense-objektet som skal konverteres
         * @return GrenseResponse-objekt med data fra Grense-objektet
         */
        fun fraGrense(grense: Grense): GrenseResponse {
            return GrenseResponse(
                id = grense.id,
                lokalid = grense.lokalid,
                grensetype = grense.grensetype,
                geometri = grense.grense.geometryToDto(),
                gyldighet = GyldighetResponse(
                    gyldigFra = grense.gyldighet.gyldigFra,
                    gyldigTil = grense.gyldighet.gyldigTil
                ),
                datafangstdato = grense.datafangstdato,
                foerstedigitaliseringsdato = grense.foerstedigitaliseringsdato,
                opphav = grense.opphav,
                informasjon = grense.informasjon,
                endretAv = grense.auditInfo.endretAv,
                endretDato = grense.auditInfo.endretdato,
                typeEndring = grense.auditInfo.typeEndring,
                maalemetode = grense.kvalitet?.maalemetode,
                noeyaktighet = grense.kvalitet?.noeyaktighet,
                kommuner = hentKommunerPaaHverSide(grense)
            )
        }

        /**
         * Henter kommunene på hver side av grensen.
         *
         * @param grense Grenseobjektet å hente kommuner for
         * @return Liste med KommuneInfoDTO for kommuner på grensen
         * @throws IllegalStateException hvis grensen har feil antall kommuner for sin grensetype
         */
        private fun hentKommunerPaaHverSide(grense: Grense): List<KommuneInfoDTO> {
            val gyldighetsdato = LocalDate.now()

            return when (grense.grensetype) {
                Grensetype.Kommunegrense, Grensetype.Fylkesgrense -> {
                    val kommuneListe = grense.kommuner(gyldighetsdato).toList()
                    if (kommuneListe.size != 2) {
                        throw IllegalStateException("En ${grense.grensetype} må ha nøyaktig 2 kommuner, fant ${kommuneListe.size} (grense ID: ${grense.lokalid})")
                    }

                    val kommune1 = kommuneListe[0]
                    val kommune2 = kommuneListe[1]

                    // Side1 first, then side2 - order is important!
                    listOf(
                        KommuneInfoDTO(
                            kommuneLokalID = kommune1.lokalid,
                            kommunenummer = kommune1.kommunenummer,
                            kommunenavn = kommune1.getAdministrativenhetNavn(gyldighetsdato).getInSpraak("nor") ?: "Ukjent",
                            fylkesLokalID = kommune1.fylkeLokalid.lokalid
                        ),
                        KommuneInfoDTO(
                            kommuneLokalID = kommune2.lokalid,
                            kommunenummer = kommune2.kommunenummer,
                            kommunenavn = kommune2.getAdministrativenhetNavn(gyldighetsdato).getInSpraak("nor") ?: "Ukjent",
                            fylkesLokalID = kommune2.fylkeLokalid.lokalid
                        )
                    )
                }

                Grensetype.Riksgrense, Grensetype.Territorialgrense, Grensetype.AvtaltAvgrensningslinje -> {
                    val kommuneListe = grense.kommuner(gyldighetsdato).toList()
                    if (kommuneListe.size != 1) {
                        throw IllegalStateException("En ${grense.grensetype} må ha nøyaktig 1 kommune, fant ${kommuneListe.size} (grense ID: ${grense.lokalid})")
                    }

                    val kommune = kommuneListe.first()

                    listOf(
                        KommuneInfoDTO(
                            kommuneLokalID = kommune.lokalid,
                            kommunenummer = kommune.kommunenummer,
                            kommunenavn = kommune.getAdministrativenhetNavn(gyldighetsdato).getInSpraak("nor") ?: "Ukjent",
                            fylkesLokalID = kommune.fylkeLokalid.lokalid
                        )
                    )
                }

                else -> {
                    throw IllegalStateException("Ukjent grensetype: ${grense.grensetype}")
                }
            }
        }
    }
}

@Schema(description = "Informasjon om en kommune på en side av en grense")
data class KommuneInfoDTO(
    @Schema(description = "Fylkes Lokal ID")
    val fylkesLokalID: String?,

    @Schema(description = "Kommune Lokal ID")
    val kommuneLokalID: String,

    @Schema(description = "Kommunenummer")
    val kommunenummer: String,

    @Schema(description = "Kommunenavn")
    val kommunenavn: String
)
