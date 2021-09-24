package no.kartverket.nibas.nibasserverapi.api.v1

import no.kartverket.nibas.nibasserverapi.api.v1.Kommune as ApiKommune
import no.kartverket.nibas.nibasserverapi.domain.Kommune as DomainKommune

fun toApiKommune(kommune: DomainKommune): ApiKommune = ApiKommune(kommune.id, kommune.name, kommune.omrade)
fun fromApiKommune(kommune: ApiKommune): DomainKommune = DomainKommune(kommune.id, kommune.name, kommune.omrade)