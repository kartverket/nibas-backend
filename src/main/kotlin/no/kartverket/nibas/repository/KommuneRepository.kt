package no.kartverket.nibas.repository

import no.kartverket.nibas.domain.Kommune
import org.springframework.data.repository.CrudRepository

interface KommuneRepository : CrudRepository<Kommune, Int> {
}