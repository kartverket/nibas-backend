package no.kartverket.nibas.service

import no.kartverket.nibas.api.v1.common.Flatetype
import no.kartverket.nibas.repository.InndelingSearchRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class InndelingerService(
    val inndelingSearchRepository: InndelingSearchRepository
) {

    @Transactional(readOnly = true)
    fun finnInndelinger(searchString: String, gyldighetsdato: LocalDate, limit: Int, filter: List<Flatetype>): List<InndelingSearchRepository.Inndeling> {
        var grunnkretser = emptyList<InndelingSearchRepository.Inndeling>()
        var stemmekretser = emptyList<InndelingSearchRepository.Inndeling>()
        var bopliktomraader = emptyList<InndelingSearchRepository.Inndeling>()
        var kommuner = emptyList<InndelingSearchRepository.Inndeling>()
        var fylker = emptyList<InndelingSearchRepository.Inndeling>()

        var internLimit = limit

        if (filter.contains(Flatetype.GRUNNKRETS)) {
            grunnkretser = inndelingSearchRepository.finnGrunnkretser(gyldighetsdato, searchString, internLimit)
            internLimit -= grunnkretser.size
        }
        if (filter.contains(Flatetype.STEMMEKRETS)) {
            stemmekretser = inndelingSearchRepository.finnStemmekretser(gyldighetsdato, searchString, internLimit)
            internLimit -= stemmekretser.size
        }
        if (filter.contains(Flatetype.BOPLIKTOMRAADE)) {
            bopliktomraader = inndelingSearchRepository.finnBopliktomraader(gyldighetsdato, searchString, internLimit)
            internLimit -= bopliktomraader.size
        }
        if (filter.contains(Flatetype.KOMMUNE)) {
            kommuner = inndelingSearchRepository.finnKommuner(gyldighetsdato, searchString, internLimit)
            internLimit -= kommuner.size
        }
        if (filter.contains(Flatetype.FYLKE)) {
            fylker = inndelingSearchRepository.finnFylker(gyldighetsdato, searchString, internLimit)
        }

        return grunnkretser + stemmekretser + bopliktomraader + kommuner + fylker
    }
}

