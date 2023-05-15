package no.kartverket.nibas.extensions.flate

import no.kartverket.nibas.api.v1.common.AdministrativEnhetNavn
import no.kartverket.nibas.api.v1.common.Fylkesnummer
import no.kartverket.nibas.api.v1.common.geojson.FeatureCollection
import no.kartverket.nibas.api.v1.response.FylkeRef
import no.kartverket.nibas.api.v1.response.FylkeResponse
import no.kartverket.nibas.api.v1.response.KommuneResponse
import no.kartverket.nibas.api.v1.response.Kommunenummer
import no.kartverket.nibas.api.v1.response.KretsResponse
import no.kartverket.nibas.api.v1.response.StemmekretsResponse
import no.kartverket.nibas.api.v1.response.toGyldighetResponse
import no.kartverket.nibas.api.v1.response.toObjektIdentifikator
import no.kartverket.nibas.common.HrefProvider
import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.domain.Fylke
import no.kartverket.nibas.domain.Grunnkrets
import no.kartverket.nibas.domain.Kommune
import no.kartverket.nibas.domain.Stemmekrets
import no.kartverket.nibas.kodelister.KodelisteService
import no.kartverket.nibas.kodelister.createKommunenummerFromUUID
import no.kartverket.nibas.extensions.geometry.flateToFeature
import no.kartverket.nibas.extensions.geometry.posisjonToFeature

fun Grunnkrets.toKretsResponse(kodelisteService: KodelisteService): KretsResponse = KretsResponse(
    id = lokalid,
    navn = grunnkretsnavn!!,
    nummer = grunnkretsnummer!!,
    kommunenummer = kodelisteService.createKommunenummerFromUUID(kommune.kommunenummer),
    features = FeatureCollection(
        features = listOf(
            posisjon.posisjonToFeature(version),
            flateToFeature(version)
        )
    )
)

fun Fylke.toFylkeRef(identifikator: Identifikator, antallFramtidigeVersjoner: Int = 0, hrefProvider: HrefProvider): FylkeRef = FylkeRef(
    id = identifikator.toObjektIdentifikator(),
    navn = administrativenhetnavn.map { AdministrativEnhetNavn(it.navn!!, it.spraak?.id!!, it.version, it.rekkefoelge) },
    href = hrefProvider.getHref(identifikator),
    antallFramtidigeVersjoner = antallFramtidigeVersjoner
)


fun Fylke.toFylkeResponse(identifikator: Identifikator, kodelisteService: KodelisteService): FylkeResponse = FylkeResponse(
    id = identifikator.toObjektIdentifikator(),
    administrativenhetnavn = administrativenhetnavn.map { AdministrativEnhetNavn(it.navn!!, it.spraak?.id!!, it.version, it.rekkefoelge) },
    fylkesnummer = Fylkesnummer(
        id = fylkesnummer,
        kodeverdi = kodelisteService.getCodeValueByFylkesnummerId(fylkesnummer)?.toInt()!!
    ),
    samiskforvaltningsomraade = samiskforvaltningsomraade!!,
    oppdateringsdato = auditInfo.endretdato,
    features = FeatureCollection(features = listOf((posisjon.posisjonToFeature(version)), flateToFeature(version))),
    version = version
)

fun Kommune.toKommuneResponse(identifikator: Identifikator, kommunenummer: Kommunenummer): KommuneResponse = KommuneResponse(
    id = identifikator.toObjektIdentifikator(),
    administrativenhetnavn = administrativenhetnavn.map { AdministrativEnhetNavn(it.navn!!, it.spraak?.id!!, it.version, it.rekkefoelge) },
    kommunenummer = kommunenummer,
    samiskforvaltningsomraade = samiskforvaltningsomraade!!,
    oppdateringsdato = auditInfo.endretdato,
    features = FeatureCollection(features = listOf(posisjon.posisjonToFeature(version), flateToFeature(version))),
    version = version
)

fun Stemmekrets.toKretsResponse(kodelisteService: KodelisteService): KretsResponse = KretsResponse(
    id = lokalid,
    navn = stemmekretsnavn!!,
    nummer = stemmekretsnummer!!,
    kommunenummer = kodelisteService.createKommunenummerFromUUID(kommune.kommunenummer),
    features = FeatureCollection(
        features = listOf(
            posisjon.posisjonToFeature(version),
            flateToFeature(version)
        )
    )
)

fun Stemmekrets.toStemmekretsResponse(identifikator: Identifikator, kodelisteService: KodelisteService): StemmekretsResponse = StemmekretsResponse(
    id = identifikator.toObjektIdentifikator(),
    stemmekretsnavn = stemmekretsnavn!!,
    stemmekretsnummer = stemmekretsnummer!!,
    kommunenummer = kodelisteService.createKommunenummerFromUUID(kommune.kommunenummer),
    valgdistriktsnummer = valgdistriktsnummer,
    tellekretsnavn = tellekretsnavn,
    gyldighet = gyldighet.toGyldighetResponse(),
    endringstype = auditInfo.typeEndring.value,
    oppdateringsdato = auditInfo.endretdato,
    tellekretsnummer = tellekretsnummer,
    features = FeatureCollection(features = listOf(
        posisjon.posisjonToFeature(version),
        flateToFeature(version)
    )),
    version = version
)
