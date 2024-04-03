package no.kartverket.nibas.service

import no.kartverket.nibas.repository.InndelingSearchRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class InndelingerService(
    val inndelingSearchRepository: InndelingSearchRepository
) {

    @Transactional(readOnly = true)
    fun finnInndelinger(searchString: String, gyldighetsdato: LocalDate, limit: Int): List<InndelingSearchRepository.Inndeling> {
        val grunnkretser = inndelingSearchRepository.finnGrunnkretser(gyldighetsdato, searchString, limit)
        val stemmekretser = inndelingSearchRepository.finnStemmekretser(gyldighetsdato, searchString, limit - grunnkretser.size)
        val kommuner = inndelingSearchRepository.finnKommuner(gyldighetsdato, searchString, limit - (grunnkretser.size - stemmekretser.size))
        val fylker = inndelingSearchRepository.finnFylker(gyldighetsdato, searchString, limit - (grunnkretser.size - stemmekretser.size - kommuner.size))

        return grunnkretser + stemmekretser + kommuner + fylker
    }
}

