package no.kartverket.nibas.repository

import no.kartverket.nibas.domain.Fylkesgrense
import org.springframework.data.repository.CrudRepository

interface FylkesgrenseRepository : CrudRepository<Fylkesgrense, Int>