package no.kartverket.nibas.nibasserverapi.service

import no.kartverket.nibas.nibasserverapi.domain.Kommune
import no.kartverket.nibas.nibasserverapi.repository.KommuneRepository
import no.kartverket.nibas.nibasserverapi.repository.KommunegrenseRepository
import no.kartverket.nibas.nibasserverapi.unwrap
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class KommuneService {

    @Autowired
    lateinit var kommuneRepository: KommuneRepository

    @Autowired
    lateinit var kommunegrenseRepository: KommunegrenseRepository

    fun hentAlleKommuner(): Iterable<Kommune> = kommuneRepository.findAll()
    fun hentMedId(id: Int): Kommune? = kommuneRepository.findById(id).unwrap()

    fun oppdaterKommune(oppdatertKommune: Kommune) {
        kommuneRepository.save(oppdatertKommune)
        oppdatertKommune.grenser.forEach { kommunegrense -> kommunegrenseRepository.save(kommunegrense) }
    }
}