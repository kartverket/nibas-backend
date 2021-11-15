package no.kartverket.nibas.nibasserverapi.repository

import no.kartverket.nibas.nibasserverapi.domain.Fylkesgrense
import org.springframework.data.repository.CrudRepository

interface FylkesgrenseRepository : CrudRepository<Fylkesgrense, Int>