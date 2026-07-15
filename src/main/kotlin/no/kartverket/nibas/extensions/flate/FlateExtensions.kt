package no.kartverket.nibas.extensions.flate

import no.kartverket.nibas.api.v1.common.AdministrativEnhetNavn
import no.kartverket.nibas.api.v1.common.Kommunenummer
import no.kartverket.nibas.api.v1.common.ObjektIdentifikator
import no.kartverket.nibas.api.v1.klient.response.BopliktomraadeResponse
import no.kartverket.nibas.api.v1.klient.response.FylkeResponse
import no.kartverket.nibas.api.v1.klient.response.GrunnkretsResponse
import no.kartverket.nibas.api.v1.klient.response.KommuneResponse
import no.kartverket.nibas.api.v1.klient.response.NasjonResponse
import no.kartverket.nibas.api.v1.klient.response.StemmekretsResponse
import no.kartverket.nibas.api.v1.klient.response.toGyldighetResponse
import no.kartverket.nibas.api.v1.klient.response.toObjektIdentifikator
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.common.createIdentifikator
import no.kartverket.nibas.domain.Bopliktomraade
import no.kartverket.nibas.domain.Fylke
import no.kartverket.nibas.domain.Grunnkrets
import no.kartverket.nibas.domain.Kommune
import no.kartverket.nibas.domain.Nasjon
import no.kartverket.nibas.domain.Stemmekrets
import no.kartverket.nibas.extensions.geometry.multiPolygonToDto
import no.kartverket.nibas.extensions.geometry.posisjonToFeature
import java.time.LocalDate

fun Grunnkrets.toGrunnkretsResponse(): GrunnkretsResponse {
    val kommune = this.getKommune(gyldighet.gyldigFra) ?: throw RuntimeException("Fant ikke kommune for grunnkrets")
    return GrunnkretsResponse(
        id = ObjektIdentifikator(Lokalid(lokalid), gyldighet.gyldigFra),
        navn = grunnkretsnavn!!,
        nummer = grunnkretsnummer!!,
        gyldighet = gyldighet.toGyldighetResponse(),
        datafangstdato = datafangstdato,
        kommunenummer = kommune.let { Kommunenummer(it.kommunenummerUuid, it.kommunenummer) },
        kommuneIdentifikator = ObjektIdentifikator(Lokalid(kommune.lokalid), kommune.gyldighet.gyldigFra),
        endringstype = auditInfo.typeEndring.value,
        oppdateringsdato = auditInfo.endretdato,
        representasjonspunkt = posisjon.posisjonToFeature(version),
        version = version,
        informasjon = informasjon
    )
}

fun Fylke.toFylkeResponse(gyldighetsdato: LocalDate): FylkeResponse {
    val identifikator = createIdentifikator(lokalid, gyldighet.gyldigFra)
    return FylkeResponse(
        id = identifikator.toObjektIdentifikator(),
        navn = getAdministrativenhetNavn(gyldighetsdato)
            .map { AdministrativEnhetNavn(it.navn!!, it.spraak?.id!!, it.version, it.rekkefoelge) },
        nummer = fylkesnummer,
        samiskforvaltningsomraade = samiskforvaltningsomraade!!,
        oppdateringsdato = auditInfo.endretdato,
        representasjonspunkt = posisjon.posisjonToFeature(version),
        version = version,
        gyldighet = gyldighet.toGyldighetResponse()
    )
}

fun Kommune.toKommuneResponse(gyldighetsdato: LocalDate): KommuneResponse {
    val identifikator = createIdentifikator(lokalid, gyldighet.gyldigFra)
    return KommuneResponse(
        id = identifikator.toObjektIdentifikator(),
        navn = getAdministrativenhetNavn(gyldighetsdato)
            .map { AdministrativEnhetNavn(it.navn!!, it.spraak?.id!!, it.version, it.rekkefoelge) },
        nummer = this.kommunenummer,
        samiskforvaltningsomraade = samiskforvaltningsomraade!!,
        oppdateringsdato = auditInfo.endretdato,
        representasjonspunkt = posisjon.posisjonToFeature(version),
        fylkeId = fylkeLokalid.lokalid,
        version = version,
        gyldighet = gyldighet.toGyldighetResponse()
    )
}

fun Stemmekrets.toStemmekretsResponse(): StemmekretsResponse {
    val kommune = getKommune(gyldighet.gyldigFra) ?: throw RuntimeException("Fant ikke kommune for stemmekrets")
    return StemmekretsResponse(
        id = ObjektIdentifikator(Lokalid(lokalid), gyldighet.gyldigFra),
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
        representasjonspunkt = posisjon.posisjonToFeature(version),
        datafangstdato = datafangstdato,
        version = version,
        informasjon = informasjon
    )
}

fun Nasjon.toNasjonResponse(): NasjonResponse {
    val identifikator = createIdentifikator(lokalid, gyldighet.gyldigFra)
    return NasjonResponse(
        id = ObjektIdentifikator(Lokalid(lokalid), gyldighet.gyldigFra),
        navn = getAdministrativenhetNavn(identifikator.gyldighetsdato)
            .map { AdministrativEnhetNavn(it.navn!!, it.spraak?.id!!, it.version, it.rekkefoelge) },
        omraade = omraade?.multiPolygonToDto(),
        representasjonspunkt = posisjon.posisjonToFeature(version),
        oppdateringsdato = auditInfo.endretdato,
        version = version,
        gyldighet = gyldighet.toGyldighetResponse()
    )
}

fun Bopliktomraade.toBopliktomraadeResponse(): BopliktomraadeResponse {
    val kommune = this.getKommune() ?: throw RuntimeException("Fant ikke kommune for bopliktområde")

    return BopliktomraadeResponse(
        id = ObjektIdentifikator(Lokalid(lokalid), gyldighet.gyldigFra),
        navn = navn,
        nummer = nummer,
        gyldighet = gyldighet.toGyldighetResponse(),
        datafangstdato = datafangstdato,
        kommunenummer = Kommunenummer(kommune.kommunenummerUuid, kommune.kommunenummer),
        kommuneIdentifikator = ObjektIdentifikator(Lokalid(kommune.lokalid), kommune.gyldighet.gyldigFra),
        endringstype = auditInfo.typeEndring.value,
        oppdateringsdato = auditInfo.endretdato,
        representasjonspunkt = posisjon.posisjonToFeature(version),
        version = version,
        informasjon = informasjon,
        delvisBoplikt = delvisBoplikt,
        usikkerAvgrensning = usikkerAvgrensning,
        harUsikkerAvgrensning = harUsikkerAvgrensning,
        forskriftsreferanse = forskriftsreferanse,
        materielleVilkaar = materielleVilkaar,
        gjeldendeMaterielleVilkaar = gjeldendeMaterielleVilkaar,
        andreAvgrensninger = andreAvgrensninger,
        andreLokaleAvgrensninger = andreLokaleAvgrensninger,
        url = url,
    )
}

