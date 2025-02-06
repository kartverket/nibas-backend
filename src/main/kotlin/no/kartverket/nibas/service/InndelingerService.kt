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
        var kommuner = emptyList<InndelingSearchRepository.Inndeling>()
        var fylker = emptyList<InndelingSearchRepository.Inndeling>()

        if(filter.contains(Flatetype.GRUNNKRETS)){
            grunnkretser = inndelingSearchRepository.finnGrunnkretser(gyldighetsdato, searchString, limit)
        }
        if(filter.contains(Flatetype.STEMMEKRETS)){
            stemmekretser = inndelingSearchRepository.finnStemmekretser(gyldighetsdato, searchString, limit - grunnkretser.size)
        }
        if(filter.contains(Flatetype.KOMMUNE)){
            kommuner = inndelingSearchRepository.finnKommuner(gyldighetsdato, searchString, limit - (grunnkretser.size - stemmekretser.size))
        }
        if(filter.contains(Flatetype.FYLKE)){
            fylker = inndelingSearchRepository.finnFylker(gyldighetsdato, searchString, limit - (grunnkretser.size - stemmekretser.size - kommuner.size))
        }

        return grunnkretser + stemmekretser + kommuner + fylker
    }
}

