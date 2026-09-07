package no.kartverket.nibas.api.v1.request

import no.kartverket.nibas.api.v1.common.geojson.metadata.Identifikasjon
import no.kartverket.nibas.domain.GjeldendeMaterieltVilkaar

interface BopliktomraadeRequestFields {
    val identifikasjon: Identifikasjon
    val navn: String
    val nummer: String
    val kommuneIdentifikasjon: Identifikasjon
    val version: Int
    val gjelderKunDelAvKommunen: Boolean
    val harUsikkerAvgrensning: Boolean
    val gjeldendeMaterielleVilkaar: Set<GjeldendeMaterieltVilkaar>
    val andreLokaleAvgrensninger: String?
    var forskriftsreferanse: String?
    var url: String?
}
