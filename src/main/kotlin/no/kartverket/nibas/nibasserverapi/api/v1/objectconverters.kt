package no.kartverket.nibas.nibasserverapi.api.v1

import no.kartverket.nibas.nibasserverapi.api.v1.request.Kommunegrense as KommunegrenseRequest
import no.kartverket.nibas.nibasserverapi.api.v1.response.Kommunegrense as KommunegrenseResponse
import no.kartverket.nibas.nibasserverapi.api.v1.request.Kommune as KommuneRequest
import no.kartverket.nibas.nibasserverapi.api.v1.response.Kommune as KommuneResponse
import no.kartverket.nibas.nibasserverapi.domain.Kommunegrense as DomainKommunegrense
import no.kartverket.nibas.nibasserverapi.domain.Kommune as DomainKommune

fun toKommunegrenseResponse(kommunegrense: DomainKommunegrense): KommunegrenseResponse = KommunegrenseResponse(kommunegrense.id, kommunegrense.navn, kommunegrense.grense)
fun fromKommunegrenseRequest(kommunegrense: KommunegrenseRequest): DomainKommunegrense = DomainKommunegrense(kommunegrense.id, kommunegrense.navn, kommunegrense.grense)

fun toKommuneResponse(kommune: DomainKommune): KommuneResponse = KommuneResponse(
    kommune.id,
    kommune.navn,
    kommune.kommunenummer,
    kommune.grenser.map { kommunegrense -> toKommunegrenseResponse(kommunegrense) })

fun fromKommuneRequest(kommuneId: Int, kommune: KommuneRequest): DomainKommune = DomainKommune(
    kommuneId,
    kommune.navn,
    kommune.kommunenummer,
    kommune.grenser.map { kommunegrense -> fromKommunegrenseRequest(kommunegrense)})