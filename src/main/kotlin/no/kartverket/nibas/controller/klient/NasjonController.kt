package no.kartverket.nibas.controller.klient

import no.kartverket.nibas.api.v1.common.AdministrativEnhetNavn
import no.kartverket.nibas.api.v1.common.geojson.FeatureCollection
import no.kartverket.nibas.api.v1.klient.NasjonApi
import no.kartverket.nibas.api.v1.response.NasjonRef
import no.kartverket.nibas.api.v1.response.NasjonResponse
import no.kartverket.nibas.api.v1.response.toObjektIdentifikator
import no.kartverket.nibas.common.HrefProvider
import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.common.ResponseException.NotFoundException
import no.kartverket.nibas.common.createIdentifikator
import no.kartverket.nibas.domain.GrunnkretsGrenseKobling
import no.kartverket.nibas.domain.Nasjon
import no.kartverket.nibas.service.NasjonService
import no.kartverket.nibas.extensions.geometry.flateToFeature
import no.kartverket.nibas.extensions.geometry.grenseKoblingToFeature
import no.kartverket.nibas.extensions.geometry.posisjonToFeature
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import no.kartverket.nibas.api.v1.response.NasjonRef as NasjonRefResponse

@RestController
class NasjonController(
    private val nasjonService: NasjonService,
    private val hrefProvider: HrefProvider
) : NasjonApi {

    override fun hentNasjoner(gyldighetsdato: LocalDate?): List<NasjonRefResponse> {
        val datoToUse: LocalDate = gyldighetsdato?: LocalDate.now()
        return nasjonService.hentAlleNasjoner(datoToUse).map {
            val identifikatorForNasjon = Identifikator(Lokalid(it.lokalid), datoToUse)
            it.toNasjonRef(identifikatorForNasjon, nasjonService.hentAntallGyldigeVersjonerFramITidFor(identifikatorForNasjon), hrefProvider)
        }
    }

    override fun hentNasjon(lokalid: String, gyldighetsdato: LocalDate?): NasjonResponse {
        val identifikatorForNasjon = createIdentifikator(lokalid, gyldighetsdato)
        return nasjonService.hentNasjon(identifikatorForNasjon)?.toNasjonResponse(identifikatorForNasjon)
            ?: throw NotFoundException("Nasjon $identifikatorForNasjon ble ikke funnet.")
    }

    override fun hentGrenserForNasjon(lokalid: String, gyldighetsdato: LocalDate?): FeatureCollection {
        val identifikator = createIdentifikator(lokalid, gyldighetsdato)
        val grensekoblinger = nasjonService.hentGrenserForNasjon(identifikator)
        if (grensekoblinger.isEmpty()) {
            throw NotFoundException("Fant ingen grenser for $identifikator")
        }
        return FeatureCollection(
            features = grensekoblinger.map { it.grenseKoblingToFeature(gyldighetsdato = identifikator.gyldighetsdato, version = it.grunnkrets.version) }
        )
    }
}

fun Nasjon.toNasjonRef(identifikator: Identifikator, antallFramtidigeVersjoner: Int = 0, hrefProvider: HrefProvider): NasjonRefResponse =
    NasjonRef(
        id = identifikator.toObjektIdentifikator(),
        navn = getAdministrativenhetNavn(identifikator.gyldighetsdato)
            .map { AdministrativEnhetNavn(it.navn!!, it.spraak?.id!!, it.version, it.rekkefoelge) },
        href = hrefProvider.getHref(identifikator),
        antallFramtidigeVersjoner = antallFramtidigeVersjoner
    )


fun Nasjon.toNasjonResponse(identifikator: Identifikator): NasjonResponse =
    NasjonResponse(
        id = identifikator.toObjektIdentifikator(),
        administrativenhetnavn = getAdministrativenhetNavn(identifikator.gyldighetsdato)
            .map { AdministrativEnhetNavn(it.navn!!, it.spraak?.id!!, it.version, it.rekkefoelge) },
        features = FeatureCollection(features = listOf(posisjon.posisjonToFeature(version), flateToFeature(version))),
        version = version
    )
