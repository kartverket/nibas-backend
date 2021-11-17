package no.kartverket.nibas.repository

import no.kartverket.nibas.domain.Kommunegrense
import org.springframework.data.repository.CrudRepository

interface KommunegrenseRepository : CrudRepository<Kommunegrense, Int>