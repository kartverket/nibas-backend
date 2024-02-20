package no.kartverket.nibas.extensions.flate

import no.kartverket.nibas.api.v1.common.AdministrativEnhetNavn
import no.kartverket.nibas.api.v1.common.Fylkesnummer
import no.kartverket.nibas.api.v1.common.Kommunenummer
import no.kartverket.nibas.api.v1.common.ObjektIdentifikator
import no.kartverket.nibas.api.v1.common.geojson.FeatureCollection
import no.kartverket.nibas.api.v1.klient.response.FylkeRef
import no.kartverket.nibas.api.v1.klient.response.FylkeResponse
import no.kartverket.nibas.api.v1.klient.response.GrunnkretsResponse
import no.kartverket.nibas.api.v1.klient.response.KommuneRef
import no.kartverket.nibas.api.v1.klient.response.KommuneResponse
import no.kartverket.nibas.api.v1.klient.response.StemmekretsResponse
import no.kartverket.nibas.api.v1.klient.response.toGyldighetResponse
import no.kartverket.nibas.api.v1.klient.response.toObjektIdentifikator
import no.kartverket.nibas.common.HrefProvider
import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.domain.Fylke
import no.kartverket.nibas.domain.Grunnkrets
import no.kartverket.nibas.domain.Kommune
import no.kartverket.nibas.domain.Stemmekrets
import no.kartverket.nibas.extensions.geometry.flateToFeature
import no.kartverket.nibas.extensions.geometry.posisjonToFeature
import no.kartverket.nibas.service.FylkeService
import no.kartverket.nibas.service.KommuneService
import java.time.LocalDate

fun Grunnkrets.toGrunnkretsResponse(identifikator: Identifikator): GrunnkretsResponse {
    val kommune = this.getKommune(gyldighet.gyldigFra) ?: throw RuntimeException("Fant ikke kommune for grunnkrets")
    return GrunnkretsResponse(
        id = identifikator.toObjektIdentifikator(),
        navn = grunnkretsnavn!!,
        grunnkretsnummer = grunnkretsnummer!!,
        gyldighet = gyldighet.toGyldighetResponse(),
        datafangstdato = datafangstdato,
        kommunenummer = kommune.let { Kommunenummer(it.kommunenummerUuid, it.kommunenummer) },
        kommuneIdentifikator = ObjektIdentifikator(Lokalid(kommune.lokalid), kommune.gyldighet.gyldigFra),
        endringstype = auditInfo.typeEndring.value,
        oppdateringsdato = auditInfo.endretdato,
        features = FeatureCollection(features = listOf(
            posisjon.posisjonToFeature(version),
            flateToFeature(version)
        )),
        representasjonspunkt = posisjon.posisjonToFeature(version),
        version = version,
    )
}

fun Fylke.toFylkeRef(identifikator: Identifikator, fylkeService: FylkeService, hrefProvider: HrefProvider): FylkeRef {
    val antallFramtidigeVersjoner = if (this.gyldighet.gyldigTil == null) {
        0
    } else {
        fylkeService.hentAntallGyldigeVersjonerFramITidFor(identifikator)
    }

    return FylkeRef(
        id = identifikator.toObjektIdentifikator(),
        navn = getAdministrativenhetNavn(identifikator.gyldighetsdato)
            .map { AdministrativEnhetNavn(it.navn!!, it.spraak?.id!!, it.version, it.rekkefoelge) },
        href = hrefProvider.getHref(identifikator),
        fylkesnummer = Fylkesnummer(
            id = fylkesnummerUuid,
            kodeverdi = fylkesnummer
        ),
        antallFramtidigeVersjoner = antallFramtidigeVersjoner
    )
}

fun Fylke.toFylkeResponse(identifikator: Identifikator): FylkeResponse = FylkeResponse(
    id = identifikator.toObjektIdentifikator(),
    administrativenhetnavn = getAdministrativenhetNavn(identifikator.gyldighetsdato)
        .map { AdministrativEnhetNavn(it.navn!!, it.spraak?.id!!, it.version, it.rekkefoelge) },
    fylkesnummer = Fylkesnummer(
        id = fylkesnummerUuid,
        kodeverdi = fylkesnummer
    ),
    samiskforvaltningsomraade = samiskforvaltningsomraade!!,
    oppdateringsdato = auditInfo.endretdato,
    features = FeatureCollection(features = listOf((posisjon.posisjonToFeature(version)))),
    representasjonspunkt = posisjon.posisjonToFeature(version),
    version = version
)

fun Kommune.toKommuneRef(datoToUse: LocalDate, kommuneService: KommuneService, hrefProvider: HrefProvider): KommuneRef {
    val identifikator = Identifikator(Lokalid(lokalid), datoToUse)
    val antallFramtidigeVersjoner = kommuneService.hentAntallGyldigeVersjonerFramITidFor(identifikator)

    return KommuneRef(
        id = identifikator.toObjektIdentifikator(),
        navn = getAdministrativenhetNavn(identifikator.gyldighetsdato)
            .map { AdministrativEnhetNavn(it.navn!!, it.spraak?.id!!, it.version, it.rekkefoelge) },
        href = hrefProvider.getHref(identifikator),
        kommunenummer = Kommunenummer(kommunenummerUuid, kommunenummer),
        antallFramtidigeVersjoner = antallFramtidigeVersjoner
    )
}

fun Kommune.toKommuneResponse(identifikator: Identifikator, kommunenummer: Kommunenummer): KommuneResponse = KommuneResponse(
    id = identifikator.toObjektIdentifikator(),
    administrativenhetnavn = getAdministrativenhetNavn(identifikator.gyldighetsdato)
        .map { AdministrativEnhetNavn(it.navn!!, it.spraak?.id!!, it.version, it.rekkefoelge) },
    kommunenummer = kommunenummer,
    samiskforvaltningsomraade = samiskforvaltningsomraade!!,
    oppdateringsdato = auditInfo.endretdato,
    features = FeatureCollection(features = listOf(posisjon.posisjonToFeature(version), flateToFeature(version))),
    representasjonspunkt = posisjon.posisjonToFeature(version),
    version = version
)

fun Stemmekrets.toStemmekretsResponse(identifikator: Identifikator): StemmekretsResponse {
    val kommune = getKommune(gyldighet.gyldigFra) ?: throw RuntimeException("Fant ikke kommune for stemmekrets")
    return StemmekretsResponse(
        id = identifikator.toObjektIdentifikator(),
        stemmekretsnavn = stemmekretsnavn!!,
        stemmekretsnummer = stemmekretsnummer!!,
        navn = stemmekretsnavn!!,
        nummer = stemmekretsnummer!!,
        kommunenummer = kommune.let { Kommunenummer(it.kommunenummerUuid, it.kommunenummer) },
        kommuneIdentifikator = ObjektIdentifikator(Lokalid(kommune.lokalid), kommune.gyldighet.gyldigFra),
        valgdistriktsnummer = valgdistriktsnummer,
        tellekretsnavn = tellekretsnavn,
        gyldighet = gyldighet.toGyldighetResponse(),
        endringstype = auditInfo.typeEndring.value,
        oppdateringsdato = auditInfo.endretdato,
        tellekretsnummer = tellekretsnummer,
        features = FeatureCollection(features = listOf(
            posisjon.posisjonToFeature(version),
        )),
        representasjonspunkt = posisjon.posisjonToFeature(version),
        datafangstdato = datafangstdato,
        version = version
    )
}
