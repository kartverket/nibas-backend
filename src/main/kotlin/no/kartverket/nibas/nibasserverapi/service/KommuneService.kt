package no.kartverket.nibas.nibasserverapi.service

import no.kartverket.nibas.nibasserverapi.domain.Kommune
import no.kartverket.nibas.nibasserverapi.repository.KommuneRepository
import no.kartverket.nibas.nibasserverapi.unwrap
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.*

@Service
class KommuneService {

    @Autowired
    lateinit var kommuneRepository: KommuneRepository;

    fun hentAlleKommuner(): Iterable<Kommune> = kommuneRepository.findAll();
    fun hentMedId(id: Int): Kommune? = kommuneRepository.findById(id).unwrap()
}