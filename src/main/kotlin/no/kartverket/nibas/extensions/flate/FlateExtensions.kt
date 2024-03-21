package no.kartverket.nibas.extensions.flate

import no.kartverket.nibas.api.v1.common.AdministrativEnhetNavn
import no.kartverket.nibas.api.v1.common.Fylkesnummer
import no.kartverket.nibas.api.v1.common.Kommunenummer
import no.kartverket.nibas.api.v1.common.ObjektIdentifikator
import no.kartverket.nibas.api.v1.klient.response.FylkeResponse
import no.kartverket.nibas.api.v1.klient.response.GrunnkretsResponse
import no.kartverket.nibas.api.v1.klient.response.KommuneResponse
import no.kartverket.nibas.api.v1.klient.response.StemmekretsResponse
import no.kartverket.nibas.api.v1.klient.response.toGyldighetResponse
import no.kartverket.nibas.api.v1.klient.response.toObjektIdentifikator
import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.domain.Fylke
import no.kartverket.nibas.domain.Grunnkrets
import no.kartverket.nibas.domain.Kommune
import no.kartverket.nibas.domain.Stemmekrets
import no.kartverket.nibas.extensions.geometry.posisjonToFeature

fun Grunnkrets.toGrunnkretsResponse(identifikator: Identifikator): GrunnkretsResponse {
    val kommune = this.getKommune(gyldighet.gyldigFra) ?: throw RuntimeException("Fant ikke kommune for grunnkrets")
    return GrunnkretsResponse(
        id = identifikator.toObjektIdentifikator(),
        navn = grunnkretsnavn!!,
        nummer = grunnkretsnummer!!,
        grunnkretsnummer = grunnkretsnummer!!,
        gyldighet = gyldighet.toGyldighetResponse(),
        datafangstdato = datafangstdato,
        kommunenummer = kommune.let { Kommunenummer(it.kommunenummerUuid, it.kommunenummer) },
        kommuneIdentifikator = ObjektIdentifikator(Lokalid(kommune.lokalid), kommune.gyldighet.gyldigFra),
        endringstype = auditInfo.typeEndring.value,
        oppdateringsdato = auditInfo.endretdato,
        representasjonspunkt = posisjon.posisjonToFeature(version),
        version = version,
    )
}

fun Fylke.toFylkeResponse(identifikator: Identifikator): FylkeResponse = FylkeResponse(
    id = identifikator.toObjektIdentifikator(),
    navn = getAdministrativenhetNavn(identifikator.gyldighetsdato)
        .map { AdministrativEnhetNavn(it.navn!!, it.spraak?.id!!, it.version, it.rekkefoelge) },
    fylkesnummer = Fylkesnummer(
        id = fylkesnummerUuid,
        kodeverdi = fylkesnummer
    ),
    nummer = fylkesnummer,
    samiskforvaltningsomraade = samiskforvaltningsomraade!!,
    oppdateringsdato = auditInfo.endretdato,
    representasjonspunkt = posisjon.posisjonToFeature(version),
    version = version
)

fun Kommune.toKommuneResponse(identifikator: Identifikator, kommunenummer: Kommunenummer): KommuneResponse = KommuneResponse(
    id = identifikator.toObjektIdentifikator(),
    navn = getAdministrativenhetNavn(identifikator.gyldighetsdato)
        .map { AdministrativEnhetNavn(it.navn!!, it.spraak?.id!!, it.version, it.rekkefoelge) },
    kommunenummer = kommunenummer,
    nummer = this.kommunenummer,
    samiskforvaltningsomraade = samiskforvaltningsomraade!!,
    oppdateringsdato = auditInfo.endretdato,
    representasjonspunkt = posisjon.posisjonToFeature(version),
    version = version
)

fun Stemmekrets.toStemmekretsResponse(identifikator: Identifikator): StemmekretsResponse {
    val kommune = getKommune(gyldighet.gyldigFra) ?: throw RuntimeException("Fant ikke kommune for stemmekrets")
    return StemmekretsResponse(
        id = identifikator.toObjektIdentifikator(),
        navn = stemmekretsnavn!!,
        nummer = stemmekretsnummer!!,
        stemmekretsnavn = stemmekretsnavn!!,
        stemmekretsnummer = stemmekretsnummer!!,
        kommunenummer = kommune.let { Kommunenummer(it.kommunenummerUuid, it.kommunenummer) },
        kommuneIdentifikator = ObjektIdentifikator(Lokalid(kommune.lokalid), kommune.gyldighet.gyldigFra),
        valgdistriktsnummer = valgdistriktsnummer,
        tellekretsnavn = tellekretsnavn,
        gyldighet = gyldighet.toGyldighetResponse(),
        endringstype = auditInfo.typeEndring.value,
        oppdateringsdato = auditInfo.endretdato,
        tellekretsnummer = tellekretsnummer,
        representasjonspunkt = posisjon.posisjonToFeature(version),
        datafangstdato = datafangstdato,
        version = version
    )
}
