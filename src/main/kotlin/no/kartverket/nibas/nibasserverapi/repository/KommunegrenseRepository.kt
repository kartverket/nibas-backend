package no.kartverket.nibas.nibasserverapi.repository

import no.kartverket.nibas.nibasserverapi.domain.Kommunegrense
import org.springframework.data.repository.CrudRepository

interface KommunegrenseRepository : CrudRepository<Kommunegrense, Int> {
}