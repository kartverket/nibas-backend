package no.kartverket.nibas.controller.klient

import no.kartverket.nibas.api.v1.common.geojson.FeatureCollection
import no.kartverket.nibas.api.v1.klient.KommuneApi
import no.kartverket.nibas.api.v1.klient.response.BopliktomraadeResponse
import no.kartverket.nibas.api.v1.klient.response.GrunnkretsResponse
import no.kartverket.nibas.api.v1.klient.response.InndelingResponse
import no.kartverket.nibas.api.v1.klient.response.KommuneResponse
import no.kartverket.nibas.api.v1.klient.response.StemmekretsResponse
import no.kartverket.nibas.api.v1.klient.response.toInndelingResponse
import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.common.ResponseException.NotFoundException
import no.kartverket.nibas.common.createIdentifikator
import no.kartverket.nibas.extensions.flate.toBopliktomraadeResponse
import no.kartverket.nibas.extensions.flate.toGrunnkretsResponse
import no.kartverket.nibas.extensions.flate.toKommuneResponse
import no.kartverket.nibas.extensions.flate.toStemmekretsResponse
import no.kartverket.nibas.extensions.geometry.grenseKoblingToFeature
import no.kartverket.nibas.service.KommuneService
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController("KlientKommuneController")
class KommuneController(
    private val kommuneService: KommuneService,
) : KommuneApi {

    override fun hentKommuner(fylkeId: String?, gyldighetsdato: LocalDate?): List<KommuneResponse> {
        val datoToUse: LocalDate = gyldighetsdato ?: LocalDate.now()
        return if (fylkeId == null) {
            kommuneService.hentAlleKommuner(datoToUse).map { it.toKommuneResponse(datoToUse) }
        } else {
            kommuneService.hentKommunerForFylke(Identifikator(Lokalid(fylkeId), datoToUse))
                .map { it.toKommuneResponse(datoToUse) }
        }
    }

    override fun hentKommunerForFylker(
        fylkeIder: List<String>,
        gyldighetsdato: LocalDate?
    ): List<KommuneResponse> {
        val datoToUse = gyldighetsdato ?: LocalDate.now()
        return kommuneService
            .hentKommunerForFylker(
                fylkeIder.map { id -> Identifikator(Lokalid(id), datoToUse) }
            )
            .map { it.toKommuneResponse(datoToUse) }
    }

    override fun hentKommune(lokalid: String, gyldighetsdato: LocalDate?): KommuneResponse {
        val identifikator = createIdentifikator(lokalid, gyldighetsdato)
        return kommuneService.hentKommune(identifikator)?.toKommuneResponse(identifikator.gyldighetsdato)
            ?: throw NotFoundException("Kommune $identifikator ble ikke funnet.")
    }

    override fun hentGrenserForKommune(lokalid: String, gyldighetsdato: LocalDate?): FeatureCollection {
        val identifikator = createIdentifikator(lokalid, gyldighetsdato)
        val grenseKoblinger = kommuneService.hentGrensekoblingerForKommune(identifikator)
        if (grenseKoblinger.isEmpty()) {
            throw NotFoundException("Fant ingen grenser for $identifikator")
        }
        return FeatureCollection(
            features = grenseKoblinger.map { it.grenseKoblingToFeature(identifikator.gyldighetsdato) }
        )
    }

    override fun hentKommunesGrunnkretser(lokalid: String, gyldighetsdato: LocalDate?): List<GrunnkretsResponse> {
        val kommuneIdentifikator = createIdentifikator(lokalid, gyldighetsdato)
        return kommuneService.hentKommunesGrunnkretser(kommuneIdentifikator).map {
            it.toGrunnkretsResponse()
        }
    }

    override fun hentKommunesGrunnkretsgrenser(lokalid: String, gyldighetsdato: LocalDate?): FeatureCollection {
        val identifikator = Identifikator(Lokalid(lokalid), gyldighetsdato ?: LocalDate.now())
        val grensekoblinger = kommuneService.hentGrensekoblingerForAlleGrunnkretserIKommune(identifikator)

        if (grensekoblinger.isEmpty()) {
            throw NotFoundException("Fant ingen grunnkretsgrenser for $identifikator")
        }

        return FeatureCollection(
            features = grensekoblinger
                .map { it.grenseKoblingToFeature(identifikator.gyldighetsdato) }
        )
    }

    override fun hentKommunesBopliktomraadegrenser(
        lokalid: String,
        gyldighetsdato: LocalDate?
    ): FeatureCollection {
        val identifikator = Identifikator(Lokalid(lokalid), gyldighetsdato ?: LocalDate.now())
        val grensekoblinger = kommuneService.hentGrensekoblingerForAlleBopliktomraaderIKommune(identifikator)

        if (grensekoblinger.isEmpty()) {
            return FeatureCollection(
                features = emptyList()
            )
        }

        return FeatureCollection(
            features = grensekoblinger
                .map { it.grenseKoblingToFeature(identifikator.gyldighetsdato) }
        )
    }

    override fun hentKommunesStemmekretser(lokalid: String, gyldighetsdato: LocalDate?): List<StemmekretsResponse> {
        val kommuneIdentifikator = createIdentifikator(lokalid, gyldighetsdato)
        return kommuneService.hentKommunesStemmekretser(kommuneIdentifikator).map {
            it.toStemmekretsResponse()
        }
    }

    override fun hentKommunesStemmekretsgrenser(lokalid: String, gyldighetsdato: LocalDate?): FeatureCollection {
        val identifikator = Identifikator(Lokalid(lokalid), gyldighetsdato ?: LocalDate.now())
        val grensekoblinger = kommuneService.hentGrensekoblingerForAlleStemmekretserIKommune(identifikator)

        if (grensekoblinger.isEmpty()) {
            throw NotFoundException("Fant ingen stemmekretsgrenser for $identifikator")
        }

        return FeatureCollection(
            features = grensekoblinger
                .map { it.grenseKoblingToFeature(identifikator.gyldighetsdato) }
        )
    }

    override fun hentKommunesInndelingerMedDeltGeometriGrenser(lokalid: String, gyldighetsdato: LocalDate?): FeatureCollection {
        val identifikator = Identifikator(Lokalid(lokalid), gyldighetsdato ?: LocalDate.now())
        val alleGrensekoblinger = kommuneService.hentKommunesInndelingerMedDeltGeometriGrenser(identifikator)

        if (alleGrensekoblinger.isEmpty()) {
            throw NotFoundException("Fant ingen grenser for inndelinger med delt geometri for kommunegrenser i kommune med id $identifikator")
        }

        return FeatureCollection(features = alleGrensekoblinger.map { it.grenseKoblingToFeature(identifikator.gyldighetsdato) })
    }

    override fun hentKommunesInndelingerMedDeltGeometri(lokalid: String, gyldighetsdato: LocalDate?): List<InndelingResponse> {
        val stemmekretser = hentKommunesStemmekretser(lokalid, gyldighetsdato)
        val grunnkretser = hentKommunesGrunnkretser(lokalid, gyldighetsdato)

        if (stemmekretser.isEmpty() && grunnkretser.isEmpty()) {
            throw NotFoundException("Fant ingen inndelinger med delt geometri for kommunegrenser i kommune med id $lokalid")
        }

        return (stemmekretser.map { it.toInndelingResponse() } + grunnkretser.map { it.toInndelingResponse() })
    }

    override fun hentKommunesStemmekretsgrenserHistoriske(
        lokalid: String,
        gyldigTilDate: LocalDate
    ): FeatureCollection {
        val identifikator = Identifikator(Lokalid(lokalid), gyldigTilDate)

        val grensekoblinger = kommuneService.hentGrensekoblingerForAlleStemmekretserIKommuneHistoriske(
            identifikator = identifikator
        )
        return FeatureCollection(
            features = grensekoblinger
                .map { it.grenseKoblingToFeature(gyldigTilDate) } // Tar ikke med utgåtte/gamle versjoner av kontekstegenskaper i returen, derfor gyldigTilDate her
        )
    }

    override fun hentKommunesGrunnkretsgrenserHistoriske(
        lokalid: String,
        gyldigTilDate: LocalDate
    ): FeatureCollection {
        val identifikator = Identifikator(Lokalid(lokalid), gyldigTilDate)

        val grensekoblinger = kommuneService.hentGrensekoblingerForAlleGrunnkretserIKommuneHistoriske(
            identifikator = identifikator
        )
        return FeatureCollection(
            features = grensekoblinger
                .map { it.grenseKoblingToFeature(gyldigTilDate) }
        )
    }

    override fun hentKommunesBopliktomraade(
        lokalid: String,
        gyldighetsdato: LocalDate?
    ): List<BopliktomraadeResponse> {
        val identifikator = createIdentifikator(lokalid, gyldighetsdato)
        val bopliktomraade = kommuneService.hentKommunesBopliktomraade(identifikator)

        return bopliktomraade.map { it.toBopliktomraadeResponse() }
    }
}
