package no.kartverket.nibas.repository

import no.kartverket.nibas.domain.Fylke
import org.springframework.data.repository.CrudRepository

interface FylkeRepository : CrudRepository<Fylke, Int> {
}