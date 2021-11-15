package no.kartverket.nibas.nibasserverapi.repository

import no.kartverket.nibas.nibasserverapi.domain.Fylke
import no.kartverket.nibas.nibasserverapi.domain.Kommune
import org.springframework.data.repository.CrudRepository

interface FylkeRepository : CrudRepository<Fylke, Int> {
}