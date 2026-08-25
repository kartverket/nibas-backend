package no.kartverket.nibas.api.v1.request

import no.kartverket.nibas.api.v1.common.geojson.metadata.Identifikasjon
import no.kartverket.nibas.domain.GjeldendeMaterieltVilkaar
import no.kartverket.nibas.domain.MaterieltVilkaar

interface BopliktomraadeRequestFields {
    val identifikasjon: Identifikasjon
    val navn: String
    val nummer: String
    val kommuneIdentifikasjon: Identifikasjon
    val version: Int
    val informasjon: String?
    val delvisBoplikt: Boolean?
    val gjelderKunDelAvKommunen: Boolean
    val usikkerAvgrensning: Boolean?
    val harUsikkerAvgrensning: Boolean
    val materielleVilkaar: Set<MaterieltVilkaar>?
    val gjeldendeMaterielleVilkaar: Set<GjeldendeMaterieltVilkaar>
    val andreAvgrensninger: String?
    val andreLokaleAvgrensninger: String?
    var forskriftsreferanse: String?
    var url: String?
}
