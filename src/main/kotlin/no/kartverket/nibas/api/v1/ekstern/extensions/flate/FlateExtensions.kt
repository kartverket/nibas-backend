package no.kartverket.nibas.api.v1.ekstern.extensions.flate

import no.kartverket.nibas.api.v1.common.AdministrativEnhetNavn
import no.kartverket.nibas.api.v1.common.Fylkesnummer
import no.kartverket.nibas.api.v1.common.Kommunenummer
import no.kartverket.nibas.api.v1.common.ObjektIdentifikator
import no.kartverket.nibas.api.v1.common.geojson.FeatureCollection
import no.kartverket.nibas.api.v1.ekstern.response.EksternBopliktomraadeRespons
import no.kartverket.nibas.api.v1.ekstern.response.EksternFylkeRef
import no.kartverket.nibas.api.v1.ekstern.response.EksternFylkeResponse
import no.kartverket.nibas.api.v1.ekstern.response.EksternGrunnkretsResponse
import no.kartverket.nibas.api.v1.ekstern.response.EksternKommuneRef
import no.kartverket.nibas.api.v1.ekstern.response.EksternKommuneResponse
import no.kartverket.nibas.api.v1.ekstern.response.EksternKretsResponse
import no.kartverket.nibas.api.v1.ekstern.response.EksternStemmekretsResponse
import no.kartverket.nibas.api.v1.klient.response.toGyldighetResponse
import no.kartverket.nibas.api.v1.klient.response.toObjektIdentifikator
import no.kartverket.nibas.common.HrefProvider
import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.domain.Bopliktomraade
import no.kartverket.nibas.domain.Fylke
import no.kartverket.nibas.domain.Grunnkrets
import no.kartverket.nibas.domain.Kommune
import no.kartverket.nibas.domain.Stemmekrets
import no.kartverket.nibas.extensions.geometry.flateToFeature
import no.kartverket.nibas.extensions.geometry.multiPolygonToDto
import no.kartverket.nibas.api.v1.common.geojson.MultiPolygon as MultiPolygonDto
import no.kartverket.nibas.extensions.geometry.posisjonToFeature
import no.kartverket.nibas.service.FylkeService
import no.kartverket.nibas.service.KommuneService
import java.time.LocalDate

fun Grunnkrets.toEksternKretsResponse(): EksternKretsResponse = EksternKretsResponse(
    id = lokalid,
    navn = grunnkretsnavn!!,
    nummer = grunnkretsnummer!!,
    kommunenummer = this.getKommune(gyldighet.gyldigFra)?.let { Kommunenummer(it.kommunenummerUuid, it.kommunenummer) }
        ?: Kommunenummer(id = "Ukjent", kodeverdi = "Ukjent"),
    features = FeatureCollection(
        features = listOf(
            posisjon.posisjonToFeature(version),
            flateToFeature(version)
        )
    )
)

fun Grunnkrets.toEksternGrunnkretsResponse(identifikator: Identifikator): EksternGrunnkretsResponse = EksternGrunnkretsResponse(
    id = identifikator.toObjektIdentifikator(),
    navn = grunnkretsnavn!!,
    grunnkretsnummer = grunnkretsnummer!!,
    gyldighet = gyldighet.toGyldighetResponse(),
    datafangstdato = datafangstdato,
    kommunenummer = this.getKommune(gyldighet.gyldigFra)?.let { Kommunenummer(it.kommunenummerUuid, it.kommunenummer) }
        ?: Kommunenummer(id = "Ukjent", kodeverdi = "Ukjent"),
    endringstype = auditInfo.typeEndring.value,
    oppdateringsdato = auditInfo.endretdato,
    features = FeatureCollection(features = listOf(
        posisjon.posisjonToFeature(version),
        flateToFeature(version)
    )),
    version = version
)

fun Fylke.toEksternFylkeRef(identifikator: Identifikator, fylkeService: FylkeService, hrefProvider: HrefProvider): EksternFylkeRef {
    val antallFramtidigeVersjoner = if (this.gyldighet.gyldigTil == null) {
        0
    } else {
        fylkeService.hentAntallGyldigeVersjonerFramITidFor(identifikator)
    }

    return EksternFylkeRef(
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

fun Fylke.toEksternFylkeResponse(identifikator: Identifikator): EksternFylkeResponse = EksternFylkeResponse(
    id = identifikator.toObjektIdentifikator(),
    administrativenhetnavn = getAdministrativenhetNavn(identifikator.gyldighetsdato)
        .map { AdministrativEnhetNavn(it.navn!!, it.spraak?.id!!, it.version, it.rekkefoelge) },
    fylkesnummer = Fylkesnummer(
        id = fylkesnummerUuid,
        kodeverdi = fylkesnummer
    ),
    samiskforvaltningsomraade = samiskforvaltningsomraade!!,
    oppdateringsdato = auditInfo.endretdato,
    features = FeatureCollection(features = listOf((posisjon.posisjonToFeature(version)), flateToFeature(version))),
    version = version
)

fun Kommune.toEksternKommuneRef(datoToUse: LocalDate, kommuneService: KommuneService, hrefProvider: HrefProvider): EksternKommuneRef {
    val identifikator = Identifikator(Lokalid(lokalid), datoToUse)
    val antallFramtidigeVersjoner = kommuneService.hentAntallGyldigeVersjonerFramITidFor(identifikator)

    return EksternKommuneRef(
        id = identifikator.toObjektIdentifikator(),
        navn = getAdministrativenhetNavn(identifikator.gyldighetsdato)
            .map { AdministrativEnhetNavn(it.navn!!, it.spraak?.id!!, it.version, it.rekkefoelge) },
        href = hrefProvider.getHref(identifikator),
        kommunenummer = Kommunenummer(kommunenummerUuid, kommunenummer),
        antallFramtidigeVersjoner = antallFramtidigeVersjoner
    )
}

fun Kommune.toEksternKommuneResponse(identifikator: Identifikator, kommunenummer: Kommunenummer): EksternKommuneResponse = EksternKommuneResponse(
    id = identifikator.toObjektIdentifikator(),
    administrativenhetnavn = getAdministrativenhetNavn(identifikator.gyldighetsdato)
        .map { AdministrativEnhetNavn(it.navn!!, it.spraak?.id!!, it.version, it.rekkefoelge) },
    kommunenummer = kommunenummer,
    samiskforvaltningsomraade = samiskforvaltningsomraade!!,
    oppdateringsdato = auditInfo.endretdato,
    features = FeatureCollection(features = listOf(posisjon.posisjonToFeature(version), flateToFeature(version))),
    version = version
)

fun Stemmekrets.toEksternKretsResponse(): EksternKretsResponse = EksternKretsResponse(
    id = lokalid,
    navn = stemmekretsnavn!!,
    nummer = stemmekretsnummer!!,
    kommunenummer = this.getKommune(gyldighet.gyldigFra)?.let { Kommunenummer(id = it.kommunenummerUuid, kodeverdi = it.kommunenummer) }
        ?: Kommunenummer(id = "Ukjent", kodeverdi = "Ukjent"),
    features = FeatureCollection(
        features = listOf(
            posisjon.posisjonToFeature(version),
            flateToFeature(version)
        )
    )
)

fun Stemmekrets.toEksternStemmekretsResponse(identifikator: Identifikator): EksternStemmekretsResponse = EksternStemmekretsResponse(
    id = identifikator.toObjektIdentifikator(),
    stemmekretsnavn = stemmekretsnavn!!,
    stemmekretsnummer = stemmekretsnummer!!,
    kommunenummer = this.getKommune(gyldighet.gyldigFra)?.let { Kommunenummer(it.kommunenummerUuid, it.kommunenummer) }
        ?: Kommunenummer(id = "Ukjent", kodeverdi = "Ukjent"),
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
    datafangstdato = datafangstdato,
    version = version
)

fun Bopliktomraade.toEksternBopliktomraadeResponse(gyldighetsdato: LocalDate): EksternBopliktomraadeRespons {
    val kommune = this.getKommune(gyldighetsdato) ?: throw RuntimeException("Fant ikke kommune for bopliktområde")

    return EksternBopliktomraadeRespons(
        id = ObjektIdentifikator(Lokalid(lokalid), gyldighet.gyldigFra),
        gyldighet = gyldighet.toGyldighetResponse(),
        endringstype = auditInfo.typeEndring.value,
        oppdateringsdato = auditInfo.endretdato,
        datafangstdato = datafangstdato,
        kommunenummer = Kommunenummer(kommune.kommunenummerUuid, kommune.kommunenummer),
        delvisBoplikt = delvisBoplikt,
        informasjon = informasjon,
        forskriftsreferanse = forskriftsreferanse,
        materielleVilkaar = materielleVilkaar,
        andreAvgrensninger = andreAvgrensninger,
        usikkerAvgrensning = usikkerAvgrensning,
        url = url,
        // Hvis bopliktområdet har boplikt i hele kommunen brukes kommunens område.
        features = FeatureCollection(features = listOf(
            posisjon.posisjonToFeature(version),
            flateToFeature(version).apply {
                if (!delvisBoplikt) { geometry = kommune.omraade?.multiPolygonToDto() ?: MultiPolygonDto(coordinates = emptyList()) }
            }
        )),
        version = version
    )
}
